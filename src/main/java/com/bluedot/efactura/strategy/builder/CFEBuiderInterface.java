package com.bluedot.efactura.strategy.builder;

import org.json.JSONArray;
import org.json.JSONObject;

import com.bluedot.efactura.EFacturaException;

public interface CFEBuiderInterface {

	public static int INDICADOR_FACTURACION_EXCENTO_IVA = 1;
	public static int INDICADOR_FACTURACION_IVA_TASA_MINIMA = 2;
	public static int INDICADOR_FACTURACION_IVA_TASA_BASICA = 3;
	public static int INDICADOR_FACTURACION_IVA_OTRA_TASA = 4;
	public static int INDICADOR_FACTURACION_ENTREGA_GRATUITA = 5;
	public static int INDICADOR_FACTURACION_NO_FACTURABLE = 6;
	public static int INDICADOR_FACTURACION_NO_FACTURABLE_NEGATIVO = 7;
	public static int INDICADOR_FACTURACION_REBAJAR_EN_REMITOS = 8;
	public static int INDICADOR_FACTURACION_ANULAR_EN_RESGUARDOS = 9;
	public static int INDICADOR_FACTURACION_EXPORTACION_Y_ASIMILADAS = 10;
	public static int INDICADOR_FACTURACION_IMPUESTO_PERCIBIDO = 11;
	public static int INDICADOR_FACTURACION_IVA_SUSPENSO = 12;

	void buildDetalle(JSONArray detalleJson) throws EFacturaException;

	void buildReceptor(JSONObject receptorJson) throws EFacturaException;

	void buildTotales(JSONObject totalesJson) throws EFacturaException;

	void buildIdDoc() throws EFacturaException;

	void buildCAEData() throws EFacturaException;

	void buildEmisor(JSONObject emisorJson) throws EFacturaException;

	void buildReferencia(JSONObject referencia) throws EFacturaException;

	void buildTimestampFirma() throws EFacturaException;

	Object getCFE();
}
