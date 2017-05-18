package com.bluedot.efactura.services;

import java.util.Date;

import com.bluedot.commons.error.APIException;
import com.bluedot.efactura.model.CFE;
import com.bluedot.efactura.model.ReporteDiario;
import com.bluedot.efactura.model.TipoDoc;

import dgi.soap.consultas.ACKConsultaCFERecibidos;
import dgi.soap.consultas.ACKConsultaEnviosCFE;
import dgi.soap.consultas.ACKConsultaEnviosReporte;
import dgi.soap.consultas.ACKConsultaEnviosSobre;
import dgi.soap.consultas.ACKConsultaEstadoCFE;
import dgi.soap.consultas.RucEmisoresMail;

public interface ConsultasService {

	/*
	 * Dado el Tipo de CFE, la Serie y el Número de CFE, devuelve el estado del CFE y los datos de consulta del primer sobre recibido por DGI con dicho comprobante. 
	 * Los tres campos son obligatorios
	 */
	ACKConsultaEstadoCFE consultarEstadoCFE(CFE cfe) throws APIException;
	
	/*
	 * Dado el Tipo de CFE, la Serie y el Número de CFE, 
	 * devuelve una colección con los parámetros de consulta de los sobres donde fue enviado dicho CFE y su estado. Los tres campos son obligatorios
	 */
	ACKConsultaEnviosCFE consultarEnviosCFE(CFE cfe) throws APIException;
	
	/*
	 * Dada la Fecha del Resumen, la Secuencia y el Id Emisor, 
	 * devuelve una colección con los datos de los reportes que cumplen dichas condiciones. Los campos Secuencia e Id Emisor son opcionales
	 */
	ACKConsultaEnviosReporte consultarEnviosReporte(ReporteDiario reporte) throws APIException;
	
	/*
	 * Dado el RUC Emisor, Tipo de CFE, Serie, Número de CFE, y el rango de Fechas de emisión del CFE (FechaDesde, FechaHasta), 
	 * devuelve una colección con los CFEs que cumplen dichas condiciones y el estado de los mismos.
	 * - Tipo de CFE, Serie, Número de CFE son opcionales
	 * - Rango de Fechas de emisión del CFE no debe superar los 31 días
	 */
	ACKConsultaCFERecibidos consultarCFERecibidos(String ruc, TipoDoc tipoDoc, long nroCFE, Date fechaDesde, Date FechaHasta) throws APIException;
	
	/*
	 * Dado el Id Emisor e Id Receptor, y un rango de Fechas de envío (FechaDesde, FechaHasta), 
	 * devuelve una colección con los datos de los sobres (que cumplen dichas condiciones) 
	 * incluyendo los parámetros de consulta de los mismos.
	 * - Id Emisor e Id Receptor son opcionales
	 * - Rango de Fechas de envío no debe superar los 31 días
	 */
	ACKConsultaEnviosSobre consultarEnvioSobre(long idEmisor, long idReceptor, Date fechaDesde, Date FechaHasta) throws APIException;
	
	/*
	 * Dado el Id Receptor de un reporte, devuelve la respuesta original del reporte consultado.
	 */
	String consutarRespuestaReporte(long idReceptor) throws APIException;
	
	/*
	 * Dado los filtros opcionales Filtro_eFacDocNro, Filtro_eFacDenominacion, Filtro_eFacRUCEmisorAudFchHora correspondientes a un Nro de RUC, una Denominación y una Fecha, 
	 * devuelve un XML con la lista de Emisores Electrónicos, su fecha de inicio como Emisor Electrónico y su mail de contacto con otros Emisores. 
	 * La aplicación de los filtros se realiza de la siguiente manera:
	 *  - Filtro_eFacDocNro: Si está presente, los números de RUC Emisores contenidos en la consulta contienen parcial o totalmente a dicho valor.
	 *  - Filtro_eFacDenominacion: Si está presente, las denominaciones de los RUC Emisores contenidos en la consulta contienen parcial o total a dicho valor.
	 *  - Filtro_eFacRUCEmisorAudFchHora (obligatorio): Los RUC Emisores contenidos en la consulta existen como Emisores Electrónicos o sufrieron alguna modificación de sus datos a partir de la fecha indicada. 
	 *    (Ejemplo: se modificó el mail de contacto a partir de la fecha)
	 */
	RucEmisoresMail consutarRucEmisores(String eFacDocNro, String eFacDenominacion, Date eFacRUCEmisorAudFchHora) throws APIException;
	
}
