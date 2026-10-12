package com.bluedot.efactura.controllers;

import com.bluedot.efactura.security.EmpresaAccess;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluedot.commons.controllers.AbstractController;
import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.commons.error.ErrorMessage;
import com.bluedot.commons.security.Attachment;
import com.bluedot.commons.security.AttachmentEstado;
import com.bluedot.commons.security.EmailMessage;
import com.bluedot.commons.security.Secured;
import com.bluedot.commons.serializers.JSONSerializerProvider;
import com.bluedot.commons.utils.JSONUtils;
import com.bluedot.commons.utils.Tuple;
import com.bluedot.commons.utils.messaging.AttachmentType;
import com.bluedot.commons.utils.messaging.EmailAttachmentReceiver;
import com.bluedot.efactura.microControllers.factory.EfacturaMicroControllersFactory;
import com.bluedot.efactura.microControllers.factory.EfacturaMicroControllersFactoryBuilder;
import com.bluedot.efactura.microControllers.interfaces.ServiceMicroController;
import com.bluedot.efactura.model.EmailFolderState;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.pollers.PollerManager;
import com.google.inject.Inject;
import com.play4jpa.jpa.db.Tx;

import play.libs.F.Promise;
import play.mvc.Result;
import play.mvc.Security;

@ErrorMessage
@Tx
@Security.Authenticated(Secured.class)
@EmpresaAccess
public class EmailController extends AbstractController
{

	final static Logger logger = LoggerFactory.getLogger(EmailController.class);

	@Inject
	public EmailController(PollerManager pollerManager)
	{
		super();
	}

	/**
	 * Emails recibidos por la empresa, paginados (solo lectura).
	 * 
	 * Parametros: page, pageSize (max 50), estado (emails con algun adjunto en ese estado: PROCESADO_OK,
	 * PROCESADO_ERROR, PENDIENTE, OMITIDO), desde/hasta (epoch en segundos, fecha de envio), q (asunto o remitente).
	 * Devuelve {results, count, page, pageSize}. De los adjuntos se devuelve solo nombre, estado y fecha, no el contenido.
	 */
	//TODO agregar validacion de que el usuario tiene permisos sobre esta empresa
	public Promise<Result> getEmails(String rut) throws APIException
	{
		Empresa empresa = Empresa.findByRUT(rut, true);

		int page;
		int pageSize;
		Date desde;
		Date hasta;
		try {
			page = request().getQueryString("page") != null ? Integer.parseInt(request().getQueryString("page")) : 1;
			pageSize = request().getQueryString("pageSize") != null ? Integer.parseInt(request().getQueryString("pageSize")) : 20;
			desde = request().getQueryString("desde") != null ? new Date(Long.parseLong(request().getQueryString("desde")) * 1000) : null;
			hasta = request().getQueryString("hasta") != null ? new Date(Long.parseLong(request().getQueryString("hasta")) * 1000) : null;
		} catch (NumberFormatException e) {
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).setDetailMessage("page, pageSize, desde y hasta deben ser numericos");
		}
		if (page <= 0)
			page = 1;
		if (pageSize <= 0)
			pageSize = 20;
		pageSize = Math.min(pageSize, 50);

		AttachmentEstado estado = null;
		String estadoParam = request().getQueryString("estado");
		if (estadoParam != null && !estadoParam.isEmpty()) {
			try {
				estado = AttachmentEstado.valueOf(estadoParam);
			} catch (IllegalArgumentException e) {
				throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("estado", estadoParam);
			}
		}

		// adjuntos: "si" = solo con adjuntos, "no" = solo sin adjuntos; sin parametro = todos
		String adjuntosParam = request().getQueryString("adjuntos");
		Boolean conAdjuntos = null;
		if (adjuntosParam != null && !adjuntosParam.isEmpty()) {
			if ("si".equalsIgnoreCase(adjuntosParam))
				conAdjuntos = true;
			else if ("no".equalsIgnoreCase(adjuntosParam))
				conAdjuntos = false;
			else
				throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("adjuntos", adjuntosParam);
		}

		Tuple<List<EmailMessage>, Long> emails = EmailMessage.find(empresa, request().getQueryString("q"), estado, conAdjuntos, desde, hasta, page,
				pageSize);

