package com.bluedot.efactura.microControllers.implementation;

import java.util.Date;
import java.util.List;

import org.w3c.dom.Document;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.commons.utils.Email;
import com.bluedot.commons.utils.EmailAttachmentReceiver;
import com.bluedot.commons.utils.ThreadMan;
import com.bluedot.commons.utils.XML;
import com.bluedot.efactura.microControllers.interfaces.CAEMicroController;
import com.bluedot.efactura.microControllers.interfaces.ServiceMicroController;
import com.bluedot.efactura.model.CFE;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.model.ReporteDiario;
import com.bluedot.efactura.model.SobreEmitido;
import com.bluedot.efactura.model.SobreRecibido;
import com.bluedot.efactura.services.IntercambioService;
import com.bluedot.efactura.services.RecepcionService;
import com.bluedot.efactura.services.impl.IntercambioServiceImpl;
import com.bluedot.efactura.strategy.builder.CFEBuiderInterface;
import com.bluedot.efactura.strategy.builder.CFEBuilderFactory;

import dgi.classes.entreEmpresas.EnvioCFEEntreEmpresas;
import dgi.classes.respuestas.cfe.EstadoACKCFEType;
import dgi.classes.respuestas.sobre.EstadoACKSobreType;
import play.libs.F.Promise;
import play.mvc.Result;

public class ServiceMicroControllerDefault extends MicroControllerDefault implements ServiceMicroController {

	private RecepcionService recepcionService;
	private CAEMicroController caeMicroController;

	public ServiceMicroControllerDefault(RecepcionService recepcionService, Empresa empresa, CAEMicroController caeMicroController) {
		super(empresa);
		this.recepcionService = recepcionService;
		this.caeMicroController = caeMicroController;
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

		IntercambioService service = new IntercambioServiceImpl();

		EmailAttachmentReceiver receiver = new EmailAttachmentReceiver();
		List<Email> emails = receiver.downloadEmail(empresa.getHostRecepcion(),
				110, empresa.getUserRecepcion(),
				empresa.getPassRecepcion());

		for (Email email : emails)

		{
			for (String attachmentName : email.getAttachments().keySet()) {
				try {
					String attachment = email.getAttachments().get(attachmentName);

					Document document = XML.loadXMLFromString(attachment);

					EnvioCFEEntreEmpresas envioCFEEntreEmpresas = (EnvioCFEEntreEmpresas) XML.unMarshall(document,
							EnvioCFEEntreEmpresas.class);

					Empresa empresaReceptoraCandidata = Empresa
							.findByRUT(envioCFEEntreEmpresas.getCaratula().getRutReceptor(), true);

					if (empresaReceptoraCandidata.getId() != empresa.getId())
						break;

					/*
					 * Create el SobreRecibido
					 */
					SobreRecibido sobreRecibido = new SobreRecibido();
					sobreRecibido.setEmpresaReceptora(empresa);
					Empresa empresaEmisora = Empresa.findByRUT(envioCFEEntreEmpresas.getCaratula().getRUCEmisor());
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

					service.procesarSobre(sobreRecibido);
					sobreRecibido.update();
					ThreadMan.forceTransactionFlush();

					
					service.procesarCFESobre(sobreRecibido);
					sobreRecibido.update();
					ThreadMan.forceTransactionFlush();

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
