package com.bluedot.efactura.microControllers.implementation;

import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.commons.notificationChannels.MessagingHelper;
import com.bluedot.commons.security.Attachment;
import com.bluedot.commons.security.AttachmentEstado;
import com.bluedot.commons.security.EmailMessage;
import com.bluedot.commons.utils.DateHandler;
import com.bluedot.commons.utils.ThreadMan;
import com.bluedot.commons.utils.XML;

import com.bluedot.commons.utils.messaging.Email;
import com.bluedot.commons.utils.messaging.EmailAttachmentReceiver;
import com.bluedot.efactura.microControllers.interfaces.CAEMicroController;
import com.bluedot.efactura.microControllers.interfaces.IntercambioMicroController;
import com.bluedot.efactura.microControllers.interfaces.ServiceMicroController;
import com.bluedot.efactura.model.CFE;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.model.ReporteDiario;
import com.bluedot.efactura.model.Respuesta;
import com.bluedot.efactura.model.Sobre;
import com.bluedot.efactura.model.SobreEmitido;
import com.bluedot.efactura.model.SobreRecibido;
import com.bluedot.efactura.services.ConsultasService;
import com.bluedot.efactura.services.RecepcionService;
import com.bluedot.efactura.strategy.builder.CFEBuiderInterface;
import com.bluedot.efactura.strategy.builder.CFEBuilderFactory;

import dgi.classes.entreEmpresas.EnvioCFEEntreEmpresas;
import dgi.classes.respuestas.cfe.ACKCFEdefType;
import dgi.classes.respuestas.cfe.EstadoACKCFEType;
import dgi.classes.respuestas.sobre.ACKSobredefType;
import dgi.classes.respuestas.sobre.EstadoACKSobreType;
import dgi.soap.consultas.ACKConsultaEnviosSobre;
import dgi.soap.consultas.DatosSobre;

public class ServiceMicroControllerDefault extends MicroControllerDefault implements ServiceMicroController {

	final static Logger logger = LoggerFactory.getLogger(ServiceMicroControllerDefault.class);
	
	private RecepcionService recepcionService;
	private CAEMicroController caeMicroController;
	private ConsultasService consultasService;
	private IntercambioMicroController intercambioMicroController;

	public ServiceMicroControllerDefault(RecepcionService recepcionService, Empresa empresa, CAEMicroController caeMicroController, ConsultasService consultasService, IntercambioMicroController intercambioMicroController) {
		super(empresa);
		this.recepcionService = recepcionService;
		this.caeMicroController = caeMicroController;
		this.consultasService = consultasService;
		this.intercambioMicroController = intercambioMicroController;
	}	

	@Override
	public void enviar(CFE cfe) throws APIException {

		CFEBuiderInterface builder = CFEBuilderFactory.getCFEBuilder(cfe, caeMicroController);
		//TODO mutex
		builder.asignarId();
		
		if (cfe.getEstado() == null || (cfe.getEstado() !=null  && cfe.getEstado() != EstadoACKCFEType.BE))
			recepcionService.sendCFE(cfe);
		else
			ThreadMan.forceTransactionFlush();

	}
	
	@Override
	public void reenviar(SobreEmitido sobre) throws APIException {
		try {
			recepcionService.reenviarSobre(sobre);
		} catch (APIException e) {
			if (e.getError()==APIErrors.SOBRE_YA_ENVIADO){
				consultaResultado(sobre);
			}else
				throw e;
		}
	}

	@Override
	public void consultarResultados(Date date) throws APIException {
		
		List<SobreEmitido> sobres = SobreEmitido.findByEmpresaEmisoraAndDate(this.getEmpresa(), date);

		for (Iterator<SobreEmitido> iterator = sobres.iterator(); iterator.hasNext();) {
			SobreEmitido sobre = iterator.next();
			consultaResultado((SobreEmitido) sobre);
		}
	}

