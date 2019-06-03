package com.bluedot.commons.security;

import java.util.Date;
import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.play4jpa.jpa.models.Finder;
import com.play4jpa.jpa.models.Model;

@Entity
public class Session extends Model<Session>
{

	/**
	 * 
	 */
	private static final long serialVersionUID = -6111608082703517322L;

	@Id
	private String token;
	private Date creationTimestamp;
	private Date lastAccess;
	private String ip;
	private String userAgent;
	private boolean valid;
	
	@ManyToOne(fetch=FetchType.LAZY)
	private User user;
	
	public Session(User user, String ip, String userAgent){
		this.token = UUID.randomUUID().toString();
		this.user = user;
		this.creationTimestamp = new Date();
		this.valid = true;
		this.ip = ip;
		this.userAgent = userAgent;
	}
	
	public Session(){
		
	}


	public static Finder<String, Session> find = new Finder<String, Session>(String.class, Session.class);

	public static User findUserByAuthToken(String authToken)
	{
		if (authToken == null)
		{
			return null;
		}

		try
		{
			return find.query().eq("token", authToken).findUnique().user;
		} catch (Exception e)
		{
			return null;
		}
	}

	public static Session findByAuthToken(String authToken)
	{
		if (authToken == null)
		{
			return null;
		}

		try
		{
			return find.query().eq("token", authToken).findUnique();
		} catch (Exception e)
		{
			return null;
		}
	}
	
	public static Session findByAuthToken(String authToken, boolean throwExceptionWhenMissing) throws APIException
	{
		Session session = findByAuthToken(authToken);
		
		if (session==null && throwExceptionWhenMissing)
			throw APIException.raise(APIErrors.SESSION_NOT_FOUND).withParams("authToken", authToken);
		
		return session;
	}

	public String getToken()
	{
		return token;
	}

	public void setToken(String token)
	{
		this.token = token;
	}

	public Date getCreationTimestamp()
	{
		return creationTimestamp;
	}

	public void setCreationTimestamp(Date creationTimestamp)
	{
		this.creationTimestamp = creationTimestamp;
	}

	public Date getLastAccess()
	{
		return lastAccess;
	}

	public void setLastAccess(Date lastAccess)
	{
		this.lastAccess = lastAccess;
	}

	public String getIp()
	{
		return ip;
	}

	public void setIp(String ip)
	{
		this.ip = ip;
	}

	public String getUserAgent()
	{
		return userAgent;
	}

	public void setUserAgent(String userAgent)
	{
		this.userAgent = userAgent;
	}

	public Boolean isValid()
	{
		return valid;
	}

	public void setValid(Boolean valid)
	{
		this.valid = valid;
	}

	public User getUser()
	{
		return user;
	}

	public void setUser(User user)
	{
		this.user = user;
	}

}
