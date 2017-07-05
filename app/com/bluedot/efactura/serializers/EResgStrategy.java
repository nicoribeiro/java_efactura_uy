package com.bluedot.efactura.serializers;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.bluedot.efactura.strategy.builder.CFEBuilderImpl;

import dgi.classes.entreEmpresas.CFEEmpresasType;
import dgi.classes.recepcion.CFEDefType.EResg;
import dgi.classes.recepcion.ItemResg;
import dgi.classes.recepcion.ReferenciaTipo.Referencia;
import dgi.classes.recepcion.RetPercResg;

public class EResgStrategy implements CFEEmpresasStrategy {

	@Override
	public JSONObject getEncabezado(CFEEmpresasType cfe) {
		
		JSONObject encabezado = new JSONObject();
		JSONObject idDoc = new JSONObject();
		JSONObject receptor = new JSONObject();
		JSONObject emisor = new JSONObject();
		
		JSONObject totales = new JSONObject();
		
		EResg documento = cfe.getCFE().getEResg();
		
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
		
		totales.put("TpoMoneda", String.valueOf(documento.getEncabezado().getTotales().getTpoMoneda()));
		
		if (!String.valueOf(documento.getEncabezado().getTotales().getTpoMoneda()).equals("UYU"))	
			totales.put("TpoCambio", String.valueOf(documento.getEncabezado().getTotales().getTpoCambio()));
		
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
		encabezado.put("Totales", totales);
		
		return encabezado;
	}
	
	@Override
	public JSONArray getReferencia(CFEEmpresasType cfe) {
		JSONArray referencias = new JSONArray();
		EResg documento = cfe.getCFE().getEResg();
		
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
		
		
		List<ItemResg> items = cfe.getCFE().getEResg().getDetalle().getItems();
		
		for (ItemResg itemResg : items) {
			JSONObject item = new JSONObject();
			
			item.put("NroLinDet", String.valueOf(itemResg.getNroLinDet()));
			if (itemResg.getIndFact()!=null)
				item.put("IndFact", String.valueOf(itemResg.getIndFact()));
			
			JSONArray retPercResgArray = new JSONArray();
			for (RetPercResg retPercResg : itemResg.getRetencPerceps()) {
				JSONObject retPercResgObject = new JSONObject();
				retPercResgObject.put("CodRet", String.valueOf(retPercResg.getCodRet()));
				retPercResgObject.put("MntSujetoaRet", String.valueOf(retPercResg.getMntSujetoaRet()));
				retPercResgObject.put("ValRetPerc", String.valueOf(retPercResg.getValRetPerc()));
				if (retPercResg.getTasa()!=null)
					retPercResgObject.put("Tasa", String.valueOf(retPercResg.getTasa()));
				retPercResgArray.put(retPercResgObject);
			}
			
			item.put("RetencPercep", retPercResgArray);
			
			detalle.put(item);
		}
		
		return detalle;
	}

	@Override
	public long getTimestampFirma(CFEEmpresasType cfe) {
		EResg documento = cfe.getCFE().getEResg();
		return documento.getTmstFirma().toGregorianCalendar().getTimeInMillis();
	}

}
