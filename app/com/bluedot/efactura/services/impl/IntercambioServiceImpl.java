package com.bluedot.efactura.services.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Random;
import java.util.UUID;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.w3c.dom.Document;

import com.bluedot.commons.Settings;
import com.bluedot.commons.XML;
import com.bluedot.efactura.Constants;
import com.bluedot.efactura.commons.Commons;
import com.bluedot.efactura.global.EFacturaException;
import com.bluedot.efactura.interceptors.SignatureInterceptor;
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

	@Override
	public ACKSobredefType procesarSobre(EnvioCFEEntreEmpresas envioCFEEntreEmpresas, String filename) {
		
		try {
			ACKSobredefType ackSobredefType = new ACKSobredefType();
			ackSobredefType.setVersion("1.0");
			addCaratula(ackSobredefType, envioCFEEntreEmpresas, filename);
			ackSobredefType.setDetalle(new Detalle());
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
			 * S02
			 */
			//TODO mover rut a algo configurable
			if (!envioCFEEntreEmpresas.getCaratula().getRutReceptor().equals("215071660012")){
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
				ackSobredefType.getDetalle().setEstado(EstadoACKSobreType.AS);
				ParamConsultaType params = new ParamConsultaType();
				params.setFechahora(DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()));
				params.setToken(UUID.randomUUID().toString().getBytes());
				ackSobredefType.getDetalle().setParamConsulta(params);
			}else{
				/*
				 * Se rechaza el sobre!
				 */
				ackSobredefType.getDetalle().setEstado(EstadoACKSobreType.BS);
			}
			
			
			/*
			 * Instantiate the DocumentBuilderFactory.
			 * IMPORTANT: NamespaceAwerness=true!!
			 */
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			
			Document allDocument = XML.marshall(ackSobredefType);

			SignatureInterceptor.signDocument(dbf, allDocument,"ACKSobre",null);
			
			//System.out.println(XML.documentToString(allDocument));
			
			String filenamePrefix = Settings.getInstance().getString(Constants.GENERATED_CFE_FOLDER,
					"resources" + File.separator + "cfe" + File.separator + "sobre.xml");
			
			Commons.dumpNodeToFile(allDocument, true, filenamePrefix, null);
			
			//ackSobredefType = (ACKSobredefType)XML.unMarshall(allDocument, ackSobredefType.getClass());
			
			return ackSobredefType;
			
			
		
		} catch (DatatypeConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CertificateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerFactoryConfigurationError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (EFacturaException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		
		return null;
	}

	private void addCaratula(ACKSobredefType ackSobredefType, EnvioCFEEntreEmpresas envioCFEEntreEmpresas, String filename) throws DatatypeConfigurationException {
		Caratula caratula = new Caratula();

		caratula.setCantidadCFE(envioCFEEntreEmpresas.getCaratula().getCantCFE());
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
		
		ackSobredefType.setCaratula(caratula);
		
	}

	@Override
	public ACKCFEdefType procesarCFESobre(EnvioCFEEntreEmpresas envioCFEEntreEmpresas,
			ACKSobredefType ackSobredefType, String filename) {
		
		try {
			if (ackSobredefType.getDetalle().getMotivosRechazo().size()==0){
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
				ACKCFEdefType ackcfEdefType = new ACKCFEdefType();
				addCaratula(ackcfEdefType, envioCFEEntreEmpresas, filename);
				ackcfEdefType.setVersion("1.0");
				
				int i = 1;
				for (Iterator<CFEEmpresasType> iterator = envioCFEEntreEmpresas.getCFEAdendas().iterator(); iterator.hasNext();) {
					CFEEmpresasType cfe = iterator.next();
					procesarCFE(cfe, ackcfEdefType, new BigInteger(String.valueOf(i)));
					i++;
				}
				
				
				
				/*
				 * Instantiate the DocumentBuilderFactory.
				 * IMPORTANT: NamespaceAwerness=true!!
				 */
				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				dbf.setNamespaceAware(true);
				
				Document allDocument = XML.marshall(ackcfEdefType);

				SignatureInterceptor.signDocument(dbf, allDocument,null,null);
				
				//ackcfEdefType = (ACKCFEdefType)XML.unMarshall(allDocument, ackcfEdefType.getClass());
				
				return ackcfEdefType;
			}
		} catch (DatatypeConfigurationException e) {
			e.printStackTrace();
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CertificateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//TODO si los servicios son la capa superior nunca deberian tirar exepciones distintas de APIException
		return null;
	}

	private void procesarCFE(CFEEmpresasType cfe, ACKCFEdefType ackcfEdefType, BigInteger ordinal) {
		ACKCFEDet ack = new ACKCFEDet();
		ack.setNroOrdinal(ordinal);
		
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
