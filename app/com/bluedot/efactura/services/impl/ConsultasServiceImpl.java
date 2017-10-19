package com.bluedot.efactura.services.impl;

import java.util.Date;
import java.util.GregorianCalendar;

import javax.xml.datatype.DatatypeFactory;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.efactura.interceptors.InterceptorContextHolder;
import com.bluedot.efactura.model.CFE;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.model.ReporteDiario;
import com.bluedot.efactura.model.TipoDoc;
import com.bluedot.efactura.pool.WSConsultasPool;
import com.bluedot.efactura.pool.wrappers.WSEFacturaConsultasSoapPortWrapper;
import com.bluedot.efactura.services.ConsultasService;
import com.sun.istack.logging.Logger;

import dgi.soap.consultas.ACKConsultaCFERecibidos;
import dgi.soap.consultas.ACKConsultaEnviosCFE;
import dgi.soap.consultas.ACKConsultaEnviosReporte;
import dgi.soap.consultas.ACKConsultaEnviosSobre;
import dgi.soap.consultas.ACKConsultaEstadoCFE;
import dgi.soap.consultas.CFEId;
import dgi.soap.consultas.ConsultaEnviosSobre;
import dgi.soap.consultas.RucEmisoresMail;
import dgi.soap.consultas.WSEFacturaConsultasEFACCONSULTARENVIOSSOBRE;
import dgi.soap.consultas.WSEFacturaConsultasEFACCONSULTARENVIOSSOBREResponse;
import dgi.soap.consultas.WSEFacturaConsultasEFACCONSULTARESTADOCFE;
import dgi.soap.consultas.WSEFacturaConsultasEFACCONSULTARESTADOCFEResponse;

public class ConsultasServiceImpl implements ConsultasService {

	static Logger logger = Logger.getLogger(ConsultasServiceImpl.class);
	
	@Override
	public ACKConsultaEstadoCFE consultarEstadoCFE(CFE cfe) throws APIException {
		
		try {
			WSEFacturaConsultasSoapPortWrapper portWrapper = WSConsultasPool.getInstance().checkOut();

			WSEFacturaConsultasEFACCONSULTARESTADOCFE parameters = new WSEFacturaConsultasEFACCONSULTARESTADOCFE();

			CFEId id = new CFEId();
			
			id.setNro((int) (long) cfe.getId());
			id.setSerie(cfe.getSerie());
			id.setTipoCFE((short) (int) cfe.getTipo().value);
			
			parameters.setCfeid(id);

			WSEFacturaConsultasEFACCONSULTARESTADOCFEResponse output = portWrapper.getPort().efacconsultarestadocfe(parameters);

			WSConsultasPool.getInstance().checkIn(portWrapper);
			
			logger.info("Respuesta: " + output.getAckconsultaestadocfe().toString());
			
			return output.getAckconsultaestadocfe();
		} catch (Throwable e) {
			throw APIException.raise(APIErrors.ERROR_COMUNICACION_DGI, e);
		}
		
	}

	@Override
	public ACKConsultaEnviosCFE consultarEnviosCFE(CFE cfe) throws APIException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ACKConsultaEnviosReporte consultarEnviosReporte(ReporteDiario reporte) throws APIException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ACKConsultaCFERecibidos consultarCFERecibidos(String ruc, TipoDoc tipoDoc, long nroCFE, Date fechaDesde,
			Date FechaHasta) throws APIException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ACKConsultaEnviosSobre consultarEnvioSobre(long idEmisor, long idReceptor, Date fechaDesde,
			Date FechaHasta, Empresa empresa) throws APIException {
		
		/*
		 * Colocamos en ThreadLocal al Sobre es la forma de pasarle
		 * parametros a los Interceptors
		 */
		InterceptorContextHolder.setEmpresa(empresa);
		
		try {
			WSEFacturaConsultasSoapPortWrapper portWrapper = WSConsultasPool.getInstance().checkOut();

			WSEFacturaConsultasEFACCONSULTARENVIOSSOBRE parameters = new WSEFacturaConsultasEFACCONSULTARENVIOSSOBRE();

			ConsultaEnviosSobre consulta = new ConsultaEnviosSobre();
			
			consulta.setIdEmisor(idEmisor);
			consulta.setIdReceptor(idReceptor);
			
			GregorianCalendar c = new GregorianCalendar();
			
			c.setTime(fechaDesde);
			consulta.setFechaDesde(DatatypeFactory.newInstance().newXMLGregorianCalendar(c));
			
			c.setTime(FechaHasta);
			consulta.setFechaHasta(DatatypeFactory.newInstance().newXMLGregorianCalendar(c));
			
			
			parameters.setConsultaenviossobre(consulta);

			WSEFacturaConsultasEFACCONSULTARENVIOSSOBREResponse output = portWrapper.getPort().efacconsultarenviossobre(parameters);

			WSConsultasPool.getInstance().checkIn(portWrapper);
			
			logger.info("Respuesta: " + output.getAckconsultaenviossobre().toString());
			
			return output.getAckconsultaenviossobre();
		} catch (Throwable e) {
			throw APIException.raise(APIErrors.ERROR_COMUNICACION_DGI, e);
		}
	}

	@Override
	public String consutarRespuestaReporte(long idReceptor) throws APIException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RucEmisoresMail consutarRucEmisores(String eFacDocNro, String eFacDenominacion,
			Date eFacRUCEmisorAudFchHora) throws APIException {
		// TODO Auto-generated method stub
		return null;
	}

}
