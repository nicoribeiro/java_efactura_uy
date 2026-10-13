package com.bluedot.efactura.services.impl;

import java.util.Date;
import java.util.GregorianCalendar;

import javax.xml.datatype.DatatypeFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

import dgi.soap.consultas.ACKConsultaCFERecibidos;
import dgi.soap.consultas.ACKConsultaEnviosCFE;
import dgi.soap.consultas.ACKConsultaEnviosReporte;
import dgi.soap.consultas.ACKConsultaEnviosSobre;
import dgi.soap.consultas.ACKConsultaEstadoCFE;
import dgi.soap.consultas.CFEId;
import dgi.soap.consultas.ConsultaEnviosSobre;
import dgi.soap.consultas.RucEmisoresMail;
import dgi.soap.consultas.WSEFacturaConsultasEFACCONSULTARRUCEMISORESResponse;
import dgi.soap.consultas.WSEFacturaConsultasEFACCONSULTARRUCEMISORES;
import dgi.soap.consultas.ConsultaRUCEmisores;
import dgi.soap.consultas.WSEFacturaConsultasEFACCONSULTARENVIOSSOBRE;
import dgi.soap.consultas.WSEFacturaConsultasEFACCONSULTARENVIOSSOBREResponse;
import dgi.soap.consultas.WSEFacturaConsultasEFACCONSULTARESTADOCFE;
import dgi.soap.consultas.WSEFacturaConsultasEFACCONSULTARESTADOCFEResponse;

public class ConsultasServiceImpl implements ConsultasService {

	final static Logger logger = LoggerFactory.getLogger(ConsultasServiceImpl.class);	
	
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
			
			logger.debug("Respuesta: " + output.getAckconsultaestadocfe().toString());
			
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
			
			logger.debug("Respuesta: " + output.getAckconsultaenviossobre().toString());
			
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
	public RucEmisoresMail consutarRucEmisores(String eFacDocNro, String eFacDenominacion, Date eFacRUCEmisorAudFchHora,
			Empresa firmante) throws APIException {

		/*
		 * EFACCONSULTARRUCEMISORES del servicio de consultas de DGI (documento "Servicios Consultas eFactura 1.10"): emisores
		 * electronicos que existen o cambiaron desde la fecha, filtrando opcionalmente por RUC y denominacion (contienen).
		 * Firmada con el certificado de la empresa firmante (los interceptors la toman del ThreadLocal).
		 */
		InterceptorContextHolder.setEmpresa(firmante);

		try {
			WSEFacturaConsultasSoapPortWrapper portWrapper = WSConsultasPool.getInstance().checkOut();
			// DGI manda FECHA_FIN 0000-00-00 cuando no hay fecha: que JAXB la deje en null en vez de fallar la respuesta
			((javax.xml.ws.BindingProvider) portWrapper.getPort()).getRequestContext().put("set-jaxb-validation-event-handler", Boolean.FALSE);

			ConsultaRUCEmisores consulta = new ConsultaRUCEmisores();
			consulta.setFiltroEFacDocNro(eFacDocNro != null ? eFacDocNro : "");
			consulta.setFiltroEFacDenominacion(eFacDenominacion != null ? eFacDenominacion : "");
			GregorianCalendar c = new GregorianCalendar();
			c.setTime(eFacRUCEmisorAudFchHora);
			consulta.setFiltroEFacRUCEmisorAudFchHora(DatatypeFactory.newInstance().newXMLGregorianCalendar(c));

			WSEFacturaConsultasEFACCONSULTARRUCEMISORES parameters = new WSEFacturaConsultasEFACCONSULTARRUCEMISORES();
			parameters.setConsultarucemisores(consulta);

			WSEFacturaConsultasEFACCONSULTARRUCEMISORESResponse output;
			try {
				output = portWrapper.getPort().efacconsultarrucemisores(parameters);
			} finally {
				// El puerto es del pool: el resto de las consultas sigue con la validacion por defecto
				((javax.xml.ws.BindingProvider) portWrapper.getPort()).getRequestContext().remove("set-jaxb-validation-event-handler");
			}

			WSConsultasPool.getInstance().checkIn(portWrapper);

			return output.getRucemisoresmail();
		} catch (Throwable e) {
			throw APIException.raise(APIErrors.ERROR_COMUNICACION_DGI, e);
		}
	}

}
