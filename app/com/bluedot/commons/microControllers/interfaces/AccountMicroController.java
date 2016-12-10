package com.bluedot.commons.microControllers.interfaces;

import java.util.List;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.security.Account;
import com.bluedot.commons.security.AccountType;
import com.bluedot.commons.security.Address;
import com.bluedot.commons.security.PermissionNames;
import com.bluedot.commons.security.User;
import com.bluedot.commons.security.User.Role;



public interface AccountMicroController
{

	public class SignUpConfigurator{
		public boolean sendValidationToNotificationChannels; 
		public boolean validateNotificationChannels; 
		public boolean validateAccount;
		public boolean autogenEmail;
		public String hostForValidationsLinks;
		public Role role;
		
		public SignUpConfigurator(boolean sendValidationToNotificationChannels, boolean validateNotificationChannels, boolean validateAccount, boolean autogenEmail, String hostForValidationsLinks, Role role) {
			super();
			this.sendValidationToNotificationChannels = sendValidationToNotificationChannels;
			this.validateNotificationChannels = validateNotificationChannels;
			this.validateAccount = validateAccount;
			this.autogenEmail = autogenEmail;
			this.hostForValidationsLinks = hostForValidationsLinks;
			this.role = role;
		}
	}
	
	User signUp(String emailAddress, String password, String firstName, String lastName, String companyName, AccountType accountType, List<Address> addresses, String phone,
			SignUpConfigurator signUpConfigurator) throws APIException;

	boolean sendPasswordResetEmail(String to, Account account, String resetKey, String host)throws APIException;

	void addPermissionsToUser(User user, PermissionNames[] permissions, String pattern, String id, String value);

}
