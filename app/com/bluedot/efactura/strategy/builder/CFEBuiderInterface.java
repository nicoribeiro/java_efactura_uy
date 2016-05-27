package com.bluedot.efactura.strategy.builder;

import org.json.JSONArray;
import org.json.JSONObject;

import com.bluedot.efactura.global.EFacturaException;

public interface CFEBuiderInterface {

	public static int INDICADOR_FACTURACION_EXCENTO_IVA = 1;
	public static int INDICADOR_FACTURACION_IVA_TASA_MINIMA = 2;
	public static int INDICADOR_FACTURACION_IVA_TASA_BASICA = 3;
	public static int INDICADOR_FACTURACION_IVA_OTRA_TASA = 4;
	public static int INDICADOR_FACTURACION_ENTREGA_GRATUITA = 5;
	/*
	 * 4.10. ¿Cómo se documentan las cobranzas de terceros? (01/12/2011) Las
	 * cobranzas de terceros como pueden ser las de los entes públicos, están
	 * contempladas en el campo de los CFE “montos no facturables”, debiendo
	 * utilizar en la línea de detalle el indicador de facturación 6 “Producto o
	 * servicio no facturable”. Si fuere necesario anular uno de estos conceptos
	 * en el mismo comprobante se debe utilizar el indicador de facturación 7
	 * “Producto o servicio no facturable negativo”.
	 */
	public static int INDICADOR_FACTURACION_NO_FACTURABLE = 6;
	public static int INDICADOR_FACTURACION_NO_FACTURABLE_NEGATIVO = 7;
	/*
	 * 4.20. ¿Es posible corregir un e-Remito o un e-Remito de exportación? Sí.
	 * En caso que corresponda corregir un e-Remito o un e-Remito de exportación
	 * recibido por DGI, el mismo se ajustará con otro CFE del mismo tipo. En
	 * caso que el ajuste sea negativo se debe utilizar el indicador de
	 * facturación 8 (código “B-C4” de Formato de CFE)
	 */
	public static int INDICADOR_FACTURACION_REBAJAR_EN_REMITOS = 8;

	/*
	 * 4.25. ¿Qué indicador de facturación se utiliza en los e-resguardos? En la
	 * confección de los e-resguardos no corresponde utilizar indicador de
	 * facturación. Únicamente en el caso que deba anular, total o parcialmente
	 * otro e-resguardo ya emitido, deberá utilizar el indicador de facturación
	 * 9, debiendo indicar en el área de referencia el Nº de resguardo que
	 * ajusta.y la expresión “Corrección de e-Resguardo” en la Adenda. (Creada
	 * 10/02/2015)
	 */
	public static int INDICADOR_FACTURACION_ANULAR_EN_RESGUARDOS = 9;
	public static int INDICADOR_FACTURACION_EXPORTACION_Y_ASIMILADAS = 10;
	public static int INDICADOR_FACTURACION_IMPUESTO_PERCIBIDO = 11;
	public static int INDICADOR_FACTURACION_IVA_SUSPENSO = 12;

	void buildDetalle(JSONArray detalleJson, boolean montosIncluyenIva) throws EFacturaException;

	void buildReceptor(JSONObject receptorJson) throws EFacturaException;

	void buildTotales(JSONObject totalesJson, boolean montosIncluyenIva) throws EFacturaException;

	void buildIdDoc(boolean montosIncluyenIva, int formaPago) throws EFacturaException;

	void buildCAEData() throws EFacturaException;

	void buildEmisor(JSONObject emisorJson) throws EFacturaException;

	void buildReferencia(JSONObject referencia) throws EFacturaException;

	void buildTimestampFirma() throws EFacturaException;

	Object getCFE();

}
