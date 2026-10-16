package com.bluedot.efactura.serializers;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.strategy.builder.CFEBuilderImpl;

import dgi.classes.entreEmpresas.CFEEmpresasType;
import dgi.classes.recepcion.CFEDefType.EFactExp;
import dgi.classes.recepcion.ComplFiscalType;
import dgi.classes.recepcion.IdDocFactExp;
import dgi.classes.recepcion.ItemDetFactExp;
import dgi.classes.recepcion.TotalesFactExp;
import dgi.classes.recepcion.wrappers.ReceptorFactExpWrapper;

/**
 * e-Factura de exportacion y sus notas recibidas (121-123, 221-223) a JSON, con la forma que espera
 * CFEBuilderExportacion (la misma del JSON de emision)
 */
public class EFactExpStrategy extends CommonStrategy {

	@Override
	public JSONObject getEncabezado(CFEEmpresasType cfe, Empresa empresaReceptora) {

		JSONObject encabezado = new JSONObject();
		JSONObject idDoc = new JSONObject();

		EFactExp documento = cfe.getCFE().getEFactExp();
		IdDocFactExp id = documento.getEncabezado().getIdDoc();

		idDoc.put("Nro", id.getNro());
		idDoc.put("Serie", id.getSerie());
		idDoc.put("FchEmis", CFEBuilderImpl.simpleDateFormat.format(id.getFchEmis().toGregorianCalendar().getTime()));
		idDoc.put("TipoCFE", id.getTipoCFE());
		poner(idDoc, "NroInterno", id.getNroInterno());
		poner(idDoc, "FmaPago", id.getFmaPago());
		if (id.getFchVenc() != null)
			idDoc.put("FchVenc", CFEBuilderImpl.simpleDateFormat.format(id.getFchVenc().toGregorianCalendar().getTime()));
		poner(idDoc, "ClauVenta", id.getClauVenta());
		poner(idDoc, "ModVenta", id.getModVenta());
		idDoc.put("ViaTransp", id.getViaTransp());
		// SecProf: con el, el receptor puede no venir (CFEBuilderExportacion no exige sus datos)
		poner(idDoc, "SecProf", id.getSecProf());
		poner(idDoc, "InfoAdicionalDoc", id.getInfoAdicionalDoc());

		JSONObject receptor;
		if (documento.getEncabezado().getReceptor() != null)
			receptor = receptorExportacion(new ReceptorFactExpWrapper(documento.getEncabezado().getReceptor()));
		else if (id.getSecProf() != null)
			receptor = new JSONObject();
		else
			receptor = receptorEmpresa(empresaReceptora);

		encabezado.put("IdDoc", idDoc);
		encabezado.put("Receptor", receptor);
		encabezado.put("Emisor", emisor(documento.getEncabezado().getEmisor()));
		encabezado.put("Totales", getTotales(cfe));

		return encabezado;
	}

	@Override
	public JSONArray getReferencia(CFEEmpresasType cfe) {
		return referencias(cfe.getCFE().getEFactExp().getReferencia());
	}

	@Override
	public JSONArray getDetalle(CFEEmpresasType cfe) {
		JSONArray detalle = new JSONArray();

		List<ItemDetFactExp> items = cfe.getCFE().getEFactExp().getDetalle().getItems();

		for (ItemDetFactExp itemXml : items) {
			JSONObject item = new JSONObject();

			item.put("NroLinDet", String.valueOf(itemXml.getNroLinDet()));
			poner(item, "IndFact", itemXml.getIndFact() != null ? String.valueOf(itemXml.getIndFact()) : null);
			item.put("NomItem", itemXml.getNomItem());
			poner(item, "NCM", itemXml.getNCM());
			item.put("Cantidad", String.valueOf(itemXml.getCantidad()));
			item.put("UniMed", itemXml.getUniMed() == null || itemXml.getUniMed().equals("") ? "N/A" : itemXml.getUniMed());
			item.put("PrecioUnitario", String.valueOf(itemXml.getPrecioUnitario()));
			item.put("MontoItem", String.valueOf(itemXml.getMontoItem()));
			poner(item, "DscItem", itemXml.getDscItem());

			if (itemXml.getDescuentoPct() != null)
				item.put("DescuentoPct", String.valueOf(itemXml.getDescuentoPct()));
			if (itemXml.getDescuentoMonto() != null)
				item.put("DescuentoMonto", String.valueOf(itemXml.getDescuentoMonto()));
			if (itemXml.getRecargoPct() != null)
				item.put("RecargoPct", String.valueOf(itemXml.getRecargoPct()));
			if (itemXml.getRecargoMnt() != null)
				item.put("RecargoMnt", String.valueOf(itemXml.getRecargoMnt()));

			//TODO soportar mas de un CodItem
			if (itemXml.getCodItems() != null && itemXml.getCodItems().size() > 0) {
				item.put("CodItem", String.valueOf(itemXml.getCodItems().get(0).getCod()));
				item.put("TpoCod", String.valueOf(itemXml.getCodItems().get(0).getTpoCod()));
			}

			detalle.put(item);
		}

		return detalle;
	}

	@Override
	public long getTimestampFirma(CFEEmpresasType cfe) {
		return cfe.getCFE().getEFactExp().getTmstFirma().toGregorianCalendar().getTimeInMillis();
	}

	@Override
	public ComplFiscalType getComplementoFiscalType(CFEEmpresasType cfe) {
		// La e-Factura de exportacion no tiene complemento fiscal
		return null;
	}

	@Override
	public boolean hayCompFiscal(CFEEmpresasType cfe) {
		return false;
	}

	@Override
	public JSONObject getTotales(CFEEmpresasType cfe) {
		JSONObject totales = new JSONObject();
		TotalesFactExp totalesXml = cfe.getCFE().getEFactExp().getEncabezado().getTotales();

		totales.put("TpoMoneda", String.valueOf(totalesXml.getTpoMoneda()));
		if (!String.valueOf(totalesXml.getTpoMoneda()).equals("UYU"))
			totales.put("TpoCambio", String.valueOf(totalesXml.getTpoCambio()));

		return totales;
	}

}
