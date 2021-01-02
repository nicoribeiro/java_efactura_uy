package com.bluedot.commons.security;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.criterion.Restrictions;
import org.hibernate.sql.JoinType;

import com.bluedot.commons.alerts.Alert;
import com.bluedot.commons.alerts.AlertAccept;
import com.bluedot.commons.alerts.AlertMap;
import com.bluedot.commons.alerts.AlertMetadata;
import com.bluedot.commons.alerts.AlertReceiver;
import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.commons.messages.Message;
import com.bluedot.commons.messages.MessageReceiver;
import com.play4jpa.jpa.models.DefaultQuery;
import com.play4jpa.jpa.models.Finder;
import com.play4jpa.jpa.models.Model;
import com.play4jpa.jpa.query.Query;



@Entity
public class Account extends Model<Account> implements AlertAccept, SettingsPrototype, MessageReceiver
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 512870623865216657L;

	@Id
	@GeneratedValue
	private int id;

	private Date creationTimestamp;

	@OneToMany(cascade = CascadeType.ALL, mappedBy="account", fetch=FetchType.LAZY)
	@LazyCollection(LazyCollectionOption.EXTRA)
	private List<Alert> alerts;
	
	@OneToOne(cascade = CascadeType.ALL)
	private Settings settings;

	public static Finder<Integer, Account> find = new Finder<Integer, Account>(Integer.class, Account.class);
	
	public static Finder<Integer, Alert> alertFinder = new Finder<Integer, Alert>(Integer.class, Alert.class);
	
//	@OneToOne
//	private User owner;

	@ManyToMany(cascade = CascadeType.ALL)
	private List<User> users;
	
	private String uuid;
	
	private String companyName;
	
	private Date validationDate;
	
	private Boolean validated = false;
	
//	private String resetKey = "";
//	private boolean waitingForReset = false;
	
	@Enumerated(EnumType.STRING)
	private AccountType accountType;
	
	public Account(){
		accountType = AccountType.EMISOR_ELECTRONICO;
	}
	
	public static Account findById(Integer id)
	{
		return find.byId(id);
	}
	
	public static Account findById(Integer id, boolean throwExceptionWhenMissing) throws APIException
	{
		Account account = find.byId(id);
		
		if (account==null && throwExceptionWhenMissing)
			throw APIException.raise(APIErrors.ACCOUNT_NOT_FOUND).withParams("id", id);
		
		return account;
	}
	
	public static List<Account> findAll()
	{
		return find.all();
	}
	
	public static List<Account> find(String keyword)
	{
		DefaultQuery<Account> q = (DefaultQuery<Account>) find.query();
		
		if(keyword != null)
		{
			String k = "%" + keyword + "%";
			
			q.getCriteria().createAlias("oem", "oem", JoinType.LEFT_OUTER_JOIN);
			
			q.getCriteria().createAlias("owner", "user");
			
			q.getCriteria().add(Restrictions.or(Restrictions.ilike("companyName", k), Restrictions.ilike("user.firstName", k), Restrictions.ilike("user.lastName", k), Restrictions.ilike("user.emailAddress", k),
							Restrictions.ilike("oem.name", k), Restrictions.ilike("oem.code", k)));
		}
		
		return q.findList();
	}
	
	public static Account findByUUID(String uuid)
	{
		return Account.query().eq("uuid", uuid).findUnique();
	}

	public static Query<Account> query()
	{
		return find.query();
	}

//	public static Account findByOwnerId(int ownerId)
//	{
//		return Account.query().join("owner").eq("owner.id", ownerId).findUnique();
//	}
	
	public static List<Account> findByOEM(Long oem_id)
	{
		return Account.query().join("oem").eq("oem.id", oem_id).findList();
	}
	
//	public String waitForPasswordReset()
//	{
//		String key = UUID.randomUUID().toString();
//		this.resetKey = key;
//		this.waitingForReset = true;
//		this.update();
//		
//		return key;
//	}
	
	public static List<Alert> queryAlerts(int accountId, int deviceId, Date fromTimestamp, Date toTimestamp, int perPage, int page)
	{
		Query<Alert> q = alertFinder.query().join("account").eq("account.id", accountId);
		
		if(fromTimestamp != null)
			q.ge("creationTimestamp", fromTimestamp);
		if(toTimestamp != null)
			q.le("creationTimestamp", toTimestamp);
		
		q.orderByDesc("creationTimestamp");
		
		return (page>-1&&perPage>-1)?q.findPage(page, perPage):q.findList();
	}
	
	
	
	public int getId()
	{
		return id;
	}

	public void setId(int id)
	{
		this.id = id;
	}

	public Date getCreationTimestamp()
	{
		return creationTimestamp;
	}

	public void setCreationTimestamp(Date creationTimestamp)
	{
		this.creationTimestamp = creationTimestamp;
	}

	public List<Alert> getAlerts()
	{
		return alerts;
	}

	public void setAlerts(List<Alert> alerts)
	{
		this.alerts = alerts;
	}

