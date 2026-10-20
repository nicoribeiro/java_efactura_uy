package com.bluedot.efactura.recibidos;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.security.auth.x500.X500Principal;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.validation.Schema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.bluedot.efactura.model.SobreRecibido;

import play.Play;

/**
 * Controles de DGI sobre los sobres recibidos (tabla 4.2 del formato de mensaje de respuesta: motivos de rechazo de un
 * sobre). El receptor puede usar S01 a S07 y S20; S08 (ya existe en los registros de DGI) es solo de DGI. Se implementan
 * S02, S03, S04, S05, S06 y S20. No se implementan:
 * <ul>
 * <li>S01 (formato del archivo no es el indicado): si el archivo no se puede leer como sobre no hay caratula con la que
 * armar la respuesta; queda como error del adjunto del email</li>
 * <li>S07 (supera el tamaño maximo admitido): DGI no define un tamaño maximo para el sobre entre empresas</li>
 * </ul>
 * Cada control tiene un modo en application.conf (RecepcionSobre.control.SXX), como los de los CFE (ver
 * ControlesRecepcion): OFF, LOG o ENFORCE. Un sobre rechazado (BS) no se procesa: ninguno de sus CFE se registra y el
 * emisor recibe la respuesta negativa. Por eso los controles nuevos arrancan en LOG; S05 y S20 arrancan en ENFORCE porque
 * ya rechazaban antes.
 */
public class ControlesSobre {

	final static Logger logger = LoggerFactory.getLogger(ControlesSobre.class);

	/** XSD con el que S04 valida el sobre si no se configura otro */
	static final String XSD_S04_DEFECTO = "resources/xsd/XSDs_FE_1_44_2/original/EnvioCFE_entreEmpresas.xsd";

	/** Los que ya rechazaban antes de tener modo */
	private static final List<String> ENFORCE_POR_DEFECTO = Arrays.asList("S05", "S20");

	public static ControlesRecepcion.Modo modo(String control) {
		ControlesRecepcion.Modo defecto = ENFORCE_POR_DEFECTO.contains(control) ? ControlesRecepcion.Modo.ENFORCE : ControlesRecepcion.Modo.LOG;
		try {
			String valor = Play.application().configuration().getString("RecepcionSobre.control." + control, defecto.name());
			return ControlesRecepcion.Modo.valueOf(valor.trim().toUpperCase());
		} catch (RuntimeException e) {
			return defecto;
		}
	}

	/** Lo que los controles leen del XML del sobre (como llego), parseado una vez */
	public static class Sobre {
		Document doc;
		Element caratula;
		List<Element> cfes = new ArrayList<Element>();
		X509Certificate certificado;
		String errorCertificado;
	}

	/** Parsea el XML del sobre: la caratula, los CFE y el certificado de la caratula */
	public static Sobre leer(String xmlSobre) throws Exception {
		Sobre s = new Sobre();
		s.doc = ControlesRecepcion.parsearSobre(xmlSobre);
		NodeList caratulas = s.doc.getElementsByTagNameNS(ControlesRecepcion.NS_CFE, "Caratula");
		s.caratula = caratulas.getLength() > 0 ? (Element) caratulas.item(0) : null;
		// Solo los CFE del sobre (no uno escondido en una Adenda, que correria la posicion de los demas)
		s.cfes.addAll(ControlesRecepcion.cfesDelSobre(s.doc));
		String cert = s.caratula != null ? hijo(s.caratula, "X509Certificate") : null;
		if (cert == null)
			s.errorCertificado = "la carátula no tiene certificado";
		else
			try {
				s.certificado = (X509Certificate) CertificateFactory.getInstance("X.509")
						.generateCertificate(new ByteArrayInputStream(Base64.getMimeDecoder().decode(cert)));
			} catch (Exception e) {
				s.errorCertificado = "no se pudo leer el certificado de la carátula: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
			}
		return s;
	}

