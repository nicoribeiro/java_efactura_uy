package com.bluedot.efactura.services.impl;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.json.JSONArray;
import org.json.JSONObject;

import com.bluedot.commons.IO;
import com.bluedot.commons.PrettyPrint;
import com.bluedot.commons.Settings;
import com.bluedot.efactura.Constants;
import com.bluedot.efactura.commons.Commons;
import com.bluedot.efactura.commons.EfacturaSecurity;
import com.bluedot.efactura.global.EFacturaException;
import com.bluedot.efactura.global.EFacturaException.EFacturaErrors;
import com.bluedot.efactura.impl.CAEManagerImpl;
import com.bluedot.efactura.impl.CAEManagerImpl.TipoDoc;
import com.bluedot.efactura.pool.WSEFacturaSoapPortWrapper;
import com.bluedot.efactura.pool.WSRecepcionPool;
import com.bluedot.efactura.services.RecepcionService;
import com.bluedot.efactura.strategy.report.SummaryStrategy;
import com.sun.istack.logging.Logger;

import dgi.classes.recepcion.CFEDefType;
import dgi.classes.recepcion.EnvioCFE;
import dgi.classes.recepcion.EnvioCFE.Caratula;
import dgi.classes.recepcion.IdDocFact;
import dgi.classes.recepcion.ObjectFactory;
import dgi.classes.reporte.ReporteDefType;
import dgi.soap.recepcion.Data;
import dgi.soap.recepcion.WSEFacturaEFACCONSULTARESTADOENVIO;
import dgi.soap.recepcion.WSEFacturaEFACCONSULTARESTADOENVIOResponse;
import dgi.soap.recepcion.WSEFacturaEFACRECEPCIONREPORTE;
import dgi.soap.recepcion.WSEFacturaEFACRECEPCIONREPORTEResponse;
import dgi.soap.recepcion.WSEFacturaEFACRECEPCIONSOBRE;
import dgi.soap.recepcion.WSEFacturaEFACRECEPCIONSOBREResponse;

public class RecepcionServiceImpl implements RecepcionService {

	static Logger logger = Logger.getLogger(RecepcionServiceImpl.class);

	@Override
	public Data sendCFE(String cfe) throws EFacturaException {

		try {
			Objects.requireNonNull(cfe, "Parameter CFE is required");

			WSEFacturaSoapPortWrapper portWrapper = WSRecepcionPool.getInstance().checkOut();

			WSEFacturaEFACRECEPCIONSOBRE input = new WSEFacturaEFACRECEPCIONSOBRE();
			Data data = new Data();
			data.setXmlData(cfe);
			input.setDatain(data);

			WSEFacturaEFACRECEPCIONSOBREResponse output = portWrapper.getPort().efacrecepcionsobre(input);

			WSRecepcionPool.getInstance().checkIn(portWrapper);

			return output.getDataout();
		} catch (Exception e) {
			throw EFacturaException.raise(e);
		}

	}

	@Override
	public Data sendCFE(Object cfe) throws EFacturaException {
		/*
		 * Wrap cfe object with CFEDefType
		 */
		CFEDefType cfeDefType = (new ObjectFactory()).createCFEDefType();
		cfeDefType.setVersion("1.0");
		if (cfe instanceof CFEDefType.EFact)
			cfeDefType.setEFact((CFEDefType.EFact) cfe);
		if (cfe instanceof CFEDefType.EFactExp)
			cfeDefType.setEFactExp((CFEDefType.EFactExp) cfe);
		if (cfe instanceof CFEDefType.ERem)
			cfeDefType.setERem((CFEDefType.ERem) cfe);
		if (cfe instanceof CFEDefType.ERemExp)
			cfeDefType.setERemExp((CFEDefType.ERemExp) cfe);
		if (cfe instanceof CFEDefType.EResg)
			cfeDefType.setEResg((CFEDefType.EResg) cfe);
		if (cfe instanceof CFEDefType.ETck)
			cfeDefType.setETck((CFEDefType.ETck) cfe);

		/*
		 * Add CFE to a EnvioCFE object. EvioCFE object will be sent to the
		 * Service
		 */
		EnvioCFE envioCFE = new EnvioCFE();
		envioCFE.setVersion("1.0");
		envioCFE.getCVES().add(cfeDefType);

		/*
		 * Add caratula
		 */
		addCaratulaSobre(envioCFE);

		return this.sendSobre(envioCFE);

	}

