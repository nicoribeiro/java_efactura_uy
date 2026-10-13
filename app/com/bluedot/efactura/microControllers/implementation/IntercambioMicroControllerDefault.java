package com.bluedot.efactura.microControllers.implementation;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;

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
import com.bluedot.efactura.recibidos.ControlesRecepcion;
import com.bluedot.efactura.recibidos.ControlesSobre;
import com.bluedot.efactura.recibidos.MontosRecibidos;
import com.bluedot.efactura.recibidos.RespuestaCfes;
import com.bluedot.efactura.Constants;
import com.bluedot.efactura.Environment;
import com.bluedot.efactura.commons.Commons;
import com.bluedot.efactura.interceptors.SignatureInterceptor;
import com.bluedot.efactura.microControllers.interfaces.CFEMicroController;
import com.bluedot.efactura.microControllers.interfaces.IntercambioMicroController;
import com.bluedot.efactura.model.EnvioMail;
import com.bluedot.efactura.model.CFE;
import com.bluedot.efactura.model.DireccionDocumento;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.model.FirmaDigital;
import com.bluedot.efactura.model.MotivoRechazoCFE;
import com.bluedot.efactura.model.MotivoRechazoSobre;
import com.bluedot.efactura.model.Respuesta;
import com.bluedot.efactura.model.SobreRecibido;
import com.bluedot.efactura.model.TipoDoc;
import com.bluedot.efactura.serializers.CFEEmpresasTypeWrapper;
import com.bluedot.efactura.serializers.EfacturaJSONSerializerProvider;

import dgi.classes.recepcion.CFEDefType;
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
import dgi.classes.respuestas.sobre.RechazoSobreType;
import play.Play;

public class IntercambioMicroControllerDefault implements IntercambioMicroController {

	final static Logger logger = LoggerFactory.getLogger(IntercambioMicroControllerDefault.class);
	
	private CFEMicroController cfeMicroController;

	public IntercambioMicroControllerDefault(CFEMicroController cfeMicroController) {
		this.cfeMicroController = cfeMicroController;
	}
	
	
	@Override
	public ACKSobredefType procesarSobre(Empresa empresa, SobreRecibido sobreRecibido, Document documentCrudo) throws APIException {
		
	
			
			try {
				sobreRecibido.setTimestampRecibido(new Date());
				
				EnvioCFEEntreEmpresas envioCFEEntreEmpresas = sobreRecibido.getEnvioCFEEntreEmpresas(); 
				sobreRecibido.setIdEmisor(envioCFEEntreEmpresas.getCaratula().getIdemisor().longValue());
				sobreRecibido.setFecha(envioCFEEntreEmpresas.getCaratula().getFecha().toGregorianCalendar().getTime());
				
				ACKSobredefType ackSobredefType = new ACKSobredefType();
				ackSobredefType.setVersion("1.0");
				
				Respuesta respuestaSobre = new Respuesta();
				sobreRecibido.setRespuestaSobre(respuestaSobre);
				/*
				 * solo se asigna el id
				 * ver: https://stackoverflow.com/questions/25862537/hibernate-persist-vs-save-method
				 */
				play.db.jpa.JPA.em().persist(respuestaSobre);
				sobreRecibido.update();
				respuestaSobre.setNombreArchivo("M_" + respuestaSobre.getId() + "_" + sobreRecibido.getNombreArchivo());
				
				/*
				 * Caratula
				 */
				Caratula caratula = new Caratula();
				caratula.setCantidadCFE(envioCFEEntreEmpresas.getCaratula().getCantCFE());
				caratula.setFecHRecibido(DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()));
				caratula.setTmst(DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()));
				caratula.setIDReceptor(BigInteger.valueOf(sobreRecibido.getId()));
				caratula.setIDRespuesta(new BigInteger(String.valueOf(respuestaSobre.getId())));
				caratula.setIDEmisor(envioCFEEntreEmpresas.getCaratula().getIdemisor());
				caratula.setNomArch(sobreRecibido.getNombreArchivo());
				caratula.setRUCReceptor(sobreRecibido.getEmpresaReceptora().getRut());
				caratula.setRUCEmisor(envioCFEEntreEmpresas.getCaratula().getRUCEmisor());
				ackSobredefType.setCaratula(caratula);
				ackSobredefType.setDetalle(new Detalle());
				
