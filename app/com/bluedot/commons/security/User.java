package com.bluedot.commons.security;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluedot.commons.alerts.Alert;
import com.bluedot.commons.alerts.AlertMetadata;
import com.bluedot.commons.alerts.AlertReceiver;
import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.commons.messages.Message;
import com.bluedot.commons.messages.MessageReceiver;
import com.bluedot.commons.messages.MessageSender;
import com.bluedot.commons.notificationChannels.Email;
import com.bluedot.commons.notificationChannels.NotificationChannel;
import com.bluedot.commons.notificationChannels.SMS;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.play4jpa.jpa.models.Finder;
import com.play4jpa.jpa.models.Model;


import play.i18n.Messages;

@Entity
@Table(name = "Users")
public class User extends Model<User> implements Comparable<User>, AlertReceiver, MessageReceiver, MessageSender
{
	
	final static Logger logger = LoggerFactory.getLogger(User.class);

	/**
	 * 
	 */
	private static final long serialVersionUID = -3493658269122218046L;

	public enum Role {
		USER, ADMIN
	}
	
	public enum Gender {
		MALE, FEMALE, OTHER
	}

	@Id
	@GeneratedValue
	private int id;

	@Column(length = 256, unique = true, nullable = false)
	private String emailAddress;

	@Column(length = 64, nullable = false)
	private byte[] shaPassword;

	@Transient
	@JsonIgnore
	private String password;
	
	@Column(length = 256, nullable = false)
	private String firstName;

	@Column(length = 256, nullable = false)
	private String lastName;

	@Column(nullable = false)
	private Date creationDate;

	private Date validationDate;

	private boolean validated;

	@OneToMany(cascade = CascadeType.ALL, mappedBy = "user")
	private List<Session> sessions;

	@OneToMany(cascade = CascadeType.ALL)
	private List<Address> addresses;

	private String phone;

	@Enumerated(EnumType.STRING)
	private Role role = Role.USER;

	@ManyToMany(mappedBy = "users")
	private List<Account> accounts;

	@OneToOne(cascade = CascadeType.ALL)
	private Settings settings;

	@OneToMany(cascade = CascadeType.ALL)
	private List<Permission> permissions;

	@OneToOne(cascade = CascadeType.ALL)
	private Credential credentials;

	@OneToMany(cascade = CascadeType.ALL)
	private List<NotificationChannel> notificationChannels;

	public User() {
		this.creationDate = new Date();
	}

	public User(String emailAddress, String password, String firstName, String lastName) {
		setEmailAddress(emailAddress);
		setPassword(password);
		this.firstName = firstName;
		this.lastName = lastName;
		this.creationDate = new Date();
		notificationChannels = new LinkedList<NotificationChannel>();
		this.settings = new Settings();
		this.credentials = Credential.generateCredential();
		this.credentials.setUser(this);
	}


	/*
	 *  FINDERS
	 */
	private static Finder<Integer, User> find = new Finder<Integer, User>(Integer.class, User.class);

	public static User findByEmailAddressAndPassword(String emailAddress, String password)
	{
		return find.query().ieq("emailAddress", emailAddress).eq("shaPassword", getSha512(password)).findUnique();
	}

	public static User findByEmailAddress(String emailAddress) throws APIException
	{
		return findByEmailAddress(emailAddress, false);
	}
	
	public static User findByEmailAddress(String emailAddress, boolean throwExceptionWhenMissing) throws APIException
	{
		User user = find.query().ieq("emailAddress", emailAddress).findUnique();
		
		if (user==null && throwExceptionWhenMissing)
			throw APIException.raise(APIErrors.USER_NOT_FOUND.withParams("emailAddress", emailAddress));
		
		return user;
	}
	
	

	public static Collection<User> find(Role role)
	{
		return find.query().eq("role", role).findList();
	}

	public static List<User> findAll()
	{
		return find.all();
	}

	public static List<User> findWithPermissionId(String permissionId)
	{
		return find.query().join("permissions").ilike("permissions.permissionId", permissionId + '%').findList();
	}
	
	public static User findBySMSAccessToken(String token)
	{
		return find.query().join("settings").ilike("settings.jsonSettings", '%' + token + '%').findUnique();
	}

	/*
	 * GETTERS & SETTERS
	 */
	public String getPassword()
	{
		return password;
	}

	public void setPassword(String password)
	{
		this.password = password;
		shaPassword = getSha512(password);
	}

	private String generateRandomPassword()
	{
		String[] alphabet = { "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "a", "b", "c", "d", "e", "f", "g", "h",
				"i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9" };
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 10; i++)
		{
			sb.append(alphabet[new Random().nextInt(alphabet.length)]);
		}
		return sb.toString();
	}

	public String resetPassword()
	{
		String newPassword = generateRandomPassword();

		setPassword(newPassword);

//		getMasterAccount().setWaitingForReset(false);

		this.update();

		return newPassword;
	}
	
	public static byte[] getSha512(String value)
	{
		try
		{
			if (value==null)
				return null;
			return MessageDigest.getInstance("SHA-512").digest(value.getBytes("UTF-8"));
		} catch (NoSuchAlgorithmException e)
		{
			throw new RuntimeException(e);
		} catch (UnsupportedEncodingException e)
		{
			throw new RuntimeException(e);
		}
	}
	
