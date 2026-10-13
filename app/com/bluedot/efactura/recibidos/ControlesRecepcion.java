package com.bluedot.efactura.recibidos;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.math.BigInteger;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Iterator;

import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.datatype.XMLGregorianCalendar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import com.bluedot.commons.error.APIException;
import com.bluedot.efactura.model.CFE;
import com.bluedot.efactura.model.Empresa;

import dgi.classes.respuestas.cfe.EstadoACKCFEType;

import dgi.classes.recepcion.CAEDataType;
import dgi.classes.recepcion.CFEDefType;
import play.Play;

/**
 * Controles de DGI sobre los CFE recibidos (E01-E07). Cada control tiene un modo en application.conf
 * (RecepcionCFE.control.EXX):
 * <ul>
 * <li>OFF: no se controla</li>
 * <li>LOG (por defecto): si no pasa se registra en el CFE (observacionesRecepcion) y en el log, pero se acepta igual</li>
 * <li>ENFORCE: si no pasa se rechaza el CFE con ese motivo (el ACK al emisor lo informa)</li>
 * </ul>
 * Se arranca en LOG porque un rechazo le envia una respuesta negativa al proveedor: primero se observa que rechazaria.
 */
public class ControlesRecepcion {

	public enum Modo {
		OFF, LOG, ENFORCE
	}

	public static Modo modo(String control) {
		String valor = Play.application().configuration().getString("RecepcionCFE.control." + control, Modo.LOG.name());
		try {
			return Modo.valueOf(valor.trim().toUpperCase());
		} catch (IllegalArgumentException e) {
			return Modo.LOG;
		}
	}

	/** CAEData del CFE, sea cual sea el tipo; null si no tiene */
	static CAEDataType caeData(CFEDefType xml) {
		if (xml == null)
			return null;
		if (xml.getEFact() != null)
			return xml.getEFact().getCAEData();
		if (xml.getETck() != null)
			return xml.getETck().getCAEData();
		if (xml.getERem() != null)
			return xml.getERem().getCAEData();
		if (xml.getEResg() != null)
			return xml.getEResg().getCAEData();
		if (xml.getEFactExp() != null)
			return xml.getEFactExp().getCAEData();
		if (xml.getERemExp() != null)
			return xml.getERemExp().getCAEData();
		return null;
	}

	/**
	 * E03: el numero del CFE tiene que estar en el rango del CAE con el que se emitio (CAEData: DNro a HNro).
	 *
	 * @return null si pasa; si no, el detalle de por que no
	 */
	public static String e03(CFEDefType xml, long nro) {
		CAEDataType cae = caeData(xml);
		if (cae == null)
			return "el CFE no trae los datos del CAE (CAEData)";
		BigInteger desde = cae.getDNro();
		BigInteger hasta = cae.getHNro();
		if (desde == null || hasta == null)
			return "el CAE " + cae.getCAEID() + " no indica el rango de números";
		BigInteger numero = BigInteger.valueOf(nro);
		if (numero.compareTo(desde) < 0 || numero.compareTo(hasta) > 0)
			return "el número " + nro + " está fuera del rango del CAE " + cae.getCAEID() + " (" + desde + " a " + hasta + ")";
		return null;
	}

	/**
	 * E02: el CFE (emisor, tipo, serie y numero) ya se habia recibido y aceptado. No cuenta al CFE que se esta procesando
	 * (ya esta guardado cuando se controla).
	 *
	 * @return null si pasa; si no, el detalle con las copias anteriores
	 */
	public static String e02(CFE cfe, Empresa empresaReceptora) throws APIException {
		List<CFE> anteriores = new ArrayList<CFE>();
		for (CFE otro : CFE.findByIdRecibido(empresaReceptora, cfe.getTipo(), cfe.getSerie(), cfe.getNro(), EstadoACKCFEType.AE,
				cfe.getEmpresaEmisora(), false))
			if (otro.getId() != cfe.getId())
				anteriores.add(otro);
		if (anteriores.isEmpty())
			return null;
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
		StringBuilder copias = new StringBuilder();
		for (CFE otro : anteriores) {
			if (copias.length() > 0)
				copias.append(", ");
			copias.append("id ").append(otro.getId());
			if (otro.getSobreRecibido() != null && otro.getSobreRecibido().getFecha() != null)
				copias.append(" (sobre del ").append(sdf.format(otro.getSobreRecibido().getFecha())).append(")");
		}
		return "ya se había recibido y aceptado: " + copias;
	}

