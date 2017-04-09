package com.bluedot.commons.security;

import java.util.Iterator;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Transient;

import org.apache.commons.lang3.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.play4jpa.jpa.models.Model;

import play.db.jpa.JPAApi;




@Entity
public class Settings extends Model<Settings>
{

	private static final long serialVersionUID = -1796695127142342338L;

	@Id
	@GeneratedValue
	private long id;

	@Lob
	public String jsonSettings = "{}";

	@Transient
	private SettingsPrototype parent;

	public enum DefaultSetting {

//		CheckInTimeHour(Constants.CheckInTimeHour, "16"), 
//		CheckOutTimeHour(Constants.CheckOutTimeHour, "11"), 
//		CheckInTimeMinute(Constants.CheckInTimeMinute, "00"), 
//		CheckOutTimeMinute(Constants.CheckOutTimeMinute, "00"), 
//		ThermostatConditioningActive(Constants.THERMOSTAT_CONDITIONING_ACTIVE, false), 
//		BeforeReservationTemp(Constants.BeforeReservationTemp, "70"), 
//		LightsConditioningActive(Constants.LIGHTS_CONDITIONING_ACTIVE, false), 
//		AfterReservationActivateThermostatMinute(Constants.AfterReservationActivateThermostatMinute, "30"), 
//		AfterReservationActivateThermostatHour(Constants.AfterReservationActivateThermostatHour, "0"), 
//		BeforeReservationActivateThermostatHour(Constants.BeforeReservationActivateThermostatHour, "1"), 
//		BeforeReservationActivateThermostatMinute(Constants.BeforeReservationActivateThermostatMinute, "0"), 
//		BeforeReservationTurnLightsOnHour(Constants.BeforeReservationTurnLightsOnHour, "1"), 
//		BeforeReservationTurnLightsOnMinute(Constants.BeforeReservationTurnLightsOnMinute, "0"), 
//		AfterReservationTurnLightsOffHour(Constants.AfterReservationTurnLightsOffHour, "0"), 
//		AfterReservationTurnLightsOffMinute(Constants.AfterReservationTurnLightsOffMinute, "30"), 
//		PreviousCheckInRunHour(Constants.PreviousCheckInRunHour, "6"), 
//		PreviousCheckInHour(Constants.PreviousCheckInHour, 10), 
//		ThermostatRangeActive(Constants.THERMOSTAT_RANGE_ACTIVE, false), 
//		ThermostatMaxTemp(Constants.ThermostatMaxTemp, "80"), 
//		ThermostatMinTemp(Constants.ThermostatMinTemp, "55"),
		timezone(Constants.Timezone,"EST");
//		autoCheckInEnabled(Constants.AutoCheckInEnabled,false),
//		autoCheckOutEnabled(Constants.AutoCheckOutEnabled,false),
//		NotifyGuestOnEmergencyCodeActivated(Constants.NOTIFY_GUEST_ON_EMERGENCY_CODE_ACTIVATED, false),
//		NotifyGuestOnPropertyReady(Constants.NOTIFY_GUEST_ON_PROPERTY_READY, false),
//		NotifyGuest(Constants.NOTIFY_GUEST, true),
//		KabaNormalStayLevel(Constants.KabaNormalStayLevel, 0),
//		KabaEarlyCheckInLevel(Constants.KabaEarlyCheckInLevel, 1),
//		KabaMaxQuantityPincodes(Constants.KabaMaxQuantityPincodes,3),
//		KabaReservationsEnabled(Constants.KabaReservationsEnabled,false),
//
//		
//		PoolControllerDoConditioning(Constants.POOL_CONTROLLER_DO_CONDITIONING, false),
//		
//		PoolControllerUnoccupiedStartTimeHour(Constants.POOL_CONTROLLER_UNOCCUPIED_START_TIME_HOUR,10),
//		PoolControllerUnoccupiedStartTimeMInute(Constants.POOL_CONTROLLER_UNOCCUPIED_START_TIME_MINUTE,0),
//		PoolControllerUnoccupiedEndTimeHour(Constants.POOL_CONTROLLER_UNOCCUPIED_END_TIME_HOUR,12), 
//		PoolControllerUnoccupiedEndTimeMinute(Constants.POOL_CONTROLLER_UNOCCUPIED_END_TIME_MINUTE,0), 
//		PoolControllerUnoccupiedSpeed(Constants.POOL_CONTROLLER_UNOCCUPIED_POOL_SPEED,PoolSpeed.LOW.toString()), 
//
//		PoolControllerOccupiedStartTimeHour(Constants.POOL_CONTROLLER_OCCUPIED_START_TIME_HOUR,10), 
//		PoolControllerOccupiedStartTimeMinute(Constants.POOL_CONTROLLER_OCCUPIED_START_TIME_MINUTE,0), 
//		PoolControllerOccupiedEndTimeHour(Constants.POOL_CONTROLLER_OCCUPIED_END_TIME_HOUR,16),
//		PoolControllerOccupiedEndTimeMinute(Constants.POOL_CONTROLLER_OCCUPIED_END_TIME_MINUTE,0), 
//		PoolControllerOccupiedSpeed(Constants.POOL_CONTROLLER_OCCUPIED_POOL_SPEED,PoolSpeed.HI.toString()),
//		
//		PoolControllerPreRentalConditioner(Constants.POOL_CONTROLLER_PRE_RENTAL_CONDITIONER_ACTIVE,true),
//		PoolControllerPreRentalSpaTemp(Constants.POOL_CONTROLLER_SPA_TEMP,75), 
//		PoolControllerPreRentalPoolTemp(Constants.POOL_CONTROLLER_POOL_TEMP,75), 
//		PoolControllerPreRentalPoolSpeed(Constants.POOL_CONTROLLER_PRE_RENTAL_POOL_SPEED,PoolSpeed.HI.toString()),
//		
//		PoolControllerPostRentalConditioner(Constants.POOL_CONTROLLER_POST_RENTAL_CONDITIONER_ACTIVE,true),
//		PoolControllerPostRentalPoolSpeed(Constants.POOL_CONTROLLER_POST_RENTAL_POOL_SPEED,PoolSpeed.OFF.toString()),
//		sendCheckOutLinksEnabled(Constants.SEND_CHECK_OUT_LINKS_ENABLED, false),
//		guestAfterCheckOutMessageEnabled(Constants.GUEST_AFTER_CHECKOUT_MESSAGE_ENABLED, false),
//		
//		thermostatSeasonMode(Constants.THERMOSTAT_SEASON_MODE, "SUMMER"),
//		cleaningScheduleType(Constants.CLEANING_SCHEDULE_TYPE, "FULL"),
//		qaScheduleType(Constants.QA_SCHEDULE_TYPE, "FULL");
		
