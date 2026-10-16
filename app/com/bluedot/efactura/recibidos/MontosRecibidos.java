package com.bluedot.efactura.recibidos;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluedot.efactura.model.CFE;
import com.bluedot.efactura.model.Detalle;
import com.bluedot.efactura.model.Sobre;
import com.bluedot.efactura.model.TipoDoc;

import dgi.classes.entreEmpresas.CFEEmpresasType;
import dgi.classes.entreEmpresas.EnvioCFEEntreEmpresas;
import dgi.classes.recepcion.CFEDefType;
import dgi.classes.recepcion.ItemDetFact;
import dgi.classes.recepcion.wrappers.TotalesFactExpWrapper;
import dgi.classes.recepcion.wrappers.TotalesFactTickWrapper;
import dgi.classes.recepcion.wrappers.TotalesRemExpWrapper;
import dgi.classes.recepcion.wrappers.TotalesInterface;

/**
 * Montos de los CFE recibidos. El builder calcula los totales de un CFE a partir de sus lineas (precio x cantidad), lo que
 * sirve para los emitidos pero no para los recibidos: no toma en cuenta descuentos, recargos, montos con IVA incluido ni
 * el redondeo del emisor. En los recibidos los montos se toman de los Totales del XML firmado por el emisor.
 *
 * Aplica a eFactura y eTicket y sus notas de credito y debito, a la e-Factura de exportacion y sus notas y al e-Remito de
 * exportacion (en estos dos los montos de IVA quedan en cero); los demas tipos quedan como los calcula el builder.
 *
 * Tambien corrige los descuentos y recargos de las lineas de los CFE ya guardados (antes no se guardaban).
 */
public class MontosRecibidos {

	final static Logger logger = LoggerFactory.getLogger(MontosRecibidos.class);

	/** Totales del XML, o null si el tipo de CFE no es eFactura, eTicket (o sus notas) ni de exportacion */
	private static TotalesInterface totales(CFEDefType xml) {
		if (xml == null)
			return null;
		if (xml.getEFact() != null)
			return new TotalesFactTickWrapper(xml.getEFact().getEncabezado().getTotales());
		if (xml.getETck() != null)
			return new TotalesFactTickWrapper(xml.getETck().getEncabezado().getTotales());
		if (xml.getEFactExp() != null)
			return new TotalesFactExpWrapper(xml.getEFactExp().getEncabezado().getTotales());
		if (xml.getERemExp() != null)
			return new TotalesRemExpWrapper(xml.getERemExp().getEncabezado().getTotales());
		return null;
	}

	/**
	 * Compara los montos del CFE con los Totales del XML y, con escribir, los copia al CFE (no lo guarda).
	 *
	 * @return los campos distintos (vacio si son iguales o el tipo no aplica)
	 */
	public static List<String> aplicar(CFE cfe, CFEDefType xml, boolean escribir) {
		List<String> cambios = new ArrayList<String>();
		TotalesInterface t = totales(xml);
		if (t == null)
			return cambios;

		campo(cambios, escribir, "totMntNoGrv", cfe.getTotMntNoGrv(), t.getMntNoGrv(), cfe::setTotMntNoGrv);
		campo(cambios, escribir, "totMntExpyAsim", cfe.getTotMntExpyAsim(), t.getMntExpoyAsim(), cfe::setTotMntExpyAsim);
		campo(cambios, escribir, "totMntImpPerc", cfe.getTotMntImpPerc(), t.getMntImpuestoPerc(), cfe::setTotMntImpPerc);
		campo(cambios, escribir, "totMntIVAenSusp", cfe.getTotMntIVAenSusp(), t.getMntIVaenSusp(), cfe::setTotMntIVAenSusp);
		campo(cambios, escribir, "totMntIVATasaMin", cfe.getTotMntIVATasaMin(), t.getMntNetoIVATasaMin(), cfe::setTotMntIVATasaMin);
		campo(cambios, escribir, "totMntIVATasaBas", cfe.getTotMntIVATasaBas(), t.getMntNetoIVATasaBasica(), cfe::setTotMntIVATasaBas);
		campo(cambios, escribir, "totMntIVAOtra", cfe.getTotMntIVAOtra(), t.getMntNetoIVAOtra(), cfe::setTotMntIVAOtra);
		campo(cambios, escribir, "mntIVATasaMin", cfe.getMntIVATasaMin(), t.getMntIVATasaMin(), cfe::setMntIVATasaMin);
		campo(cambios, escribir, "mntIVATasaBas", cfe.getMntIVATasaBas(), t.getMntIVATasaBasica(), cfe::setMntIVATasaBas);
		campo(cambios, escribir, "mntIVAOtra", cfe.getMntIVAOtra(), t.getMntIVAOtra(), cfe::setMntIVAOtra);
		// Las tasas son opcionales en el XML: si no vienen queda la de la tabla de IVA
		if (t.getIVATasaMin() != null)
			campo(cambios, escribir, "ivaTasaMin", cfe.getIvaTasaMin(), t.getIVATasaMin(), cfe::setIvaTasaMin);
		if (t.getIVATasaBasica() != null)
			campo(cambios, escribir, "ivaTasaBas", cfe.getIvaTasaBas(), t.getIVATasaBasica(), cfe::setIvaTasaBas);
		campo(cambios, escribir, "totMntTotal", cfe.getTotMntTotal(), t.getMntTotal(), cfe::setTotMntTotal);
		campo(cambios, escribir, "totMntRetenido", cfe.getTotMntRetenido(), t.getMntTotRetenido(), cfe::setTotMntRetenido);

		return cambios;
	}