	@Override
	public SobreEmitido consultaResultado(SobreEmitido sobreEmitido) throws APIException {
		
		
		//Intento recuperar los datos del sobre cuando el sobre por alguna razon le faltan datos. 
		if (sobreEmitido.getToken() == null || sobreEmitido.getIdReceptor() == null){
			ACKConsultaEnviosSobre respuesta = consultasService.consultarEnvioSobre(sobreEmitido.getId(), 0, DateHandler.minus(sobreEmitido.getFecha(), 5, Calendar.DAY_OF_MONTH), DateHandler.add(sobreEmitido.getFecha(), 5, Calendar.DAY_OF_MONTH), this.getEmpresa());
			
			List<DatosSobre> sobres = respuesta.getColeccionDatosSobre().getDatosSobre();
			
			for (DatosSobre sobre : sobres) {
				if (sobre.getIdEmisor()==sobreEmitido.getId()){
					sobreEmitido.setEstadoDgi(EstadoACKSobreType.fromValue(sobre.getEstadoSobre()));
					sobreEmitido.setIdReceptor(sobre.getIdReceptor());
					if (sobreEmitido.getEstadoDgi()==EstadoACKSobreType.AS){
						sobreEmitido.setToken(sobre.getParamConsulta().getToken());
						sobreEmitido.setFechaConsulta(sobre.getParamConsulta().getFechahora().toGregorianCalendar().getTime());
					}
					sobreEmitido.update();
				}
			}
		}
		
		/*
		 * Si sigue siendo null el token o el id_receptor no consulto, asumo que hubo un error de comunicacion o que el sobre no existe
		 * 
		 * Si el sobreEmitido ya tiene resultado tampoco consulto
		 */
		if (sobreEmitido.getToken() != null && sobreEmitido.getIdReceptor() != null){
			if (sobreEmitido.getResultado_dgi()== null || sobreEmitido.getResultado_dgi().isEmpty())
				recepcionService.consultaResultadoSobre(sobreEmitido);
		}
		
		return sobreEmitido;
	}

	@Override
	public ReporteDiario generarReporteDiario(Date date) throws APIException {
		return recepcionService.generarReporteDiario(date, this.getEmpresa());
	}

	@Override
	public void anularDocumento(CFE cfe) throws APIException {
		boolean anular = false;
		/*
		 * El sobre fue rechazado
		 */
		if (cfe.getSobreEmitido()!=null && cfe.getSobreEmitido().getEstadoDgi() != null && cfe.getSobreEmitido().getEstadoDgi()== EstadoACKSobreType.BS)
			anular = true;
			
		/*
		 * El cfe fue rechazado
		 */
		if (cfe.getEstado() != null && cfe.getEstado() == EstadoACKCFEType.BE)
			anular = true;
		
		
		if (anular){
			if (cfe.getGeneradorId()==null){
				throw APIException.raise(APIErrors.CFE_YA_FUE_ANULADO);
			}else{
				cfe.setGeneradorId(null);
				cfe.update();
			}
		}else
			throw APIException.raise(APIErrors.CFE_NO_SE_PUEDE_ANULAR);
		
		
	}
	
	@Override
	public List<EmailMessage> obtenerYProcesarEmailsEntrantesDesdeServerCorreo() throws APIException {
		// TODO mutex

		List<EmailMessage> emailsModelo = new LinkedList<EmailMessage>();
		
		EmailAttachmentReceiver receiver = new EmailAttachmentReceiver();
		
		int offset = this.getEmpresa().getOffsetMail()==0 ? 1 : this.getEmpresa().getOffsetMail();
		int messageQuantity = 100;
		List<Email> emails = null;
		do {
			
			emails = receiver.downloadEmail("imap", this.getEmpresa().getHostRecepcion(),
					this.getEmpresa().getPuertoRecepcion(), this.getEmpresa().getUserRecepcion(),
					this.getEmpresa().getPassRecepcion(), offset, messageQuantity);
			
			try {
				emailsModelo.addAll(procesarEmails(emails, false));
				offset += emails.size();
				this.getEmpresa().setOffsetMail(offset);
				this.getEmpresa().update();
				ThreadMan.forceTransactionFlush();
			} catch (APIException e) {
				throw e;
			}
			
		} while (emails.size() == messageQuantity);
		
		return emailsModelo;
	}
	
	
	private List<EmailMessage> procesarEmails(List<Email> emails, boolean retry) throws APIException {
	
		int i = 1;
		
		List<EmailMessage> emailsModelo = new LinkedList<EmailMessage>();
		
		for (Email email : emails)

		{
			logger.info(i+"-Procesando email: " + email.getMessageId());
			
			if (EmailMessage.findByMessageId(email.getMessageId()).size()>0){
				logger.info(i+"-Este Email ya fue procesado messageId:" + email.getMessageId());
				i++;
				continue;
			}
			
			/*
			 * Convierto El datatype Email a un Email del modelo y lo persisto junto con sus Attachments
			 */
			EmailMessage emailModel = new EmailMessage(email);
			emailModel.setEmpresa(getEmpresa());
			if (!play.db.jpa.JPA.em().contains(emailModel))
				emailModel.save();
			emailsModelo.add(emailModel);
			ThreadMan.forceTransactionFlush();
			
			logger.info(i+"-Email persistido: " + email.getMessageId());
			logger.info(i+"-Email contiene " + email.getAttachments().size() + " attachments");
			
			procesarAttachments(i, emailModel, retry);
			
			logger.info(i+"-Fin procesaminto email: " + email.getMessageId());
			i++;
		}

		return emailsModelo;
	}