		private String key;
		private Object value;

		private DefaultSetting(String key, Object value) {
			this.key = key;
			this.value = value;
		}

		public String getKey()
		{
			return key;
		}

		public Object getValue()
		{
			return value;
		}

	}

	public long getId()
	{
		return id;
	}

	public void setId(long id)
	{
		this.id = id;
	}

	public JSONObject getJsonSettingsObject(JPAApi jpaApi)
	{
		return this.getJsonSettingsObject(jpaApi, null);
	}
	
	public JSONObject getJsonSettingsObject(JPAApi jpaApi, String schema)
	{
		JSONObject Obj1;

		JSONObject merged = null;

		try
		{
			if (parent == null)
			{
				merged = new JSONObject();
			} else
			{
				Obj1 = parent.getSettings(jpaApi).getJsonSettingsObject(jpaApi, schema);
				if(Obj1 != null)
				{
					String[] names = JSONObject.getNames(Obj1);
					if(names != null)
						merged = new JSONObject(Obj1, names);
				}else
					merged = new JSONObject();
			}

			JSONObject Obj2 = getJsonObject(schema);

			if (JSONObject.getNames(Obj2) != null)
			{
				for (String key : JSONObject.getNames(Obj2))
				{
					merged.put(key, Obj2.get(key));
				}
			}
			return merged;
		} catch (JSONException e)
		{
			e.printStackTrace();
			return new JSONObject();
		}

		

	}
	
	public String getJsonSettings(JPAApi jpaApi, String schema)
	{
		JSONObject Obj1;

		JSONObject merged = null;

		try
		{
			if (parent == null)
			{
				merged = new JSONObject();
			} else
			{
				Obj1 = parent.getSettings(jpaApi).getJsonSettingsObject(jpaApi, schema);
				if(Obj1 != null)
				{
					String[] names = JSONObject.getNames(Obj1);
					if(names != null)
						merged = new JSONObject(Obj1, names);
				}else
					merged = new JSONObject();
			}

			JSONObject Obj2 = getJsonObject(schema);

			if (JSONObject.getNames(Obj2) != null)
			{
				for (String key : JSONObject.getNames(Obj2))
				{
					merged.put(key, Obj2.get(key));
				}
			}
			return merged.toString();
		} catch (JSONException e)
		{
			e.printStackTrace();
			return "{}";
		}

	}
	
	

	public void setJsonSettings(String json)
	{
		this.jsonSettings = json;
	}

	/**
	 * Returns local Settings, not all the hierarchy.
	 * @return
	 */
	public JSONObject getJsonObject()
	{
		return this.getJsonObject(null);
	}

