package com.bluedot.efactura.serializers;

import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluedot.commons.serializers.JSONSerializer;
import com.bluedot.commons.utils.XML;

import dgi.classes.entreEmpresas.CFEEmpresasType;

public class CFEEmpresasTypeSerializer<T> extends JSONSerializer<CFEEmpresasType> {

	final static Logger logger = LoggerFactory.getLogger(CFEEmpresasTypeSerializer.class);
	
	@Override
	public JSONObject objectToJson(CFEEmpresasType cfe, boolean shrinkSerializarion) throws JSONException {
		JSONObject cfeJson = new JSONObject();
		
		try {
			if (cfe.getAdenda()!=null)
				if (cfe.getAdenda() instanceof org.apache.xerces.dom.ElementNSImpl)
					cfeJson.put("Adenda", XML.documentToString(((org.apache.xerces.dom.ElementNSImpl) cfe.getAdenda()).getOwnerDocument()));
				else
					if (cfe.getAdenda() instanceof byte[])
						cfeJson.put("Adenda", new String((byte[])cfe.getAdenda()));
					else
						if (cfe.getAdenda() instanceof String)
							cfeJson.put("Adenda", cfe.getAdenda());
							else
								logger.info("Adenda no es de un tipo conocido por lo que no se puede serializar");
		} catch (TransformerFactoryConfigurationError | TransformerException e) {
			e.printStackTrace();
		}

		CFEEmpresasStrategy strategy = getStrategy(cfe);

		if (strategy==null)
			logger.info("No se pudo obtener Estrategia");
		
		cfeJson.put("TmstFirma", strategy.getTimestampFirma(cfe));
		
		cfeJson.put("Encabezado", strategy.getEncabezado(cfe));
		
		cfeJson.put("Detalle", strategy.getDetalle(cfe));
		
		JSONArray referencias = strategy.getReferencia(cfe);
		if (referencias.length() > 0)
			cfeJson.put("Referencia", referencias);
		
		return cfeJson;
	}

	private CFEEmpresasStrategy getStrategy(CFEEmpresasType cfe) {
		if (cfe.getCFE().getEFact()!=null)
			return new EFactStrategy();
		
		if (cfe.getCFE().getERem()!=null)
			return new ERemStrategy();
		
		if (cfe.getCFE().getEResg()!=null)
			return new EResgStrategy();
		
		if (cfe.getCFE().getETck()!=null)
			return new ETckStrategy();
		
		return null;
	}
	
}
