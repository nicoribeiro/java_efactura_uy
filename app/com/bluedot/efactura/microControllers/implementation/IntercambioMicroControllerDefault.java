package com.bluedot.efactura.microControllers.implementation;

import java.math.BigInteger;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.commons.utils.XML;
import com.bluedot.efactura.interceptors.SignatureInterceptor;
import com.bluedot.efactura.microControllers.interfaces.CAEMicroController;
import com.bluedot.efactura.microControllers.interfaces.CFEMicroController;
import com.bluedot.efactura.microControllers.interfaces.IntercambioMicroController;
import com.bluedot.efactura.model.CFE;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.model.FirmaDigital;
import com.bluedot.efactura.model.MotivoRechazoCFE;
import com.bluedot.efactura.model.MotivoRechazoSobre;
import com.bluedot.efactura.model.SobreRecibido;
import com.bluedot.efactura.model.TipoDoc;
import com.bluedot.efactura.serializers.EfacturaJSONSerializerProvider;
import com.bluedot.efactura.services.ConsultasService;
import com.bluedot.efactura.services.RecepcionService;
import com.bluedot.efactura.strategy.builder.CFEBuilderFactory;

import dgi.classes.entreEmpresas.CFEEmpresasType;
import dgi.classes.entreEmpresas.EnvioCFEEntreEmpresas;
import dgi.classes.respuestas.cfe.ACKCFEdefType;
import dgi.classes.respuestas.cfe.ACKCFEdefType.ACKCFEDet;
import dgi.classes.respuestas.cfe.EstadoACKCFEType;
import dgi.classes.respuestas.cfe.RechazoCFEDGIType;
import dgi.classes.respuestas.sobre.ACKSobredefType;
import dgi.classes.respuestas.sobre.ACKSobredefType.Caratula;
import dgi.classes.respuestas.sobre.ACKSobredefType.Detalle;
import dgi.classes.respuestas.sobre.EstadoACKSobreType;
import dgi.classes.respuestas.sobre.ParamConsultaType;
import dgi.classes.respuestas.sobre.RechazoSobreType;

public class IntercambioMicroControllerDefault implements IntercambioMicroController {

	final static Logger logger = LoggerFactory.getLogger(IntercambioMicroControllerDefault.class);
	
	private CFEMicroController cfeMicroController;

	public IntercambioMicroControllerDefault(CFEMicroController cfeMicroController) {
		this.cfeMicroController = cfeMicroController;
	}
	
	
	@Override
	public ACKSobredefType procesarSobre(Empresa empresa, SobreRecibido sobreRecibido) throws APIException {
		
	
			
			try {
				sobreRecibido.setTimestampRecibido(new Date());
				
				
				
				EnvioCFEEntreEmpresas envioCFEEntreEmpresas = sobreRecibido.getEnvioCFEEntreEmpresas(); 
				sobreRecibido.setIdEmisor(envioCFEEntreEmpresas.getCaratula().getIdemisor().longValue());
				sobreRecibido.setFecha(envioCFEEntreEmpresas.getCaratula().getFecha().toGregorianCalendar().getTime());
				
				ACKSobredefType ackSobredefType = new ACKSobredefType();
				ackSobredefType.setVersion("1.0");
				
				
				/*
				 * Caratula
				 */
				Caratula caratula = new Caratula();
				caratula.setCantidadCFE(envioCFEEntreEmpresas.getCaratula().getCantCFE());
				caratula.setFecHRecibido(DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()));
				caratula.setTmst(DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()));
				caratula.setIDReceptor(BigInteger.valueOf(sobreRecibido.getId()));
				//TODO cambiar este id
				caratula.setIDRespuesta(new BigInteger("1"));
				caratula.setIDEmisor(envioCFEEntreEmpresas.getCaratula().getIdemisor());
				caratula.setNomArch(sobreRecibido.getNombreArchivo());
				caratula.setRUCReceptor(sobreRecibido.getEmpresaReceptora().getRut());
				caratula.setRUCEmisor(envioCFEEntreEmpresas.getCaratula().getRUCEmisor());
				ackSobredefType.setCaratula(caratula);
				ackSobredefType.setDetalle(new Detalle());
				
