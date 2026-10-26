package com.bluedot.efactura.serializers;

import org.json.JSONException;
import org.json.JSONObject;

import com.bluedot.commons.serializers.JSONSerializer;
import com.bluedot.efactura.model.CAE;
import com.bluedot.efactura.model.Detalle;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.model.Sucursal;
import com.bluedot.efactura.model.Titular;

public class EmpresaSerializer<T> extends JSONSerializer<Empresa> {

	JSONSerializer<Sucursal> sucursalSerializer;
	
	public EmpresaSerializer(JSONSerializer<Sucursal> sucursalSerializer) {
		this.sucursalSerializer = sucursalSerializer;
	}
	
	@Override
	public JSONObject objectToJson(Empresa p, boolean shrinkSerializarion) throws JSONException
	{
		
		JSONObject empresa = new JSONObject();
		
		empresa.put("id", p.getId());
		// rut y razon tambien en la version reducida (CFE, sobres, CAE, reportes) para no tener que buscar la empresa aparte
		empresa.put("rut", p.getRut());
		empresa.put("razon", p.getRazon());

		if (!shrinkSerializarion) {
			empresa.put("nombreComercial", p.getNombreComercial());
			empresa.put("mailRecepcion", p.getMailRecepcion());
			empresa.put("hostRecepcion", p.getHostRecepcion());
			empresa.put("userRecepcion", p.getUserRecepcion());
			empresa.put("puertoRecepcion", p.getPuertoRecepcion());
			empresa.put("emisorElectronico", p.isEmisorElectronico());
			empresa.put("activa", p.isActiva());
			java.text.SimpleDateFormat dia = new java.text.SimpleDateFormat("yyyy-MM-dd");
			empresa.put("fechaInicioEmisorDgi", p.getFechaInicioEmisorDgi() != null ? dia.format(p.getFechaInicioEmisorDgi()) : JSONObject.NULL);
			empresa.put("fechaFinTransicionDgi", p.getFechaFinTransicionDgi() != null ? dia.format(p.getFechaFinTransicionDgi()) : JSONObject.NULL);
			empresa.put("fechaFinEmisorDgi", p.getFechaFinEmisorDgi() != null ? dia.format(p.getFechaFinEmisorDgi()) : JSONObject.NULL);
			empresa.put("urlWebserviceDgi", p.getUrlWebserviceDgi() != null ? p.getUrlWebserviceDgi() : JSONObject.NULL);
			empresa.put("mailContactoTecnicoDgi", p.getMailContactoTecnicoDgi() != null ? p.getMailContactoTecnicoDgi() : JSONObject.NULL);
			empresa.put("mailNotificaciones", p.getMailNotificaciones());
			empresa.put("fromEnvio", p.getFromEnvio());
			empresa.put("offsetMail", p.getOffsetMail());
			empresa.put("diasRespuestaCfe", p.getDiasRespuestaCfe() != null ? p.getDiasRespuestaCfe() : JSONObject.NULL);
			empresa.put("diasRespuestaCfeEfectivo", com.bluedot.efactura.recibidos.RespuestaCfes.dias(p));
			// Reporte diario: si lo manda el poller y hasta que dia ya deberia estar (los mas nuevos se mandan con atraso a
			// proposito; el panel no los cuenta como faltantes)
			play.Configuration config = play.Play.application().configuration();
			boolean automatico = com.bluedot.efactura.pollers.ReporteDiarioPoller.cubre(p, config, new java.util.Date());
			empresa.put("reporteDiario", new JSONObject().put("automatico", automatico).put("hastaEsperado", automatico
					? com.bluedot.efactura.pollers.ReporteDiarioPoller.hastaEsperado(config, java.time.LocalDateTime.now()).toString()
					: JSONObject.NULL));
			empresa.put("paginaWeb", p.getPaginaWeb());
			empresa.put("resolucion", p.getResolucion());
			empresa.put("tieneLogo", p.getLogo() != null && p.getLogo().length > 0);
			empresa.put("sucursales", sucursalSerializer.objectToJson(p.getSucursales(), false));
			// Solo las fechas de validez: nunca el certificado ni la clave privada
			if (p.getFirmaDigital() != null) {
				JSONObject firma = new JSONObject();
				firma.put("validaDesde", p.getFirmaDigital().getValidaDesde() != null ? sdf.format(p.getFirmaDigital().getValidaDesde()) : JSONObject.NULL);
				firma.put("validaHasta", p.getFirmaDigital().getValidaHasta() != null ? sdf.format(p.getFirmaDigital().getValidaHasta()) : JSONObject.NULL);
				empresa.put("firmaDigital", firma);
			} else
				empresa.put("firmaDigital", JSONObject.NULL);
		}
		
		return empresa;
	}

}
