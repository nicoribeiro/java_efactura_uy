package com.bluedot.efactura.recibidos;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.commons.utils.XML;
import com.bluedot.efactura.commons.Commons;
import com.bluedot.efactura.interceptors.SignatureInterceptor;
import com.bluedot.efactura.model.CFE;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.model.EnvioMail;
import com.bluedot.efactura.model.FirmaDigital;
import com.bluedot.efactura.model.MotivoRechazoCFE;
import com.bluedot.efactura.model.Respuesta;
import com.bluedot.efactura.model.SobreRecibido;

import dgi.classes.entreEmpresas.CFEEmpresasType;
import dgi.classes.entreEmpresas.EnvioCFEEntreEmpresas;
import dgi.classes.respuestas.cfe.ACKCFEdefType;
import dgi.classes.respuestas.cfe.ACKCFEdefType.ACKCFEDet;
import dgi.classes.respuestas.cfe.EstadoACKCFEType;
import dgi.classes.respuestas.cfe.RechazoCFEDGIType;
import dgi.classes.respuestas.sobre.EstadoACKSobreType;
import play.Play;

/**
 * Respuesta a los CFE de un sobre recibido (ME_, "respuesta a consulta de comprobantes" del formato de mensaje de
 * respuesta de DGI): la aceptacion (AE) o el rechazo (BE) de cada CFE. Es obligatoria para el receptor y, segun DGI,
 * implica ademas la aceptacion comercial; el CFE aceptado por DGI es valido igual, si se rechaza el emisor tiene que hacer
 * una nota de correccion.
 *
 * No se envia al recibir el sobre sino cuando se cumple el plazo de la empresa (Empresa.diasRespuestaCfe, o
 * RespuestaCFE.diasPorDefecto si no tiene): en ese tiempo se puede rechazar un CFE desde el panel. La envia
 * RespuestaCfePoller, o alguien a mano. Se arma con el estado guardado de cada CFE (lo que dejaron los controles de
 * recepcion en modo enforce y los rechazos a mano), no con el de la recepcion.
 */
public class RespuestaCfes {

	final static Logger logger = LoggerFactory.getLogger(RespuestaCfes.class);

	/** Sin respuesta automatica: solo a mano */
	public static final int NUNCA = -1;

	/** Dias de espera de la empresa: los suyos o, si no tiene, RespuestaCFE.diasPorDefecto (si falta, solo a mano) */
	public static int dias(Empresa empresa) {
		if (empresa != null && empresa.getDiasRespuestaCfe() != null)
			return Math.max(NUNCA, empresa.getDiasRespuestaCfe());
		try {
			Integer porDefecto = Play.application().configuration().getInt("RespuestaCFE.diasPorDefecto", NUNCA);
			return porDefecto != null ? Math.max(NUNCA, porDefecto) : NUNCA;
		} catch (RuntimeException e) {
			return NUNCA;
		}
	}

	/** Cuando se envia sola la respuesta de un sobre pendiente; null si no esta pendiente o la empresa no responde sola */
	public static Date programada(SobreRecibido sobre) {
		if (sobre == null || !sobre.isRespuestaCfePendiente() || sobre.getTimestampRecibido() == null)
			return null;
		int dias = dias(sobre.getEmpresaReceptora());
		if (dias == NUNCA)
			return null;
		return new Date(sobre.getTimestampRecibido().getTime() + dias * 24L * 3600 * 1000);
	}

