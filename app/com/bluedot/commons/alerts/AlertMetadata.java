package com.bluedot.commons.alerts;

import javax.persistence.Entity;
import javax.persistence.Id;

import com.play4jpa.jpa.models.Finder;
import com.play4jpa.jpa.models.Model;

import play.db.jpa.JPAApi;

@Entity
public class AlertMetadata extends Model<AlertMetadata>
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 2334770203094813413L;

	@Id
	private int id;

	private String description;
	
	private String parameters;

	public AlertMetadata(int id, String description) {
		super();
		this.id = id;
		this.description = description;
	}

	public AlertMetadata() {
		super();
	}
	
	public enum enumeration
	{
		DEVICE_ON(1),
		DEVICE_OFF(2),
		DIMMER_EXCEED_PERCENTAGE(3),
		EMERGENCY_PIN_ENTERED(4),
		PINCODE_PROGRAM_FAILED(5),
		LOCK_CLOSE(6),
		LOCK_OPEN(7),
		DEVICE_LOW_BAT(8),
		STORAGE_EXCEED_PERCENTAGE(9),
		THERMOSTAT_TEMP_OUTSIDE_USER_RANGE(10),
		SENSOR_TRIPPED(11),
		SENSOR_UNTRIPPED(12),
		CAMERA_MOTION_DETECTION(13),
		HUMIDITY_OUTSIDE_USER_RANGE(14),
		LIGHT_LEVEL_OUTSIDE_USER_RANGE(15),
		GATEWAY_COMMUNICATION(16),
		GUEST_ACCESS_PROPERTY(18),
		FAILED_REPORT(19),
		PROPERTY_CLEANING_FINISHED(20),
		PROPERTY_QA_FINISHED(21),
		PROPERTY_MAINTENANCE_FINISHED(22),
		PROPERTY_CHECK_OUT(23),
		PROPERTY_MAINTENANCE_NEEDED(24),
		PROPERTY_MAINTENANCE_HOLD(25),
		PROPERTY_MAINTENANCE_STARTED(26),
		PROPERTY_CLEANING_NEEDED(27),
		PROPERTY_QA_NEEDED(28),
		PROPERTY_CUSTOM1_NEEDED(29),
		PROPERTY_CUSTOM2_NEEDED(30),
		EMERGENCY_CODE_ACTIVATED_GUEST(40),
		EMERGENCY_CODE_ACTIVATED_PM(41),
		PROPERTY_IS_READY(42),
		PROPERTY_CLEANING_STARTED(43),
		GUEST_NOTIFICATION(46),
		GUEST_MESSAGE(47),
		
		PROPERTY_QA_STARTED(48),
		PROPERTY_QA_HOLD(49),
		PROPERTY_CLEANING_HOLD(50),
		
		PROPERTY_CUSTOM1_STARTED(51),
		PROPERTY_CUSTOM1_HOLD(52),
		PROPERTY_CUSTOM1_FINISHED(53),
		
		PROPERTY_CUSTOM2_STARTED(54),
		PROPERTY_CUSTOM2_HOLD(55),
		PROPERTY_CUSTOM2_FINISHED(56);
		
		public int id; 
		
		private enumeration(int id)
		{
			this.id = id;
		}
	}

	private static Finder<Integer, AlertMetadata> find = new Finder<Integer, AlertMetadata>(Integer.class, AlertMetadata.class);

	public static final int DEVICE_ON = 1;
	public static final int DEVICE_OFF = 2;
	public static final int DIMMER_EXCEED_PERCENTAGE = 3;
	public static final int EMERGENCY_PIN_ENTERED = 4;
	public static final int PINCODE_PROGRAM_FAILED = 5;
	public static final int LOCK_CLOSE = 6;
	public static final int LOCK_OPEN = 7;
	public static final int DEVICE_LOW_BAT = 8;
	public static final int STORAGE_EXCEED_PERCENTAGE = 9;
	public static final int THERMOSTAT_TEMP_OUTSIDE_USER_RANGE = 10;
	public static final int SENSOR_TRIPPED = 11;
	public static final int SENSOR_UNTRIPPED = 12;
	public static final int CAMERA_MOTION_DETECTION = 13;
	public static final int HUMIDITY_OUTSIDE_USER_RANGE = 14;
	public static final int LIGHT_LEVEL_OUTSIDE_USER_RANGE = 15;
	public static final int GATEWAY_COMMUNICATION = 16;
	public static final int GUEST_ACCESS_PROPERTY = 18;
	public static final int FAILED_REPORT = 19;
	public static final int PROPERTY_CLEANING_FINISHED = 20;
	public static final int PROPERTY_QA_FINISHED = 21;
	public static final int PROPERTY_MAINTENANCE_FINISHED = 22;
	public static final int PROPERTY_CHECK_OUT = 23;
	public static final int PROPERTY_MAINTENANCE_NEEDED = 24;
	public static final int PROPERTY_MAINTENANCE_HOLD = 25;
	public static final int PROPERTY_MAINTENANCE_STARTED = 26;
	public static final int PROPERTY_CLEANING_NEEDED = 27;
	public static final int PROPERTY_QA_NEEDED = 28;
	public static final int PROPERTY_CUSTOM1_NEEDED = 29;
	public static final int PROPERTY_CUSTOM2_NEEDED = 30;
	public static final int EMERGENCY_CODE_ACTIVATED_GUEST = 40;
	public static final int EMERGENCY_CODE_ACTIVATED_PM = 41;
	public static final int PROPERTY_IS_READY = 42;
	public static final int PROPERTY_CLEANING_STARTED = 43;
	public static final int GUEST_NOTIFICATION = 46;
	public static final int GUEST_MESSAGE = 47;
	
	public static final int PROPERTY_QA_STARTED = 48;
	public static final int PROPERTY_QA_HOLD = 49;
	public static final int PROPERTY_CLEANING_HOLD = 50;
	
	public static final int PROPERTY_CUSTOM1_STARTED = 51;
	public static final int PROPERTY_CUSTOM1_HOLD = 52;
	public static final int PROPERTY_CUSTOM1_FINISHED = 53;
	
	public static final int PROPERTY_CUSTOM2_STARTED = 54;
	public static final int PROPERTY_CUSTOM2_HOLD = 55;
	public static final int PROPERTY_CUSTOM2_FINISHED = 56;
	
	public static AlertMetadata findById(JPAApi jpaApi, Integer id)
	{
		return find.byId(jpaApi, id);
	}

	public int getId()
	{
		return id;
	}

	public void setId(int id)
	{
		this.id = id;
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	public String getParameters()
	{
		return parameters;
	}

	public void setParameters(String parameters)
	{
		this.parameters = parameters;
	}
	
}