	/** Fecha y hora de firma del CFE (TmstFirma), sea cual sea el tipo; null si no tiene */
	static XMLGregorianCalendar tmstFirma(CFEDefType xml) {
		if (xml == null)
			return null;
		if (xml.getEFact() != null)
			return xml.getEFact().getTmstFirma();
		if (xml.getETck() != null)
			return xml.getETck().getTmstFirma();
		if (xml.getERem() != null)
			return xml.getERem().getTmstFirma();
		if (xml.getEResg() != null)
			return xml.getEResg().getTmstFirma();
		if (xml.getEFactExp() != null)
			return xml.getEFactExp().getTmstFirma();
		if (xml.getERemExp() != null)
			return xml.getERemExp().getTmstFirma();
		return null;
	}

	private static LocalDate dia(XMLGregorianCalendar fecha) {
		return LocalDate.of(fecha.getYear(), fecha.getMonth(), fecha.getDay());
	}

	/**
	 * E07: el CFE se tiene que haber firmado con el CAE vigente: la fecha de firma (TmstFirma, el dia en la zona horaria
	 * de la firma) no puede ser posterior al vencimiento del CAE (CAEData FecVenc).
	 *
	 * @return null si pasa; si no, el detalle de por que no
	 */
	public static String e07(CFEDefType xml) {
		CAEDataType cae = caeData(xml);
		if (cae == null || cae.getFecVenc() == null)
			return "el CAE no indica la fecha de vencimiento";
		XMLGregorianCalendar firma = tmstFirma(xml);
		if (firma == null)
			return "el CFE no trae la fecha de firma (TmstFirma)";
		LocalDate diaFirma = dia(firma);
		LocalDate vencimiento = dia(cae.getFecVenc());
		if (diaFirma.isAfter(vencimiento))
			return "firmado el " + diaFirma + ", después del vencimiento del CAE " + cae.getCAEID() + " (" + vencimiento + ")";
		return null;
	}

	private static final String NS_CFE = "http://cfe.dgi.gub.uy";