				//TODO terminar de implementar controles que faltan
				/*
				 * CONTROLES SOBRE
				 * 
				 * S01 Formato del archivo no es el indicado
				 * 
				 * S02 No coincide RUC de Sobre, Certificado, envío o CFE
				 * 
				 * S03 Certificado electrónico no es válido
				 * 
				 * S04 No cumple validaciones según Formato de sobre
				 * 
				 * S05 No coinciden cantidad CFE de carátula y contenido
				 * 
				 * S06 No coinciden certificado de sobre y comprobantes
				 * 
				 * S07 Sobre enviado supera el tamaño máximo admitido
				 * 
				 * S08 Ya existe sobre con el mismo idEmisor
				 * 
				 */

				/*
				 * Controlo que no exista el sobre en mi sistema (para evitar envios dobles)
				 */
				List<SobreRecibido> sobres = SobreRecibido.findSobreRecibido(envioCFEEntreEmpresas.getCaratula().getIdemisor().longValue(), sobreRecibido.getEmpresaEmisora(),sobreRecibido.getEmpresaReceptora());
				/*
				 * S08
				 */
				if (sobres.size()>1){
					RechazoSobreType rechazo = new RechazoSobreType();
					rechazo.setMotivo(MotivoRechazoSobre.S08.name());
					rechazo.setGlosa("Ya existe sobre con idEmisor:" + envioCFEEntreEmpresas.getCaratula().getIdemisor());
					ackSobredefType.getDetalle().getMotivosRechazo().add(rechazo);
				}
					
				/*
				 * S02
				 */
				if (!envioCFEEntreEmpresas.getCaratula().getRutReceptor().equals(empresa.getRut())){
					RechazoSobreType rechazo = new RechazoSobreType();
					rechazo.setMotivo(MotivoRechazoSobre.S02.name());
					rechazo.setGlosa("No coincide RUC de Sobre, Certificado, envío o CFE");
					ackSobredefType.getDetalle().getMotivosRechazo().add(rechazo);
				}
				
