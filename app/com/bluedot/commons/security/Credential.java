package com.bluedot.commons.security;

import java.util.Date;
import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.commons.utils.Crypto;
import com.play4jpa.jpa.models.Finder;
import com.play4jpa.jpa.models.Model;

import play.db.jpa.JPAApi;

@Entity
public class Credential extends Model<Credential>
{
	private static final long serialVersionUID = -126483893823406025L;

	@Id
	@GeneratedValue
	private long id;
	
	private String token;
	private String secret;
	private long nonce;
	
	private boolean valid;
	private Date creationTimestamp;
	private Date revokedTimestamp;
	
	@OneToOne
	private User user;
	
	public Credential(){}
	
	public static Credential generateCredential()
	{
		Credential c = new Credential();
		c.nonce = 0;
		c.creationTimestamp = new Date();
		c.valid = true;
		
		try{
			c.token = Crypto.Hash256(UUID.randomUUID().toString());
			c.secret = Crypto.Hash256(UUID.randomUUID().toString()); 
		}catch(Exception e){
			
		}
		
		return c;
	}
	
	public static Finder<String, Credential> find = new Finder<String, Credential>(String.class, Credential.class);
	
	public static Credential findByKey(JPAApi jpaApi, String key)
	{
		if (key == null) return null;

		try
		{
			return find.query(jpaApi).eq("token", key).findUnique();
		} catch (Exception e)
		{
			return null;
		}
	}
	
	public static Credential findByKey(JPAApi jpaApi, String key, boolean throwExceptionWhenMissing) throws APIException
	{
		Credential credential = findByKey(jpaApi, key);
		
		if (credential==null && throwExceptionWhenMissing)
			throw APIException.raise(APIErrors.CREDENTIAL_NOT_FOUND.withParams("key", key));
		
		return credential;
	}
	
	public void revoke()
	{
		this.valid = false;
		this.revokedTimestamp = new Date();
		// AUDIT-EVENT
	}
	
	public long getId()
	{
		return id;
	}
	public void setId(long id)
	{
		this.id = id;
	}
	public String getToken()
	{
		return token;
	}
	public void setToken(String key)
	{
		this.token = key;
	}
	public String getSecret()
	{
		return secret;
	}
	public void setSecret(String secret)
	{
		this.secret = secret;
	}
	public long getNonce()
	{
		return nonce;
	}
	public void setNonce(long nonce)
	{
		this.nonce = nonce;
	}
	public boolean isValid()
	{
		return valid;
	}
	public void setValid(boolean valid)
	{
		this.valid = valid;
	}
	public Date getCreationTimestamp()
	{
		return creationTimestamp;
	}
	public void setCreationTimestamp(Date creationTimestamp)
	{
		this.creationTimestamp = creationTimestamp;
	}
	public Date getRevokedTimestamp()
	{
		return revokedTimestamp;
	}
	public void setRevokedTimestamp(Date revokedTimestamp)
	{
		this.revokedTimestamp = revokedTimestamp;
	}
	
	public User getUser()
	{
		return user;
	}

	public void setUser(User user)
	{
		this.user = user;
	}

	@Override
	public String toString()
	{
		return this.token + " - " + this.secret;
	}
	
	public static void main(String[] args)
	{
		Credential c = Credential.generateCredential();
		System.out.println(c);
	}
}
