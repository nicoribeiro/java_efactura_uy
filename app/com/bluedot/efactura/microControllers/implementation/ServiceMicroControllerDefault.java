package com.bluedot.efactura.microControllers.implementation;

import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.commons.security.EmailMessage;
import com.bluedot.commons.utils.DateHandler;
import com.bluedot.commons.utils.ThreadMan;
import com.bluedot.commons.utils.XML;
import com.bluedot.commons.utils.messaging.Attachment;
import com.bluedot.commons.utils.messaging.Email;
import com.bluedot.commons.utils.messaging.EmailAttachmentReceiver;
import com.bluedot.efactura.microControllers.interfaces.CAEMicroController;
import com.bluedot.efactura.microControllers.interfaces.IntercambioMicroController;
import com.bluedot.efactura.microControllers.interfaces.ServiceMicroController;
import com.bluedot.efactura.model.CFE;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.model.ReporteDiario;
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
					sobreEmitido.setEstado(EstadoACKSobreType.fromValue(sobre.getEstadoSobre()));
					sobreEmitido.setIdReceptor(sobre.getIdReceptor());
					if (sobreEmitido.getEstado()==EstadoACKSobreType.AS){
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
		if (cfe.getSobre()!=null && cfe.getSobre().getEstado() != null && cfe.getSobre().getEstado()== EstadoACKSobreType.BS)
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
		//TODO terminar
		// TODO mutex

		EmailAttachmentReceiver receiver = new EmailAttachmentReceiver();
		
		for (int i = 1; i < 5000; i=i+100) {
			List<Email> emails = receiver.downloadEmail("imap", empresa.getHostRecepcion(),
					empresa.getPuertoRecepcion(), empresa.getUserRecepcion(),
					empresa.getPassRecepcion(),i, i+99);
			procesarEmails(emails);
		}
		
		
		
	}
	
	private void procesarEmails(List<Email> emails) throws APIException {
	
		for (Email email : emails)

		{
			/*
			 * Convierto El datatype Email a un Email del modelo
			 */
			EmailMessage emailModel = new EmailMessage(email);
			
			if (EmailMessage.findByMessageId(emailModel.getMessageId()).size()>0){
				logger.info("Este Email ya fue procesado messageId:" + emailModel.getMessageId());
				continue;
			}
			
			emailModel.save();
			
			for (Attachment attachment : email.getAttachments()) {
				try {
					
					logger.info("Procesando Adjunto: " + attachment.getName());
					
					Document document = XML.loadXMLFromString(attachment.getPayload());
					
					logger.debug("Attachment: " + attachment.getPayload());
					
					/*
					 * 1 - Es la respuesta de los CFE dentro de un SobreEmitido (respuesta de aceptacion)
					 */
					if (attachment.getName().substring(0, 3).equalsIgnoreCase("ME_")){
						ACKCFEdefType ackCFEdefType = (ACKCFEdefType) XML.unMarshall(document,
								ACKCFEdefType.class);
						Sobre sobre = SobreEmitido.findById(ackCFEdefType.getCaratula().getIDEmisor().longValue(), true);
						
						if (sobre instanceof SobreEmitido){
							SobreEmitido sobreEmitido = (SobreEmitido) sobre;
							sobreEmitido.setResultado_empresa(attachment.getPayload());
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
							sobreEmitido.setRespuesta_empresa(attachment.getPayload());
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
								empresaEmisora = new Empresa(envioCFEEntreEmpresas.getCaratula().getRUCEmisor(), null, null, null, null, null, 0);
								empresaEmisora.save();
							}
							sobreRecibido.setEmpresaEmisora(empresaEmisora);
							sobreRecibido.setEnvioCFEEntreEmpresas(envioCFEEntreEmpresas);
							sobreRecibido.setNombreArchivo(attachment.getName());
							sobreRecibido.setXmlEmpresa(attachment.getPayload());
							sobreRecibido.save();
							sobreRecibido.getEmails().add(emailModel);
							ThreadMan.forceTransactionFlush();

							/*
							 * PROCESO SOBRE
							 */
							intercambioMicroController.procesarSobre(empresa, sobreRecibido);
							sobreRecibido.update();
							ThreadMan.forceTransactionFlush();

							/*
							 * PROCESO CFE DENTRO DE SOBRE
							 */
							if (sobreRecibido.getEstado()==EstadoACKSobreType.AS){
								intercambioMicroController.procesarCFESobre(empresa, sobreRecibido);
								sobreRecibido.update();
								ThreadMan.forceTransactionFlush();
							}

						}else{
							sobreRecibido.getEmails().add(emailModel);
							sobreRecibido.update();
							ThreadMan.forceTransactionFlush();
						}
						
						
					}
					
					

				} catch (APIException e) {
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}

			}

		}


	}

	@Override
	public void enviarMailEmpresa(CFE cfe) throws APIException{
		recepcionService.enviarMailEmpresa(cfe);
	}

	

}
