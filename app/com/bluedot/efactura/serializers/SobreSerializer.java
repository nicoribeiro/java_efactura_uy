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
			sobreJson.put("fecha", sdf.format(sobre.getFecha()));
			sobreJson.put("idEmisor", sobre.getIdEmisor());
			sobreJson.put("motivo", sobre.getMotivo());
			sobreJson.put("nombreArchivo", sobre.getNombreArchivo());
			sobreJson.put("respuestaCfes", sobre.getRespuestaCfes()!=null? respuestaSerializer.objectToJson(sobre.getRespuestaCfes()): JSONObject.NULL);
			sobreJson.put("respuestaSobre", sobre.getRespuestaSobre()!=null? respuestaSerializer.objectToJson(sobre.getRespuestaSobre()): JSONObject.NULL);
			sobreJson.put("xmlEmpresa", sobre.getXmlEmpresa());
		}
		
		if (sobre instanceof SobreEmitido) {
			SobreEmitido sobreEmitido = (SobreEmitido) sobre;
			sobreJson.put("estadoDgi", sobreEmitido.getEstadoDgi());
			sobreJson.put("xmlDgi", sobreEmitido.getXmlDgi());
			sobreJson.put("fechaConsulta", sdf.format(sobreEmitido.getFechaConsulta()));
			sobreJson.put("respuesta_dgi", sobreEmitido.getRespuesta_dgi());
			sobreJson.put("resultado_dgi", sobreEmitido.getResultado_dgi());
			sobreJson.put("idReceptor", sobreEmitido.getIdReceptor());
			sobreJson.put("token", sobreEmitido.getToken());
			sobreJson.put("cfes", cfeSerializer.objectToJson(sobreEmitido.getCfes(), true));
		}
		
		if (sobre instanceof SobreRecibido) {
			SobreRecibido sobreRecibido = (SobreRecibido) sobre;
			sobreJson.put("timestampRecibido", sdf.format(sobreRecibido.getTimestampRecibido()));
			sobreJson.put("timestampProcesado", sdf.format(sobreRecibido.getTimestampRecibido()));
			sobreJson.put("cfes", cfeSerializer.objectToJson(sobreRecibido.getCfes(), true));
		}
		
		return sobreJson;
	}

}
