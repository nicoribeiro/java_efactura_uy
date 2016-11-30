package com.bluedot.efactura.services.impl;

import java.math.BigInteger;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.w3c.dom.Document;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.utils.XML;
import com.bluedot.efactura.commons.Commons;
import com.bluedot.efactura.interceptors.SignatureInterceptor;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.model.SobreRecibido;
import com.bluedot.efactura.services.IntercambioService;

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

public class IntercambioServiceImpl implements IntercambioService {

	private Commons commons;
	
	@Inject
	public void setCommons(Commons commons) {
		this.commons = commons;
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
				 */

				/*
				 * Controlo que no exista el sobre en mi sistema (para evitar envios dobles)
				 */
				List<SobreRecibido> sobres = SobreRecibido.findSobreRecibido(envioCFEEntreEmpresas.getCaratula().getIdemisor().longValue(), sobreRecibido.getEmpresaEmisora(),sobreRecibido.getEmpresaReceptora());
				// 1 porque el sobre actual ya fue persistido
				if (sobres.size()>1){
					RechazoSobreType rechazo = new RechazoSobreType();
					rechazo.setMotivo("S08");
					rechazo.setGlosa("Ya existe sobre con idEmisor:" + envioCFEEntreEmpresas.getCaratula().getIdemisor());
					ackSobredefType.getDetalle().getMotivosRechazo().add(rechazo);
				}
					
				/*
				 * S02
				 */
				if (!envioCFEEntreEmpresas.getCaratula().getRutReceptor().equals(empresa.getRut())){
					RechazoSobreType rechazo = new RechazoSobreType();
					rechazo.setMotivo("S02");
					rechazo.setGlosa("No coincide RUC de Sobre, Certificado, envío o CFE");
					ackSobredefType.getDetalle().getMotivosRechazo().add(rechazo);
				}
				
				/*
				 * S05
				 */
				if (envioCFEEntreEmpresas.getCaratula().getCantCFE() != envioCFEEntreEmpresas.getCFEAdendas().size()){
					RechazoSobreType rechazo = new RechazoSobreType();
					rechazo.setMotivo("S05");
					rechazo.setGlosa("No coinciden cantidad CFE de carátula y contenido");
					ackSobredefType.getDetalle().getMotivosRechazo().add(rechazo);
				}
				
				
				if (ackSobredefType.getDetalle().getMotivosRechazo().size()==0){
					/*
					 * Se acepta el sobre!
					 */
					sobreRecibido.setEstado(EstadoACKSobreType.AS);
					ackSobredefType.getDetalle().setEstado(EstadoACKSobreType.AS);
					ParamConsultaType params = new ParamConsultaType();
					params.setFechahora(DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()));
					params.setToken(UUID.randomUUID().toString());
					ackSobredefType.getDetalle().setParamConsulta(params);
				}else{
					/*
					 * Se rechaza el sobre!
					 */
					sobreRecibido.setEstado(EstadoACKSobreType.BS);
					ackSobredefType.getDetalle().setEstado(EstadoACKSobreType.BS);
				}
				
				
				/*
				 * Instantiate the DocumentBuilderFactory.
				 * IMPORTANT: NamespaceAwerness=true!!
				 */
				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				dbf.setNamespaceAware(true);
				
				Document allDocument = XML.marshall(ackSobredefType);