		// Adjuntos de la pagina en una sola consulta y sin el contenido
		Map<Long, JSONArray> adjuntosPorEmail = new HashMap<Long, JSONArray>();
		List<Long> ids = new LinkedList<Long>();
		for (EmailMessage email : emails.item1) {
			ids.add(email.getId());
			adjuntosPorEmail.put(email.getId(), new JSONArray());
		}
		for (Object[] fila : Attachment.findResumenByEmails(ids)) {
			JSONObject adjunto = new JSONObject();
			adjunto.put("id", fila[0]);
			adjunto.put("name", fila[1] != null ? fila[1] : JSONObject.NULL);
			adjunto.put("estado", fila[2] != null ? ((AttachmentEstado) fila[2]).name() : JSONObject.NULL);
			adjunto.put("timestamp", fila[3]);
			adjunto.put("error", fila[5] != null ? fila[5] : JSONObject.NULL);
			adjunto.put("errorStack", fila[6] != null ? fila[6] : JSONObject.NULL);
			adjunto.put("integro", integro((String) fila[1], (AttachmentType) fila[7]));
			adjuntosPorEmail.get((Long) fila[4]).put(adjunto);
		}

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
		JSONArray results = new JSONArray();
		for (EmailMessage email : emails.item1) {
			JSONObject json = new JSONObject();
			json.put("id", email.getId());
			json.put("messageId", email.getMessageId() != null ? email.getMessageId() : JSONObject.NULL);
			json.put("from", email.getFromEmail() != null ? email.getFromEmail() : JSONObject.NULL);
			json.put("subject", email.getSubject() != null ? email.getSubject() : JSONObject.NULL);
			json.put("sentDate", email.getSentDate() != null ? sdf.format(email.getSentDate()) : JSONObject.NULL);
			json.put("attachments", adjuntosPorEmail.get(email.getId()));
			results.put(json);
		}