	private void addCaratulaSobre(EnvioCFE signed) throws EFacturaException {
		try {
			List<CFEDefType> cfes = signed.getCVES();

			String RUCemisor = null;

			String RUCreceptor = null;

			for (Iterator<CFEDefType> iterator = cfes.iterator(); iterator.hasNext();) {
				CFEDefType cfeDefType = iterator.next();

				String rucEmisor = null;
				String rucReceptor = "219999830019";

				/*
				 * TODO el receptor es la DGI cuando envio a la DGI, es el cliente cuando se lo mando al cliente.
				 * ademas, el RUT de la DGI es diferente en Testing que en Produccion.
				 */
				
				if (cfeDefType.getEFact() != null) {
					rucEmisor = cfeDefType.getEFact().getEncabezado().getEmisor().getRUCEmisor();
					//rucReceptor = cfeDefType.getEFact().getEncabezado().getReceptor().getDocRecep();
				}

				if (cfeDefType.getEFactExp() != null) {
					rucEmisor = cfeDefType.getEFactExp().getEncabezado().getEmisor().getRUCEmisor();
					//rucReceptor = cfeDefType.getEFactExp().getEncabezado().getReceptor().getDocRecep();
				}

				if (cfeDefType.getERem() != null) {
					rucEmisor = cfeDefType.getERem().getEncabezado().getEmisor().getRUCEmisor();
					//rucReceptor = cfeDefType.getERem().getEncabezado().getReceptor().getDocRecep();
				}

				if (cfeDefType.getERemExp() != null) {
					rucEmisor = cfeDefType.getERemExp().getEncabezado().getEmisor().getRUCEmisor();
					//rucReceptor = cfeDefType.getERemExp().getEncabezado().getReceptor().getDocRecep();
				}

				if (cfeDefType.getEResg() != null) {
					rucEmisor = cfeDefType.getEResg().getEncabezado().getEmisor().getRUCEmisor();
					//rucReceptor = cfeDefType.getEResg().getEncabezado().getReceptor().getDocRecep();
				}

				if (cfeDefType.getETck() != null) {
					rucEmisor = cfeDefType.getETck().getEncabezado().getEmisor().getRUCEmisor();
					//rucReceptor = cfeDefType.getETck().getEncabezado().getReceptor().getDocRecep();
				}

				if (RUCemisor == null)
					RUCemisor = rucEmisor;
				else if (!RUCemisor.equals(rucEmisor))
					throw EFacturaException.raise(EFacturaErrors.BAD_PARAMETER_VALUE)
							.setDetailMessage("Cannot have many RUCemisor values on one envelope");

				if (RUCreceptor == null)
					RUCreceptor = rucReceptor;
				else if (!RUCemisor.equals(rucReceptor))
					throw EFacturaException.raise(EFacturaErrors.BAD_PARAMETER_VALUE)
							.setDetailMessage("Cannot have many RUCreceptor on one envelope");

			}

			Caratula caratula = (new ObjectFactory()).createEnvioCFECaratula();

			caratula.setCantCFE(cfes.size());
			caratula.setFecha(DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()));
			caratula.setIdemisor(new BigInteger("1"));
			caratula.setRUCEmisor(RUCemisor);
			caratula.setRutReceptor(RUCreceptor);
			caratula.setVersion("1.0");
			caratula.setX509Certificate(EfacturaSecurity.getCertificate(Commons.getCetificateAlias(),
					Commons.getCertificatePassword(), Commons.getKeyStore()));

			signed.setCaratula(caratula);
		} catch (Exception e) {
			throw EFacturaException.raise(e);
		}

	}

	@Override
	public Data sendSobre(EnvioCFE envioCFE) throws EFacturaException {
		try {
			JAXBContext context = JAXBContext.newInstance(EnvioCFE.class);
			Marshaller marshaller = context.createMarshaller();
			StringWriter sw = new StringWriter();
			marshaller.marshal(envioCFE, sw);

			//TODO optimizar esto, se hace dump 2 veces
			
			// Dump sobre
			for (int i = 0; i < envioCFE.getCVES().size(); i++) {
				Commons.dumpSobreToFile(envioCFE, i, false, null);
			}
			
			Data data = this.sendCFE(PrettyPrint.prettyPrintXML(sw.toString()));

			// Dump sobre y response to disk
			for (int i = 0; i < envioCFE.getCVES().size(); i++) {
				Commons.dumpSobreToFile(envioCFE, i, false, data);
			}

			return data;
		} catch (Exception e) {
			throw EFacturaException.raise(e);
		}

	}

	@Override
	public Data consultaEstado(String token, String idReceptor) throws EFacturaException {

		try {
			Objects.requireNonNull(token, "Parameter token is required");
			Objects.requireNonNull(idReceptor, "Parameter idReceptor is required");

			WSEFacturaSoapPortWrapper portWrapper = WSRecepcionPool.getInstance().checkOut();

			WSEFacturaEFACCONSULTARESTADOENVIO input = new WSEFacturaEFACCONSULTARESTADOENVIO();
			String xml = "<ConsultaCFE xmlns=\"http://dgi.gub.uy\"><IdReceptor>" + idReceptor + "</IdReceptor><Token>"
					+ token + "</Token> </ConsultaCFE>";
			Data data = new Data();
			data.setXmlData(xml);

			input.setDatain(data);

			WSEFacturaEFACCONSULTARESTADOENVIOResponse output = portWrapper.getPort().efacconsultarestadoenvio(input);

			WSRecepcionPool.getInstance().checkIn(portWrapper);

			return output.getDataout();
		} catch (Exception e) {
			throw EFacturaException.raise(e);
		}

	}

	@Override
	public Data consultaResultado(TipoDoc tipo, String serie, int nro, Date fecha) throws EFacturaException {
		try {
			String folder = Commons.getCfeFolder(fecha, tipo);
			File docResponse = new File(folder + File.separator + serie + "_" + nro + "_response.xml");

			File docRsesult = new File(folder + File.separator + serie + "_" + nro + "_result.xml");

			if (!docResponse.exists())
				return null;
			
			if (!docRsesult.exists()) {
				String response = IO.readFile(folder + File.separator + serie + "_" + nro + "_response.xml",
						Charset.forName("UTF-8"));
				
				/* 
				 * En unas respuestas raras de la DGI no aparece el campo estado, si para eso lo tratamos como un
				 * documento a anular. 
				 */
				
				if (response.contains("<Estado>")) {
					String estado = response.split("<Estado>")[1].split("</Estado>")[0];

					// Hay unos casos raros en que la DGI no retorna un valor en el campo token. Retorna solo el tag <Token/>
					if (estado.equals("AS") && !response.contains("<Token/>")) {
						
						String token = response.split("<Token>")[1].split("</Token>")[0];

						String idReceptor = response.split("<IDReceptor>")[1].split("</IDReceptor>")[0];

						Data result = consultaEstado(token, idReceptor);

						IO.writeFile(folder + File.separator + serie + "_" + nro + "_result.xml",
								PrettyPrint.prettyPrintXML(result.getXmlData()));

						return result;
					}
				}
			}else{
				
				Data data = new Data();
				data.setXmlData(IO.readFile(docRsesult.getCanonicalPath(),Charset.forName("UTF-8")));
				return data;
			}
				
			return null;

		} catch (Exception e) {
			throw EFacturaException.raise(e);
		}

	}

	@Override
	public Data generarReporteDiario(Date date) throws EFacturaException {
		try {
			Objects.requireNonNull(date, "Parameter date is required");

			this.consultarResultados(date);

			SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
			SimpleDateFormat caratulaFormatter = new SimpleDateFormat("yyyy-MM-dd");

			String folder = Settings.getInstance().getString(Constants.GENERATED_CFE_FOLDER,
					"resources" + File.separator + "cfe") + File.separator + formatter.format(date);

			File directorio = new File(folder);

			ReporteDefType reporte = new ReporteDefType();

			/*
			 * Caratula
			 */
			dgi.classes.reporte.ReporteDefType.Caratula caratula = new dgi.classes.reporte.ReporteDefType.Caratula();
			caratula.setCantComprobantes(new BigInteger("0"));
			caratula.setFechaResumen(
					DatatypeFactory.newInstance().newXMLGregorianCalendar(caratulaFormatter.format(date)));
			caratula.setIDEmisor(new BigInteger("1"));
			caratula.setRUCEmisor("215071660012");
			// TODO hacer traking de los envios
			caratula.setSecEnvio(new BigInteger("1"));
			GregorianCalendar cal = new GregorianCalendar();
			cal.setTime(new Date());
			caratula.setTmstFirmaEnv(DatatypeFactory.newInstance().newXMLGregorianCalendar(cal));
			caratula.setVersion("1.0");
			reporte.setCaratula(caratula);

			/*
			 * Resumenes
			 */
			String[] tipoDocs = directorio.list();
			for (String tipoDoc : tipoDocs) {
				File tipoDocDir = new File(directorio + File.separator + tipoDoc);
				if (tipoDocDir.isDirectory()) {
					TipoDoc tipo = TipoDoc.fromInt(Integer.parseInt(tipoDoc));
					SummaryStrategy strategy = new SummaryStrategy.Builder().withTipo(tipo).build();
					strategy.buildSummary(reporte, date);
				}
			}

			return sendReporte(reporte, date);

		} catch (Exception e) {
			throw EFacturaException.raise(e);
		}

	}

	private Data sendReporte(String reporte, Date date) throws EFacturaException {
		try {
			WSEFacturaSoapPortWrapper portWrapper = WSRecepcionPool.getInstance().checkOut();

			WSEFacturaEFACRECEPCIONREPORTE input = new WSEFacturaEFACRECEPCIONREPORTE();
			Data data = new Data();
			data.setXmlData(reporte);
			input.setDatain(data);

			WSEFacturaEFACRECEPCIONREPORTEResponse output = portWrapper.getPort().efacrecepcionreporte(input);

			WSRecepcionPool.getInstance().checkIn(portWrapper);

			return output.getDataout();
		} catch (Exception e) {
			throw EFacturaException.raise(e);
		}
	}

	private Data sendReporte(ReporteDefType reporte, Date date) throws EFacturaException {
		try {
			JAXBContext context = JAXBContext.newInstance(ReporteDefType.class);
			Marshaller marshaller = context.createMarshaller();
			StringWriter sw = new StringWriter();
			marshaller.marshal(reporte, sw);

			Data response = this.sendReporte(PrettyPrint.prettyPrintXML(sw.toString()), date);

			// Dump sobre y response to disk
			Commons.dumpReporteToFile(reporte, false, response, date);

			return response;
		} catch (Exception e) {
			throw EFacturaException.raise(e);
		}
	}

	@Override
	public List<ResultadoConsulta> consultarResultados(Date date) throws EFacturaException {
		try {

			SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");

			String folder = Settings.getInstance().getString(Constants.GENERATED_CFE_FOLDER,
					"resources" + File.separator + "cfe") + File.separator + formatter.format(date);

			File directorio = new File(folder);

			String[] tipoDocs = directorio.list();

			LinkedList<ResultadoConsulta> resultList = new LinkedList<ResultadoConsulta>();
			
			for (String tipoDoc : tipoDocs) {
				File tipoDocDir = new File(directorio + File.separator + tipoDoc);
				if (tipoDocDir.isDirectory()) {
					TipoDoc tipo = TipoDoc.fromInt(Integer.parseInt(tipoDoc));
					String[] docs = tipoDocDir.list();
					ResultadoConsulta result = new ResultadoConsulta(TipoDoc.fromInt(Integer.parseInt(tipoDoc)),0,0,0,0,0);
					for (int i = 0; i < docs.length; i++) {
						if (docs[i].contains("_unsigned"))
							result.unsigned++;
						if (docs[i].contains("_signed"))
							result.signed++;
						if (docs[i].contains("_response")) {
							result.conRespuesta++;
							String serie = docs[i].split("_")[0];
							int nro = Integer.parseInt(docs[i].split("_")[1]);
							logger.info(tipo + " " + serie + " " + nro + " " + formatter.format(date));
							try {
								Data data = this.consultaResultado(tipo, serie, nro, date);
								if (data==null)
									result.conError++;
								else
									result.conResultado++;
							} catch (EFacturaException e) {
								e.printStackTrace();
								result.conError++;
							}
							
						}
					}
					resultList.add(result);
					
				}
			}
			return resultList;

		} catch (Exception e) {
			throw EFacturaException.raise(e);
		}
	}
