package com.bluedot.efactura.microControllers.implementation;

import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.commons.utils.Email;
import com.bluedot.commons.utils.EmailAttachmentReceiver;
import com.bluedot.commons.utils.ThreadMan;
import com.bluedot.commons.utils.XML;
import com.bluedot.efactura.microControllers.interfaces.ServiceMicroController;
import com.bluedot.efactura.model.CFE;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.model.ReporteDiario;
import com.bluedot.efactura.model.Sobre;
import com.bluedot.efactura.model.SobreEmitido;
import com.bluedot.efactura.model.SobreRecibido;
import com.bluedot.efactura.services.IntercambioService;
import com.bluedot.efactura.services.RecepcionService;
import com.bluedot.efactura.strategy.builder.CFEBuilder;
import com.bluedot.efactura.strategy.builder.CFEBuilderProvider;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import dgi.classes.entreEmpresas.EnvioCFEEntreEmpresas;
import dgi.classes.respuestas.cfe.ACKCFEdefType;
import dgi.classes.respuestas.cfe.EstadoACKCFEType;
import dgi.classes.respuestas.sobre.ACKSobredefType;
import dgi.classes.respuestas.sobre.EstadoACKSobreType;
import play.db.jpa.JPAApi;

public class ServiceMicroControllerDefault extends MicroControllerDefault implements ServiceMicroController {

	final static Logger logger = LoggerFactory.getLogger(ServiceMicroControllerDefault.class);
	
	private RecepcionService recepcionService;
	private CFEBuilderProvider cfeBuilderProvider;
	private IntercambioService intercambioService;
	private JPAApi jpaApi;

	@Inject
	public ServiceMicroControllerDefault(@Assisted Empresa empresa, RecepcionService recepcionService, CFEBuilderProvider cfeBuilderProvider, JPAApi jpaApi, IntercambioService intercambioService) {
		super(empresa);
		this.recepcionService = recepcionService;
		this.cfeBuilderProvider = cfeBuilderProvider;
		this.intercambioService = intercambioService;
		this.jpaApi = jpaApi;
	}	

	@Override
	public void enviar(CFE cfe) throws APIException {

		cfeBuilderProvider.setCfe(cfe);
		cfeBuilderProvider.setEmpresa(empresa);
		
		CFEBuilder builder = cfeBuilderProvider.get();
		//TODO mutex
		builder.asignarId();
		
		recepcionService.sendCFE(cfe);

	}
	
	@Override
	public void reenviar(SobreEmitido sobre) throws APIException {
		recepcionService.reenviarSobre(sobre);
	}

	@Override
	public void consultarResultados(Date date) throws APIException {
		recepcionService.consultarResultados(date, empresa);
	}

	@Override
	public SobreEmitido consultaResultado(SobreEmitido sobre) throws APIException {
		recepcionService.consultaResultadoSobre(sobre);
		return sobre;
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
	
	@Override
	public void getDocumentosEntrantes() throws APIException {
		//TODO terminar
		// TODO mutex

		

		EmailAttachmentReceiver receiver = new EmailAttachmentReceiver();
		List<Email> emails = receiver.downloadEmail(empresa.getHostRecepcion(),
				110, empresa.getUserRecepcion(),
				empresa.getPassRecepcion());

		for (Email email : emails)

		{
			for (String attachmentName : email.getAttachments().keySet()) {
				try {
					String attachment = email.getAttachments().get(attachmentName);

					logger.info("Procesando Adjunto: " + attachmentName);
					
					Document document = XML.loadXMLFromString(attachment);
					
					/*
					 * Es la respuesta de los CFE dentro de un SobreEmitido (respuesta de aceptacion)
					 */
					if (attachmentName.substring(0, 3).equalsIgnoreCase("ME_")){
						ACKCFEdefType ackCFEdefType = (ACKCFEdefType) XML.unMarshall(document,
								ACKCFEdefType.class);
						Sobre sobre = SobreEmitido.findById(jpaApi, ackCFEdefType.getCaratula().getIDEmisor().longValue(), true);
						
						if (sobre instanceof SobreEmitido){
							SobreEmitido sobreEmitido = (SobreEmitido) sobre;
							sobreEmitido.setResultado_empresa(attachment);
							sobreEmitido.update();
						}
					}
					
					/*
					 * Es una respuesta a la recepcion de un SobreEmitido (respuesta de recepcion)
					 */
					if (attachmentName.substring(0, 2).equalsIgnoreCase("M_")){
						ACKSobredefType ackSobredefType = (ACKSobredefType) XML.unMarshall(document,
								ACKSobredefType.class);
						Sobre sobre = SobreEmitido.findById(jpaApi, ackSobredefType.getCaratula().getIDEmisor().longValue(), true);
						
						if (sobre instanceof SobreEmitido){
							SobreEmitido sobreEmitido = (SobreEmitido) sobre;
							sobreEmitido.setRespuesta_empresa(attachment);
							sobreEmitido.update();
						}
						
					}
					
					/*
					 * Es un sobre con CFE adentro
					 */
					if (attachmentName.substring(0, 3).equalsIgnoreCase("Sob")){

						EnvioCFEEntreEmpresas envioCFEEntreEmpresas = (EnvioCFEEntreEmpresas) XML.unMarshall(document,
								EnvioCFEEntreEmpresas.class);

						Empresa empresaReceptoraCandidata = Empresa
								.findByRUT(jpaApi, envioCFEEntreEmpresas.getCaratula().getRutReceptor(), true);

						if (empresaReceptoraCandidata.getId() != empresa.getId())
							break;

						/*
						 * Create el SobreRecibido
						 */
						SobreRecibido sobreRecibido = new SobreRecibido();
						sobreRecibido.setEmpresaReceptora(empresa);
						Empresa empresaEmisora = Empresa.findByRUT(jpaApi, envioCFEEntreEmpresas.getCaratula().getRUCEmisor());
						if (empresaEmisora == null) {
							empresaEmisora = new Empresa(envioCFEEntreEmpresas.getCaratula().getRUCEmisor(), null, null, null, null, null, 0, null);
							empresaEmisora.save();
						}
						sobreRecibido.setEmpresaEmisora(empresaEmisora);
						sobreRecibido.setEnvioCFEEntreEmpresas(envioCFEEntreEmpresas);
						sobreRecibido.setNombreArchivo(attachmentName);
						sobreRecibido.setXmlEmpresa(attachment);
						sobreRecibido.save();
						ThreadMan.forceTransactionFlush();

						/*
						 * PROCESO SOBRE
						 */
						intercambioService.procesarSobre(empresa, sobreRecibido);
						sobreRecibido.update();
						ThreadMan.forceTransactionFlush();

						/*
						 * PROCESO CFE DENTRO DE SOBRE
						 */
						intercambioService.procesarCFESobre(empresa, sobreRecibido);
						sobreRecibido.update();
						ThreadMan.forceTransactionFlush();
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
