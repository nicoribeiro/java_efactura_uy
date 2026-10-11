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
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.google.inject.Provider;

import dgi.classes.recepcion.TipMonType;
import play.Application;

public class PollerTipoDeCambioRunner extends PollerRunner {

	//private static final long SLEEP_TIME_IN_MILLIS = 60l * 1000l * 60l * 1l; // 1 Hour
	private static final long SLEEP_TIME_IN_MILLIS = 10l * 1000l; // 10 sec
	
	static Logger logger = LoggerFactory.getLogger(PollerTipoDeCambioRunner.class);

	private String pattern = "yyyy-MM-dd";
	private SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
	
	private Provider<Application> applicationProvider;

	public PollerTipoDeCambioRunner(Provider<Application> applicationProvider) {
		super(logger, SLEEP_TIME_IN_MILLIS);
		this.applicationProvider = applicationProvider;
	}
	
	@Override
	protected void executeConcreteAction() throws APIException {
		
		
		GregorianCalendar gc_fecha =  new GregorianCalendar();
		gc_fecha.add((GregorianCalendar.DAY_OF_MONTH), -3650); //10 año para atras

		GregorianCalendar gc_hoy =  new GregorianCalendar();// hoy
		gc_hoy.add((GregorianCalendar.MINUTE), -5); //le resto 5 min para que funcione bien la condicion del while

		JsonNode cotizaciones = JsonNodeFactory.instance.objectNode();
		try {
			cotizaciones = getCotizaciones();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		while (gc_hoy.getTime().after(gc_fecha.getTime())) { // No ejecuta para el dia actual 

			TipoDeCambio tipodecambio = TipoDeCambio.findByFechaYMoneda(gc_fecha.getTime(), TipMonType.USD);

			if (tipodecambio==null) {
				logger.info("Falta cotizacion para: " + simpleDateFormat.format(gc_fecha.getTime()) + ".");
				if (getCotizacionPesosFecha(gc_fecha.toZonedDateTime().toLocalDate(), cotizaciones, TipoCotizacion.COMPRA) != BigDecimal.ZERO && getCotizacionPesosFecha(gc_fecha.toZonedDateTime().toLocalDate(), cotizaciones, TipoCotizacion.VENTA) != BigDecimal.ZERO) {
					tipodecambio = new TipoDeCambio();
					tipodecambio.save();
					tipodecambio.setFecha(gc_fecha.getTime());
					tipodecambio.setMoneda(TipMonType.USD);
					tipodecambio.setCompra(getCotizacionPesosFecha(gc_fecha.toZonedDateTime().toLocalDate(), cotizaciones, TipoCotizacion.COMPRA));
					tipodecambio.setVenta(getCotizacionPesosFecha(gc_fecha.toZonedDateTime().toLocalDate(), cotizaciones, TipoCotizacion.VENTA));
					tipodecambio.setInterbancario(getCotizacionPesosFecha(gc_fecha.toZonedDateTime().toLocalDate(), cotizaciones, TipoCotizacion.PROMEDIO));
					logger.info("Valor de la cotizacion COMPRA para " + simpleDateFormat.format(gc_fecha.getTime()) + " es " + tipodecambio.getCompra().doubleValue());
					logger.info("Valor de la cotizacion VENTA para " + simpleDateFormat.format(gc_fecha.getTime()) + " es " + tipodecambio.getVenta().doubleValue());
				}else {
					logger.info("Valor de la cotizacion para " + simpleDateFormat.format(gc_fecha.getTime()) + " es cero, no se guarda el valor.");
				}
			} else { 
				if (tipodecambio.getInterbancario() == null) {
					tipodecambio.setInterbancario(getCotizacionPesosFecha(gc_fecha.toZonedDateTime().toLocalDate(), cotizaciones, TipoCotizacion.PROMEDIO));
					tipodecambio.update();
				}
			}
			gc_fecha.add((GregorianCalendar.DAY_OF_MONTH), 1);
		}

		
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