				/*
				 * S05
				 */
				if (envioCFEEntreEmpresas.getCaratula().getCantCFE() != envioCFEEntreEmpresas.getCFEAdendas().size()){
					RechazoSobreType rechazo = new RechazoSobreType();
					rechazo.setMotivo(MotivoRechazoSobre.S05.name());
					rechazo.setGlosa("No coinciden cantidad CFE de carátula y contenido");
					ackSobredefType.getDetalle().getMotivosRechazo().add(rechazo);
				}
				
				
				if (ackSobredefType.getDetalle().getMotivosRechazo().size()==0){
					/*
					 * Se acepta el sobre!
					 */
					sobreRecibido.setEstadoEmpresa(EstadoACKSobreType.AS);
					ackSobredefType.getDetalle().setEstado(EstadoACKSobreType.AS);
					ParamConsultaType params = new ParamConsultaType();
					params.setFechahora(DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()));
					params.setToken(UUID.randomUUID().toString());
					ackSobredefType.getDetalle().setParamConsulta(params);
				}else{
					/*
					 * Se rechaza el sobre!
					 */
					sobreRecibido.setEstadoEmpresa(EstadoACKSobreType.BS);
					ackSobredefType.getDetalle().setEstado(EstadoACKSobreType.BS);
				}
				
				
				/*
				 * Instantiate the DocumentBuilderFactory.
				 * IMPORTANT: NamespaceAwerness=true!!
				 */
				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				dbf.setNamespaceAware(true);
				
				Document allDocument = XML.marshall(ackSobredefType);

				allDocument = SignatureInterceptor.signDocument(dbf, allDocument,"ACKSobre",null, empresa.getFirmaDigital().getKeyStore(), FirmaDigital.KEY_ALIAS, FirmaDigital.KEYSTORE_PASSWORD);
				
				sobreRecibido.setRespuesta_empresa(XML.documentToString(allDocument));
				
				//TODO enviar correo a la empresa con el resultado primario (el secundario debe pasar por administracion de la empresa)
				
				sobreRecibido.setAckSobredefType(ackSobredefType);
				
				return ackSobredefType;
			} catch (TransformerFactoryConfigurationError | Exception e) {
				throw APIException.raise(e);
			}
			
		
	}

	@Override
	public ACKCFEdefType procesarCFESobre(Empresa empresa, SobreRecibido sobreRecibido) throws APIException {
		
			try {
				sobreRecibido.setTimestampProcesado(new Date());
				
				EnvioCFEEntreEmpresas envioCFEEntreEmpresas = sobreRecibido.getEnvioCFEEntreEmpresas(); 
				String filename = sobreRecibido.getNombreArchivo();
				ACKSobredefType ackSobredefType = sobreRecibido.getAckSobredefType();
				
				sobreRecibido.setCantComprobantes(envioCFEEntreEmpresas.getCFEAdendas().size());
				
				if (ackSobredefType.getDetalle().getMotivosRechazo().size()==0){
					
					ACKCFEdefType ackcfEdefType = new ACKCFEdefType();
					addCaratula(ackcfEdefType, envioCFEEntreEmpresas, filename);
					ackcfEdefType.setVersion("1.0");
					
					/*
					 * Proceso uno a uno todos los CFE dentro del sobre
					 */
					int i = 1;
					for (Iterator<CFEEmpresasType> iterator = envioCFEEntreEmpresas.getCFEAdendas().iterator(); iterator.hasNext();) {
						CFEEmpresasType cfeEmpresasTypee = iterator.next();
						procesarCFE(cfeEmpresasTypee, ackcfEdefType, new BigInteger(String.valueOf(i)));
						i++;
					}
					
					/*
					 * Serializo el XML respuesta
					 */
					DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
					dbf.setNamespaceAware(true);
					Document allDocument = XML.marshall(ackcfEdefType);
					allDocument = SignatureInterceptor.signDocument(dbf, allDocument,null,null, empresa.getFirmaDigital().getKeyStore(), FirmaDigital.KEY_ALIAS, FirmaDigital.KEYSTORE_PASSWORD);
					sobreRecibido.setResultado_empresa(XML.documentToString(allDocument));
					
					return ackcfEdefType;
				}
				return null;
			} catch (TransformerFactoryConfigurationError | Exception e) {
				throw APIException.raise(e);
			}
		
		
		//TODO si los servicios son la capa superior nunca deberian tirar exepciones distintas de APIException
	}

	private void procesarCFE(CFEEmpresasType cfeEmpresasType, ACKCFEdefType ackcfEdefType, BigInteger ordinal) throws APIException {
		
		/*
		 * Serializo el CFEEmpresasType a JSONObject
		 */
		JSONObject cfeJson = EfacturaJSONSerializerProvider.getCFEEmpresasTypeSerializer().objectToJson(cfeEmpresasType);
		logger.debug("cfeJson: " + cfeJson.toString());
		
		TipoDoc tipoDoc = TipoDoc.fromInt(cfeJson.getJSONObject("Encabezado").getJSONObject("IdDoc").getInt("TipoCFE"));
		
		/*
		 * Creo un CFE de mi modelo
		 */
 		CFE cfe = cfeMicroController.create(tipoDoc, cfeJson, false);
 		
		
 		/*
 		 * Por defecto se acepta, luego en los controles se cambia de estado si corresponde
 		 */
 		cfe.setEstado(EstadoACKCFEType.AE);
 		RechazoCFEDGIType rechazo = null;
 		
		
 		
 		/*
		 * CONTROLES CFE
		 * 
		 * E02 Tipo y No de CFE ya existe en los registros
		 * 
		 * E03 Tipo y No de CFE no se corresponden con el CAE
		 * 
		 * E04 Firma electrónica no es válida
		 * 
		 * E05 No cumple validaciones (*) de Formato comprobantes
		 * 
		 * E07 Fecha Firma de CFE no se corresponde con fecha CAE
		 */
		
		/*
		 * E02
		 */
 		List<CFE> cfes = CFE.findById(cfe.getEmpresaEmisora(), cfe.getTipo(), cfe.getSerie(), cfe.getNro(), EstadoACKCFEType.AE, false);

 		if (cfes.size()>1)
			throw APIException.raise(APIErrors.CFE_NO_ENCONTRADO).setDetailMessage("RUT+NRO+SERIE+TIPODOC no identifica a un unico cfe");
 		
 		if (cfes.size()==1){
	 		rechazo = new RechazoCFEDGIType();
			rechazo.setMotivo("E02");
			rechazo.setGlosa("Tipo y No de CFE ya existe en los registros");
			cfe.getMotivo().add(MotivoRechazoCFE.E02);
			cfe.setEstado(EstadoACKCFEType.BE);
 		}
 		
 		//TODO estos controles
 		/*
		 * E03
		 */
 		
 		/*
		 * E04
		 */
 		
 		/*
		 * E05
		 */
 		
 		/*
		 * E07
		 */
		
		/*
		 * Genero la respuesta
		 */
		ACKCFEDet respuestaCFE = buildAck(ordinal, tipoDoc, cfe, rechazo);
		ackcfEdefType.getACKCFEDet().add(respuestaCFE);
		
		/*
		 * Registro el resultado
		 */
		sumarizarResultado(cfe.getEstado(), ackcfEdefType);
		
		
		cfe.save();
	}


	/**
	 * @param ordinal
	 * @param tipoDoc
	 * @param cfe
	 * @return
	 * @throws APIException 
	 * @throws DatatypeConfigurationException
	 */
	private ACKCFEDet buildAck(BigInteger ordinal, TipoDoc tipoDoc, CFE cfe, RechazoCFEDGIType rechazo) throws APIException {
		ACKCFEDet ack = new ACKCFEDet();
		ack.setNroOrdinal(ordinal);
		ack.setNroCFE(new BigInteger(String.valueOf(cfe.getNro())));
		ack.setSerie(cfe.getSerie());
		
		GregorianCalendar cal = new GregorianCalendar();
		cal.setTime(cfe.getFecha());
		try {
			ack.setFechaCFE(DatatypeFactory.newInstance().newXMLGregorianCalendar(cal));
		} catch (DatatypeConfigurationException e) {
			throw APIException.raise(e);
		}
		
		ack.setTipoCFE(new BigInteger(String.valueOf(tipoDoc.value)));

		if (rechazo!=null)
			ack.getMotivosRechazoCF().add(rechazo);
		
		ack.setEstado(cfe.getEstado());
		
		return ack;
	}

	private void sumarizarResultado(EstadoACKCFEType estado, ACKCFEdefType ackcfEdefType) {
		
		ackcfEdefType.getCaratula().setCantenSobre(ackcfEdefType.getCaratula().getCantenSobre().add(new BigInteger("1")));
		ackcfEdefType.getCaratula().setCantResponden(ackcfEdefType.getCaratula().getCantResponden().add(new BigInteger("1")));
		
		
		
		switch (estado) {
		case AE:
			ackcfEdefType.getCaratula().setCantCFEAceptados(ackcfEdefType.getCaratula().getCantCFEAceptados().add(new BigInteger("1")));
			break;
		case BE:
			ackcfEdefType.getCaratula().setCantCFERechazados(ackcfEdefType.getCaratula().getCantCFERechazados().add(new BigInteger("1")));
			break;
		case CE:
			//TODO esto es de uso exclusivo de la DGI chequear.
			break;
		}
		
	}

	private void addCaratula(ACKCFEdefType ackcfEdefType, EnvioCFEEntreEmpresas envioCFEEntreEmpresas, String filename) throws DatatypeConfigurationException {
		dgi.classes.respuestas.cfe.ACKCFEdefType.Caratula caratula = new dgi.classes.respuestas.cfe.ACKCFEdefType.Caratula();

		caratula.setFecHRecibido(DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()));
		caratula.setTmst(DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()));
		//TODO hace un incremental aca
		caratula.setIDReceptor(new BigInteger("1"));
		caratula.setIDRespuesta(new BigInteger("1"));
		caratula.setIDEmisor(envioCFEEntreEmpresas.getCaratula().getIdemisor());
		caratula.setNomArch(filename);
		//TODO sacar RUT para afuera
		caratula.setRUCReceptor("215071660012");
		caratula.setRUCEmisor(envioCFEEntreEmpresas.getCaratula().getRUCEmisor());
		
		caratula.setCantenSobre(BigInteger.ZERO);
		caratula.setCantResponden(BigInteger.ZERO);
		caratula.setCantCFCAceptados(BigInteger.ZERO);
		caratula.setCantCFCObservados(BigInteger.ZERO);
		caratula.setCantCFEAceptados(BigInteger.ZERO);
		caratula.setCantCFERechazados(BigInteger.ZERO);
		caratula.setCantOtrosRechazados(BigInteger.ZERO);
		
		ackcfEdefType.setCaratula(caratula);
		
	}

}