				/*
				 * CONTROLES DEL SOBRE (ver ControlesSobre: cuales se implementan, cuales no y por que)
				 */
				if (!envioCFEEntreEmpresas.getCaratula().getRutReceptor().equals(empresa.getRut()))
					rechazarSobre(ackSobredefType, sobreRecibido, MotivoRechazoSobre.S02,
							"el sobre es para el RUC " + envioCFEEntreEmpresas.getCaratula().getRutReceptor() + ", no para " + empresa.getRut());

				ControlesSobre.Sobre xmlSobre = null;
				try {
					xmlSobre = ControlesSobre.leer(sobreRecibido.getXmlEmpresa());
				} catch (Exception e) {
					logger.warn("No se pudo leer el XML del sobre " + sobreRecibido.getNombreArchivo() + " para los controles: " + e.getMessage());
				}
				if (xmlSobre != null) {
					if (ControlesSobre.modo("S02") != ControlesRecepcion.Modo.OFF)
						aplicarControlSobre(ackSobredefType, sobreRecibido, MotivoRechazoSobre.S02, ControlesSobre.s02(xmlSobre));
					if (ControlesSobre.modo("S03") != ControlesRecepcion.Modo.OFF)
						aplicarControlSobre(ackSobredefType, sobreRecibido, MotivoRechazoSobre.S03, ControlesSobre.s03(xmlSobre));
					if (ControlesSobre.modo("S04") != ControlesRecepcion.Modo.OFF)
						aplicarControlSobre(ackSobredefType, sobreRecibido, MotivoRechazoSobre.S04, ControlesSobre.s04(xmlSobre));
					if (ControlesSobre.modo("S05") != ControlesRecepcion.Modo.OFF)
						aplicarControlSobre(ackSobredefType, sobreRecibido, MotivoRechazoSobre.S05, ControlesSobre.s05(xmlSobre));
					if (ControlesSobre.modo("S06") != ControlesRecepcion.Modo.OFF)
						aplicarControlSobre(ackSobredefType, sobreRecibido, MotivoRechazoSobre.S06, ControlesSobre.s06(xmlSobre));
				}
				if (ControlesSobre.modo("S20") != ControlesRecepcion.Modo.OFF)
					aplicarControlSobre(ackSobredefType, sobreRecibido, MotivoRechazoSobre.S20,
							ControlesSobre.s20(sobreRecibido, envioCFEEntreEmpresas.getCaratula().getIdemisor().longValue()));
				
