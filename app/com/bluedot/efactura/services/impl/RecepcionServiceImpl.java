package com.bluedot.efactura.services.impl;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.commons.notificationChannels.MessagingHelper;
import com.bluedot.commons.utils.ThreadMan;
import com.bluedot.commons.utils.XML;
import com.bluedot.efactura.commons.Commons;
import com.bluedot.efactura.commons.EfacturaSecurity;
import com.bluedot.efactura.interceptors.InterceptorContextHolder;
import com.bluedot.efactura.interceptors.NamespacesInterceptor;
import com.bluedot.efactura.interceptors.SignatureInterceptor;
import com.bluedot.efactura.model.CFE;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.model.MotivoRechazoCFE;
import com.bluedot.efactura.model.ReporteDiario;
import com.bluedot.efactura.model.SobreEmitido;
import com.bluedot.efactura.model.TipoDoc;
import com.bluedot.efactura.pool.WSEFacturaSoapPortWrapper;
import com.bluedot.efactura.pool.WSRecepcionPool;
import com.bluedot.efactura.services.RecepcionService;
import com.bluedot.efactura.strategy.report.SummaryStrategy;
import com.sun.istack.logging.Logger;

import dgi.classes.entreEmpresas.CFEEmpresasType;
import dgi.classes.entreEmpresas.EnvioCFEEntreEmpresas;
import dgi.classes.recepcion.CFEDefType;
import dgi.classes.recepcion.EnvioCFE;
import dgi.classes.recepcion.EnvioCFE.Caratula;
import dgi.classes.recepcion.ObjectFactory;
import dgi.classes.reporte.ReporteDefType;
import dgi.classes.respuestas.cfe.ACKCFEdefType;
import dgi.classes.respuestas.cfe.ACKCFEdefType.ACKCFEDet;
import dgi.classes.respuestas.cfe.EstadoACKCFEType;
import dgi.classes.respuestas.cfe.RechazoCFEDGIType;
import dgi.classes.respuestas.reporte.ACKRepDiariodefType;
import dgi.classes.respuestas.sobre.ACKSobredefType;
import dgi.classes.respuestas.sobre.EstadoACKSobreType;
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
	public void sendCFE(CFE cfe, String adenda) throws APIException {
		//TODO soportar mas de un CFE por sobre
		
		/*
		 * Creo el CFEDefType
		 */
		CFEDefType cfeDefType = (new ObjectFactory()).createCFEDefType();
		cfeDefType.setVersion("1.0");
		
		
		switch (cfe.getTipo()) {

		case eFactura:
		case eFactura_Contingencia:
		case Nota_de_Credito_de_eFactura:
		case Nota_de_Credito_de_eFactura_Contingencia:
		case Nota_de_Debito_de_eFactura:
		case Nota_de_Debito_de_eFactura_Contingencia:
			cfeDefType.setEFact(cfe.getEfactura());
			break;
		case eResguardo:
		case eResguardo_Contingencia:
			cfeDefType.setEResg(cfe.getEresguardo());
			break;
		case eTicket:
		case eTicket_Contingencia:
		case Nota_de_Debito_de_eTicket:
		case Nota_de_Debito_de_eTicket_Contingencia:
		case Nota_de_Credito_de_eTicket:
		case Nota_de_Credito_de_eTicket_Contingencia:
			cfeDefType.setETck(cfe.getEticket());
			break;
		case eFactura_Exportacion:
		case eFactura_Exportacion_Contingencia:
		case eFactura_Venta_por_Cuenta_Ajena:
		case eFactura_Venta_por_Cuenta_Ajena_Contingencia:
		case eRemito:
		case eRemito_Contingencia:
		case eRemito_de_Exportacion:
		case eRemito_de_Exportacion_Contingencia:
		case eTicket_Venta_por_Cuenta_Ajena:
		case eTicket_Venta_por_Cuenta_Ajena_Contingencia:
		case Nota_de_Debito_de_eTicket_Venta_por_Cuenta_Ajena:
		case Nota_de_Debito_de_eTicket_Venta_por_Cuenta_Ajena_Contingencia:
		case Nota_de_Debito_de_eFactura_Venta_por_Cuenta_Ajena:
		case Nota_de_Debito_de_eFactura_Venta_por_Cuenta_Ajena_Contingencia:
		case Nota_de_Credito_de_eTicket_Venta_por_Cuenta_Ajena:
		case Nota_de_Credito_de_eTicket_Venta_por_Cuenta_Ajena_Contingencia:
		case Nota_de_Credito_de_eFactura_Venta_por_Cuenta_Ajena:
		case Nota_de_Credito_de_eFactura_Venta_por_Cuenta_Ajena_Contingencia:
		case Nota_de_Debito_de_eFactura_Exportacion:
		case Nota_de_Debito_de_eFactura_Exportacion_Contingencia:
		case Nota_de_Credito_de_eFactura_Exportacion:
		case Nota_de_Credito_de_eFactura_Exportacion_Contingencia:
			throw APIException.raise(APIErrors.NOT_SUPPORTED).setDetailMessage("Envio de CFE a DGI de tipo " + cfe.getTipo().value);
		}
		
		/*
		 * Creo el sobre que contiene los CFEs
		 */
		SobreEmitido sobre = new SobreEmitido(cfe.getEmpresaEmisora(), cfe.getEmpresaReceptora(), "", 1, null);
		sobre.getCfes().add(cfe);
		sobre.setFecha(new Date());
		cfe.setSobre(sobre);
		sobre.save();
		
		/*
		 * Se necesita el id del sobre para generar el nombre de archivo, y el idEmisor
		 * por lo tanto debemos forzar la escritura a la BBDD para que se
		 * genere el id.
		 */
		ThreadMan.forceTransactionFlush();
		
		/*
		 * Creo EnvioCFE.
		 */
		EnvioCFE envioCFE = new EnvioCFE();
		envioCFE.setVersion("1.0");
		envioCFE.getCVES().add(cfeDefType);
		sobre.setEnvioCFE(envioCFE);
		addCaratulaSobre(sobre);
		

		/*
		 * Creo EnvioCFEEntreEmpresas
		 */
		if (cfe.getEmpresaReceptora() != null && cfe.getEmpresaReceptora().isEmisorElectronico()) {
			CFEEmpresasType cfeEmpresasType = (new dgi.classes.entreEmpresas.ObjectFactory()).createCFEEmpresasType();
			cfeEmpresasType.setCFE(cfeDefType);
			cfeEmpresasType.setAdenda(adenda);
			EnvioCFEEntreEmpresas envioCFEEntreEmpresas = (new dgi.classes.entreEmpresas.ObjectFactory())
					.createEnvioCFEEntreEmpresas();
			envioCFEEntreEmpresas.setVersion("1.0");
			envioCFEEntreEmpresas.getCFEAdendas().add(cfeEmpresasType);
			addCaratulaSobre(envioCFEEntreEmpresas);
			sobre.setEnvioCFEEntreEmpresas(envioCFEEntreEmpresas);
			
		}
		
		/*
		 * Envio a la DGI
		 */
		this.enviarSobreDGI(sobre);
		
		/*
		 * Envio a la empresa
		 */
		if (cfe.getEmpresaReceptora() != null && cfe.getEmpresaReceptora().isEmisorElectronico())
			this.enviarSobreEmpresa(sobre);

	}

	private void addCaratulaSobre(SobreEmitido sobre) throws APIException {
		try {
			EnvioCFE envioCFE = sobre.getEnvioCFE();
			
			List<CFEDefType> cfes = envioCFE.getCVES();

			String RUCemisor = null;

			String RUCreceptor = null;

			for (Iterator<CFEDefType> iterator = cfes.iterator(); iterator.hasNext();) {
				CFEDefType cfeDefType = iterator.next();

				String rucEmisor = null;
				String rucReceptor = Commons.getRucDGI();

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
					throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE.withParams("RUCemisor"))
							.setDetailMessage("Cannot have many RUCemisor values on one envelope");

				if (RUCreceptor == null)
					RUCreceptor = rucReceptor;
				else if (!RUCemisor.equals(rucReceptor))
					throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE.withParams("RUCreceptor"))
							.setDetailMessage("Cannot have many RUCreceptor on one envelope");

			}

			Caratula caratula = (new ObjectFactory()).createEnvioCFECaratula();

			caratula.setCantCFE(cfes.size());
			caratula.setFecha(DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()));
			caratula.setIdemisor(new BigInteger(String.valueOf(sobre.getId())));
			caratula.setRUCEmisor(RUCemisor);
			caratula.setRutReceptor(RUCreceptor);
			caratula.setVersion("1.0");
			caratula.setX509Certificate(EfacturaSecurity.getCertificate(Commons.getCetificateAlias(),
					Commons.getCertificatePassword(), Commons.getKeyStore()));

			envioCFE.setCaratula(caratula);
		} catch (Exception e) {
			throw APIException.raise(e);
		}

	}

	private void addCaratulaSobre(EnvioCFEEntreEmpresas signed) throws APIException {
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
					throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE)
							.setDetailMessage("Cannot have many RUCemisor values on one envelope");

				if (RUCreceptor == null)
					RUCreceptor = rucReceptor;
				else if (!RUCemisor.equals(rucReceptor))
					throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE)
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
			throw APIException.raise(e);
		}

	}

	private Data enviarSobreDGI(SobreEmitido sobre) throws APIException {

		try {
			String xmlSobre = XML.objectToString(sobre.getEnvioCFE());
			sobre.setXmlDgi(xmlSobre);

			/*
			 * Colocamos en ThreadLocal al Sobre es la forma de pasarle
			 * parametros a los Interceptors
			 */
			InterceptorContextHolder.setSobreEmitido(sobre);

			Data response;
			try {
				WSEFacturaSoapPortWrapper portWrapper = WSRecepcionPool.getInstance().checkOut();

				WSEFacturaEFACRECEPCIONSOBRE input = new WSEFacturaEFACRECEPCIONSOBRE();
				Data data = new Data();
				data.setXmlData(xmlSobre);
				input.setDatain(data);

				WSEFacturaEFACRECEPCIONSOBREResponse output = portWrapper.getPort().efacrecepcionsobre(input);

				response = output.getDataout();

				WSRecepcionPool.getInstance().checkIn(portWrapper);
			} catch (Throwable e) {
				throw APIException.raise(APIErrors.ERROR_COMUNICACION_DGI, e);
			}

			/*
			 * Borramos el contexto
			 */
			InterceptorContextHolder.clear();

			sobre.setRespuesta_dgi(response.getXmlData());

			ACKSobredefType ACKSobre = (ACKSobredefType) XML.unMarshall(XML.loadXMLFromString(response.getXmlData()),
					ACKSobredefType.class);

			sobre.setIdReceptor(ACKSobre.getCaratula().getIDReceptor().longValue());

			sobre.setEstado(ACKSobre.getDetalle().getEstado());

			if (sobre.getEstado() == EstadoACKSobreType.AS) {
				sobre.setToken(ACKSobre.getDetalle().getParamConsulta().getToken());
			}

			return response;
		} catch (Exception e) {
			throw APIException.raise(e);
		}

	}

	private void enviarSobreEmpresa(SobreEmitido sobre)
			throws APIException {
		try {
			Document allDocument = XML.objectToDocument(sobre.getEnvioCFEEntreEmpresas());

			/*
			 * Instantiate the DocumentBuilderFactory. 
			 * 
			 * IMPORTANT:NamespaceAwerness=true!!
			 */
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);

			allDocument = XML
					.loadXMLFromString(NamespacesInterceptor.doNamespaceChanges(XML.documentToString(allDocument)));

			SignatureInterceptor.signDocument(dbf, allDocument, "ns0:CFE", "DGICFE:CFE_Adenda");

			sobre.setXmlEmpresa(XML.documentToString(allDocument));

			/*
			 * Se necesita el id del sobre para generar el nombre de archivo,
			 * por lo tanto debemos forzar la escritura a la BBDD para que se
			 * genere el id.
			 */
			ThreadMan.forceTransactionFlush();

			Map<String, String> attachments = new TreeMap<String, String>();

			attachments.put(sobre.getNombreArchivo(), sobre.getXmlEmpresa());
			sobre.update();

			MessagingHelper.sendEmail(sobre.getEmpresaReceptora().getMailRecepcion(),
					"Hay un nuevo CFE de " + sobre.getEmpresaEmisora().getRazon() + " disponible para usted", null,
					"Un nuevo CFE para usted", false, "", attachments);

		} catch (Exception e) {
			throw APIException.raise(e);
		}

	}

	@Override
	public Data consultaResultadoSobre(String token, Long idReceptor) throws APIException {

		if (token == null)
			throw APIException.raise(APIErrors.MISSING_PARAMETER.withParams("token"));
		if (idReceptor == null)
			throw APIException.raise(APIErrors.MISSING_PARAMETER.withParams("idReceptor"));

		try {
			WSEFacturaSoapPortWrapper portWrapper = WSRecepcionPool.getInstance().checkOut();

			WSEFacturaEFACCONSULTARESTADOENVIO input = new WSEFacturaEFACCONSULTARESTADOENVIO();
			String xml = "<ConsultaCFE xmlns=\"http://dgi.gub.uy\"><IdReceptor>" + idReceptor + "</IdReceptor><Token>"
					+ token.toString() + "</Token> </ConsultaCFE>";
			Data data = new Data();
			data.setXmlData(xml);

			input.setDatain(data);

			WSEFacturaEFACCONSULTARESTADOENVIOResponse output = portWrapper.getPort().efacconsultarestadoenvio(input);

			WSRecepcionPool.getInstance().checkIn(portWrapper);

			return output.getDataout();
		} catch (Throwable e) {
			throw APIException.raise(APIErrors.ERROR_COMUNICACION_DGI, e);
		}

	}

	@Override
	public void consultaResultadoSobre(SobreEmitido sobre) throws APIException {
		try {
			if (sobre.getToken()==null || sobre.getIdReceptor()==null)
				return;
			
			Data result = consultaResultadoSobre(sobre.getToken(), sobre.getIdReceptor());

			sobre.setResultado_dgi(result.getXmlData());
			sobre.update();

			ACKCFEdefType ACKcfe = (ACKCFEdefType) XML.unMarshall(XML.loadXMLFromString(result.getXmlData()),
					ACKCFEdefType.class);

			for (Iterator<ACKCFEDet> iterator = ACKcfe.getACKCFEDet().iterator(); iterator.hasNext();) {
				ACKCFEDet ACKcfeDet = iterator.next();
				CFE cfe = sobre.getCFE(ACKcfeDet.getNroCFE().longValue(), ACKcfeDet.getSerie(),
						TipoDoc.fromInt(ACKcfeDet.getTipoCFE().intValue()));
				if (cfe != null) {
					cfe.setEstado(ACKcfeDet.getEstado());
					cfe.update();
					if (cfe.getEstado() != EstadoACKCFEType.AE) {
						for (Iterator<RechazoCFEDGIType> iterator2 = ACKcfeDet.getMotivosRechazoCF()
								.iterator(); iterator2.hasNext();) {
							RechazoCFEDGIType rechazo = iterator2.next();
							cfe.getMotivo().add(MotivoRechazoCFE.valueOf(rechazo.getMotivo()));
						}

					}
				} else {
					// TODO ver que se hace si no encuentra al cfe dentro de los
					// CFE del sobre
				}
			}

		} catch (Exception e) {
			throw APIException.raise(e);
		}

	}

	//TODO agregar mutex
	@Override
	public ReporteDiario generarReporteDiario(Date fecha, Empresa empresa) throws APIException {
		try {
			if (fecha==null)
				throw APIException.raise(APIErrors.MISSING_PARAMETER.withParams("fecha"));

			/*
			 *  Consulto los resultados para los CFEs
			 */
			this.consultarResultados(fecha, empresa);

			SimpleDateFormat caratulaFormatter = new SimpleDateFormat("yyyy-MM-dd");


			/*
			 * Creo el Reporte Diario
			 */
			ReporteDefType reporte = new ReporteDefType();
			ReporteDiario reporteDiario = new ReporteDiario(empresa, fecha);
			reporteDiario.save();
			reporteDiario.setReporteDefType(reporte);
			
			/*
			 * Para que el id de ReporteDiario se asigne 
			 */
			ThreadMan.forceTransactionFlush();
			
			/*
			 * Caratula
			 */
			dgi.classes.reporte.ReporteDefType.Caratula caratula = new dgi.classes.reporte.ReporteDefType.Caratula();
			caratula.setCantComprobantes(new BigInteger("0"));
			caratula.setFechaResumen(
					DatatypeFactory.newInstance().newXMLGregorianCalendar(caratulaFormatter.format(fecha)));
			caratula.setIDEmisor(new BigInteger((String.valueOf(reporteDiario.getId()))));
			caratula.setRUCEmisor(empresa.getRut());
			caratula.setSecEnvio(new BigInteger(String.valueOf(reporteDiario.getSecuencial())));
			GregorianCalendar cal = new GregorianCalendar();
			cal.setTime(new Date());
			caratula.setTmstFirmaEnv(DatatypeFactory.newInstance().newXMLGregorianCalendar(cal));
			caratula.setVersion("1.0");
			reporte.setCaratula(caratula);

			/*
			 * Resumenes
			 */
			for (TipoDoc tipoDoc : TipoDoc.values()) {
					SummaryStrategy strategy = new SummaryStrategy.Builder().withTipo(tipoDoc).build();
					if (strategy!=null)
						strategy.buildSummary(empresa, reporte, fecha);
			}

			Data data = sendReporte(XML.objectToString(reporte), fecha);

			reporteDiario.setRespuesta(data.getXmlData());
			
			ACKRepDiariodefType ACKreporte = (ACKRepDiariodefType) XML.unMarshall(XML.loadXMLFromString(data.getXmlData()),
					ACKRepDiariodefType.class);
			
			reporteDiario.setIdReceptor(ACKreporte.getCaratula().getIDReceptor().toString());
			
			reporteDiario.setEstado(ACKreporte.getDetalle().getEstado());
			
			ThreadMan.forceTransactionFlush();
			
			return reporteDiario;

		} catch (Exception e) {
			throw APIException.raise(e);
		}

	}

	private Data sendReporte(String reporte, Date date) throws APIException {
		try {
			WSEFacturaSoapPortWrapper portWrapper = WSRecepcionPool.getInstance().checkOut();

			WSEFacturaEFACRECEPCIONREPORTE input = new WSEFacturaEFACRECEPCIONREPORTE();
			Data data = new Data();
			data.setXmlData(reporte);
			input.setDatain(data);

			WSEFacturaEFACRECEPCIONREPORTEResponse output = portWrapper.getPort().efacrecepcionreporte(input);

			WSRecepcionPool.getInstance().checkIn(portWrapper);

			return output.getDataout();
		} catch (Throwable e) {
			throw APIException.raise(APIErrors.ERROR_COMUNICACION_DGI, e);
		}
	}

