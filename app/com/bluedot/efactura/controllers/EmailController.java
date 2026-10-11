package com.bluedot.efactura.controllers;

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
import com.bluedot.efactura.microControllers.factory.EfacturaMicroControllersFactory;
import com.bluedot.efactura.microControllers.factory.EfacturaMicroControllersFactoryBuilder;
import com.bluedot.efactura.microControllers.interfaces.ServiceMicroController;
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

		Tuple<List<EmailMessage>, Long> emails = EmailMessage.find(empresa, request().getQueryString("q"), estado, desde, hasta, page, pageSize);

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