//	public Account getMasterAccount()
//	{
//		return Account.findByOwnerId(getId());
//	}
	
	public String getEmailAddress()
	{
		return emailAddress;
	}

	public void setEmailAddress(String emailAddress)
	{
		this.emailAddress = emailAddress.toLowerCase();
	}
	
	public String getFirstName()
	{
		return firstName;
	}

	public void setFirstName(String firstName)
	{
		this.firstName = firstName;
	}

	public int getId()
	{
		return id;
	}

	public void setId(int id)
	{
		this.id = id;
	}

	public byte[] getShaPassword()
	{
		return shaPassword;
	}

	public void setShaPassword(byte[] shaPassword)
	{
		this.shaPassword = shaPassword;
	}

	public String getLastName()
	{
		return lastName;
	}

	public void setLastName(String lastName)
	{
		this.lastName = lastName;
	}

	public Date getCreationDate()
	{
		return creationDate;
	}

	public void setCreationDate(Date creationDate)
	{
		this.creationDate = creationDate;
	}

	public List<Session> getSessions()
	{
		return sessions;
	}

	public void setSessions(List<Session> sessions)
	{
		this.sessions = sessions;
	}

	public List<Address> getAddresses()
	{
		return addresses;
	}

	public void setAddresses(List<Address> addresses)
	{
		this.addresses = addresses;
	}

	public String getPhone()
	{
		return phone;
	}

	public void setPhone(String phone)
	{
		this.phone = phone;
	}

	public Role getRole()
	{
		return role;
	}

	public void setRole(Role role)
	{
		this.role = role;
	}

	public Date getValidationDate()
	{
		return validationDate;
	}

	public void setValidationDate(Date validationDate)
	{
		this.validationDate = validationDate;
	}

	public boolean isValidated()
	{
		return validated;
	}

	public void setValidated(boolean validated)
	{
		this.validated = validated;
	}

	public List<NotificationChannel> getNotificationChannels()
	{
		return notificationChannels;
	}

	public static User findById(int userId)
	{
		return find.byId(userId);
	}

	public static User findById(int id, boolean throwExceptionWhenMissing) throws APIException
	{
		User user = find.byId(id);

		if (user == null && throwExceptionWhenMissing)
			throw APIException.raise(APIErrors.USER_NOT_FOUND.withParams("id", id));

		return user;
	}

	public List<Permission> getPermissions()
	{
		return permissions;
	}

	public void setPermissions(List<Permission> permissions)
	{
		this.permissions = permissions;
	}

	public boolean hasPermission(String permissionId)
	{
		logger.info("Checking if user: " + getEmailAddress() + " has permission " + permissionId);
		for (Permission p : getPermissions())
		{
			if (p.getPermissionId().equalsIgnoreCase(permissionId))
				return true;
		}
		return false;
	}

	public List<Account> getAccounts()
	{
		return accounts;
	}

	public void setAccounts(List<Account> accounts)
	{
		this.accounts = accounts;
	}

	public Settings getSettings()
	{
		if (settings == null)
		{
			settings = new Settings();
			settings.save();
		}
		return settings;
	}

	public void setSettings(Settings settings)
	{
		this.settings = settings;
	}

	public void setNotificationChannels(List<NotificationChannel> notificationChannels)
	{
		this.notificationChannels = notificationChannels;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj == null)
			return false;
		if (!User.class.isAssignableFrom(obj.getClass()))
			return false;
		return ((User) obj).getEmailAddress().equalsIgnoreCase(this.getEmailAddress());
	}

	public Email getEmailNotificationChannel()
	{
		for (NotificationChannel nc : getNotificationChannels())
		{
			if (nc instanceof Email)
				return (Email) nc;
		}
		return null;
	}

	public List<SMS> getSMSNotificationChannel()
	{
		List<SMS> sms = new LinkedList<SMS>();

		for (NotificationChannel nc : getNotificationChannels())
		{
			if (nc instanceof SMS)
				sms.add((SMS) nc);
		}
		return sms;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see models.AlertReciver#sendAlert(models.Alert)
	 */
	@Override
	public void receiveAlert(Alert alert)
	{
		logger.info("User {} receiveing alert ", getId());
		
		if(!allowAlertDelivery(alert))
		{
			logger.info("User {} denied alert {}", getId(), alert);
			return;
		}
		
		for (NotificationChannel notificationChannel : notificationChannels)
		{
			notificationChannel.sendAlert(alert);
		}

	}

	public void receiveAlertByEmail(Alert alert)
	{
		NotificationChannel email = getEmailNotificationChannel();
		if (email != null && allowAlertDelivery(alert))
			email.sendAlert(alert);
	}
	
	private boolean allowAlertDelivery(Alert alert)
	{
		return true;
	}

	public Credential getCredentials()
	{
		return credentials;
	}

	public void setCredentials(Credential credentials)
	{
		credentials.setUser(this);
		this.credentials = credentials;
	}

	@Override
	public int compareTo(User o)
	{
		if (o.id > this.id)
			return 1;
		else if (o.id < this.id)
			return -1;
		else
			return 0;
	}

	public boolean isAdminOnAccount(Account account)
	{
		for (Permission p : getPermissions())
		{
			if (p.getPermissionId().equals("access-level-account_" + account.getId() + ":ADMIN"))
				return true;
		}

		return false;
	}

	@Override
	public int hashCode()
	{
		return getEmailAddress().hashCode();
	}

	@Override
	public String getURN()
	{
		return "user:" + id;
	}

	
	public void notify(Message message)
	{
		// TODO Auto-generated method stub
		
	}
	

	@Override
	public boolean appliesCustomRulesToAlert(Alert alert)
	{
		return getSettings().getBool(Constants.SMS_STATUS_MANAGEMENT_ENABLED);
	}
	

}
