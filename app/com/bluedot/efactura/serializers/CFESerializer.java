package com.bluedot.efactura.serializers;

import org.json.JSONException;
import org.json.JSONObject;

import com.bluedot.commons.serializers.JSONSerializer;
import com.bluedot.efactura.model.CAE;
import com.bluedot.efactura.model.CFE;
import com.bluedot.efactura.model.Detalle;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.model.Sobre;
import com.bluedot.efactura.model.Titular;

public class CFESerializer<T> extends JSONSerializer<CFE> {
	
	JSONSerializer<Empresa> empresaSerializer;
	JSONSerializer<Detalle> detalleSerializer;
	JSONSerializer<CAE> caeSerializer;
	JSONSerializer<Titular> titularSerializer;
	JSONSerializer<Sobre> sobreSerializer;
	
	public CFESerializer(JSONSerializer<Empresa> empresaSerializer, JSONSerializer<Detalle> detalleSerializer, JSONSerializer<CAE> caeSerializer, JSONSerializer<Titular> titularSerializer) {
		this.empresaSerializer = empresaSerializer;
		this.detalleSerializer = detalleSerializer;
		this.caeSerializer = caeSerializer;
		this.titularSerializer = titularSerializer;
	}
	
	public JSONSerializer<Sobre> getSobreSerializer() {
		return sobreSerializer;
	}

	public void setSobreSerializer(JSONSerializer<Sobre> sobreSerializer) {
		this.sobreSerializer = sobreSerializer;
	}

	@Override
	public JSONObject objectToJson(CFE cfe, boolean shrinkSerializarion) throws JSONException
	{
		
		JSONObject cfeJson = new JSONObject();
		
		cfeJson.put("id", cfe.getId());
		
		if (!shrinkSerializarion) {
			//TODO revisar que la nomenclatura de los parametros es diferente en el serializer (aqui) que el formato del POST Documento. combianar a una comun
			cfeJson.put("nro", cfe.getNro());
			cfeJson.put("serie", cfe.getSerie());

			if (cfe.getEmpresaReceptora()!=null)
				cfeJson.put("empresaReceptora", empresaSerializer.objectToJson(cfe.getEmpresaReceptora(), true));
			
			if (cfe.getEmpresaEmisora()!=null)
				cfeJson.put("empresaEmisora", empresaSerializer.objectToJson(cfe.getEmpresaEmisora(), true));
			
			cfeJson.put("detalle", cfe.getDetalle()!=null? detalleSerializer.objectToJson(cfe.getDetalle()): JSONObject.NULL);
			cfeJson.put("estado", cfe.getEstado());
			
			if (cfe.getTitular()!=null) 
					cfeJson.put("titular", titularSerializer.objectToJson(cfe.getTitular()));
			
			cfeJson.put("ordinal", cfe.getOrdinal());
			cfeJson.put("tipo", cfe.getTipo().name());
			
			cfeJson.put("fechaEmision", sdf.format(cfe.getFechaEmision()));
			cfeJson.put("fechaGeneracion", sdf.format(cfe.getFechaGeneracion()));
			cfeJson.put("facturadoDesde", cfe.getFacturadoDesde()!=null? sdf.format(cfe.getFacturadoDesde()): JSONObject.NULL);
			cfeJson.put("facturadoHasta", cfe.getFacturadoHasta()!=null? sdf.format(cfe.getFacturadoHasta()): JSONObject.NULL);
			
			cfeJson.put("indMontoBruto", cfe.isIndMontoBruto());
			
			cfeJson.put("tipoCambio"	, cfe.getTipoCambio());
			cfeJson.put("totMntNoGrv"	, cfe.getTotMntNoGrv());
			cfeJson.put("totMntExpyAsim"	, cfe.getTotMntExpyAsim());
			cfeJson.put("totMntImpPerc", cfe.getTotMntImpPerc());
			cfeJson.put("totMntIVAenSusp", cfe.getTotMntIVAenSusp());
			cfeJson.put("totMntIVATasaMin", cfe.getTotMntIVATasaMin());
			cfeJson.put("totMntIVATasaBas", cfe.getTotMntIVATasaBas());
			cfeJson.put("totMntIVAOtra", cfe.getTotMntIVAOtra());
			cfeJson.put("mntIVATasaMin", cfe.getMntIVATasaMin());
			cfeJson.put("mntIVATasaBas", cfe.getMntIVATasaBas());
			cfeJson.put("mntIVAOtra", cfe.getMntIVAOtra());
			cfeJson.put("ivaTasaMin", cfe.getIvaTasaMin());
			cfeJson.put("ivaTasaBas", cfe.getIvaTasaBas());
			cfeJson.put("totMntTotal", cfe.getTotMntTotal());
			cfeJson.put("totMntRetenido", cfe.getTotMntRetenido());
			cfeJson.put("totValRetPerc", cfe.getTotValRetPerc());
			cfeJson.put("cantLineas", cfe.getCantLineas());
			cfeJson.put("hash", cfe.getHash());
			cfeJson.put("formaDePago", cfe.getFormaDePago());
			cfeJson.put("vencimiento", cfe.getVencimiento()!=null? sdf.format(cfe.getVencimiento()): JSONObject.NULL );
			cfeJson.put("moneda", cfe.getMoneda());
			cfeJson.put("tipoCambio", cfe.getTipoCambio());
			
			cfeJson.put("estado", cfe.getEstado());
			cfeJson.put("generadorJson", new JSONObject(cfe.getGeneradorJson()));
			cfeJson.put("generadorId", cfe.getGeneradorId());
			cfeJson.put("cae", cfe.getCae()!=null? caeSerializer.objectToJson(cfe.getCae()): JSONObject.NULL);
			
			if (cfe.getSobreEmitido()!=null)
				cfeJson.put("sobreEmitido", sobreSerializer.objectToJson(cfe.getSobreEmitido(), true));
			
			if (cfe.getSobreRecibido()!=null)
				cfeJson.put("sobreRecibido", sobreSerializer.objectToJson(cfe.getSobreRecibido(), true));
		
			
			if (cfe.getReferencia()!=null) {
				cfeJson.put("referencia", this.objectToJson(cfe.getReferencia()));
				cfeJson.put("razonReferencia", cfe.getRazonReferencia());
			}
			
			cfeJson.put("adenda", cfe.getAdenda());
			//cfeJson.put("retencionesPercepciones", cfe.getRetencionesPercepciones());
			cfeJson.put("obligatorioReferencia", cfe.isObligatorioReferencia());		
			
		}
		
		return cfeJson;
	}

	

}
