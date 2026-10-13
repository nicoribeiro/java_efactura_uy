package com.bluedot.efactura.pollers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.Date;
import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.utils.ExcelToJSONConverter2;
import com.bluedot.efactura.model.TipoCotizacion;
import com.bluedot.efactura.model.TipoDeCambio;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Provider;

import dgi.classes.recepcion.TipMonType;
import play.Application;

public class PollerTipoDeCambioRunner extends PollerRunner {

	
	static Logger logger = LoggerFactory.getLogger(PollerTipoDeCambioRunner.class);

	private String pattern = "yyyy-MM-dd";
	private SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
	
	private Provider<Application> applicationProvider;

	public PollerTipoDeCambioRunner(Provider<Application> applicationProvider) {
		super(logger, intervaloConfig(applicationProvider.get().configuration(), "TipoDeCambioPoller", 60));
		this.applicationProvider = applicationProvider;
	}
	
	/**
	 * Completa las cotizaciones USD que faltan (o les falta el interbancario) de los ultimos
	 * TipoDeCambioPoller.diasAtras dias (default 30), hasta ayer. Primero consulta la base en una sola consulta y solo
	 * descarga el archivo de INE si falta algo. Antes recorria 10 anios dia por dia y descargaba el archivo en cada
	 * ejecucion (cada 10 segundos).
	 */
	@Override
	protected void executeConcreteAction() throws APIException {
		int diasAtras = applicationProvider.get().configuration().getInt("TipoDeCambioPoller.diasAtras", 30);

		// De hace diasAtras dias hasta ayer (el dia actual no se carga: INE lo publica despues)
		GregorianCalendar hasta = new GregorianCalendar();
		hasta.set(GregorianCalendar.HOUR_OF_DAY, 0);
		hasta.set(GregorianCalendar.MINUTE, 0);
		hasta.set(GregorianCalendar.SECOND, 0);
		hasta.set(GregorianCalendar.MILLISECOND, 0);
		hasta.add(GregorianCalendar.DAY_OF_MONTH, -1);
		GregorianCalendar desde = (GregorianCalendar) hasta.clone();
		desde.add(GregorianCalendar.DAY_OF_MONTH, -(diasAtras - 1));

		Map<String, TipoDeCambio> existentes = new HashMap<String, TipoDeCambio>();
		for (TipoDeCambio tc : TipoDeCambio.findByMonedaEntre(TipMonType.USD, desde.getTime(), hasta.getTime()))
			existentes.put(simpleDateFormat.format(tc.getFecha()), tc);

		List<Date> faltantes = new ArrayList<Date>();
		List<TipoDeCambio> sinInterbancario = new ArrayList<TipoDeCambio>();
		for (GregorianCalendar dia = (GregorianCalendar) desde.clone(); !dia.after(hasta); dia.add(GregorianCalendar.DAY_OF_MONTH, 1)) {
			TipoDeCambio tc = existentes.get(simpleDateFormat.format(dia.getTime()));
			if (tc == null)
				faltantes.add(dia.getTime());
			else if (tc.getInterbancario() == null)
				sinInterbancario.add(tc);
		}

		if (faltantes.isEmpty() && sinInterbancario.isEmpty()) {
			estado.setResumen("Sin días faltantes en los últimos " + diasAtras + " días (hasta " + simpleDateFormat.format(hasta.getTime()) + ")");
			return;
		}

		JsonNode cotizaciones;
		try {
			cotizaciones = getCotizaciones();
		} catch (IOException e) {
			logger.error("No se pudo descargar el archivo de cotizaciones de INE", e);
			resultado("Faltan " + faltantes.size() + " día(s) y no se pudo descargar el archivo de INE: " + e.getMessage(), null);
			return;
		}

		int cargados = 0, sinDato = 0;
		for (Date fecha : faltantes) {
			LocalDate dia = new java.sql.Date(fecha.getTime()).toLocalDate();
			logger.info("Falta cotizacion para: " + simpleDateFormat.format(fecha) + ".");
			BigDecimal compra = getCotizacionPesosFecha(dia, cotizaciones, TipoCotizacion.COMPRA);
			BigDecimal venta = getCotizacionPesosFecha(dia, cotizaciones, TipoCotizacion.VENTA);
			// compareTo: la funcion devuelve new BigDecimal(0) cuando no hay dato (con != nunca era igual a ZERO)
			if (compra.compareTo(BigDecimal.ZERO) != 0 && venta.compareTo(BigDecimal.ZERO) != 0) {
				TipoDeCambio tipodecambio = new TipoDeCambio();
				tipodecambio.save();
				tipodecambio.setFecha(fecha);
				tipodecambio.setMoneda(TipMonType.USD);
				tipodecambio.setCompra(compra);
				tipodecambio.setVenta(venta);
				tipodecambio.setInterbancario(getCotizacionPesosFecha(dia, cotizaciones, TipoCotizacion.PROMEDIO));
				logger.info("Cotizacion " + simpleDateFormat.format(fecha) + ": compra " + compra + ", venta " + venta);
				cargados++;
			} else {
				logger.info("Valor de la cotizacion para " + simpleDateFormat.format(fecha) + " es cero, no se guarda el valor.");
				sinDato++;
			}
		}

		for (TipoDeCambio tc : sinInterbancario) {
			tc.setInterbancario(getCotizacionPesosFecha(new java.sql.Date(tc.getFecha().getTime()).toLocalDate(), cotizaciones, TipoCotizacion.PROMEDIO));
			tc.update();
		}

		resultado(cargados + " cotización(es) cargada(s)" + (sinDato > 0 ? ", " + sinDato + " sin dato en INE" : "")
				+ (sinInterbancario.isEmpty() ? "" : ", " + sinInterbancario.size() + " interbancario(s) completado(s)"), null);
	}

