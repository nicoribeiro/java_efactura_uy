package com.bluedot.commons.microControllers.implementation;


import java.text.MessageFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.commons.microControllers.interfaces.AccountMicroController;
import com.bluedot.commons.notificationChannels.Email;
import com.bluedot.commons.notificationChannels.MessagingHelper;
import com.bluedot.commons.notificationChannels.NotificationChannel;
import com.bluedot.commons.notificationChannels.SMS;
import com.bluedot.commons.security.Account;
import com.bluedot.commons.security.AccountType;
import com.bluedot.commons.security.Address;
import com.bluedot.commons.security.Permission;
import com.bluedot.commons.security.PermissionNames;
import com.bluedot.commons.security.User;
import com.google.inject.Inject;

import play.db.jpa.JPAApi;
import play.i18n.Messages;




public class AccountMicroControllerDefault implements AccountMicroController
{

	private JPAApi jpaApi;
	private MessagingHelper messagingHelper;
	
	@Inject
	public AccountMicroControllerDefault(JPAApi jpaApi, MessagingHelper messagingHelper) {
		super();
		this.jpaApi = jpaApi;
		this.messagingHelper = messagingHelper;
	}
	
	
	@Override
	public User signUp(String emailAddress, String password, String firstName, String lastName, String companyName, AccountType accountType, List<Address> addresses, String phone, SignUpConfigurator signUpConfigurator) throws APIException
	{
		if (signUpConfigurator.autogenEmail && (emailAddress==null || emailAddress.equals("")))
			emailAddress = "auto-generated-"+UUID.randomUUID().toString().substring(0, 8)+"@somemail.com";
		
		if (User.findByEmailAddress(jpaApi, emailAddress) != null)
			throw APIException.raise(APIErrors.USER_ALREADY_EXISTS.withParams("emailAddress", emailAddress));
		
		/*
		 * USER
		 */
		if (password==null)
			password = UUID.randomUUID().toString();
		User user = new User(emailAddress, password, firstName, lastName);
		
		if (signUpConfigurator.role!=null)
			user.setRole(signUpConfigurator.role);
		
		user.setAddresses(addresses);
		
		/*
		 * PHONE
		 */
		if(phone != null && !"".equals(phone))
		{
			NotificationChannel phoneNotificationChannel = new SMS("User phone", phone);
			user.setPhone(phone);
			user.getNotificationChannels().add(phoneNotificationChannel);
			
			if (signUpConfigurator.validateNotificationChannels)
				phoneNotificationChannel.validate(phoneNotificationChannel.getValidationKey());
			
			if (signUpConfigurator.sendValidationToNotificationChannels)
				phoneNotificationChannel.sendValidationKey(messagingHelper, messagingHelper.getValidationHost(signUpConfigurator.hostForValidationsLinks));
		}
		
		/*
		 * EMAIL
		 */
		NotificationChannel emailNotificationChannel = new Email("User Email", emailAddress);
		user.getNotificationChannels().add(emailNotificationChannel);
		if (signUpConfigurator.validateNotificationChannels)
			emailNotificationChannel.validate(emailNotificationChannel.getValidationKey());
		
		if (signUpConfigurator.sendValidationToNotificationChannels)
			emailNotificationChannel.sendValidationKey(messagingHelper, messagingHelper.getValidationHost(signUpConfigurator.hostForValidationsLinks));
		
		user.save(jpaApi);

		
		/*
		 * ACCOUNT
		 */
		Account account = new Account();
		
		if(accountType != null)
			account.setAccountType(accountType);
		
		account.setCreationTimestamp(new Date());
		account.setUuid(UUID.randomUUID().toString());

		if (companyName != null)
			account.setCompanyName(companyName);
		
		if (signUpConfigurator.validateAccount){
			account.setValidated(true);
			account.setValidationDate(new Date());
		}else
			sendAccountValidationEmail(emailAddress, account, signUpConfigurator.hostForValidationsLinks);
		
		account.save(jpaApi);
		
		return user;
	}
	
	
	@Override
	public void addPermissionsToUser(User user, PermissionNames[] permissions, String pattern, String id, String value){
		for (PermissionNames permissionNames : permissions)
		{
			Permission permission = new Permission();
			permission.setPermissionId(MessageFormat.format(permissionNames.pattern, id));
			user.getPermissions().add(permission);
		}
		Permission permission = new Permission();
		permission.setPermissionId(MessageFormat.format(pattern, String.valueOf(id), value));
		user.getPermissions().add(permission);
	}
	
	
	private boolean sendAccountValidationEmail(String to, Account account, String host)
	{
		String activationLink = messagingHelper.getValidationHost(host) + "/api/v1/accounts/" + account.getUuid() + "/validate";

		StringBuilder htmlEmailBody = new StringBuilder();

		htmlEmailBody.append("<h3>Thank you for join</h3>");
		htmlEmailBody.append("<p>Validate your account by clicking <a href=\"" + activationLink + "\">here</a></p>");

		StringBuilder textEmailBody = new StringBuilder();

		textEmailBody.append("Thank you for join");
		textEmailBody.append("\nValidate your account by accessing this URL: " + activationLink);

		return messagingHelper.withPlayConfig().sendEmail(to, textEmailBody.toString(), htmlEmailBody.toString(), "Welcome!", true);
	}
	
	@Override
	public boolean sendPasswordResetEmail(String to, Account account, String resetKey, String host)
	{
		String resetLink = messagingHelper.getValidationHost(host) + "/api/v1/accounts/" + account.getUuid() + "/reset?key=" + resetKey;

		StringBuilder htmlEmailBody = new StringBuilder();

		htmlEmailBody.append("<h3>Password recovery</h3>");
		htmlEmailBody.append("<p>Reset your password by clicking <a href=\"" + resetLink + "\">here</a></p>");

		StringBuilder textEmailBody = new StringBuilder();

		textEmailBody.append("Password recovery");
		textEmailBody.append("\nReset your password by accessing this URL: " + resetLink);

		return messagingHelper.withPlayConfig().sendEmail(to, textEmailBody.toString(), htmlEmailBody.toString(), "Reset your password", true);
	}

}
