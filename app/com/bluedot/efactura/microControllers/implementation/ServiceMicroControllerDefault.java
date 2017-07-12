package com.bluedot.efactura.microControllers.implementation;

import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
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
		
		recepcionService.sendCFE(cfe);

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
		
		List<SobreEmitido> sobres = SobreEmitido.findByEmpresaEmisoraAndDate(empresa, date);

		for (Iterator<SobreEmitido> iterator = sobres.iterator(); iterator.hasNext();) {
			SobreEmitido sobre = iterator.next();
			consultaResultado((SobreEmitido) sobre);
		}
	}

	@Override
	public SobreEmitido consultaResultado(SobreEmitido sobreEmitido) throws APIException {
		
		if (sobreEmitido.getToken() == null || sobreEmitido.getIdReceptor() == null){
			ACKConsultaEnviosSobre respuesta = consultasService.consultarEnvioSobre(sobreEmitido.getId(), 0, DateHandler.minus(sobreEmitido.getFecha(), 5, Calendar.DAY_OF_MONTH), DateHandler.add(sobreEmitido.getFecha(), 5, Calendar.DAY_OF_MONTH));
			
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
		
		recepcionService.consultaResultadoSobre(sobreEmitido);
		return sobreEmitido;
	}

	@Override
	public ReporteDiario generarReporteDiario(Date date) throws APIException {
		return recepcionService.generarReporteDiario(date, empresa);
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
	/**
	 * Hay 3 tipos de correos que pueden llegar a ser validos
	 * 
	 * 1 - Respuesta a un sobre con CFE emitidos por una empresa manejada por este sistema. 
	 * 2 - Respuesta a la recepcion de un SobreEmitido
	 * 3 - Un sobre con CFE emitidos por otra empresa hacia una empresa manejada por el sistema.
	 */
	@Override
	public void getDocumentosEntrantes() throws APIException {
		// TODO mutex

		EmailAttachmentReceiver receiver = new EmailAttachmentReceiver();
		
		int offset = empresa.getOffsetMail()==0 ? 1 : empresa.getOffsetMail();
		int messageQuantity = 100;
		List<Email> emails = null;
		do {
			
			emails = receiver.downloadEmail("imap", empresa.getHostRecepcion(),
					empresa.getPuertoRecepcion(), empresa.getUserRecepcion(),
					empresa.getPassRecepcion(), offset, messageQuantity);
			
			try {
				procesarEmails(emails);
				offset += emails.size();
				empresa.setOffsetMail(offset);
				empresa.update();
				ThreadMan.forceTransactionFlush();
			} catch (APIException e) {
				throw e;
			} catch (Exception e) {
				throw APIException.raise(e);
			}
			
		} while (emails.size() == messageQuantity);
		
	}
	
	private void procesarEmails(List<Email> emails) throws APIException, Exception {
	
		for (Email email : emails)

		{
			
			if (EmailMessage.findByMessageId(email.getMessageId()).size()>0){
				logger.info("Este Email ya fue procesado messageId:" + email.getMessageId());
				continue;
			}
			
			/*
			 * Convierto El datatype Email a un Email del modelo y lo persisto junto con sus Attachments
			 */
			EmailMessage emailModel = new EmailMessage(email);
			emailModel.save();
			ThreadMan.forceTransactionFlush();
			
			logger.info("Procesado email: " + email.getMessageId());
			logger.info("Email contiene " + email.getAttachments().size() + " attachments");
			
			for (Attachment attachment : emailModel.getAttachments()) {
				try {
					
					/*
					 * Si hubo una ecepcion las entidades se desatachearon del persistence context con el rollback.
					 * Por lo tanto, como no estoy seguro si estan o no debo volverlas a pedir a la BBDD.
					 */
					attachment = Attachment.findById(attachment.getId());
					
					logger.info("Procesando Adjunto: " + attachment.getName());
					
					Document document = XML.loadXMLFromString(attachment.getPayload());
					
					logger.debug("Attachment payload: " + attachment.getPayload());
					
					/*
					 * 1 - Es la respuesta de los CFE dentro de un SobreEmitido (respuesta de aceptacion)
					 */
					if (attachment.getName().substring(0, 3).equalsIgnoreCase("ME_")){
						ACKCFEdefType ackCFEdefType = (ACKCFEdefType) XML.unMarshall(document,
								ACKCFEdefType.class);
						Sobre sobre = SobreEmitido.findById(ackCFEdefType.getCaratula().getIDEmisor().longValue(), true);
						
						if (sobre instanceof SobreEmitido){
							SobreEmitido sobreEmitido = (SobreEmitido) sobre;
							
							Respuesta respuestaCfe = new Respuesta();
							sobreEmitido.setRespuestaCfes(respuestaCfe);
							respuestaCfe.setNombreArchivo(attachment.getName());
							respuestaCfe.setPayload(attachment.getPayload());
							respuestaCfe.save();
							sobreEmitido.update();
							
							
						}
					}
					
					/*
					 * 2 - Es una respuesta a la recepcion de un SobreEmitido (respuesta de recepcion)
					 */
					if (attachment.getName().substring(0, 2).equalsIgnoreCase("M_")){
						ACKSobredefType ackSobredefType = (ACKSobredefType) XML.unMarshall(document,
								ACKSobredefType.class);
						Sobre sobre = SobreEmitido.findById(ackSobredefType.getCaratula().getIDEmisor().longValue(), true);
						
						if (sobre instanceof SobreEmitido){
							SobreEmitido sobreEmitido = (SobreEmitido) sobre;
							sobreEmitido.setEstadoEmpresa(ackSobredefType.getDetalle().getEstado());
							
							Respuesta respuestaSobre = new Respuesta();
							sobreEmitido.setRespuestaSobre(respuestaSobre);
							respuestaSobre.setNombreArchivo(attachment.getName());
							respuestaSobre.setPayload(attachment.getPayload());
							respuestaSobre.save();
							sobreEmitido.update();
						}
						
					}
					
					/*
					 * 3 - Es un sobre con CFE adentro
					 */
					if (attachment.getName().substring(0, 3).equalsIgnoreCase("Sob")){

						EnvioCFEEntreEmpresas envioCFEEntreEmpresas = (EnvioCFEEntreEmpresas) XML.unMarshall(document,
								EnvioCFEEntreEmpresas.class);

						Empresa empresaReceptoraCandidata = Empresa
								.findByRUT(envioCFEEntreEmpresas.getCaratula().getRutReceptor(), true);

						if (empresaReceptoraCandidata.getId() != empresa.getId())
							break;

						List<Sobre> sobres = SobreRecibido.findByNombre(attachment.getName());
						
						SobreRecibido sobreRecibido=null;
						
						if (sobres.size()>0 && sobres.get(0) instanceof SobreRecibido)
							sobreRecibido = (SobreRecibido) sobres.get(0);
						
						if (sobreRecibido==null){
							/*
							 * Create el SobreRecibido
							 */
							sobreRecibido = new SobreRecibido();
							sobreRecibido.setEmpresaReceptora(empresa);
							Empresa empresaEmisora = Empresa.findByRUT(envioCFEEntreEmpresas.getCaratula().getRUCEmisor());
							if (empresaEmisora == null) {
								empresaEmisora = new Empresa(envioCFEEntreEmpresas.getCaratula().getRUCEmisor(), null, null, null, null, null);
								empresaEmisora.save();
							}
							sobreRecibido.setEmpresaEmisora(empresaEmisora);
							sobreRecibido.setEnvioCFEEntreEmpresas(envioCFEEntreEmpresas);
							sobreRecibido.setNombreArchivo(attachment.getName());
							sobreRecibido.setXmlEmpresa(attachment.getPayload());
							sobreRecibido.save();
							sobreRecibido.getEmails().add(emailModel);

							/*
							 * PROCESO SOBRE
							 */
							intercambioMicroController.procesarSobre(empresa, sobreRecibido, document);
							sobreRecibido.update();

							/*
							 * PROCESO CFE DENTRO DE SOBRE
							 */
							if (sobreRecibido.getEstadoEmpresa()==EstadoACKSobreType.AS){
								intercambioMicroController.procesarCFESobre(empresa, sobreRecibido);
								sobreRecibido.update();
							}

						}else{
							sobreRecibido.getEmails().add(emailModel);
							sobreRecibido.update();
						}
						
						
					}
					attachment.setEstado(AttachmentEstado.PROCESADO_OK);
					attachment.update();
					/*
					 * Para que se guarden en la BBDD los cambios referentes al procesamiento del ultimo attachment
					 */
					ThreadMan.forceTransactionFlush();
					

				} catch (APIException | Exception e) {
					logger.error("APIException is: ", e);
					/*
					 * si hubo una ecepcion en el procesamiento del attachment debo rollbaquear y setear el estado del attachment a PROCESADO_ERROR
					 */
					play.db.jpa.JPA.em().getTransaction().rollback();
					play.db.jpa.JPA.em().getTransaction().begin();
					Attachment attachmentFromBD = Attachment.findById(attachment.getId());
					attachmentFromBD.setEstado(AttachmentEstado.PROCESADO_ERROR);
					attachmentFromBD.update();
					ThreadMan.forceTransactionFlush();
					/*
					 * Vuelvo a cargar la empsa para que este en el persistence context
					 */
					empresa = Empresa.findByRUT(empresa.getRut());
					
					/*
					 * Envio mail a los administradores notificando del error
					 */
					Map<String, String> attachments = new TreeMap<String, String>();
					attachments.put(attachmentFromBD.getName(), attachmentFromBD.getPayload());
					String fullStackTrace = org.apache.commons.lang.exception.ExceptionUtils.getFullStackTrace(e);
					new MessagingHelper()
						.withCustomConfig(empresa.getFromEnvio(), empresa.getHostRecepcion(), Integer.parseInt(empresa.getPuertoRecepcion()),
								empresa.getUserRecepcion(), empresa.getPassRecepcion())
						.withAttachment(attachments)
						.sendEmail(empresa.getMailNotificaciones(), fullStackTrace, null, "Error Procesando Archivo Recibido", false);
					
				}

			}
			
			logger.info("Fin procesaminto email: " + email.getMessageId());
		}


	}

	@Override
	public void enviarMailEmpresa(CFE cfe) throws APIException{
		recepcionService.enviarMailEmpresa(cfe);
	}

	

}
