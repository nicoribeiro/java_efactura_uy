package com.bluedot.efactura.serializers;

import org.json.JSONException;
import org.json.JSONObject;

import com.bluedot.commons.serializers.JSONSerializer;
import com.bluedot.efactura.model.CFE;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.model.Respuesta;
import com.bluedot.efactura.model.Sobre;
import com.bluedot.efactura.model.SobreEmitido;
import com.bluedot.efactura.model.SobreRecibido;

public class SobreSerializer<T> extends JSONSerializer<Sobre> {

	JSONSerializer<Empresa> empresaSerializer;
	JSONSerializer<Respuesta> respuestaSerializer;
	JSONSerializer<CFE> cfeSerializer;
	
	public SobreSerializer(JSONSerializer<Empresa> empresaSerializer, JSONSerializer<Respuesta> respuestaSerializer, JSONSerializer<CFE> cfeSerializer) {
		this.empresaSerializer = empresaSerializer;
		this.respuestaSerializer = respuestaSerializer;
		this.cfeSerializer = cfeSerializer;
	}
	
	
	@Override
	public JSONObject objectToJson(Sobre sobre, boolean shrinkSerializarion) throws JSONException
	{
		
		JSONObject sobreJson = new JSONObject();
		
		sobreJson.put("id", sobre.getId());
		
		if (!shrinkSerializarion) {
			sobreJson.put("cantComprobantes", sobre.getCantComprobantes());
			sobreJson.put("cfes", cfeSerializer.objectToJson(sobre.getCfes(), true));
			sobreJson.put("empresaReceptora", sobre.getEmpresaReceptora()!=null? empresaSerializer.objectToJson(sobre.getEmpresaReceptora(), true): JSONObject.NULL);
			sobreJson.put("empresaEmisora", sobre.getEmpresaEmisora()!=null? empresaSerializer.objectToJson(sobre.getEmpresaEmisora(), true): JSONObject.NULL);
			sobreJson.put("estadoEmpresa", sobre.getEstadoEmpresa());
			sobreJson.put("fecha", sobre.getFecha() != null ? sdf.format(sobre.getFecha()) : JSONObject.NULL);
			sobreJson.put("idEmisor", sobre.getIdEmisor());
			sobreJson.put("motivo", sobre.getMotivo());
			sobreJson.put("nombreArchivo", sobre.getNombreArchivo());
			sobreJson.put("respuestaCfes", sobre.getRespuestaCfes()!=null? respuestaSerializer.objectToJson(sobre.getRespuestaCfes()): JSONObject.NULL);
			sobreJson.put("respuestaSobre", sobre.getRespuestaSobre()!=null? respuestaSerializer.objectToJson(sobre.getRespuestaSobre()): JSONObject.NULL);
			sobreJson.put("xmlEmpresa", sobre.getXmlEmpresa());
		
			if (sobre instanceof SobreEmitido) {
				SobreEmitido sobreEmitido = (SobreEmitido) sobre;
				sobreJson.put("estadoDgi", sobreEmitido.getEstadoDgi());
				sobreJson.put("xmlDgi", sobreEmitido.getXmlDgi());
				if (sobreEmitido.getFechaConsulta()!=null) 
					sobreJson.put("fechaConsulta", sdf.format(sobreEmitido.getFechaConsulta()));
				else
					sobreJson.put("fechaConsulta","");
				sobreJson.put("respuesta_dgi", sobreEmitido.getRespuesta_dgi());
				sobreJson.put("resultado_dgi", sobreEmitido.getResultado_dgi());
				sobreJson.put("idReceptor", sobreEmitido.getIdReceptor());
				sobreJson.put("errorEnvioDgi", sobreEmitido.getErrorEnvioDgi() != null ? sobreEmitido.getErrorEnvioDgi() : JSONObject.NULL);
				sobreJson.put("fechaUltimoIntentoDgi", sobreEmitido.getFechaUltimoIntentoDgi() != null ? sdf.format(sobreEmitido.getFechaUltimoIntentoDgi()) : JSONObject.NULL);
				sobreJson.put("intentosDgi", sobreEmitido.getIntentosDgi() != null ? sobreEmitido.getIntentosDgi() : JSONObject.NULL);
				// El sobre para la empresa receptora se le manda recien cuando DGI acepta un CFE (AE): hasta entonces no se envio
				boolean aceptado = false;
				if (sobreEmitido.getCfes() != null)
					for (CFE cfe : sobreEmitido.getCfes())
						aceptado |= cfe.getEstado() == dgi.classes.respuestas.cfe.EstadoACKCFEType.AE;
				sobreJson.put("cfeAceptadoDgi", aceptado);
				sobreJson.put("cfes", cfeSerializer.objectToJson(sobreEmitido.getCfes(), true));
			}
			
			if (sobre instanceof SobreRecibido) {
				SobreRecibido sobreRecibido = (SobreRecibido) sobre;
				sobreJson.put("timestampRecibido", sobreRecibido.getTimestampRecibido() != null ? sdf.format(sobreRecibido.getTimestampRecibido()) : JSONObject.NULL);
				sobreJson.put("timestampProcesado", sobreRecibido.getTimestampProcesado() != null ? sdf.format(sobreRecibido.getTimestampProcesado()) : JSONObject.NULL);
				sobreJson.put("respuestaCfePendiente", sobreRecibido.isRespuestaCfePendiente());
				java.util.Date programada = com.bluedot.efactura.recibidos.RespuestaCfes.programada(sobreRecibido);
				sobreJson.put("respuestaCfeProgramada", programada != null ? sdf.format(programada) : JSONObject.NULL);
				sobreJson.put("observacionesRecepcion", sobreRecibido.getObservacionesRecepcion() != null ? sobreRecibido.getObservacionesRecepcion() : JSONObject.NULL);
				sobreJson.put("cfes", cfeSerializer.objectToJson(sobreRecibido.getCfes(), true));
			}
		
		}
		
		return sobreJson;
	}

}
