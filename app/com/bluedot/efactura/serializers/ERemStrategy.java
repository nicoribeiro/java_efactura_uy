package com.bluedot.efactura.serializers;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.strategy.builder.CFEBuilderImpl;

import dgi.classes.entreEmpresas.CFEEmpresasType;
import dgi.classes.recepcion.ItemRem;
import dgi.classes.recepcion.CFEDefType.ERem;
import dgi.classes.recepcion.ReferenciaTipo.Referencia;

public class ERemStrategy implements CFEEmpresasStrategy {

	@Override
	public JSONObject getEncabezado(CFEEmpresasType cfe, Empresa empresaReceptora) {
		
		JSONObject encabezado = new JSONObject();
		JSONObject idDoc = new JSONObject();
		JSONObject receptor = new JSONObject();
		JSONObject emisor = new JSONObject();
		
		ERem documento = cfe.getCFE().getERem();
		
		idDoc.put("Nro", documento.getEncabezado().getIdDoc().getNro());
		idDoc.put("Serie", documento.getEncabezado().getIdDoc().getSerie());
		idDoc.put("FchEmis", CFEBuilderImpl.simpleDateFormat.format(documento.getEncabezado().getIdDoc().getFchEmis().toGregorianCalendar().getTime()));
		idDoc.put("TipoCFE", documento.getEncabezado().getIdDoc().getTipoCFE());
		
		receptor.put("TipoDocRecep", documento.getEncabezado().getReceptor().getTipoDocRecep());
		receptor.put("CiudadRecep", documento.getEncabezado().getReceptor().getCiudadRecep());
		receptor.put("DeptoRecep", documento.getEncabezado().getReceptor().getDeptoRecep());
		receptor.put("CodPaisRecep", documento.getEncabezado().getReceptor().getCodPaisRecep());
		receptor.put("DocRecep", documento.getEncabezado().getReceptor().getDocRecep());
		receptor.put("RznSocRecep", documento.getEncabezado().getReceptor().getRznSocRecep());
		receptor.put("DirRecep", documento.getEncabezado().getReceptor().getDirRecep());
		
		emisor.put("CdgDGISucur", documento.getEncabezado().getEmisor().getCdgDGISucur());
		emisor.put("Ciudad", documento.getEncabezado().getEmisor().getCiudad());
		emisor.put("Departamento", documento.getEncabezado().getEmisor().getDepartamento());
		emisor.put("DomFiscal", documento.getEncabezado().getEmisor().getDomFiscal());
		emisor.put("RUCEmisor", documento.getEncabezado().getEmisor().getRUCEmisor());
		emisor.put("RznSoc", documento.getEncabezado().getEmisor().getRznSoc());
		emisor.put("NomComercial", documento.getEncabezado().getEmisor().getNomComercial());
		
		encabezado.put("IdDoc", idDoc);
		encabezado.put("Receptor", receptor);
		encabezado.put("Emisor", emisor);
		encabezado.put("Totales", getTotales(cfe));
		
		return encabezado;
	}
	
	@Override
	public JSONArray getReferencia(CFEEmpresasType cfe) {
		JSONArray referencias = new JSONArray();
		ERem documento = cfe.getCFE().getERem();
		
		if (documento.getReferencia()==null)
			return referencias;
		
		for (Referencia referencia : documento.getReferencia().getReferencias()) {
			JSONObject referenciaJson = new JSONObject();
			referenciaJson.put("NroCFERef", referencia.getNroCFERef());
			referenciaJson.put("NroLinRef", referencia.getNroLinRef());
			if (referencia.getFechaCFEref()!=null)
				referenciaJson.put("FechaCFEref", CFEBuilderImpl.simpleDateFormat.format(referencia.getFechaCFEref().toGregorianCalendar().getTime()));
			referenciaJson.put("IndGlobal", referencia.getIndGlobal());
			referenciaJson.put("RazonRef", referencia.getRazonRef());
			referenciaJson.put("Serie", referencia.getSerie());
			referenciaJson.put("TpoDocRef", referencia.getTpoDocRef());
			referencias.put(referenciaJson);
		}
		
		return referencias;
	}

	@Override
	public JSONArray getDetalle(CFEEmpresasType cfe) {
		JSONArray detalle = new JSONArray();
		
		
		List<ItemRem> items = cfe.getCFE().getERem().getDetalle().getItems();
		
		for (ItemRem itemRem : items) {
			JSONObject item = new JSONObject();
			
			item.put("NroLinDet", String.valueOf(itemRem.getNroLinDet()));
			if (itemRem.getIndFact()!=null)
				item.put("IndFact", String.valueOf(itemRem.getIndFact()));
			item.put("NomItem", itemRem.getNomItem());
			item.put("Cantidad", String.valueOf(itemRem.getCantidad()));
			item.put("UniMed", itemRem.getUniMed()==null || itemRem.getUniMed().equals("") ? "N/A" : itemRem.getUniMed());
			item.put("DscItem", String.valueOf(itemRem.getDscItem()));
			
			//TODO soportar mas de un CodItem
			if (itemRem.getCodItems()!=null &&  itemRem.getCodItems().size()>0){ 
				item.put("CodItem", String.valueOf(itemRem.getCodItems().get(0).getCod()));
				item.put("TpoCod", String.valueOf(itemRem.getCodItems().get(0).getTpoCod()));
			}
			
			detalle.put(item);
		}
		
		return detalle;
	}

	@Override
	public long getTimestampFirma(CFEEmpresasType cfe) {
		ERem documento = cfe.getCFE().getERem();
		return documento.getTmstFirma().toGregorianCalendar().getTimeInMillis();
	}

	@Override
	public JSONObject getCompFiscal(CFEEmpresasType cfe) {
		return null;
	}
	
	@Override
	public boolean hayCompFiscal(CFEEmpresasType cfe) {
		return false;
	}

	@Override
	public JSONObject getTotales(CFEEmpresasType cfe) {
		JSONObject totales = new JSONObject();
		
		totales.put("CantLinDet", String.valueOf(cfe.getCFE().getERem().getDetalle().getItems().size()));
		
		return totales;
	}

}