	/** Un monto ausente en el XML es cero */
	private static void campo(List<String> cambios, boolean escribir, String nombre, Double actual, BigDecimal xml,
			Consumer<Double> setter) {
		double nuevo = xml != null ? xml.doubleValue() : 0d;
		if (actual != null && Math.abs(actual - nuevo) < 0.005)
			return;
		cambios.add(nombre);
		if (escribir)
			setter.accept(nuevo);
	}

	/** Lineas del XML, o null si el tipo de CFE no es eFactura ni eTicket (o sus notas) */
	private static List<ItemDetFact> items(CFEDefType xml) {
		if (xml.getEFact() != null && xml.getEFact().getDetalle() != null)
			return xml.getEFact().getDetalle().getItems();
		if (xml.getETck() != null && xml.getETck().getDetalle() != null)
			return xml.getETck().getDetalle().getItems();
		return null;
	}

	/**
	 * Compara los descuentos y recargos de las lineas del CFE con los del XML y, con escribir, los copia (y guarda cada
	 * linea que cambia).
	 *
	 * @return cantidad de lineas distintas
	 */
	public static int lineas(CFE cfe, CFEDefType xml, boolean escribir) {
		List<ItemDetFact> items = items(xml);
		if (items == null)
			return 0;
		int distintas = 0;
		// Copia: guardar una linea modifica la coleccion del CFE mientras se recorre
		List<Detalle> detalles = new ArrayList<Detalle>(cfe.getDetalle());
		for (ItemDetFact item : items)
			for (Detalle detalle : detalles) {
				if (detalle.getNroLinea() != item.getNroLinDet())
					continue;
				List<String> cambios = new ArrayList<String>();
				campo(cambios, escribir, "descuentoPorcentaje", detalle.getDescuentoPorcentaje(), item.getDescuentoPct(), detalle::setDescuentoPorcentaje);
				campo(cambios, escribir, "descuentoMonto", detalle.getDescuentoMonto(), item.getDescuentoMonto(), detalle::setDescuentoMonto);
				campo(cambios, escribir, "recargoPorcentaje", detalle.getRecargoPorcentaje(), item.getRecargoPct(), detalle::setRecargoPorcentaje);
				campo(cambios, escribir, "recargoMonto", detalle.getRecargoMonto(), item.getRecargoMnt(), detalle::setRecargoMonto);
				if (!cambios.isEmpty()) {
					distintas++;
					if (escribir)
						detalle.update();
				}
			}
		return distintas;
	}

	/**
	 * Corrige los montos de los CFE de un sobre recibido con los Totales y las lineas de su XML.
	 *
	 * @param guardar
	 *            false = solo informa lo que cambiaria
	 * @return un elemento por CFE corregido (tipo, serie, nro, total anterior y nuevo, campos cambiados)
	 */
	public static List<JSONObject> corregirSobre(Sobre sobre, boolean guardar) {
		List<JSONObject> corregidos = new ArrayList<JSONObject>();
		EnvioCFEEntreEmpresas envio = sobre.getEnvioCFEEntreEmpresas();
		if (envio == null)
			return corregidos;

		for (CFEEmpresasType cfeEmpresas : envio.getCFEAdendas()) {
			CFEDefType xml = cfeEmpresas.getCFE();
			if (xml == null)
				continue;

			CFE cfe = null;
			if (xml.getEFact() != null) {
				dgi.classes.recepcion.IdDocFact id = xml.getEFact().getEncabezado().getIdDoc();
				cfe = sobre.getCFE(id.getNro().longValue(), id.getSerie(), TipoDoc.fromInt(id.getTipoCFE().intValue()));
			} else if (xml.getETck() != null) {
				dgi.classes.recepcion.IdDocTck id = xml.getETck().getEncabezado().getIdDoc();
				cfe = sobre.getCFE(id.getNro().longValue(), id.getSerie(), TipoDoc.fromInt(id.getTipoCFE().intValue()));
			} else if (xml.getEFactExp() != null) {
				dgi.classes.recepcion.IdDocFactExp id = xml.getEFactExp().getEncabezado().getIdDoc();
				cfe = sobre.getCFE(id.getNro().longValue(), id.getSerie(), TipoDoc.fromInt(id.getTipoCFE().intValue()));
			} else if (xml.getERemExp() != null) {
				dgi.classes.recepcion.IdDocRemExp id = xml.getERemExp().getEncabezado().getIdDoc();
				cfe = sobre.getCFE(id.getNro().longValue(), id.getSerie(), TipoDoc.fromInt(id.getTipoCFE().intValue()));
			}
			if (cfe == null)
				continue;

			Double totalAnterior = cfe.getTotMntTotal();
			TotalesInterface t = totales(xml);
			List<String> cambios = aplicar(cfe, xml, guardar);
			int lineas = lineas(cfe, xml, guardar);
			if (cambios.isEmpty() && lineas == 0)
				continue;

			if (guardar)
				cfe.update();

			JSONObject corregido = new JSONObject();
			corregido.put("tipo", cfe.getTipo().name());
			corregido.put("serie", cfe.getSerie());
			corregido.put("nro", cfe.getNro());
			corregido.put("totalAnterior", totalAnterior);
			corregido.put("total", t.getMntTotal() != null ? t.getMntTotal().doubleValue() : 0d);
			corregido.put("campos", cambios);
			corregido.put("lineas", lineas);
			corregidos.add(corregido);
		}
		return corregidos;
	}
}
