package com.bluedot.efactura.serializers;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.bluedot.efactura.model.TipoDoc;
import com.bluedot.efactura.strategy.builder.CFEBuilderImpl;

import dgi.classes.entreEmpresas.CFEEmpresasType;
import dgi.classes.recepcion.ComplFiscalType;
import dgi.classes.recepcion.CFEDefType.EFact;
import dgi.classes.recepcion.ItemDetFact;
import dgi.classes.recepcion.ReferenciaTipo.Referencia;

public class EFactStrategy extends CommonStrategy {

	@Override
	public JSONObject getEncabezado(CFEEmpresasType cfe) {
		
		JSONObject encabezado = new JSONObject();
		JSONObject idDoc = new JSONObject();
		JSONObject receptor = new JSONObject();
		JSONObject emisor = new JSONObject();
		
		EFact documento = cfe.getCFE().getEFact();
		
		idDoc.put("Nro", documento.getEncabezado().getIdDoc().getNro());
		idDoc.put("Serie", documento.getEncabezado().getIdDoc().getSerie());
		idDoc.put("FchEmis", CFEBuilderImpl.simpleDateFormat.format(documento.getEncabezado().getIdDoc().getFchEmis().toGregorianCalendar().getTime()));
		idDoc.put("TipoCFE", documento.getEncabezado().getIdDoc().getTipoCFE());
		idDoc.put("FmaPago", documento.getEncabezado().getIdDoc().getFmaPago());
		idDoc.put("MntBruto", documento.getEncabezado().getIdDoc().getMntBruto()==null? 0:documento.getEncabezado().getIdDoc().getMntBruto());
		
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
		EFact documento = cfe.getCFE().getEFact();
		
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
		
		List<ItemDetFact> items = cfe.getCFE().getEFact().getDetalle().getItems();
		
		for (ItemDetFact itemDetFact : items) {
			JSONObject item = new JSONObject();
			
			item.put("NroLinDet", String.valueOf(itemDetFact.getNroLinDet()));
			item.put("IndFact", String.valueOf(itemDetFact.getIndFact()));
			item.put("NomItem", itemDetFact.getNomItem());
			item.put("Cantidad", String.valueOf(itemDetFact.getCantidad()));
			item.put("UniMed", itemDetFact.getUniMed()==null || itemDetFact.getUniMed().equals("") ? "N/A" : itemDetFact.getUniMed());
			item.put("PrecioUnitario", String.valueOf(itemDetFact.getPrecioUnitario()));
			item.put("MontoItem", String.valueOf(itemDetFact.getMontoItem()));
			item.put("DscItem", String.valueOf(itemDetFact.getDscItem()));
			
			
			//TODO soportar mas de un CodItem
			if (itemDetFact.getCodItems()!=null &&  itemDetFact.getCodItems().size()>0){ 
				item.put("CodItem", String.valueOf(itemDetFact.getCodItems().get(0).getCod()));
				item.put("TpoCod", String.valueOf(itemDetFact.getCodItems().get(0).getTpoCod()));
			}
			
			detalle.put(item);
		}
		
		return detalle;
	}

	@Override
	public long getTimestampFirma(CFEEmpresasType cfe) {
		EFact documento = cfe.getCFE().getEFact();
		return documento.getTmstFirma().toGregorianCalendar().getTimeInMillis();
	}
	
	@Override
	public ComplFiscalType getComplementoFiscalType(CFEEmpresasType cfe) {
		return cfe.getCFE().getEFact().getComplFiscal();
	}

	@Override
	public boolean hayCompFiscal(CFEEmpresasType cfe) {
		EFact documento = cfe.getCFE().getEFact();
		TipoDoc tipoDoc = TipoDoc.fromInt(documento.getEncabezado().getIdDoc().getTipoCFE().intValue());
		
		switch (tipoDoc) {
			case eFactura_Venta_por_Cuenta_Ajena:
			case eFactura_Venta_por_Cuenta_Ajena_Contingencia:
			case Nota_de_Credito_de_eFactura_Venta_por_Cuenta_Ajena:
			case Nota_de_Credito_de_eFactura_Venta_por_Cuenta_Ajena_Contingencia:
			case Nota_de_Debito_de_eFactura_Venta_por_Cuenta_Ajena:
			case Nota_de_Debito_de_eFactura_Venta_por_Cuenta_Ajena_Contingencia:
				return true;
		default:
			break;
		}
		return false;
	}
	
	@Override
	public JSONObject getTotales(CFEEmpresasType cfe) {
		JSONObject totales = new JSONObject();
		
		totales.put("TpoMoneda", String.valueOf(cfe.getCFE().getEFact().getEncabezado().getTotales().getTpoMoneda()));
		
		if (!String.valueOf(cfe.getCFE().getEFact().getEncabezado().getTotales().getTpoMoneda()).equals("UYU"))	
			totales.put("TpoCambio", String.valueOf(cfe.getCFE().getEFact().getEncabezado().getTotales().getTpoCambio()));
		
		return totales;
	}

}