	private JsonNode getCotizaciones() throws IOException {
		String prefix = "ine_file_";
		String suffix = ".xlsx";

		//Creating a temp file
		File tempFile = File.createTempFile(prefix, suffix);
		logger.debug("Temp file created: "+tempFile.getAbsolutePath());

		String ine_file_url = applicationProvider.get().configuration().getString("ine.cotizaciones");

		/*
		 * ESTO ES PARA BYPASEAR EL CHECK DE LOS CERTIFICADOS SSL
		 * hay un problam con el certificado de ine donde se baja el archivo
		 */
		TrustManager trm = new X509TrustManager() {
		    public X509Certificate[] getAcceptedIssuers() {
		        return null;
		    }

		    public void checkClientTrusted(X509Certificate[] certs, String authType) {

		    }

		    public void checkServerTrusted(X509Certificate[] certs, String authType) {
		    }
		};
		
		SSLContext sc;
		try {
			sc = SSLContext.getInstance("SSL");
			sc.init(null, new TrustManager[] { trm }, null);
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
			
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeyManagementException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		/*
		 * ACA TERMINA EL BYPASSS
		 */
		
		
		//Me bajo el archivo de cotizaciones del ine
		URL website = new URL(ine_file_url);
		ReadableByteChannel rbc = Channels.newChannel(website.openStream());
		FileOutputStream fos = new FileOutputStream(tempFile);
		fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
		fos.close();

		// Creating a file object with specific file path
		ExcelToJSONConverter2 converter = new ExcelToJSONConverter2();
		JsonNode data = converter.excelToJson(tempFile, logger);
		logger.debug("Excel file contains the Data:\n" + data.toString());

		//Deleting the file
		tempFile.delete();
		logger.debug("Temp file deleted.........");
		
		return data;

	}

	private BigDecimal getCotizacionPesosFecha(LocalDate fecha, JsonNode data, TipoCotizacion tc)  {

		JsonNode cotizacion = data.get("Cotizaciones").get(fecha.toString());

		int intentos = 0;
		
		while (cotizacion == null) {
			
			if (intentos >=30)
				return new BigDecimal(0);
			
			fecha = fecha.minusDays(1);
			intentos++;
			cotizacion = data.get("Cotizaciones").get(fecha.toString());
		}

		double compra = cotizacion.get("compra").asDouble();
		double venta = cotizacion.get("venta").asDouble();
		
		BigDecimal resultado;
		
		switch (tc) {
		case COMPRA:
			resultado = new BigDecimal(compra);
			break;
		case VENTA:
			resultado = new BigDecimal(venta);
			break;
		case PROMEDIO:
			resultado = new BigDecimal((compra+venta)/2);
			break;
		default:
			resultado = new BigDecimal(0);
			break;
		}
		
		resultado = resultado.setScale(2, RoundingMode.HALF_UP);
		return resultado;
		
		
		
	}

}