package com.bluedot.efactura.serializers;

import org.json.JSONArray;
import org.json.JSONObject;

public class AdendaSerializer {

	public static void convertAdenda(StringBuilder stringBuilder, Object object) {

		if (object instanceof JSONArray) {
			JSONArray jsonArray = (JSONArray) object;
			for (int i = 0; i < jsonArray.length(); i++) {
				convertAdenda(stringBuilder, jsonArray.get(i));
			}
		}

		if (object instanceof JSONObject) {
			JSONObject jsonObject = (JSONObject) object;
			for (int i = 0; i < jsonObject.names().length(); i++) {
				Object child = jsonObject.get(jsonObject.names().getString(i));
				stringBuilder.append(splitCamelCase(jsonObject.names().getString(i)) + ": ");
				convertAdenda(stringBuilder, child);
				stringBuilder.append('\n');
			}
			
		}

		if (object instanceof String || object instanceof Integer || object instanceof Long) {
			stringBuilder.append(object);
		}

	}
	
	static String splitCamelCase(String s) {
		   return s.replaceAll(
		      String.format("%s|%s|%s",
		         "(?<=[A-Z])(?=[A-Z][a-z])",
		         "(?<=[^A-Z])(?=[A-Z])",
		         "(?<=[A-Za-z])(?=[^A-Za-z])"
		      ),
		      " "
		   );
		}
	
}