//	private Data sendReporte(ReporteDefType reporte, Date date) throws APIException {
//		try {
//			JAXBContext context = JAXBContext.newInstance(ReporteDefType.class);
//			Marshaller marshaller = context.createMarshaller();
//			StringWriter sw = new StringWriter();
//			marshaller.marshal(reporte, sw);
//
//			Data response = this.sendReporte(PrettyPrint.prettyPrintXML(sw.toString()), date);
//
//			// Dump sobre y response to disk
//			Commons.dumpReporteToFile(reporte, false, response, date);
//
//			return response;
//		} catch (Exception e) {
//			throw APIException.raise(e);
//		}
//	}

	@Override
	public void consultarResultados(Date date, Empresa empresa) throws APIException {

		List<SobreEmitido> sobres = SobreEmitido.findByEmpresaEmisoraAndDate(empresa, date);

		for (Iterator<SobreEmitido> iterator = sobres.iterator(); iterator.hasNext();) {
			SobreEmitido sobre = iterator.next();
				this.consultaResultadoSobre((SobreEmitido) sobre);
			
		}

	}

//	@Override
//	public void anularDocumento(TipoDoc tipo, String serie, int nro, Date date, Empresa empresa) throws APIException {
//		try {
//			CAE caeDataJson = factory.getCAEMicroController(empresa).getCAE(tipo);
//
//			SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
//
//			String anuladoFilePath = Play.application().configuration().getString(Constants.GENERATED_CFE_FOLDER,
//					"resources" + File.separator + "cfe") + File.separator + formatter.format(date) + File.separator
//					+ tipo.value + File.separator + serie + "_" + nro + "_unsigned.xml";
//			File anuladoFile = new File(anuladoFilePath);
//
//			if (!anuladoFile.exists()) {
//				IO.writeFile(anuladoFilePath, "narf");
//			}
//
//		} catch (IOException e) {
//			throw APIException.raise(e);
//		}
//
//	}

	// TODO metodo para pasar la prueba de homologacion
//	@Override
//	public void anularNextDocumento(TipoDoc tipo, Date date, Empresa empresa) throws APIException {
//		try {
//			IdDocFact id = factory.getCAEMicroController(empresa).getIdDocFact(tipo, false, 2);
//
//			// TODO la forma de generar las anulaciones es generar un archivo
//			// dummy, esto es una cagada porque hay que borrar los archivos
//			// dummys luego de generar el reporte diario y antes de subir los
//			// datos a DGI
//
//			SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
//
//			String anuladoFilePath = Play.application().configuration().getString(Constants.GENERATED_CFE_FOLDER,
//					"resources" + File.separator + "cfe") + File.separator + formatter.format(date) + File.separator
//					+ tipo.value + File.separator + id.getSerie() + "_" + id.getNro() + "_unsigned.xml";
//			File anuladoFile = new File(anuladoFilePath);
//
//			if (!anuladoFile.exists()) {
//				IO.writeFile(anuladoFilePath, "narf");
//			}
//
//		} catch (IOException e) {
//			throw APIException.raise(e);
//		} catch (DatatypeConfigurationException e) {
//			throw APIException.raise(e);
//		}
//
//	}

}