	/**
	 * Hay 3 tipos de correos que pueden llegar a ser validos
	 * 
	 * 1 - Respuesta a un sobre con CFE emitidos por una empresa manejada por este sistema. 
	 * 2 - Respuesta a la recepcion de un SobreEmitido
	 * 3 - Un sobre con CFE emitidos por otra empresa hacia una empresa manejada por el sistema.
	 */
	@Override
	public void procesarAttachments(int index, EmailMessage emailMessage, boolean retry) {
		if (emailMessage.getAttachments()==null || emailMessage.getAttachments().size()==0)
			logger.info(index+"-Email no contiene adjuntos, se aborta el procesamiento");
		
		LinkedList<Integer> attachmentsIds = new LinkedList<>(); 
		
		
		/*
		 * Tomo los ids de los attachments para luego pedirlos por id, esto previene la exception:
		 * 
		 * java.util.ConcurrentModificationException: null
		 * 
		 * que pasa cuando se modifica la lista de attachments cuendo se le pide a hibernate que vuelva a traer el atachment
		 * 
		 * 
		 */
		
		for (Attachment attachment : emailMessage.getAttachments()) {
			attachmentsIds.add(attachment.getId());
		}
		
		for (Integer attachmentId : attachmentsIds) {
			procesarAttachment(index, attachmentId, retry);	
		}
		
	}