				if (ackSobredefType.getDetalle().getMotivosRechazo().size()==0){
					/*
					 * Se acepta el sobre!
					 */
					sobreRecibido.setEstadoEmpresa(EstadoACKSobreType.AS);
					ackSobredefType.getDetalle().setEstado(EstadoACKSobreType.AS);
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

				if (empresa.getFirmaDigital()==null)
					throw APIException.raise(APIErrors.MISSING_PARAMETER).withParams("FirmaDigital").setDetailMessage("La Empresa no tiene Firma Digital");
				
				allDocument = SignatureInterceptor.signDocument(dbf, allDocument,"ACKSobre",null, empresa.getFirmaDigital().getKeyStore(), FirmaDigital.KEY_ALIAS, FirmaDigital.KEYSTORE_PASSWORD);
				
				respuestaSobre.setPayload(XML.documentToString(allDocument));
				respuestaSobre.update();
				
				/*
				 * Envio la respuesta al emisor
				 */
				if (sobreRecibido.getEmpresaEmisora().getMailRecepcion()!=null && !sobreRecibido.getEmpresaEmisora().getMailRecepcion().isEmpty())
					Commons.enviarMail(empresa, sobreRecibido.getEmpresaEmisora().getMailRecepcion(), respuestaSobre.getNombreArchivo(), respuestaSobre.getPayload().getBytes(), "",
							EnvioMail.Contexto.automatico(EnvioMail.Tipo.ACK_SOBRE).sobre(sobreRecibido.getId()));
				
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
				
				/*
				 * Si el sobre no fue rechazado con errores S0X entonces proceso los CFE internos
				 */
				if (ackSobredefType.getDetalle().getMotivosRechazo().size()==0){
					
					ACKCFEdefType ackcfEdefType = new ACKCFEdefType();
					addCaratula(empresa, ackcfEdefType, envioCFEEntreEmpresas, filename);
					ackcfEdefType.setVersion("1.0");
					
					/*
					 * Proceso uno a uno todos los CFE dentro del sobre
					 */
					int i = 1;
					for (Iterator<CFEEmpresasType> iterator = envioCFEEntreEmpresas.getCFEAdendas().iterator(); iterator.hasNext();) {
						CFEEmpresasType cfeEmpresasType = iterator.next();
						procesarCfeEntrante(new CFEEmpresasTypeWrapper(cfeEmpresasType, sobreRecibido.getEmpresaReceptora()), ackcfEdefType, i, sobreRecibido);
						i++;
					}
					
					/*
					 * La respuesta a los CFE (ME_) no se envia ahora: queda pendiente hasta que se cumpla el plazo de la empresa
					 * (en ese tiempo se puede rechazar un CFE desde el panel) y la envia RespuestaCfePoller. Con plazo 0 se envia
					 * enseguida. Ver RespuestaCfes.
					 */
					sobreRecibido.setRespuestaCfePendiente(true);
					sobreRecibido.update();
					if (RespuestaCfes.dias(empresa) == 0) {
						try {
							RespuestaCfes.enviar(sobreRecibido, "automático");
						} catch (APIException e) {
							// Queda pendiente: la reintenta RespuestaCfePoller
							logger.warn("No se pudo enviar la respuesta a los CFE del sobre " + sobreRecibido.getNombreArchivo() + ": " + e.getMessage());
						}
					}
					
					return ackcfEdefType;
				}
				return null;
			} catch (TransformerFactoryConfigurationError | Exception e) {
				throw APIException.raise(e);
			}
		
		
		//TODO si los servicios son la capa superior nunca deberian tirar exepciones distintas de APIException
	}

	private void procesarCfeEntrante(CFEEmpresasTypeWrapper cfeEmpresasTypeWrapper, ACKCFEdefType ackcfEdefType, int ordinal, SobreRecibido sobreRecibido) throws APIException {
		
		/*
		 * Serializo el CFEEmpresasType a JSONObject
		 */
		JSONObject cfeJson = EfacturaJSONSerializerProvider.getCFEEmpresasTypeSerializer().objectToJson(cfeEmpresasTypeWrapper);
		logger.debug("cfeJson: " + cfeJson.toString());
		
		TipoDoc tipoDoc = TipoDoc.fromInt(cfeJson.getJSONObject("Encabezado").getJSONObject("IdDoc").getInt("TipoCFE"));
		
		/*
		 * Creo un CFE de mi modelo
		 */
 		CFE cfe = cfeMicroController.create(tipoDoc, cfeJson, false);
 		// Los montos se toman del XML del emisor: los que calcula el builder no consideran descuentos ni redondeos
 		MontosRecibidos.aplicar(cfe, cfeEmpresasTypeWrapper.getCfeEmpresasType().getCFE(), true);
 		cfe.setSobreRecibido(sobreRecibido);
 		cfe.setOrdinal(ordinal);
		
 		/*
 		 * Por defecto se acepta, luego en los controles se cambia de estado si corresponde
 		 */
 		cfe.setEstado(EstadoACKCFEType.AE);
 		RechazoCFEDGIType rechazo = null;
 		
 		//TODO HACK!!!!
 		Environment env = Environment.valueOf(Play.application().configuration().getString(Constants.ENVIRONMENT));
		
		if (env==Environment.test) {
	 		if (ordinal == 2 || ordinal == 4 || ordinal == 6 || ordinal == 8) {
				cfe.setEstado(EstadoACKCFEType.BE);
				rechazo = new RechazoCFEDGIType();
				rechazo.setMotivo("E05");
				rechazo.setGlosa("No cumple validaciones (*) de Formato comprobantes");
				cfe.getMotivo().add(MotivoRechazoCFE.E05);
	 		}
		} else {
			/*
			 * CONTROLES DE RECEPCION (ver ControlesRecepcion). Segun la tabla 4.3 del formato de DGI (motivos de rechazo
			 * de un CFE), el receptor puede usar E02 a E07 y E10 a E12; E01 y E08, E09, E13 a E15 son solo de DGI. Se
			 * implementan E02, E03, E04, E05, E07, E10 y E12. No se implementan:
			 * 
			 * E06 (importe excede montos habituales): "habitual" no esta definido por DGI y depende de cada relacion
			 * comercial; un umbral contra el historial del emisor daria falsos positivos (compras puntuales grandes) y
			 * rechazar por eso le corresponde a una persona, no a un control automatico.
			 * 
			 * E11 (tipo de CAE especial no se corresponde con el CAE): lo unico que se puede controlar con el CFE es la
			 * coherencia interna de CAEEspecial, CausalCAEEsp, IndMontoBruto y los indicadores de facturacion, que DGI ya
			 * valida antes de aceptar el CFE; el tipo de CAE que DGI le asigno al emisor no se puede consultar.
			 */
			
			/*
			 * E02: el mismo CFE (emisor, tipo, serie y numero) ya se recibio y acepto. Se busca entre los recibidos por la
			 * empresa (antes se buscaba entre los emitidos y nunca encontraba nada), sin contar al que se esta procesando.
			 */
	 		if (ControlesRecepcion.modo("E02") != ControlesRecepcion.Modo.OFF)
	 			rechazo = aplicarControl(cfe, rechazo, MotivoRechazoCFE.E02, ControlesRecepcion.e02(cfe, sobreRecibido.getEmpresaReceptora()));
	 		
	 		/*
			 * E03: el numero del CFE esta en el rango del CAE (CAEData)
			 */
	 		CFEDefType xmlCfe = cfeEmpresasTypeWrapper.getCfeEmpresasType().getCFE();
	 		if (ControlesRecepcion.modo("E03") != ControlesRecepcion.Modo.OFF)
	 			rechazo = aplicarControl(cfe, rechazo, MotivoRechazoCFE.E03, ControlesRecepcion.e03(xmlCfe, cfe.getNro()));
	 		
	 		/*
			 * E04: firma electronica del CFE valida (con el certificado de la firma, vigente al firmar)
			 */
	 		if (ControlesRecepcion.modo("E04") != ControlesRecepcion.Modo.OFF)
	 			rechazo = aplicarControl(cfe, rechazo, MotivoRechazoCFE.E04, ControlesRecepcion.e04(sobreRecibido.getXmlEmpresa(), ordinal, xmlCfe));
	 		
	 		/*
			 * E05: formato del CFE (XSD de DGI, RecepcionCFE.formato.xsd)
			 */
	 		if (ControlesRecepcion.modo("E05") != ControlesRecepcion.Modo.OFF)
	 			rechazo = aplicarControl(cfe, rechazo, MotivoRechazoCFE.E05, ControlesRecepcion.e05(sobreRecibido.getXmlEmpresa(), ordinal));
	 		
	 		/*
			 * E07: fecha de firma del CFE dentro de la vigencia del CAE
			 */
	 		if (ControlesRecepcion.modo("E07") != ControlesRecepcion.Modo.OFF)
	 			rechazo = aplicarControl(cfe, rechazo, MotivoRechazoCFE.E07, ControlesRecepcion.e07(xmlCfe));
	 		
	 		/*
			 * E10: tipo de cambio valido (y cerca del interbancario de INE para USD)
			 */
	 		if (ControlesRecepcion.modo("E10") != ControlesRecepcion.Modo.OFF)
	 			rechazo = aplicarControl(cfe, rechazo, MotivoRechazoCFE.E10, ControlesRecepcion.e10(xmlCfe));
	 		
	 		/*
			 * E12: los documentos referenciados (zona de referencia) estan en los recibidos de este emisor
			 */
	 		if (ControlesRecepcion.modo("E12") != ControlesRecepcion.Modo.OFF)
	 			rechazo = aplicarControl(cfe, rechazo, MotivoRechazoCFE.E12, ControlesRecepcion.e12(xmlCfe, sobreRecibido.getEmpresaReceptora(), cfe.getEmpresaEmisora()));
		}
		
		/*
		 * Genero la respuesta
		 */
		ACKCFEDet respuestaCFE = buildAck(new BigInteger(String.valueOf(ordinal)), tipoDoc, cfe, rechazo);
		ackcfEdefType.getACKCFEDet().add(respuestaCFE);
		
		/*
		 * Registro el resultado
		 */
		sumarizarResultado(cfe.getEstado(), ackcfEdefType);
		
		
		cfe.save();
	}


	/**
	 * Aplica el resultado de un control de recepcion (ControlesRecepcion) al CFE: si no paso (detalle != null) lo registra
	 * en el log y en las observaciones del CFE y, en modo ENFORCE, lo rechaza con ese motivo (si no tenia ya otro rechazo).
	 *
	 * @return el rechazo del CFE (el que tenia o el nuevo)
	 */
	private RechazoCFEDGIType aplicarControl(CFE cfe, RechazoCFEDGIType rechazo, MotivoRechazoCFE motivo, String detalle) {
		if (detalle == null)
			return rechazo;
		ControlesRecepcion.Modo modo = ControlesRecepcion.modo(motivo.name());
		logger.warn("CONTROL " + motivo.name() + " (" + modo + ") CFE " + cfe.getTipo().value + " " + cfe.getSerie() + "-" + cfe.getNro()
				+ " de " + (cfe.getEmpresaEmisora() != null ? cfe.getEmpresaEmisora().getRut() : "?") + ": " + detalle);
		ControlesRecepcion.observar(cfe, motivo.name() + (modo == ControlesRecepcion.Modo.LOG ? " (observado)" : ""), detalle);
		if (modo == ControlesRecepcion.Modo.ENFORCE && rechazo == null) {
			rechazo = new RechazoCFEDGIType();
			rechazo.setMotivo(motivo.name());
			rechazo.setGlosa(motivo.getMotivo());
			cfe.getMotivo().add(motivo);
			cfe.setEstado(EstadoACKCFEType.BE);
		}
		return rechazo;
	}

	/**
	 * Aplica el resultado de un control del sobre (ControlesSobre): si no paso (detalle != null) lo registra en el log y en
	 * las observaciones del sobre y, en modo ENFORCE, lo rechaza con ese motivo.
	 */
	private void aplicarControlSobre(ACKSobredefType ack, SobreRecibido sobre, MotivoRechazoSobre motivo, String detalle) {
		if (detalle == null)
			return;
		ControlesRecepcion.Modo modo = ControlesSobre.modo(motivo.name());
		if (modo == ControlesRecepcion.Modo.ENFORCE) {
			rechazarSobre(ack, sobre, motivo, detalle);
			return;
		}
		logger.warn("CONTROL " + motivo.name() + " (" + modo + ") sobre " + sobre.getNombreArchivo() + ": " + detalle);
		observarSobre(sobre, motivo.name() + " (observado)", detalle);
	}

	/** Rechaza el sobre con ese motivo: la glosa es la de la tabla de DGI y el detalle va aparte (hasta 500 caracteres) */
	private void rechazarSobre(ACKSobredefType ack, SobreRecibido sobre, MotivoRechazoSobre motivo, String detalle) {
		logger.warn("CONTROL " + motivo.name() + " (ENFORCE) sobre " + sobre.getNombreArchivo() + ": " + detalle);
		observarSobre(sobre, motivo.name(), detalle);
		RechazoSobreType rechazo = new RechazoSobreType();
		rechazo.setMotivo(motivo.name());
		rechazo.setGlosa(motivo.getMotivo());
		if (detalle != null)
			rechazo.setDetalle(detalle.length() > 500 ? detalle.substring(0, 497) + "..." : detalle);
		ack.getDetalle().getMotivosRechazo().add(rechazo);
	}

	private static void observarSobre(SobreRecibido sobre, String control, String detalle) {
		String linea = control + ": " + detalle;
		String actuales = sobre.getObservacionesRecepcion();
		sobre.setObservacionesRecepcion(actuales == null || actuales.isEmpty() ? linea : actuales + "; " + linea);
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
		
		try {
			ack.setFechaCFE(DatatypeFactory.newInstance().newXMLGregorianCalendar(new SimpleDateFormat("yyyy-MM-dd").format(cfe.getFechaEmision())));
			ack.setTmstCFE(DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()));
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

	private void addCaratula(Empresa empresa, ACKCFEdefType ackcfEdefType, EnvioCFEEntreEmpresas envioCFEEntreEmpresas, String filename) throws DatatypeConfigurationException {
		dgi.classes.respuestas.cfe.ACKCFEdefType.Caratula caratula = new dgi.classes.respuestas.cfe.ACKCFEdefType.Caratula();

		caratula.setFecHRecibido(DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()));
		caratula.setTmst(DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()));
		//TODO hace un incremental aca
		caratula.setIDReceptor(new BigInteger("1"));
		caratula.setIDRespuesta(new BigInteger("1"));
		caratula.setIDEmisor(envioCFEEntreEmpresas.getCaratula().getIdemisor());
		caratula.setNomArch(filename);
		caratula.setRUCReceptor(empresa.getRut());
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
