package com.bluedot.efactura.serializers;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.strategy.builder.CFEBuilderImpl;

import dgi.classes.entreEmpresas.CFEEmpresasType;
import dgi.classes.recepcion.CFEDefType.ERemExp;
import dgi.classes.recepcion.ComplFiscalType;
import dgi.classes.recepcion.IdDocRemExp;
import dgi.classes.recepcion.ItemRemExp;
import dgi.classes.recepcion.TotalesRemExp;
import dgi.classes.recepcion.wrappers.ReceptorRemExpWrapper;

/**
 * e-Remito de exportacion recibido (124, 224) a JSON, con la forma que espera CFEBuilderRemitoExportacion (la misma del
 * JSON de emision)
 */
public class ERemExpStrategy extends CommonStrategy {

	@Override
	public JSONObject getEncabezado(CFEEmpresasType cfe, Empresa empresaReceptora) {

		JSONObject encabezado = new JSONObject();
		JSONObject idDoc = new JSONObject();

		ERemExp documento = cfe.getCFE().getERemExp();
		IdDocRemExp id = documento.getEncabezado().getIdDoc();

		idDoc.put("Nro", id.getNro());
		idDoc.put("Serie", id.getSerie());
		idDoc.put("FchEmis", CFEBuilderImpl.simpleDateFormat.format(id.getFchEmis().toGregorianCalendar().getTime()));
		idDoc.put("TipoCFE", id.getTipoCFE());
		poner(idDoc, "NroInterno", id.getNroInterno());
		poner(idDoc, "TipoTraslado", id.getTipoTraslado());
		poner(idDoc, "IndPropiedad", id.getIndPropiedad());
		poner(idDoc, "TipoDocProp", id.getTipoDocProp());
		poner(idDoc, "CodPaisProp", id.getCodPaisProp());
		poner(idDoc, "DocProp", id.getDocProp());
		poner(idDoc, "DocPropExt", id.getDocPropExt());
		poner(idDoc, "RznSocProp", id.getRznSocProp());
		poner(idDoc, "ClauVenta", id.getClauVenta());
		idDoc.put("ModVenta", id.getModVenta());
		idDoc.put("ViaTransp", id.getViaTransp());
		// SecProf: con el, el receptor puede no venir (CFEBuilderRemitoExportacion no exige sus datos)
		poner(idDoc, "SecProf", id.getSecProf());
		poner(idDoc, "InfoAdicionalDoc", id.getInfoAdicionalDoc());

		JSONObject receptor;
		if (documento.getEncabezado().getReceptor() != null)
			receptor = receptorExportacion(new ReceptorRemExpWrapper(documento.getEncabezado().getReceptor()));
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
		return referencias(cfe.getCFE().getERemExp().getReferencia());
	}

	@Override
	public JSONArray getDetalle(CFEEmpresasType cfe) {
		JSONArray detalle = new JSONArray();

		List<ItemRemExp> items = cfe.getCFE().getERemExp().getDetalle().getItems();

		for (ItemRemExp itemXml : items) {
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
		return cfe.getCFE().getERemExp().getTmstFirma().toGregorianCalendar().getTimeInMillis();
	}

	@Override
	public ComplFiscalType getComplementoFiscalType(CFEEmpresasType cfe) {
		// El e-Remito de exportacion no tiene complemento fiscal
		return null;
	}

	@Override
	public boolean hayCompFiscal(CFEEmpresasType cfe) {
		return false;
	}

	@Override
	public JSONObject getTotales(CFEEmpresasType cfe) {
		JSONObject totales = new JSONObject();
		TotalesRemExp totalesXml = cfe.getCFE().getERemExp().getEncabezado().getTotales();

		totales.put("TpoMoneda", String.valueOf(totalesXml.getTpoMoneda()));
		if (!String.valueOf(totalesXml.getTpoMoneda()).equals("UYU"))
			totales.put("TpoCambio", String.valueOf(totalesXml.getTpoCambio()));

		return totales;
	}

}