	/**
	 * Arma, firma, guarda y envia por mail al emisor la respuesta a los CFE del sobre, y lo marca como respondido.
	 *
	 * @param origen
	 *            "automático" o el mail del usuario que la pidio (queda en el registro de envios)
	 */
	public static Respuesta enviar(SobreRecibido sobre, String origen) throws APIException {
		Empresa empresa = sobre.getEmpresaReceptora();
		if (sobre.getRespuestaCfes() != null)
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).setDetailMessage("El sobre " + sobre.getId() + " ya tiene respuesta a sus CFE");
		if (sobre.getEstadoEmpresa() != EstadoACKSobreType.AS)
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).setDetailMessage("El sobre " + sobre.getId() + " fue rechazado: sus CFE no se responden");
		if (empresa.getFirmaDigital() == null)
			throw APIException.raise(APIErrors.MISSING_PARAMETER).withParams("FirmaDigital").setDetailMessage("La Empresa no tiene Firma Digital");
		String destino = sobre.getEmpresaEmisora() != null ? sobre.getEmpresaEmisora().getMailRecepcion() : null;
		if (destino == null || destino.trim().isEmpty())
			throw APIException.raise(APIErrors.MISSING_PARAMETER).withParams("mailRecepcion")
					.setDetailMessage("El emisor " + (sobre.getEmpresaEmisora() != null ? sobre.getEmpresaEmisora().getRut() : "?") + " no tiene mail de recepción");

		try {
			Respuesta respuesta = new Respuesta();
			play.db.jpa.JPA.em().persist(respuesta);
			respuesta.setNombreArchivo("ME_" + respuesta.getId() + "_" + sobre.getNombreArchivo());

			ACKCFEdefType ack = armar(sobre, respuesta.getId());

			DocumentBuilderFactory dbf = XML.documentBuilderFactory();
			dbf.setNamespaceAware(true);
			Document documento = XML.marshall(ack);
			documento = SignatureInterceptor.signDocument(dbf, documento, null, null, empresa.getFirmaDigital().getKeyStore(), FirmaDigital.KEY_ALIAS,
					FirmaDigital.KEYSTORE_PASSWORD);
			respuesta.setPayload(XML.documentToString(documento));
			respuesta.update();

			sobre.setRespuestaCfes(respuesta);
			sobre.setRespuestaCfePendiente(false);
			sobre.update();

			try {
				Commons.enviarMail(empresa, destino, respuesta.getNombreArchivo(), respuesta.getPayload().getBytes(), "",
						new EnvioMail.Contexto(EnvioMail.Tipo.ACK_CFE, origen).sobre(sobre.getId()));
			} catch (APIException e) {
				// Si el mail no salio el sobre sigue pendiente (se reintenta); la respuesta armada queda guardada sin usar
				sobre.setRespuestaCfes(null);
				sobre.setRespuestaCfePendiente(true);
				sobre.update();
				throw e;
			}
			logger.info("Respuesta a CFE {} enviada a {} ({} aceptados, {} rechazados)", respuesta.getNombreArchivo(), destino,
					ack.getCaratula().getCantCFEAceptados(), ack.getCaratula().getCantCFERechazados());
			return respuesta;
		} catch (APIException e) {
			throw e;
		} catch (Exception e) {
			throw APIException.raise(e);
		}
	}

	/** XSD de la respuesta a los CFE del receptor (para la vista previa) si no se configura otro */
	static final String XSD_DEFECTO = "resources/xsd/XSDs_FE_1_44_2/original/ACKCFE_Partes.xsd";

	/**
	 * Vista previa de la respuesta a los CFE de un sobre: la arma y la firma como se enviaria, sin guardarla ni enviarla
	 * (IDRespuesta 0), y la valida contra el XSD de DGI (RespuestaCFE.xsd).
	 *
	 * @return {xml, errores (null si cumple el XSD)}
	 */
	public static String[] vistaPrevia(SobreRecibido sobre) throws APIException {
		Empresa empresa = sobre.getEmpresaReceptora();
		if (empresa.getFirmaDigital() == null)
			throw APIException.raise(APIErrors.MISSING_PARAMETER).withParams("FirmaDigital").setDetailMessage("La Empresa no tiene Firma Digital");
		try {
			DocumentBuilderFactory dbf = XML.documentBuilderFactory();
			dbf.setNamespaceAware(true);
			Document documento = XML.marshall(armar(sobre, 0));
			documento = SignatureInterceptor.signDocument(dbf, documento, null, null, empresa.getFirmaDigital().getKeyStore(), FirmaDigital.KEY_ALIAS,
					FirmaDigital.KEYSTORE_PASSWORD);
			String xml = XML.documentToString(documento);
			javax.xml.validation.Schema schema = ControlesRecepcion.schema("RespuestaCFE.xsd", XSD_DEFECTO);
			String errores = schema != null ? ControlesRecepcion.validarXsd(schema, ControlesRecepcion.parsearSobre(xml)) : "no se pudo cargar el XSD";
			return new String[] { xml, errores };
		} catch (Exception e) {
			throw APIException.raise(e);
		}
	}

	/** La respuesta, con el estado guardado de cada CFE del sobre */
	static ACKCFEdefType armar(SobreRecibido sobre, long idRespuesta) throws Exception {
		EnvioCFEEntreEmpresas envio = sobre.getEnvioCFEEntreEmpresas();
		List<CFE> cfes = CFE.findBySobreRecibido(sobre);
		DatatypeFactory df = DatatypeFactory.newInstance();

		ACKCFEdefType ack = new ACKCFEdefType();
		ack.setVersion("1.0");
		ACKCFEdefType.Caratula caratula = new ACKCFEdefType.Caratula();
		caratula.setRUCReceptor(sobre.getEmpresaReceptora().getRut());
		caratula.setRUCEmisor(envio.getCaratula().getRUCEmisor());
		caratula.setIDRespuesta(BigInteger.valueOf(idRespuesta));
		caratula.setNomArch(sobre.getNombreArchivo());
		caratula.setFecHRecibido(calendario(df, sobre.getTimestampRecibido() != null ? sobre.getTimestampRecibido() : new Date()));
		caratula.setIDEmisor(envio.getCaratula().getIdemisor());
		caratula.setIDReceptor(BigInteger.valueOf(sobre.getId()));
		caratula.setCantenSobre(BigInteger.valueOf(envio.getCFEAdendas().size()));
		caratula.setCantResponden(BigInteger.valueOf(cfes.size()));
		caratula.setCantCFCAceptados(BigInteger.ZERO);
		caratula.setCantCFCObservados(BigInteger.ZERO);
		caratula.setCantOtrosRechazados(BigInteger.ZERO);
		caratula.setTmst(calendario(df, new Date()));

		int aceptados = 0, rechazados = 0;
		for (CFE cfe : cfes) {
			ACKCFEDet det = new ACKCFEDet();
			det.setNroOrdinal(BigInteger.valueOf(cfe.getOrdinal()));
			det.setTipoCFE(BigInteger.valueOf(cfe.getTipo().value));
			det.setSerie(cfe.getSerie());
			det.setNroCFE(BigInteger.valueOf(cfe.getNro()));
			det.setFechaCFE(df.newXMLGregorianCalendar(new SimpleDateFormat("yyyy-MM-dd").format(cfe.getFechaEmision())));
			// Fecha de la firma del CFE (no la de la respuesta)
			XMLGregorianCalendar firma = null;
			if (cfe.getOrdinal() >= 1 && cfe.getOrdinal() <= envio.getCFEAdendas().size()) {
				CFEEmpresasType cfeXml = envio.getCFEAdendas().get(cfe.getOrdinal() - 1);
				firma = ControlesRecepcion.tmstFirma(cfeXml.getCFE());
			}
			if (firma != null)
				det.setTmstCFE(firma);

			if (cfe.getEstado() == EstadoACKCFEType.BE) {
				rechazados++;
				det.setEstado(EstadoACKCFEType.BE);
				for (MotivoRechazoCFE motivo : cfe.getMotivo()) {
					RechazoCFEDGIType rechazo = new RechazoCFEDGIType();
					rechazo.setMotivo(motivo.name());
					rechazo.setGlosa(motivo.getMotivo());
					String detalle = esComercial(motivo) ? cfe.getDetalleRechazo() : null;
					if (detalle != null && !detalle.trim().isEmpty())
						rechazo.setDetalle(detalle.length() > 500 ? detalle.substring(0, 497) + "..." : detalle);
					det.getMotivosRechazoCF().add(rechazo);
				}
			} else {
				aceptados++;
				det.setEstado(EstadoACKCFEType.AE);
			}
			ack.getACKCFEDet().add(det);
		}
		caratula.setCantCFEAceptados(BigInteger.valueOf(aceptados));
		caratula.setCantCFERechazados(BigInteger.valueOf(rechazados));
		ack.setCaratula(caratula);
		return ack;
	}

	/** Rechazo comercial (E20 en adelante, a mano), no de un control de recepcion */
	public static boolean esComercial(MotivoRechazoCFE motivo) {
		return motivo.name().compareTo("E20") >= 0;
	}

	private static XMLGregorianCalendar calendario(DatatypeFactory df, Date fecha) {
		GregorianCalendar c = new GregorianCalendar();
		c.setTime(fecha);
		return df.newXMLGregorianCalendar(c);
	}
}
