package com.bluedot.efactura.services.impl;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.KeyStore;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.bluedot.commons.IO;
import com.bluedot.commons.PrettyPrint;
import com.bluedot.commons.Settings;
import com.bluedot.commons.XML;
import com.bluedot.commons.XmlSignature;
import com.bluedot.efactura.Constants;
import com.bluedot.efactura.commons.Commons;
import com.bluedot.efactura.commons.EfacturaSecurity;
import com.bluedot.efactura.global.EFacturaException;
import com.bluedot.efactura.global.EFacturaException.EFacturaErrors;
import com.bluedot.efactura.impl.CAEManagerImpl;
import com.bluedot.efactura.impl.CAEManagerImpl.TipoDoc;
import com.bluedot.efactura.interceptors.NamespacesInterceptor;
import com.bluedot.efactura.interceptors.SignatureInterceptor;
import com.bluedot.efactura.pool.WSEFacturaSoapPortWrapper;
import com.bluedot.efactura.pool.WSRecepcionPool;
import com.bluedot.efactura.services.RecepcionService;
import com.bluedot.efactura.strategy.report.SummaryStrategy;
import com.sun.istack.logging.Logger;

import dgi.classes.entreEmpresas.CFEEmpresasType;
import dgi.classes.entreEmpresas.EnvioCFEEntreEmpresas;
import dgi.classes.recepcion.CFEDefType;
import dgi.classes.recepcion.CFEDefType.EFact;
import dgi.classes.recepcion.CFEDefType.EFactExp;
import dgi.classes.recepcion.CFEDefType.ERem;
import dgi.classes.recepcion.CFEDefType.ERemExp;
import dgi.classes.recepcion.CFEDefType.EResg;
import dgi.classes.recepcion.CFEDefType.ETck;
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
	public Data sendCFE(EFact cfe, String adenda) throws EFacturaException {

		CFEDefType cfeDefType = (new ObjectFactory()).createCFEDefType();
		cfeDefType.setEFact((CFEDefType.EFact) cfe);
		return sendCFE(cfeDefType, adenda);
	}

	@Override
	public Data sendCFE(EFactExp cfe, String adenda) throws EFacturaException {
		CFEDefType cfeDefType = (new ObjectFactory()).createCFEDefType();
		cfeDefType.setEFactExp((CFEDefType.EFactExp) cfe);
		return sendCFE(cfeDefType, adenda);
	}

	@Override
	public Data sendCFE(ERem cfe, String adenda) throws EFacturaException {
		CFEDefType cfeDefType = (new ObjectFactory()).createCFEDefType();
		cfeDefType.setERem((CFEDefType.ERem) cfe);
		return sendCFE(cfeDefType, adenda);
	}

	@Override
	public Data sendCFE(ERemExp cfe, String adenda) throws EFacturaException {
		CFEDefType cfeDefType = (new ObjectFactory()).createCFEDefType();
		cfeDefType.setERemExp((CFEDefType.ERemExp) cfe);
		return sendCFE(cfeDefType, adenda);
	}

	@Override
	public Data sendCFE(EResg cfe, String adenda) throws EFacturaException {
		CFEDefType cfeDefType = (new ObjectFactory()).createCFEDefType();
		cfeDefType.setEResg((CFEDefType.EResg) cfe);
		return sendCFE(cfeDefType, adenda);
	}

	@Override
	public Data sendCFE(ETck cfe, String adenda) throws EFacturaException {
		CFEDefType cfeDefType = (new ObjectFactory()).createCFEDefType();
		cfeDefType.setETck((CFEDefType.ETck) cfe);
		return sendCFE(cfeDefType, adenda);
	}

	private Data sendCFE(CFEDefType cfeDefType, String adenda) throws EFacturaException {
		/*
		 * Wrap cfe object with CFEDefType
		 */

		CFEEmpresasType cfeEmpresasType = (new dgi.classes.entreEmpresas.ObjectFactory()).createCFEEmpresasType();

		cfeDefType.setVersion("1.0");

		cfeEmpresasType.setCFE(cfeDefType);
		cfeEmpresasType.setAdenda(adenda);

		/*
		 * Agrego CFEDefType a un EnvioCFE.
		 */
		EnvioCFE envioCFE = new EnvioCFE();
		envioCFE.setVersion("1.0");
		envioCFE.getCVES().add(cfeDefType);
		addCaratulaSobre(envioCFE);

		/*
		 * Agrego el CFEEmpresasType a un EnvioCFEEntreEmpresas
		 */
		EnvioCFEEntreEmpresas envioCFEEntreEmpresas = (new dgi.classes.entreEmpresas.ObjectFactory())
				.createEnvioCFEEntreEmpresas();
		envioCFEEntreEmpresas.setVersion("1.0");
		envioCFEEntreEmpresas.getCFEAdendas().add(cfeEmpresasType);
		addCaratulaSobre(envioCFEEntreEmpresas);
		this.enviarCFEaEmpresa(envioCFEEntreEmpresas);

		return this.enviarCFEaDGI(envioCFE);

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
				 * TODO el receptor es la DGI cuando envio a la DGI, el RUT de
				 * la DGI es diferente en Testing que en Produccion.
				 */

				if (cfeDefType.getEFact() != null) {
					rucEmisor = cfeDefType.getEFact().getEncabezado().getEmisor().getRUCEmisor();
				}

				if (cfeDefType.getEFactExp() != null) {
					rucEmisor = cfeDefType.getEFactExp().getEncabezado().getEmisor().getRUCEmisor();
				}

				if (cfeDefType.getERem() != null) {
					rucEmisor = cfeDefType.getERem().getEncabezado().getEmisor().getRUCEmisor();
				}

				if (cfeDefType.getERemExp() != null) {
					rucEmisor = cfeDefType.getERemExp().getEncabezado().getEmisor().getRUCEmisor();
				}

				if (cfeDefType.getEResg() != null) {
					rucEmisor = cfeDefType.getEResg().getEncabezado().getEmisor().getRUCEmisor();
				}

				if (cfeDefType.getETck() != null) {
					rucEmisor = cfeDefType.getETck().getEncabezado().getEmisor().getRUCEmisor();
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
			//TODO hace un incremental aca
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

	private void addCaratulaSobre(EnvioCFEEntreEmpresas signed) throws EFacturaException {
		try {
			List<CFEEmpresasType> cfes = signed.getCFEAdendas();

			String RUCemisor = null;

			String RUCreceptor = null;

			for (Iterator<CFEEmpresasType> iterator = cfes.iterator(); iterator.hasNext();) {
				CFEDefType cfeDefType = iterator.next().getCFE();

				String rucEmisor = null;
				String rucReceptor = null;

				if (cfeDefType.getEFact() != null) {
					rucEmisor = cfeDefType.getEFact().getEncabezado().getEmisor().getRUCEmisor();
					rucReceptor = cfeDefType.getEFact().getEncabezado().getReceptor().getDocRecep();
				}

				if (cfeDefType.getEFactExp() != null) {
					rucEmisor = cfeDefType.getEFactExp().getEncabezado().getEmisor().getRUCEmisor();
					rucReceptor = cfeDefType.getEFactExp().getEncabezado().getReceptor().getDocRecep();
				}

				if (cfeDefType.getERem() != null) {
					rucEmisor = cfeDefType.getERem().getEncabezado().getEmisor().getRUCEmisor();
					rucReceptor = cfeDefType.getERem().getEncabezado().getReceptor().getDocRecep();
				}

				if (cfeDefType.getERemExp() != null) {
					rucEmisor = cfeDefType.getERemExp().getEncabezado().getEmisor().getRUCEmisor();
					rucReceptor = cfeDefType.getERemExp().getEncabezado().getReceptor().getDocRecep();
				}

				if (cfeDefType.getEResg() != null) {
					rucEmisor = cfeDefType.getEResg().getEncabezado().getEmisor().getRUCEmisor();
					rucReceptor = cfeDefType.getEResg().getEncabezado().getReceptor().getDocRecep();
				}

				if (cfeDefType.getETck() != null) {
					rucEmisor = cfeDefType.getETck().getEncabezado().getEmisor().getRUCEmisor();
					rucReceptor = cfeDefType.getETck().getEncabezado().getReceptor().getDocRecep();
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

			dgi.classes.entreEmpresas.EnvioCFEEntreEmpresas.Caratula caratula = (new dgi.classes.entreEmpresas.ObjectFactory())
					.createEnvioCFEEntreEmpresasCaratula();

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

	private Data enviarCFEaDGI(EnvioCFE envioCFE) throws EFacturaException {
		try {
			String cfe = toString(envioCFE);
			// TODO optimizar esto, se hace dump 2 veces

			// Dump sobre a disco
			for (int i = 0; i < envioCFE.getCVES().size(); i++) {
				Commons.dumpSobreToFile(envioCFE, i, false, null);
			}

			WSEFacturaSoapPortWrapper portWrapper = WSRecepcionPool.getInstance().checkOut();

			WSEFacturaEFACRECEPCIONSOBRE input = new WSEFacturaEFACRECEPCIONSOBRE();
			Data data = new Data();
			data.setXmlData(cfe);
			input.setDatain(data);

			WSEFacturaEFACRECEPCIONSOBREResponse output = portWrapper.getPort().efacrecepcionsobre(input);

			WSRecepcionPool.getInstance().checkIn(portWrapper);

			Data response = output.getDataout();

			// Dump sobre y respuesta a disco
			for (int i = 0; i < envioCFE.getCVES().size(); i++) {
				Commons.dumpSobreToFile(envioCFE, i, false, data);
			}

			return response;
		} catch (Exception e) {
			throw EFacturaException.raise(e);
		}

	}

	private String toString(EnvioCFE envioCFE) throws JAXBException {
		JAXBContext context = JAXBContext.newInstance(EnvioCFE.class);
		Marshaller marshaller = context.createMarshaller();
		StringWriter sw = new StringWriter();
		marshaller.marshal(envioCFE, sw);
		String cfe = sw.toString();
		return cfe;
	}

	private Document toDocument(EnvioCFEEntreEmpresas envioCFE) throws JAXBException, ParserConfigurationException {
		 DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		    dbf.setNamespaceAware(true);
		    Document doc = dbf.newDocumentBuilder().newDocument(); 

		    JAXBContext context = JAXBContext.newInstance(envioCFE.getClass());
		    context.createMarshaller().marshal(envioCFE, doc);

		    return doc;
	}

	private void enviarCFEaEmpresa(EnvioCFEEntreEmpresas envioCFEEntreEmpresas) throws EFacturaException {
		try {
			/*
			 * TODO Se deberia enviar un mail a la empresa.
			 * 
			 * el mail sale de hacer una consulta por RUT a la DGI
			 * 
			 */
			Document allDocument = toDocument(envioCFEEntreEmpresas);

			/*
			 * Instantiate the DocumentBuilderFactory.
			 * IMPORTANT: NamespaceAwerness=true!!
			 */
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			
			allDocument = XML.loadXMLFromString(NamespacesInterceptor.doNamespaceChanges(XML.documentToString(allDocument)));

			SignatureInterceptor.signDocument(dbf, allDocument,"ns0:CFE","DGICFE:CFE_Adenda");
			
			String filenamePrefix = Commons.getFilenamePrefix(allDocument.getDocumentElement());
			
			// Dump sobre a disco
			Commons.dumpNodeToFile(allDocument, true, filenamePrefix, null);
			
			
		} catch (Exception e) {
			e.printStackTrace();
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
				 * En unas respuestas raras de la DGI no aparece el campo
				 * estado, si para eso lo tratamos como un documento a anular.
				 */

				if (response.contains("<Estado>")) {
					String estado = response.split("<Estado>")[1].split("</Estado>")[0];

					// Hay unos casos raros en que la DGI no retorna un valor en
					// el campo token. Retorna solo el tag <Token/>
					if (estado.equals("AS") && !response.contains("<Token/>")) {

						String token = response.split("<Token>")[1].split("</Token>")[0];

						String idReceptor = response.split("<IDReceptor>")[1].split("</IDReceptor>")[0];

						Data result = consultaEstado(token, idReceptor);

						IO.writeFile(folder + File.separator + serie + "_" + nro + "_result.xml",
								PrettyPrint.prettyPrintXML(result.getXmlData()));

						return result;
					}
				}
			} else {

				Data data = new Data();
				data.setXmlData(IO.readFile(docRsesult.getCanonicalPath(), Charset.forName("UTF-8")));
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
					ResultadoConsulta result = new ResultadoConsulta(TipoDoc.fromInt(Integer.parseInt(tipoDoc)), 0, 0,
							0, 0, 0);
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
								if (data == null)
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

	// TODO rehacer nuevamente
	@Override
	public void anularDocumento(TipoDoc tipo, String serie, int nro, Date date) throws EFacturaException {
		try {
			JSONObject caeDataJson = CAEManagerImpl.getInstance().getCaeJson(tipo);

			SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");

			String anuladoFilePath = Settings.getInstance().getString(Constants.GENERATED_CFE_FOLDER,
					"resources" + File.separator + "cfe") + File.separator + formatter.format(date) + File.separator
					+ tipo.value + File.separator + serie + "_" + nro + "_unsigned.xml";
			File anuladoFile = new File(anuladoFilePath);

			if (!anuladoFile.exists()) {
				IO.writeFile(anuladoFilePath, "narf");
			}

		} catch (IOException e) {
			throw EFacturaException.raise(e);
		}

	}

	// metodo para pasar la prueba de homologacion
	@Override
	public void anularNextDocumento(TipoDoc tipo, Date date) throws EFacturaException {
		try {
			IdDocFact id = CAEManagerImpl.getInstance().getIdDocFact(tipo, false, 2);

			// TODO la forma de generar las anulaciones es generar un archivo
			// dummy, esto es una cagada porque hay que borrar los archivos
			// dummys luego de generar el reporte diario y antes de subir los
			// datos a DGI

			SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");

			String anuladoFilePath = Settings.getInstance().getString(Constants.GENERATED_CFE_FOLDER,
					"resources" + File.separator + "cfe") + File.separator + formatter.format(date) + File.separator
					+ tipo.value + File.separator + id.getSerie() + "_" + id.getNro() + "_unsigned.xml";
			File anuladoFile = new File(anuladoFilePath);

			if (!anuladoFile.exists()) {
				IO.writeFile(anuladoFilePath, "narf");
			}

		} catch (IOException e) {
			throw EFacturaException.raise(e);
		} catch (DatatypeConfigurationException e) {
			throw EFacturaException.raise(e);
		}

	}

}