	/**
	 * S02: el RUC emisor de la caratula coincide con el del certificado (si el certificado indica un RUC; los de persona
	 * fisica no lo tienen) y con el emisor de cada CFE.
	 *
	 * @return null si pasa; si no, el detalle
	 */
	public static String s02(Sobre s) {
		String ruc = s.caratula != null ? hijo(s.caratula, "RUCEmisor") : null;
		if (ruc == null)
			return null;
		List<String> problemas = new ArrayList<String>();
		String rucCertificado = rucDelCertificado(s.certificado);
		if (rucCertificado != null && !rucCertificado.equals(ruc))
			problemas.add("el certificado es del RUC " + rucCertificado);
		List<String> cfes = new ArrayList<String>();
		for (int i = 0; i < s.cfes.size(); i++) {
			String rucCfe = primero(s.cfes.get(i), "RUCEmisor");
			if (rucCfe != null && !rucCfe.equals(ruc))
				cfes.add("CFE " + (i + 1) + " del RUC " + rucCfe);
		}
		if (!cfes.isEmpty())
			problemas.add(String.join(", ", cfes));
		return problemas.isEmpty() ? null : "la carátula es del RUC " + ruc + " pero " + String.join("; ", problemas);
	}

	/**
	 * S03: el certificado de la caratula se puede leer, estaba vigente en la fecha del sobre (o ahora, si la caratula no
	 * la tiene) y es de la PKI Uruguay (emitido por Abitab o Correo, firmados por la ACRN: PkiUruguay).
	 *
	 * @return null si pasa; si no, el detalle
	 */
	public static String s03(Sobre s) {
		if (s.certificado == null)
			return s.errorCertificado;
		Date fecha = new Date();
		String texto = s.caratula != null ? hijo(s.caratula, "Fecha") : null;
		if (texto != null)
			try {
				fecha = DatatypeFactory.newInstance().newXMLGregorianCalendar(texto).toGregorianCalendar().getTime();
			} catch (Exception e) {
			}
		if (fecha.before(s.certificado.getNotBefore()) || fecha.after(s.certificado.getNotAfter())) {
			SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
			return "el certificado no estaba vigente el " + sdf.format(fecha) + " (vigencia " + sdf.format(s.certificado.getNotBefore()) + " a "
					+ sdf.format(s.certificado.getNotAfter()) + ")";
		}
		// Y es de la PKI Uruguay (antes valia cualquier certificado, tambien uno autofirmado)
		return PkiUruguay.validar(s.certificado, fecha);
	}

	/**
	 * S04: el sobre cumple el formato (XSD de DGI EnvioCFE_entreEmpresas, RecepcionSobre.formato.xsd). Si no se puede
	 * cargar el XSD no se controla.
	 *
	 * @return null si pasa; si no, los primeros errores del XSD
	 */
	public static String s04(Sobre s) {
		Schema schema = ControlesRecepcion.schema("RecepcionSobre.formato.xsd", XSD_S04_DEFECTO);
		if (schema == null)
			return null;
		try {
			return ControlesRecepcion.validarXsd(schema, s.doc);
		} catch (Exception e) {
			return "no se pudo validar el formato: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
		}
	}

	/**
	 * S05: la cantidad de CFE de la caratula coincide con los CFE del sobre.
	 *
	 * @return null si pasa; si no, el detalle
	 */
	public static String s05(Sobre s) {
		String cant = s.caratula != null ? hijo(s.caratula, "CantCFE") : null;
		if (cant == null)
			return null;
		try {
			if (Integer.parseInt(cant) == s.cfes.size())
				return null;
		} catch (NumberFormatException e) {
		}
		return "la carátula indica " + cant + " CFE y el sobre trae " + s.cfes.size();
	}

