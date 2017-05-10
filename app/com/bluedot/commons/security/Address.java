package com.bluedot.commons.security;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import com.fasterxml.jackson.databind.JsonNode;
import com.play4jpa.jpa.models.Model;

@Entity
public class Address extends Model<Address>
{

	/**
	 * 
	 */
	private static final long serialVersionUID = -2756678107662822141L;
	
	@Id
	@GeneratedValue
	private int id;
	
	private String streetLine1;
	
	private String streetLine2;
	
	private String state;
	
	private String city;
	
	private String country;
	
	private String zipcode;
	
	public int getId()
	{
		return id;
	}
	public void setId(int id)
	{
		this.id = id;
	}
	public String getStreetLine1()
	{
		return streetLine1;
	}
	public void setStreetLine1(String streetLine1)
	{
		this.streetLine1 = streetLine1;
	}
	public String getStreetLine2()
	{
		return streetLine2;
	}
	public void setStreetLine2(String streetLine2)
	{
		this.streetLine2 = streetLine2;
	}
	public String getState()
	{
		return state;
	}
	public void setState(String state)
	{
		this.state = state;
	}
	public String getCity()
	{
		return city;
	}
	public void setCity(String city)
	{
		this.city = city;
	}
	public String getCountry()
	{
		return country;
	}
	public void setCountry(String country)
	{
		this.country = country;
	}
	public String getZipcode()
	{
		return zipcode;
	}
	public void setZipcode(String zipcode)
	{
		this.zipcode = zipcode;
	}
	
	public static Address fromJson(JsonNode json)
	{
		Address address = new Address();
		address.setStreetLine1(json.get("streetLine1").asText());
		address.setStreetLine2(json.has("streetLine2")?json.get("streetLine2").asText():"");
		address.setCountry(json.get("country").asText());
		address.setState(json.get("state").asText());
		address.setCity(json.get("city").asText());
		address.setZipcode(json.get("zipcode").asText());
		return address;
	}
	
}
