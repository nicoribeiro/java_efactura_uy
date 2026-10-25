package com.bluedot.commons.microControllers.implementation;


import java.text.MessageFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.commons.microControllers.interfaces.AccountMicroController;
import com.bluedot.commons.security.Account;
import com.bluedot.commons.security.AccountType;
import com.bluedot.commons.security.Address;
import com.bluedot.commons.security.Permission;
import com.bluedot.commons.security.PermissionNames;
import com.bluedot.commons.security.User;





public class AccountMicroControllerDefault implements AccountMicroController
{

	@Override
	public User signUp(String emailAddress, String password, String firstName, String lastName, String companyName, AccountType accountType, List<Address> addresses, String phone, SignUpConfigurator signUpConfigurator) throws APIException
	{
		if (signUpConfigurator.autogenEmail && (emailAddress==null || emailAddress.equals("")))
			emailAddress = "auto-generated-"+UUID.randomUUID().toString().substring(0, 8)+"@somemail.com";
		
		if (User.findByEmailAddress(emailAddress) != null)
			throw APIException.raise(APIErrors.USER_ALREADY_EXISTS).withParams("emailAddress", emailAddress);
		
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
			user.setPhone(phone);
		
		user.save();

		
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
		}
		
		account.save();
		
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
}
