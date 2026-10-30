package com.bluedot.efactura.serializers;


import org.json.JSONException;
import org.json.JSONObject;

import com.bluedot.commons.serializers.JSONSerializer;
import com.bluedot.efactura.model.CAE;
import com.bluedot.efactura.model.CFE;
import com.bluedot.efactura.model.Detalle;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.model.RespuestaReceptor;
import com.bluedot.efactura.model.Sobre;
import com.bluedot.efactura.model.SobreEmitido;
import com.bluedot.efactura.model.SobreRecibido;
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

	/**
	 * Respuestas entre empresas del formato de mensaje de respuesta de DGI: respuestaM = la del sobre (AS recibido, BS
	 * rechazado) y respuestaME = la del CFE (AE aceptado, BE rechazado). En los recibidos son las nuestras (ME puede estar
	 * PENDIENTE); en los emitidos, las de la empresa receptora (ME se lee de su respuesta). null = no hay; NO_CORRESPONDE =
	 * emitido cuyo sobre nunca se le envio al receptor (no es emisor electronico): nunca va a haber respuesta.
	 */
	private static void respuestasEntreEmpresas(CFE cfe, JSONObject cfeJson) {
		Object m = JSONObject.NULL, me = JSONObject.NULL;
		if (cfe.getSobreRecibido() != null) {
			SobreRecibido sobre = cfe.getSobreRecibido();
			if (sobre.getEstadoEmpresa() != null)
				m = sobre.getEstadoEmpresa().name();
			if (sobre.getRespuestaCfes() != null)
				me = cfe.getEstado() != null ? cfe.getEstado().name() : "ENVIADA";
			else if (sobre.isRespuestaCfePendiente())
				me = "PENDIENTE";
		} else if (cfe.getSobreEmitido() != null && cfe.getSobreEmitido().getXmlEmpresa() == null) {
			m = me = "NO_CORRESPONDE";
		} else if (cfe.getSobreEmitido() != null) {
			SobreEmitido sobre = cfe.getSobreEmitido();
			if (sobre.getEstadoEmpresa() != null)
				m = sobre.getEstadoEmpresa().name();
			if (sobre.getRespuestaCfes() != null)
				me = RespuestaReceptor.leer(sobre.getRespuestaCfes().getPayload()).estado(cfe.getTipo(), cfe.getSerie(), cfe.getNro());
		}
		cfeJson.put("respuestaM", m);
		cfeJson.put("respuestaME", me);
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
			cfeJson.put("observacionesRecepcion", cfe.getObservacionesRecepcion() != null ? cfe.getObservacionesRecepcion() : JSONObject.NULL);
			cfeJson.put("motivosRechazo", cfe.getMotivo() != null ? new org.json.JSONArray(cfe.getMotivo().stream().map(Enum::name).toArray()) : new org.json.JSONArray());
			cfeJson.put("detalleRechazo", cfe.getDetalleRechazo() != null ? cfe.getDetalleRechazo() : JSONObject.NULL);
			respuestasEntreEmpresas(cfe, cfeJson);
			
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
			cfeJson.put("nroInterno", cfe.getNroInterno() != null ? cfe.getNroInterno() : org.json.JSONObject.NULL);
			cfeJson.put("datosIdDoc", cfe.getDatosIdDoc() != null ? new JSONObject(cfe.getDatosIdDoc()) : org.json.JSONObject.NULL);
			cfeJson.put("cae", cfe.getCae()!=null? caeSerializer.objectToJson(cfe.getCae()): JSONObject.NULL);
			
			if (cfe.getSobreEmitido()!=null) {
				JSONObject sobreJson = sobreSerializer.objectToJson(cfe.getSobreEmitido(), true);
				// Para el panel: si DGI ya acepto el sobre (AS) y todavia no respondio por el CFE, el CFE esta "Recibido"
				if (cfe.getSobreEmitido().getEstadoDgi() != null)
					sobreJson.put("estadoDgi", cfe.getSobreEmitido().getEstadoDgi().name());
				// Sin estadoDgi el sobre no llego a DGI: el error del ultimo intento, para no mostrarlo como "Sin respuesta"
				else if (cfe.getSobreEmitido().getErrorEnvioDgi() != null) {
					sobreJson.put("errorEnvioDgi", cfe.getSobreEmitido().getErrorEnvioDgi());
					if (cfe.getSobreEmitido().getFechaUltimoIntentoDgi() != null)
						sobreJson.put("fechaUltimoIntentoDgi", sdf.format(cfe.getSobreEmitido().getFechaUltimoIntentoDgi()));
					if (cfe.getSobreEmitido().getIntentosDgi() != null)
						sobreJson.put("intentosDgi", cfe.getSobreEmitido().getIntentosDgi());
				}
				cfeJson.put("sobreEmitido", sobreJson);
			}
			
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