//	public User getOwner()
//	{
//		return owner;
//	}
//
//	public void setOwner(User owner)
//	{
//		this.owner = owner;
//	}

	public List<User> getUsers()
	{
		return users;
	}

	public void setUsers(List<User> users)
	{
		this.users = users;
	}
	
	public String getUuid()
	{
		return uuid;
	}

	public void setUuid(String uuid)
	{
		this.uuid = uuid;
	}
	

	public List<AlertReceiver> getAlertRecivers(Alert alert)
	{
		alerts.add(alert);

		update();
		
		List<AlertReceiver> list = new LinkedList<AlertReceiver>(users);
		
//		list.add(owner);
		
		return list;
		
		
	}

	public Date getValidationDate()
	{
		return validationDate;
	}

	public void setValidationDate(Date validationDate)
	{
		this.validationDate = validationDate;
	}

	public Boolean getValidated()
	{
		return validated;
	}

	public void setValidated(Boolean validated)
	{
		this.validated = validated;
    }
    
	public Alert getAlertById(int alertId)
	{
		for (Alert alert : alerts)
		{
			if (alert.getId()==alertId)
				return alert;	
		}
		return null;
	}
	
	public boolean isUserInAccount(User user){
//		if (user.getId()==owner.getId())
//			return true;
		
		for(User accountUser : users){
			if (user.getId()==accountUser.getId())
				return true;
		
				
		}
		return false;
	}
	
	public boolean isUserInAccountWithLevel(User user, AccountAccessLevel level){
//		if (user.getId()==owner.getId())
//			return false;
		
		for(User accountUser : users){
			if (user.getId()==accountUser.getId())
			{
				for(Permission p : user.getPermissions())
				{
					switch (level)
					{
					
					case VIEWER:
					case ADMIN:
						if(p.getPermissionId().equals("access-level-account_" + this.getId()+":" + level.toString()))
							return true;
						break;
					}
					
				}
			}	
		}
		return false;
	}
	
	public AccountAccessLevel getUserLevelAtAccount(User user){
//		if (user.getId()==owner.getId())
//			return null;
		
		for(User accountUser : users){
			if (user.getId()==accountUser.getId())
			{
				for(Permission p : user.getPermissions())
				{
					if(p.getPermissionId().startsWith("access-level-account_" + this.getId()+":")){
						return AccountAccessLevel.valueOf(p.getPermissionId().split(":")[1]);
					}
				}
			}	
		}
		return null;
	}
	

	@Override
	public Map<Integer,AlertMetadata> getPosibleAlertMetadatas()
	{
		return null;
	}

	@Override
	public AlertMap getAlertMap()
	{
		return null;
	}

//	public String getResetKey()
//	{
//		return resetKey;
//	}
//
//	public void setResetKey(String resetKey)
//	{
//		this.resetKey = resetKey;
//	}
//
//	public boolean isWaitingForReset()
//	{
//		return waitingForReset;
//	}
//
//	public void setWaitingForReset(boolean waitingForReset)
//	{
//		this.waitingForReset = waitingForReset;
//	}
	
	@Override
	public Settings getSettings()
	{
		if (settings == null){
			settings = new Settings();
			settings.save();
		}
		settings.setParent(new DefaultSystemSettings());
		return settings;
	}
	
	public String getCompanyName()
	{
		return companyName;
	}

	public void setCompanyName(String companyName)
	{
		this.companyName = companyName;
	}
	
	
	@Override
	public boolean equals(Object obj)
	{
		if(obj == null)return false;
		if(!Account.class.isAssignableFrom(obj.getClass()))return false;
		return ((Account)obj).getId() == this.getId();
	}


	@Override
	public String getURN()
	{
		return "account:"+id;
	}

	@Override
	public void notify(Message message)
	{
		// TODO Auto-generated method stub
		
	}

	public AccountType getAccountType()
	{
		return accountType;
	}

	public void setAccountType(AccountType accountType)
	{
		this.accountType = accountType;
	}
	
}
