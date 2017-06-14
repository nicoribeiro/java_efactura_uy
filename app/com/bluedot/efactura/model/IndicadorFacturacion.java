package com.bluedot.efactura.model;

import java.util.Map;
import java.util.TreeMap;

public enum IndicadorFacturacion {

	INDICADOR_FACTURACION_EXCENTO_IVA(1, true),

	INDICADOR_FACTURACION_IVA_TASA_MINIMA(2, true),

	INDICADOR_FACTURACION_IVA_TASA_BASICA(3, true),

	INDICADOR_FACTURACION_IVA_OTRA_TASA(4, true),

	INDICADOR_FACTURACION_ENTREGA_GRATUITA(5, true),

	/*
	 * 4.10. ¿Cómo se documentan las cobranzas de terceros? (01/12/2011) Las
	 * cobranzas de terceros como pueden ser las de los entes públicos, están
	 * contempladas en el campo de los CFE “montos no facturables”, debiendo
	 * utilizar en la línea de detalle el indicador de facturación 6 “Producto o
	 * servicio no facturable”. Si fuere necesario anular uno de estos conceptos
	 * en el mismo comprobante se debe utilizar el indicador de facturación 7
	 * “Producto o servicio no facturable negativo”.
	 */
	INDICADOR_FACTURACION_NO_FACTURABLE(6, true),

	INDICADOR_FACTURACION_NO_FACTURABLE_NEGATIVO(7, true),

	/*
	 * 4.20. ¿Es posible corregir un e-Remito o un e-Remito de exportación? Sí.
	 * En caso que corresponda corregir un e-Remito o un e-Remito de exportación
	 * recibido por DGI, el mismo se ajustará con otro CFE del mismo tipo. En
	 * caso que el ajuste sea negativo se debe utilizar el indicador de
	 * facturación 8 (código “B-C4” de Formato de CFE)
	 */
	INDICADOR_FACTURACION_REBAJAR_EN_REMITOS(8),

	/*
	 * 4.25. ¿Qué indicador de facturación se utiliza en los e-resguardos? En la
	 * confección de los e-resguardos no corresponde utilizar indicador de
	 * facturación. Únicamente en el caso que deba anular, total o parcialmente
	 * otro e-resguardo ya emitido, deberá utilizar el indicador de facturación
	 * 9, debiendo indicar en el área de referencia el Nº de resguardo que
	 * ajusta.y la expresión “Corrección de e-Resguardo” en la Adenda. (Creada
	 * 10/02/2015)
	 */
	INDICADOR_FACTURACION_ANULAR_EN_RESGUARDOS(9),

	INDICADOR_FACTURACION_EXPORTACION_Y_ASIMILADAS(10),

	INDICADOR_FACTURACION_IMPUESTO_PERCIBIDO(11),

	INDICADOR_FACTURACION_IVA_SUSPENSO(12);

	private int indice;
	private boolean soportado;

	public static int maxIndice;

	private static Map<Integer, IndicadorFacturacion> idMap = new TreeMap<Integer, IndicadorFacturacion>();

	IndicadorFacturacion(int indice) {
		this.indice = indice;
		this.soportado = false;
	}

	IndicadorFacturacion(int indice, boolean soportado) {
		this.indice = indice;
		this.soportado = soportado;
	}

	static {
		maxIndice = 0;
		for (IndicadorFacturacion iva : values()) {
			idMap.put(iva.indice, iva);
			if (iva.indice > maxIndice)
				maxIndice = iva.indice;
		}
	}

	public static IndicadorFacturacion fromInt(int i) {
		return idMap.get(i);
	}

	public int getIndice() {
		return indice;
	}

	public boolean isSoportado() {
		return soportado;
	}

}