				allDocument = SignatureInterceptor.signDocument(dbf, allDocument,"ACKSobre",null,commons);
				
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
						CFEEmpresasType cfe = iterator.next();
						procesarCFE(cfe, ackcfEdefType, new BigInteger(String.valueOf(i)));
						i++;
					}
					
					/*
					 * Serializo el XML respuesta
					 */
					DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
					dbf.setNamespaceAware(true);
					Document allDocument = XML.marshall(ackcfEdefType);
					allDocument = SignatureInterceptor.signDocument(dbf, allDocument,null,null,commons);
					sobreRecibido.setResultado_empresa(XML.documentToString(allDocument));
					
					return ackcfEdefType;
				}
				return null;
			} catch (TransformerFactoryConfigurationError | Exception e) {
				throw APIException.raise(e);
			}
		
		
		//TODO si los servicios son la capa superior nunca deberian tirar exepciones distintas de APIException
	}

	private void procesarCFE(CFEEmpresasType cfe, ACKCFEdefType ackcfEdefType, BigInteger ordinal) {
		ACKCFEDet ack = new ACKCFEDet();
		ack.setNroOrdinal(ordinal);
		
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
		
		
		if (cfe.getCFE().getEFact()!=null){
			ack.setNroCFE(cfe.getCFE().getEFact().getEncabezado().getIdDoc().getNro());
			ack.setSerie(cfe.getCFE().getEFact().getEncabezado().getIdDoc().getSerie());
			ack.setFechaCFE(cfe.getCFE().getEFact().getEncabezado().getIdDoc().getFchEmis());
			ack.setTipoCFE(cfe.getCFE().getEFact().getEncabezado().getIdDoc().getTipoCFE());
			ack.setTmstCFE(cfe.getCFE().getEFact().getTmstFirma());
		}
		
		if (cfe.getCFE().getERem()!=null){
			ack.setNroCFE(cfe.getCFE().getERem().getEncabezado().getIdDoc().getNro());
			ack.setSerie(cfe.getCFE().getERem().getEncabezado().getIdDoc().getSerie());
			ack.setFechaCFE(cfe.getCFE().getERem().getEncabezado().getIdDoc().getFchEmis());
			ack.setTipoCFE(cfe.getCFE().getERem().getEncabezado().getIdDoc().getTipoCFE());
			ack.setTmstCFE(cfe.getCFE().getERem().getTmstFirma());
		}
		
		if (cfe.getCFE().getEResg()!=null){
			ack.setNroCFE(cfe.getCFE().getEResg().getEncabezado().getIdDoc().getNro());
			ack.setSerie(cfe.getCFE().getEResg().getEncabezado().getIdDoc().getSerie());
			ack.setFechaCFE(cfe.getCFE().getEResg().getEncabezado().getIdDoc().getFchEmis());
			ack.setTipoCFE(cfe.getCFE().getEResg().getEncabezado().getIdDoc().getTipoCFE());
			ack.setTmstCFE(cfe.getCFE().getEResg().getTmstFirma());
		}
		
		if (cfe.getCFE().getETck()!=null){
			ack.setNroCFE(cfe.getCFE().getETck().getEncabezado().getIdDoc().getNro());
			ack.setSerie(cfe.getCFE().getETck().getEncabezado().getIdDoc().getSerie());
			ack.setFechaCFE(cfe.getCFE().getETck().getEncabezado().getIdDoc().getFchEmis());
			ack.setTipoCFE(cfe.getCFE().getETck().getEncabezado().getIdDoc().getTipoCFE());
			ack.setTmstCFE(cfe.getCFE().getETck().getTmstFirma());
		}
		
		//TODO faltan cfe.getCFE().getEFactExp() y cfe.getCFE().getERemExp()
		
		//TODO no se hacen chequeos ninguno, siempre se acepta. Hacer los chequeos 
		//TODO esta HARCODED A MORIR!!!!!!
		int nro = ack.getNroCFE().intValue();
		EstadoACKCFEType estado;
		RechazoCFEDGIType rechazo = new RechazoCFEDGIType();
		switch (nro) {
		case 2:
			estado = EstadoACKCFEType.BE;
			rechazo.setMotivo("E05");
			rechazo.setGlosa("Tipo y No de CFE no se corresponden con el CAE");
			ack.getMotivosRechazoCF().add(rechazo);
			break;
		case 4:
			estado = EstadoACKCFEType.BE;
			rechazo.setMotivo("E05");
			rechazo.setGlosa("Tipo y No de CFE no se corresponden con el CAE");
			ack.getMotivosRechazoCF().add(rechazo);
			break;
		case 6:
			estado = EstadoACKCFEType.BE;
			rechazo.setMotivo("E05");
			rechazo.setGlosa("Tipo y No de CFE no se corresponden con el CAE");
			ack.getMotivosRechazoCF().add(rechazo);
			break;
		case 8:
			estado = EstadoACKCFEType.BE;
			rechazo.setMotivo("E05");
			rechazo.setGlosa("Tipo y No de CFE no se corresponden con el CAE");
			ack.getMotivosRechazoCF().add(rechazo);
			break;
		default:
			estado = EstadoACKCFEType.AE;
			break;
		}
		ack.setEstado(estado);
		addResultado(estado, ackcfEdefType);
		
		
		ackcfEdefType.getACKCFEDet().add(ack);
	}

	private void addResultado(EstadoACKCFEType estado, ACKCFEdefType ackcfEdefType) {
		
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