	/**
	 * E04: la firma electronica del CFE es valida. El emisor firma el CFE como documento propio (Reference URI="" con
	 * transformacion enveloped), asi que se valida sobre una copia del CFE como documento aparte (nunca sobre el sobre: se
	 * parsea de nuevo desde el texto, sin tocar nada). Se usa el certificado de la firma o, si no lo trae, el de la
	 * caratula del sobre (verificando que sea el numero de serie que indica la firma), y se controla que estuviera vigente
	 * en la fecha de firma (TmstFirma).
	 *
	 * @param xmlSobre
	 *            el XML del sobre recibido (como llego)
	 * @param ordinal
	 *            posicion del CFE en el sobre, desde 1
	 * @return null si pasa; si no, el detalle de por que no
	 */
	public static String e04(String xmlSobre, int ordinal, CFEDefType xml) {
		if (xmlSobre == null || xmlSobre.isEmpty())
			return "no está el XML del sobre para validar la firma";
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			javax.xml.parsers.DocumentBuilder builder = dbf.newDocumentBuilder();
			// Sin handler el parser escribe "[Fatal Error] ..." en la consola; el error ya se informa en el detalle
			builder.setErrorHandler(new org.xml.sax.helpers.DefaultHandler() {
				@Override
				public void fatalError(org.xml.sax.SAXParseException e) throws org.xml.sax.SAXException {
					throw e;
				}
			});
			// Algunos emisores mandan el XML con BOM (U+FEFF) al principio: el parser no lo acepta antes de <?xml
			String texto = xmlSobre.charAt(0) == '\uFEFF' ? xmlSobre.substring(1) : xmlSobre;
			Document sobre = builder.parse(new InputSource(new StringReader(texto)));

			NodeList cfes = sobre.getElementsByTagNameNS(NS_CFE, "CFE");
			if (ordinal < 1 || ordinal > cfes.getLength())
				return "no se encontró el CFE " + ordinal + " en el sobre";

			// El CFE como documento propio, como lo firmo el emisor
			Document doc = dbf.newDocumentBuilder().newDocument();
			doc.appendChild(doc.importNode(cfes.item(ordinal - 1), true));

			Element firma = null;
			NodeList firmas = doc.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
			for (int i = 0; i < firmas.getLength() && firma == null; i++)
				if (firmas.item(i).getParentNode() == doc.getDocumentElement())
					firma = (Element) firmas.item(i);
			if (firma == null)
				return "el CFE no está firmado";

			// El certificado viene en la firma o, como en los sobres entre empresas, en la caratula del sobre (la firma
			// del CFE lo identifica por emisor y numero de serie: X509IssuerSerial)
			NodeList certs = firma.getElementsByTagNameNS(XMLSignature.XMLNS, "X509Certificate");
			if (certs.getLength() == 0)
				certs = sobre.getElementsByTagNameNS(NS_CFE, "X509Certificate");
			if (certs.getLength() == 0)
				return "no se encontró el certificado (ni en la firma ni en la carátula del sobre)";
			X509Certificate cert = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(
					new ByteArrayInputStream(Base64.getMimeDecoder().decode(texto(certs.item(0)))));

			NodeList series = firma.getElementsByTagNameNS(XMLSignature.XMLNS, "X509SerialNumber");
			if (series.getLength() > 0 && !texto(series.item(0)).isEmpty()
					&& !new BigInteger(texto(series.item(0))).equals(cert.getSerialNumber()))
				return "la firma indica el certificado número " + texto(series.item(0)) + " pero el del sobre es el " + cert.getSerialNumber();

			DOMValidateContext contexto = new DOMValidateContext(cert.getPublicKey(), firma);
			XMLSignature signature = XMLSignatureFactory.getInstance("DOM").unmarshalXMLSignature(contexto);
			if (!signature.validate(contexto)) {
				StringBuilder motivo = new StringBuilder("la firma no es válida");
				if (!signature.getSignatureValue().validate(contexto))
					motivo.append(": el valor de la firma no corresponde al certificado");
				for (Iterator<?> it = signature.getSignedInfo().getReferences().iterator(); it.hasNext();) {
					Reference ref = (Reference) it.next();
					if (!ref.validate(contexto))
						motivo.append(": el contenido firmado fue modificado (digest de la referencia \"").append(ref.getURI()).append("\")");
				}
				return motivo.toString();
			}

			// El certificado tenia que estar vigente cuando se firmo
			XMLGregorianCalendar tmst = tmstFirma(xml);
			if (tmst != null) {
				java.util.Date fechaFirma = tmst.toGregorianCalendar().getTime();
				if (fechaFirma.before(cert.getNotBefore()) || fechaFirma.after(cert.getNotAfter()))
					return "el certificado no estaba vigente al firmar (vigencia " + dia(cert.getNotBefore()) + " a " + dia(cert.getNotAfter())
							+ ", firmado el " + dia(tmst) + ")";
			}
			return null;
		} catch (Exception e) {
			return "no se pudo validar la firma: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
		}
	}

	private static String texto(Node nodo) {
		return nodo.getTextContent() != null ? nodo.getTextContent().trim() : "";
	}

	private static LocalDate dia(java.util.Date fecha) {
		return fecha.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
	}

	/** Agrega una observacion de control a las del CFE ("E03: ...; E07: ...") */
	public static void observar(CFE cfe, String control, String detalle) {
		String linea = control + ": " + detalle;
		String actuales = cfe.getObservacionesRecepcion();
		cfe.setObservacionesRecepcion(actuales == null || actuales.isEmpty() ? linea : actuales + "; " + linea);
	}
}
