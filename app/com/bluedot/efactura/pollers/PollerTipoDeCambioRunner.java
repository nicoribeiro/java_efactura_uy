package com.bluedot.efactura.pollers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.GregorianCalendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.utils.ExcelToJSONConverter;
import com.bluedot.efactura.model.TipoDeCambio;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import dgi.classes.recepcion.TipMonType;

public class PollerTipoDeCambioRunner extends PollerRunner {

	private static final long SLEEP_TIME_IN_MILLIS = 60l * 1000l * 60l * 6l; // 6 Hours
	
	static Logger logger = LoggerFactory.getLogger(PollerTipoDeCambioRunner.class);

	private String pattern = "yyyy-MM-dd";
	private SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);

	private enum TipoCotizacion {
		COMPRA,
		VENTA,
		PROMEDIO
	}
	
	public PollerTipoDeCambioRunner() {
		super(logger, SLEEP_TIME_IN_MILLIS);
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
				tipodecambio = new TipoDeCambio();
				tipodecambio.save();
				tipodecambio.setFecha(gc_fecha.getTime());
				tipodecambio.setMoneda(TipMonType.USD);
				tipodecambio.setCompra(getCotizacionPesosFecha(gc_fecha.toZonedDateTime().toLocalDate(), cotizaciones, TipoCotizacion.COMPRA));
				tipodecambio.setVenta(getCotizacionPesosFecha(gc_fecha.toZonedDateTime().toLocalDate(), cotizaciones, TipoCotizacion.VENTA));
				logger.info("Valor de la cotizacion COMPRA para " + simpleDateFormat.format(gc_fecha.getTime()) + " es " + tipodecambio.getCompra().doubleValue());
				logger.info("Valor de la cotizacion VENTA para " + simpleDateFormat.format(gc_fecha.getTime()) + " es " + tipodecambio.getVenta().doubleValue());
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

		String ine_file_url = "https://www5.ine.gub.uy/documents/Estad%C3%ADsticasecon%C3%B3micas/SERIES%20Y%20OTROS/Cotizaci%C3%B3n%20de%20monedas/Cotizaci%C3%B3n%20monedas.xlsx";

		//Me bajo el archivo de cotizaciones del ine
		URL website = new URL(ine_file_url);
		ReadableByteChannel rbc = Channels.newChannel(website.openStream());
		FileOutputStream fos = new FileOutputStream(tempFile);
		fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
		fos.close();

		// Creating a file object with specific file path
		ExcelToJSONConverter converter = new ExcelToJSONConverter();
		JsonNode data = converter.excelToJson(tempFile, logger);
		logger.debug("Excel file contains the Data:\n" + data.toString());

		//Deleting the file
		tempFile.delete();
		logger.debug("Temp file deleted.........");
		
		return data;

	}

	private BigDecimal getCotizacionPesosFecha(LocalDate fecha, JsonNode data, TipoCotizacion tc)  {

		JsonNode cotizacion = data.get("Cotización al público").get(fecha.toString());

		int intentos = 0;
		
		while (cotizacion == null) {
			
			if (intentos >=30)
				return new BigDecimal(0);
			
			fecha = fecha.minusDays(1);
			intentos++;
			cotizacion = data.get("Cotización al público").get(fecha.toString());
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