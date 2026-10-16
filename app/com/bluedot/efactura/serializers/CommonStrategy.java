package com.bluedot.efactura.serializers;

import org.json.JSONArray;
import org.json.JSONObject;

import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.model.Sucursal;
import com.bluedot.efactura.strategy.builder.CFEBuilderImpl;

import dgi.classes.entreEmpresas.CFEEmpresasType;
import dgi.classes.recepcion.ComplFiscalDataType;
import dgi.classes.recepcion.ComplFiscalType;
import dgi.classes.recepcion.Emisor;
import dgi.classes.recepcion.ReferenciaTipo;
import dgi.classes.recepcion.ReferenciaTipo.Referencia;
import dgi.classes.recepcion.wrappers.ReceptorInterface;


public abstract class CommonStrategy implements CFEEmpresasStrategy{

	public CommonStrategy() {
		super();
	}

	public abstract ComplFiscalType getComplementoFiscalType(CFEEmpresasType cfe);
	
	@Override
	public JSONObject getCompFiscal(CFEEmpresasType cfe) {
		
		JSONObject compFiscal = new JSONObject();
		
		ComplFiscalDataType complFiscalDataType = getComplementoFiscalType(cfe).getComplFiscalData();
		
		compFiscal.put("TipoDocMdte", complFiscalDataType.getTipoDocMdte());
		compFiscal.put("DocMdte", complFiscalDataType.getDocMdte());
		compFiscal.put("NombreMdte", complFiscalDataType.getNombreMdte());
		compFiscal.put("Pais", complFiscalDataType.getPais());
		compFiscal.put("RUCEmisor", complFiscalDataType.getRUCEmisor());
		
		return compFiscal;
	}

	/* Partes comunes a las estrategias de exportacion (EFactExpStrategy y ERemExpStrategy) */

	static JSONObject emisor(Emisor emisorXml) {
		JSONObject emisor = new JSONObject();
		emisor.put("CdgDGISucur", emisorXml.getCdgDGISucur());
		emisor.put("Ciudad", emisorXml.getCiudad());
		emisor.put("Departamento", emisorXml.getDepartamento());
		emisor.put("DomFiscal", emisorXml.getDomFiscal());
		emisor.put("RUCEmisor", emisorXml.getRUCEmisor());
		emisor.put("RznSoc", emisorXml.getRznSoc());
		emisor.put("NomComercial", emisorXml.getNomComercial());
		return emisor;
	}

	/**
	 * Receptor cuando el CFE no lo trae (es opcional en el XSD): la empresa receptora del sobre, como en EFactStrategy, para
	 * que el builder tenga los datos obligatorios del receptor
	 */
	static JSONObject receptorEmpresa(Empresa empresaReceptora) {
		JSONObject receptor = new JSONObject();
		if (empresaReceptora == null)
			return receptor;
		receptor.put("TipoDocRecep", 2);
		receptor.put("CodPaisRecep", "UY");
		receptor.put("DocRecep", empresaReceptora.getRut());
		receptor.put("RznSocRecep", empresaReceptora.getRazon());
		receptor.put("PaisRecep", "Uruguay");
		if (empresaReceptora.getSucursales().size() > 0) {
			// Tomo una sucursal cualquiera para la direccion
			Sucursal sucursal = empresaReceptora.getSucursales().iterator().next();
			receptor.put("CiudadRecep", sucursal.getCiudad());
			receptor.put("DeptoRecep", sucursal.getDepartamento());
			receptor.put("DirRecep", sucursal.getDomicilioFiscal());
		} else {
			// no tengo datos por lo que los lleno vacios
			receptor.put("CiudadRecep", "N/A");
			receptor.put("DeptoRecep", "N/A");
			receptor.put("DirRecep", "N/A");
		}
		return receptor;
	}

	/** Receptor de exportacion (Receptor_Fact_Exp o Receptor_Rem_Exp): solo los campos que vienen en el XML */
	static JSONObject receptorExportacion(ReceptorInterface receptorXml) {
		JSONObject receptor = new JSONObject();
		if (receptorXml.getTipoDocRecep() != 0)
			receptor.put("TipoDocRecep", receptorXml.getTipoDocRecep());
		poner(receptor, "CodPaisRecep", receptorXml.getCodPaisRecep());
		poner(receptor, "DocRecep", receptorXml.getDocRecep());
		poner(receptor, "DocRecepExt", receptorXml.getDocRecepExt());
		poner(receptor, "RznSocRecep", receptorXml.getRznSocRecep());
		poner(receptor, "DirRecep", receptorXml.getDirRecep());
		poner(receptor, "CiudadRecep", receptorXml.getCiudadRecep());
		poner(receptor, "DeptoRecep", receptorXml.getDeptoRecep());
		poner(receptor, "PaisRecep", receptorXml.getPaisRecep());
		poner(receptor, "CP", receptorXml.getCP());
		poner(receptor, "InfoAdicional", receptorXml.getInfoAdicional());
		poner(receptor, "LugarDestEnt", receptorXml.getLugarDestEnt());
		poner(receptor, "CompraID", receptorXml.getCompraID());
		return receptor;
	}

	static JSONArray referencias(ReferenciaTipo referenciaTipo) {
		JSONArray referencias = new JSONArray();
		if (referenciaTipo == null)
			return referencias;

		for (Referencia referencia : referenciaTipo.getReferencias()) {
			JSONObject referenciaJson = new JSONObject();
			referenciaJson.put("NroCFERef", referencia.getNroCFERef());
			referenciaJson.put("NroLinRef", referencia.getNroLinRef());
			if (referencia.getFechaCFEref() != null)
				referenciaJson.put("FechaCFEref", CFEBuilderImpl.simpleDateFormat.format(referencia.getFechaCFEref().toGregorianCalendar().getTime()));
			referenciaJson.put("IndGlobal", referencia.getIndGlobal());
			referenciaJson.put("RazonRef", referencia.getRazonRef());
			referenciaJson.put("Serie", referencia.getSerie());
			referenciaJson.put("TpoDocRef", referencia.getTpoDocRef());
			referencias.put(referenciaJson);
		}
		return referencias;
	}

	/** Pone el valor si no es null (JSONObject.put con null borra la clave, esto lo deja explicito) */
	static void poner(JSONObject json, String campo, Object valor) {
		if (valor != null)
			json.put(campo, valor);
	}

}