	/**
	 * Returns local Settings, not all the hierarchy.
	 * @return
	 */
	public JSONObject getJsonObject(String schema)
	{
		try
		{
			
			JSONObject json = new JSONObject(StringEscapeUtils.unescapeJava(jsonSettings));

			if (schema==null || schema.equals(""))
				return json;
			
			JSONObject result = new JSONObject();

			for (@SuppressWarnings("unchecked")
			Iterator<String> iterator = json.keys(); iterator.hasNext();)
			{
				String key = iterator.next();

				if (key.startsWith(schema+"_"))
					result.put(key, json.get(key));

			}

			return result;
		} catch (JSONException e)
		{
			e.printStackTrace();
			return null;
		}
	}

	public int getInt(JPAApi jpaApi, String key)
	{
		return Integer.parseInt(getSetting(jpaApi, key));

	}
	
	public double getDouble(JPAApi jpaApi, String key)
	{
		return Double.parseDouble(getSetting(jpaApi, key));

	}

	public String getString(JPAApi jpaApi, String key)
	{

		return StringEscapeUtils.unescapeHtml4(getSetting(jpaApi, key));

	}

	public void setSetting(String key, Object value)
	{
		try
		{

			JSONObject obj = getJsonObject();
			if (value instanceof String)
			{
				String escaped = StringEscapeUtils.escapeHtml4(value.toString());
				obj.put(key, escaped);
			} else
			{
				obj.put(key, value);
			}
			setJsonSettings(obj.toString());
		} catch (JSONException e)
		{
			e.printStackTrace();
		}
	}
	
	public void unsetSetting(String key)
	{
		JSONObject obj = getJsonObject();
		if (obj.has(key))
			obj.remove(key);
		
		setJsonSettings(obj.toString());
	}

	public void updateSettings(JSONObject settings) throws APIException
	{
		this.updateSettings(settings, null);
	}

	public void updateSettings(JSONObject settings, String schema) throws APIException
	{
		for (@SuppressWarnings("unchecked")
		Iterator<String> keys = settings.keys(); keys.hasNext();)
		{
			String key = keys.next();
			try
			{
				if (schema != null && !schema.equals(""))
				{
					if (key.startsWith(schema))
						this.setSetting(key, settings.get(key));
					else
						throw APIException.raise(APIErrors.SETTING_SCHEMA_ERROR);
				} else
					this.setSetting(key, settings.get(key));
			} catch (JSONException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public void unsetSettings(JSONObject settings) throws APIException
	{
		this.unsetSettings(settings, null);
	}
	
	public void unsetSettings(JSONObject settings, String schema) throws APIException
	{
		for (@SuppressWarnings("unchecked")
		Iterator<String> keys = settings.keys(); keys.hasNext();)
		{
			String key = keys.next();
			if (schema != null && !schema.equals(""))
			{
				if (key.startsWith(schema))
					this.unsetSetting(key);
				else
					throw APIException.raise(APIErrors.SETTING_SCHEMA_ERROR);
			} else
				this.unsetSetting(key);
		}
	}

	public void setString(String key, String value)
	{
		try
		{
			getJsonObject().put(key, value);
		} catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	public boolean getBool(JPAApi jpaApi, String key)
	{
		return Boolean.parseBoolean(getSetting(jpaApi, key, "false"));
	}

	public void setBool(String key, boolean value)
	{
		try
		{
			getJsonObject().put(key, value);
		} catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	public JSONArray getArray(JPAApi jpaApi, String key)
	{
		try
		{
			return new JSONArray(getSetting(jpaApi, key));
		} catch (JSONException e)
		{
			e.printStackTrace();
		}
		return null;
	}

	public JSONObject getObject(JPAApi jpaApi, String key)
	{
		try
		{
			return new JSONObject(getSetting(jpaApi, key));
		} catch (JSONException e)
		{
			e.printStackTrace();
		}
		return null;
	}

	public boolean has(JPAApi jpaApi, String key)
	{
		if (parent == null)
			return getJsonObject().has(key);
		else
			return getJsonObject().has(key) || parent.getSettings(jpaApi).has(jpaApi, key);
	}

	private String getSetting(JPAApi jpaApi, String key)
	{
		return getSetting(jpaApi, key, null);
	}

	private String getSetting(JPAApi jpaApi, String key, String defaultValue)
	{
		try
		{
			if (parent == null)
				if (!getJsonObject().has(key))
					return defaultValue;
				else
					return getJsonObject().get(key).toString();
			else
				return getJsonObject().has(key) ? getJsonObject().get(key).toString() : parent.getSettings(jpaApi).getSetting(jpaApi, key, defaultValue);
		} catch (JSONException e)
		{
			e.printStackTrace();
			return null;
		}
	}

	public void setParent(SettingsPrototype parent)
	{
		this.parent = parent;
	}

	public SettingsPrototype getParent()
	{
		return parent;
	}

}
