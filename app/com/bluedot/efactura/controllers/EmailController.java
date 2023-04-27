package com.bluedot.efactura.controllers;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluedot.commons.controllers.AbstractController;
import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.commons.error.ErrorMessage;
import com.bluedot.commons.security.AttachmentEstado;
import com.bluedot.commons.security.EmailMessage;
import com.bluedot.commons.security.Secured;
import com.bluedot.commons.serializers.JSONSerializerProvider;
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