	/**
	 * @param index
	 * @param emailMessage
	 * @param attachment
	 * @return resultado del procesamiento: true = OK
	 */
	public void procesarAttachment(int index, int attachmentId, boolean retry) {
		boolean procesar_Sob = true;
		boolean procesar_M = true;
		boolean procesar_ME = true;
		
		try {
			
			Attachment attachment = Attachment.findById(attachmentId);
			
			logger.info(index+"-Procesando Adjunto: " + attachment.getName());

			if (attachment.getEstado()==AttachmentEstado.PROCESADO_OK && ! retry) {
				logger.info(index+"-El Adjunto ya fue procesado correctamente, se aborta el procesamiento");
				return;
			}
			
			if (attachment.getName()==null) {
				logger.info(index+"-El Adjunto no tiene nombre, se aborta el procesamiento");
				return;
			}
			
			if (!attachment.getName().toUpperCase().endsWith("XML")) {
				logger.info(index+"-El Adjunto no tiene extension XML, se aborta el procesamiento");
				attachment.setEstado(AttachmentEstado.OMITIDO);
				attachment.update();
				return;
			}
				
			Document document = XML.loadXMLFromString(attachment.getPayload());
			
			logger.debug(index+"-Attachment payload: " + attachment.getPayload());
			
			
			/*
			 * 1 - Es la respuesta de los CFE dentro de un SobreEmitido (respuesta de aceptacion)
			 */
			if (attachment.getName().substring(0, 3).equalsIgnoreCase("ME_") && procesar_ME){
				ACKCFEdefType ackCFEdefType = (ACKCFEdefType) XML.unMarshall(document,
						ACKCFEdefType.class);
				Sobre sobre = SobreEmitido.findById(ackCFEdefType.getCaratula().getIDEmisor().longValue(), true);
				
				if (!ackCFEdefType.getCaratula().getRUCEmisor().equals(this.getEmpresa().getRut())) {
					logger.info(index+"-La empresa emisora no es igual a la empresa local, se aborta el procesamiento");
					return;
				}
				
				if (sobre.getEmpresaEmisora() != getEmpresa()) {
					logger.info(index+"-La empresa emisora del sobre no es igual a la empresa local, se aborta el procesamiento");
					return;
				}
				
				setEmpresa(attachment);
				
				if (sobre instanceof SobreEmitido){
					SobreEmitido sobreEmitido = (SobreEmitido) sobre;
					
					if (sobreEmitido.getRespuestaCfes()==null) {
						//chequeo que sea null ya que puede ser un retry, si ya fue creado no hago nada y retorno OK
						Respuesta respuestaCfe = new Respuesta();
						sobreEmitido.setRespuestaCfes(respuestaCfe);
						respuestaCfe.setNombreArchivo(attachment.getName());
						respuestaCfe.setPayload(attachment.getPayload());
						respuestaCfe.save();
						sobreEmitido.update();
					} else {
						logger.info(index+"-La respuesta de los cfe del sobre con id ? ya fue procesada", ackCFEdefType.getCaratula().getIDEmisor().longValue());
					}
				}
			}
			
			/*
			 * 2 - Es una respuesta a la recepcion de un SobreEmitido (respuesta de recepcion)
			 */
			if (attachment.getName().substring(0, 2).equalsIgnoreCase("M_") && procesar_M){
				ACKSobredefType ackSobredefType = (ACKSobredefType) XML.unMarshall(document,
						ACKSobredefType.class);
				Sobre sobre = SobreEmitido.findById(ackSobredefType.getCaratula().getIDEmisor().longValue(), true);
				
				if (!ackSobredefType.getCaratula().getRUCEmisor().equals(this.getEmpresa().getRut())) {
					logger.info(index+"-La empresa emisora no es igual a la empresa local, se aborta el procesamiento");
					return;
				}
				
				if (sobre.getEmpresaEmisora() != getEmpresa()) {
					logger.info(index+"-La empresa emisora del sobre no es igual a la empresa local, se aborta el procesamiento");
					return;
				}
				
				setEmpresa(attachment);
				
				if (sobre instanceof SobreEmitido){
					SobreEmitido sobreEmitido = (SobreEmitido) sobre;
					sobreEmitido.setEstadoEmpresa(ackSobredefType.getDetalle().getEstado());
					
					if (sobreEmitido.getRespuestaSobre()==null) {
						//chequeo que sea null ya que puede ser un retry, si ya fue creado no hago nada y retorno OK
						Respuesta respuestaSobre = new Respuesta();
						sobreEmitido.setRespuestaSobre(respuestaSobre);
						respuestaSobre.setNombreArchivo(attachment.getName());
						respuestaSobre.setPayload(attachment.getPayload());
						respuestaSobre.save();
						sobreEmitido.update();
					}else {
						logger.info(index+"-La respuesta del sobre con id ? ya fue procesada", ackSobredefType.getCaratula().getIDEmisor().longValue());
					}
				}
			}
			
			/*
			 * 3 - Es un sobre con CFE adentro
			 */
			if (attachment.getName().substring(0, 3).equalsIgnoreCase("Sob") && procesar_Sob){

				EnvioCFEEntreEmpresas envioCFEEntreEmpresas = (EnvioCFEEntreEmpresas) XML.unMarshall(document,
						EnvioCFEEntreEmpresas.class);

				Empresa empresaReceptoraCandidata = Empresa
						.findByRUT(envioCFEEntreEmpresas.getCaratula().getRutReceptor(), true);

				if (empresaReceptoraCandidata.getId() != this.getEmpresa().getId()) {
					logger.info(index+"-La empresa receptora no es igual a la empresa local, se aborta el procesamiento");
					return;
				}
				
				setEmpresa(attachment);

				List<Sobre> sobres = SobreRecibido.findByNombre(attachment.getName());
				
				SobreRecibido sobreRecibido=null;
				
				if (sobres.size()>0 && sobres.get(0) instanceof SobreRecibido)
					sobreRecibido = (SobreRecibido) sobres.get(0);
				
				if (sobreRecibido==null){
					/*
					 * El sobre no existe paso a procesar el SobreRecibido y procesar los CFE
					 */
					sobreRecibido = new SobreRecibido();
					sobreRecibido.setEmpresaReceptora(this.getEmpresa());
					Empresa empresaEmisora = Empresa.findByRUT(envioCFEEntreEmpresas.getCaratula().getRUCEmisor());
					if (empresaEmisora == null) {
						empresaEmisora = new Empresa(envioCFEEntreEmpresas.getCaratula().getRUCEmisor(), null, null);
						empresaEmisora.save();
					}
					sobreRecibido.setEmpresaEmisora(empresaEmisora);
					sobreRecibido.setEnvioCFEEntreEmpresas(envioCFEEntreEmpresas);
					sobreRecibido.setNombreArchivo(attachment.getName());
					sobreRecibido.setXmlEmpresa(attachment.getPayload());
					sobreRecibido.save();
					sobreRecibido.getEmails().add(attachment.getEmailMessage());

					/*
					 * PROCESO SOBRE
					 */
					intercambioMicroController.procesarSobre(this.getEmpresa(), sobreRecibido, document);
					sobreRecibido.update();

					/*
					 * PROCESO CFE DENTRO DE SOBRE
					 */
					if (sobreRecibido.getEstadoEmpresa()==EstadoACKSobreType.AS){
						intercambioMicroController.procesarCFESobre(this.getEmpresa(), sobreRecibido);
						sobreRecibido.update();
					}

				}else{
					//El sobre existe no lo vuelvo a procesar
					boolean encontre = false;
					for (Iterator<EmailMessage> iterator = sobreRecibido.getEmails().iterator(); iterator.hasNext();) {
						EmailMessage email = iterator.next();
						if (email.getId() == attachment.getEmailMessage().getId())
							encontre = true;
					}
					
					if (!encontre) {
						sobreRecibido.getEmails().add(attachment.getEmailMessage());
						sobreRecibido.update();
					}
				}
				
				
			}
			attachment.setEstado(AttachmentEstado.PROCESADO_OK);
			attachment.update();
			/*
			 * Para que se guarden en la BBDD los cambios referentes al procesamiento del ultimo attachment
			 */
			ThreadMan.forceTransactionFlush();
			

		} catch (APIException | Exception e) {
			logger.error(index+"-APIException is: ", e);
			/*
			 * si hubo una excepcion en el procesamiento del attachment debo rollbaquear y setear el estado del attachment a PROCESADO_ERROR
			 */
			play.db.jpa.JPA.em().getTransaction().rollback();
			play.db.jpa.JPA.em().getTransaction().begin();
			Attachment attachmentFromBD = Attachment.findById(attachmentId);
			attachmentFromBD.setEstado(AttachmentEstado.PROCESADO_ERROR);
			attachmentFromBD.update();
			ThreadMan.forceTransactionFlush();
			/*
			 * Vuelvo a cargar la empresa para que este en el persistence context
			 */
			this.setEmpresa(Empresa.findByRUT(this.getEmpresa().getRut()));
			
			/*
			 * Agrego el Email a la lista de emails Erroneos de la empresa;
			 */
			boolean agregar = true;
			for(EmailMessage email : this.getEmpresa().getEmailsRecibidosError()) {
				if (attachmentFromBD.getEmailMessage().getId() == email.getId()) {
					agregar = false;
					break;
				}
			}
			if (agregar)
				this.getEmpresa().getEmailsRecibidosError().add(EmailMessage.findById(attachmentFromBD.getEmailMessage().getId()));
			
			ThreadMan.forceTransactionFlush();
			
			/*
			 * Envio mail a los administradores notificando del error
			 */
			Map<String, byte[]> attachments = new TreeMap<String, byte[]>();
			attachments.put(attachmentFromBD.getName(), attachmentFromBD.getPayload().getBytes());
			String fullStackTrace = org.apache.commons.lang.exception.ExceptionUtils.getFullStackTrace(e);
			new MessagingHelper()
				.withCustomConfig(this.getEmpresa().getFromEnvio(), this.getEmpresa().getHostRecepcion(), Integer.parseInt(this.getEmpresa().getPuertoRecepcion()),
						this.getEmpresa().getUserRecepcion(), this.getEmpresa().getPassRecepcion())
				.withAttachment(attachments)
				.sendEmail(this.getEmpresa().getMailNotificaciones(), fullStackTrace, null, "Error Procesando Archivo Recibido", false);
			
		}
	}

	private void setEmpresa(Attachment attachment) {
		if (attachment.getEmailMessage().getEmpresa()==null) {
			attachment.getEmailMessage().setEmpresa(this.getEmpresa());
			if (attachment.getEmailMessage().getId()!=0) {
				/*
				 *  Es una instancia que ya fue salvada, por lo tanto le hago un update. Sino tiene id fue creada
				 *  en esta ejecucion y ya se guardaran los cambios porque se hizo un save antes.
				 */
				attachment.getEmailMessage().update();
			}
		}
	}

	@Override
	public void enviarCfeEmpresa(CFE cfe) throws APIException{
		recepcionService.enviarCorreoReceptorElectronico(cfe);
	}

	@Override
	public void enviarCorreoReceptorElectronico(SobreEmitido sobre) throws APIException {
		recepcionService.enviarCorreoReceptorElectronico(sobre);
	}

	

}