		return json(JSONUtils.createObjectList(results, emails.item2, page, pageSize).toString());
	}

	/**
	 * El contenido guardado del adjunto es el original: los binarios guardados como texto (antes de BASE64) perdieron los
	 * bytes que no eran UTF-8
	 */
	private static boolean integro(String nombre, AttachmentType tipo) {
		return tipo == AttachmentType.BASE64 || EmailAttachmentReceiver.esTexto(nombre, null);
	}

	private static final Map<String, String> TIPOS_ARCHIVO = new HashMap<String, String>();
	static {
		TIPOS_ARCHIVO.put("xml", "application/xml");
		TIPOS_ARCHIVO.put("txt", "text/plain");
		TIPOS_ARCHIVO.put("pdf", "application/pdf");
		TIPOS_ARCHIVO.put("png", "image/png");
		TIPOS_ARCHIVO.put("jpg", "image/jpeg");
		TIPOS_ARCHIVO.put("jpeg", "image/jpeg");
		TIPOS_ARCHIVO.put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
		TIPOS_ARCHIVO.put("xls", "application/vnd.ms-excel");
		TIPOS_ARCHIVO.put("doc", "application/msword");
		TIPOS_ARCHIVO.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
	}

	/**
	 * Descarga un adjunto de un email recibido por la empresa, con su nombre original. Si es un binario guardado antes de
	 * BASE64 se devuelve igual, pero el archivo puede estar danado (ver integro en getEmails).
	 */
	public Promise<Result> getAdjunto(String rut, long emailId, int adjuntoId) throws APIException
	{
		Empresa empresa = Empresa.findByRUT(rut, true);

		Attachment adjunto = Attachment.findById(adjuntoId);
		if (adjunto == null || adjunto.getEmailMessage() == null || adjunto.getEmailMessage().getId() != emailId
				|| adjunto.getEmailMessage().getEmpresa() == null || adjunto.getEmailMessage().getEmpresa().getId() != empresa.getId())
			throw APIException.raise(APIErrors.ATTACHMENT_NOT_FOUND).withParams("id", String.valueOf(adjuntoId));

		byte[] contenido;
		if (adjunto.getPayload() == null)
			contenido = new byte[0];
		else if (adjunto.getAttachmentType() == AttachmentType.BASE64)
			contenido = java.util.Base64.getDecoder().decode(adjunto.getPayload());
		else
			contenido = adjunto.getPayload().getBytes(java.nio.charset.StandardCharsets.UTF_8);

		String nombre = adjunto.getName() != null ? adjunto.getName() : "adjunto-" + adjunto.getId();
		int punto = nombre.lastIndexOf('.');
		String extension = punto >= 0 ? nombre.substring(punto + 1).toLowerCase() : "";
		String tipo = TIPOS_ARCHIVO.containsKey(extension) ? TIPOS_ARCHIVO.get(extension) : "application/octet-stream";

		// Nombre en ASCII para el header (los demas caracteres se reemplazan) y completo en filename* (RFC 5987)
		String ascii = nombre.replaceAll("[^A-Za-z0-9._ -]", "_");
		String utf8;
		try {
			utf8 = java.net.URLEncoder.encode(nombre, "UTF-8").replace("+", "%20");
		} catch (java.io.UnsupportedEncodingException e) {
			utf8 = ascii;
		}
		response().setHeader("Content-Disposition", "attachment; filename=\"" + ascii + "\"; filename*=UTF-8''" + utf8);
		response().setHeader("X-Adjunto-Integro", String.valueOf(integro(adjunto.getName(), adjunto.getAttachmentType())));
		return Promise.<Result>pure(ok(contenido).as(tipo));
	}

	/**
	 * Estado de la sincronizacion IMAP de cada carpeta del buzon de la empresa (solo lectura): ultimo UID procesado y
	 * fecha de la ultima sincronizacion.
	 */
	//TODO agregar validacion de que el usuario tiene permisos sobre esta empresa (cubierto por @EmpresaAccess)
	public Promise<Result> getCarpetas(String rut) throws APIException
	{
		Empresa empresa = Empresa.findByRUT(rut, true);

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
		JSONArray carpetas = new JSONArray();
		for (EmailFolderState state : EmailFolderState.findByEmpresa(empresa)) {
			JSONObject json = new JSONObject();
			json.put("folderName", state.getFolderName());
			json.put("uidValidity", state.getUidValidity());
			json.put("lastUid", state.getLastUid());
			json.put("ultimaSincronizacion", state.getUltimaSincronizacion() != null ? sdf.format(state.getUltimaSincronizacion()) : JSONObject.NULL);
			carpetas.put(json);
		}

		return json(carpetas.toString());
	}

	public Promise<Result> getEmailEntrantes(String rut, String estado) throws APIException
	{
		AttachmentEstado attachmentEstado = getAttachmentEstado(estado);

		Empresa empresaReceptora = Empresa.findByRUT(rut, true);

		if (attachmentEstado == AttachmentEstado.PROCESADO_ERROR) {
			JSONArray emailArray = JSONSerializerProvider.getEmailMessageSerializer().objectToJson(empresaReceptora.getEmailsRecibidosError());
			return json(emailArray.toString());
		} else
			throw APIException.raise(APIErrors.NOT_SUPPORTED);

	}

	public Promise<Result> emailEntrantesTodos(String rut, String accion) throws APIException
	{
		return emailEntrantes(rut, null, accion);
	}

	public Promise<Result> emailEntrantes(String rut, Long id, String accion) throws APIException
	{
		EmailAction emailAction = getEmailAction(accion);

		Empresa empresaReceptora = Empresa.findByRUT(rut, true);

		EfacturaMicroControllersFactory factory = (new EfacturaMicroControllersFactoryBuilder()).getMicroControllersFactory();

		if (emailAction == EmailAction.POLL) {
			List<EmailMessage> emailsModelo = factory.getServiceMicroController(empresaReceptora).obtenerYProcesarEmailsEntrantesDesdeServerCorreo();
			JSONArray jsonArray = JSONSerializerProvider.getEmailMessageSerializer().objectToJson(emailsModelo, true);
			return json(jsonArray.toString());
		}

		if (emailAction == EmailAction.RETRY) {
			ServiceMicroController serviceMicroController = factory.getServiceMicroController(empresaReceptora);
			int i = 1;
			
			if (id != null) {
				EmailMessage email = EmailMessage.findById(id,true);
				serviceMicroController.procesarAttachments(i, email, true);
				JSONObject jsonObject = JSONSerializerProvider.getEmailMessageSerializer().objectToJson(email);
				return json(jsonObject.toString());
				
			} else {
				JSONArray jsonArray = new JSONArray();
				
				for (EmailMessage email : empresaReceptora.getEmailsRecibidosError()) {
					serviceMicroController.procesarAttachments(i, email, true);
					JSONObject jsonObject = JSONSerializerProvider.getEmailMessageSerializer().objectToJson(email);
					jsonArray.put(jsonObject);
					i++;
				}
				return json(jsonArray.toString());
			}
			

		}
		return json(OK);
	}

	/**
	 * @param accion
	 * @throws APIException
	 */
	private EmailAction getEmailAction(String accion) throws APIException
	{
		EmailAction accionEmail;
		try {
			accionEmail = EmailAction.valueOf(accion);
			if (accionEmail == EmailAction.NO_ACTION)
				throw APIException.raise(APIErrors.MISSING_PARAMETER).withParams("accion");
		} catch (IllegalArgumentException e) {
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("accion");
		}

		return accionEmail;
	}

	private AttachmentEstado getAttachmentEstado(String estado) throws APIException
	{
		AttachmentEstado attachmentEstado;
		try {
			attachmentEstado = AttachmentEstado.valueOf(estado);
		} catch (IllegalArgumentException e) {
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("estado");
		}

		return attachmentEstado;
	}

}