//TODO rehacer nuevamente
	@Override
	public void anularDocumento(TipoDoc tipo, String serie, int nro, Date date) throws EFacturaException {
		try {
			JSONObject caeDataJson =  CAEManagerImpl.getInstance().getCaeJson(tipo);
			
			
			
			SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");

			
			String anuladoFilePath = Settings.getInstance().getString(Constants.GENERATED_CFE_FOLDER,
					"resources" + File.separator + "cfe") + File.separator + formatter.format(date) + File.separator + tipo.value + File.separator + serie + "_" + nro + "_unsigned.xml";
			File anuladoFile = new File(anuladoFilePath);
			
			if (!anuladoFile.exists()){
				IO.writeFile(anuladoFilePath, "narf");	
			}
			
			
			
		
		} catch (IOException e) {
			throw EFacturaException.raise(e);
		}
		
		
	}
	
	//metodo para pasar la prueba de homologacion
	@Override
	public void anularNextDocumento(TipoDoc tipo, Date date) throws EFacturaException {
		try {
			IdDocFact id =  CAEManagerImpl.getInstance().getIdDocFact(tipo, false,2);
			
		//TODO la forma de generar las anulaciones es generar un archivo dummy, esto es una cagada porque hay que borrar los archivos dummys luego de generar el reporte diario y antes de subir los datos a DGI	
			
			SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");

			
			String anuladoFilePath = Settings.getInstance().getString(Constants.GENERATED_CFE_FOLDER,
					"resources" + File.separator + "cfe") + File.separator + formatter.format(date) + File.separator + tipo.value + File.separator + id.getSerie() + "_" + id.getNro() + "_unsigned.xml";
			File anuladoFile = new File(anuladoFilePath);
			
			if (!anuladoFile.exists()){
				IO.writeFile(anuladoFilePath, "narf");	
			}
			
			
			
		
		} catch (IOException e) {
			throw EFacturaException.raise(e);
		} catch (DatatypeConfigurationException e) {
			throw EFacturaException.raise(e);
		}
		
		
	}

}