	/**
	 * S06: cada CFE esta firmado con el certificado de la caratula: el certificado de la firma, si lo trae, es el mismo; si
	 * no, el numero de serie que indica la firma (X509IssuerSerial) es el del certificado de la caratula.
	 *
	 * @return null si pasa; si no, el detalle
	 */
	public static String s06(Sobre s) {
		if (s.certificado == null)
			return null;
		List<String> distintos = new ArrayList<String>();
		for (int i = 0; i < s.cfes.size(); i++) {
			Element firma = null;
			for (Node n = s.cfes.get(i).getFirstChild(); n != null && firma == null; n = n.getNextSibling())
				if (n instanceof Element && XMLSignature.XMLNS.equals(n.getNamespaceURI()) && "Signature".equals(n.getLocalName()))
					firma = (Element) n;
			if (firma == null)
				continue;
			NodeList certs = firma.getElementsByTagNameNS(XMLSignature.XMLNS, "X509Certificate");
			if (certs.getLength() > 0) {
				try {
					byte[] cert = Base64.getMimeDecoder().decode(ControlesRecepcion.texto(certs.item(0)));
					if (!Arrays.equals(cert, s.certificado.getEncoded()))
						distintos.add("CFE " + (i + 1) + " firmado con otro certificado");
				} catch (Exception e) {
					distintos.add("CFE " + (i + 1) + ": no se pudo leer el certificado de la firma");
				}
				continue;
			}
			NodeList series = firma.getElementsByTagNameNS(XMLSignature.XMLNS, "X509SerialNumber");
			if (series.getLength() > 0) {
				String serie = ControlesRecepcion.texto(series.item(0));
				try {
					if (!new BigInteger(serie).equals(s.certificado.getSerialNumber()))
						distintos.add("CFE " + (i + 1) + " firmado con el certificado número " + serie);
				} catch (NumberFormatException e) {
					distintos.add("CFE " + (i + 1) + ": número de serie del certificado inválido (" + serie + ")");
				}
			}
		}
		return distintos.isEmpty() ? null
				: "el certificado de la carátula es el número " + s.certificado.getSerialNumber() + " y " + String.join(", ", distintos);
	}

	/**
	 * S20: el sobre ya existe en los registros del receptor: un sobre recibido antes (id menor) del mismo emisor con el
	 * mismo id del emisor (Idemisor). Solo los anteriores: al recibirlo son los unicos que existen, y asi la simulacion sobre
	 * los guardados da lo mismo (el primero pasa, las copias no).
	 *
	 * @return null si pasa; si no, el detalle
	 */
	public static String s20(SobreRecibido sobre, long idEmisor) {
		List<String> otros = new ArrayList<String>();
		for (SobreRecibido otro : SobreRecibido.findSobreRecibido(idEmisor, sobre.getEmpresaEmisora(), sobre.getEmpresaReceptora()))
			if (otro.getId() < sobre.getId())
				otros.add(otro.getId() + (otro.getNombreArchivo() != null ? " (" + otro.getNombreArchivo() + ")" : ""));
		Collections.sort(otros);
		return otros.isEmpty() ? null : "ya se recibió un sobre con Idemisor " + idEmisor + ": " + String.join(", ", otros);
	}

	/** RUC que indica el certificado (SERIALNUMBER=RUC...); null si no indica uno (por ejemplo, de persona fisica) */
	static String rucDelCertificado(X509Certificate cert) {
		if (cert == null)
			return null;
		String nombre = cert.getSubjectX500Principal().getName(X500Principal.RFC2253, Collections.singletonMap("2.5.4.5", "SERIALNUMBER"));
		for (String parte : nombre.split(",")) {
			parte = parte.trim();
			if (!parte.toUpperCase().startsWith("SERIALNUMBER="))
				continue;
			String valor = parte.substring("SERIALNUMBER=".length()).trim();
			if (!valor.toUpperCase().startsWith("RUC"))
				return null;
			String digitos = valor.replaceAll("[^0-9]", "");
			return digitos.length() == 12 ? digitos : null;
		}
		return null;
	}

	/** Texto de un hijo directo con ese nombre; null si no esta o esta vacio */
	private static String hijo(Element padre, String nombre) {
		for (Node n = padre.getFirstChild(); n != null; n = n.getNextSibling())
			if (n instanceof Element && nombre.equals(n.getLocalName())) {
				String t = ControlesRecepcion.texto(n);
				return t.isEmpty() ? null : t;
			}
		return null;
	}

	/** Texto del primer descendiente con ese nombre; null si no esta o esta vacio */
	private static String primero(Element padre, String nombre) {
		NodeList l = padre.getElementsByTagNameNS(ControlesRecepcion.NS_CFE, nombre);
		if (l.getLength() == 0)
			return null;
		String t = ControlesRecepcion.texto(l.item(0));
		return t.isEmpty() ? null : t;
	}
}
