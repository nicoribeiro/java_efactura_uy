package com.bluedot.commons.microControllers.interfaces;

import java.util.List;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.security.AccountType;
import com.bluedot.commons.security.Address;
import com.bluedot.commons.security.User;
import com.bluedot.commons.security.User.Role;



public interface AccountMicroController
{

	public class SignUpConfigurator{
		public boolean validateAccount;
		public boolean autogenEmail;
		public Role role;
		
		public SignUpConfigurator(boolean validateAccount, boolean autogenEmail, Role role) {
			super();
			this.validateAccount = validateAccount;
			this.autogenEmail = autogenEmail;
			this.role = role;
		}
	}
	
	User signUp(String emailAddress, String password, String firstName, String lastName, String companyName, AccountType accountType, List<Address> addresses, String phone,
			SignUpConfigurator signUpConfigurator) throws APIException;


}
