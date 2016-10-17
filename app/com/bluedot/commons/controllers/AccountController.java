package com.bluedot.commons.controllers;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.ErrorMessage;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.commons.microControllers.factory.MicroControllerFactoryBuilder;
import com.bluedot.commons.microControllers.factory.MicroControllerFactoryDefault;
import com.bluedot.commons.microControllers.factory.MicroControllersFactory;
import com.bluedot.commons.microControllers.interfaces.AccountMicroController.SignUpConfigurator;
import com.bluedot.commons.notificationChannels.Email;
import com.bluedot.commons.notificationChannels.NotificationChannel;
import com.bluedot.commons.notificationChannels.SMS;
import com.bluedot.commons.security.Account;
import com.bluedot.commons.security.AccountAccessLevel;
import com.bluedot.commons.security.AccountType;
import com.bluedot.commons.security.Address;
import com.bluedot.commons.security.Permission;
import com.bluedot.commons.security.PermissionNames;
import com.bluedot.commons.security.PermissionValidator;
import com.bluedot.commons.security.PromiseCallback;
import com.bluedot.commons.security.Secured;
import com.bluedot.commons.security.User;
import com.bluedot.commons.security.ValidateJsonPost;
import com.bluedot.commons.serializers.JSONSerializerProvider;
import com.bluedot.commons.utils.ThreadMan;
import com.fasterxml.jackson.databind.JsonNode;
import com.play4jpa.jpa.db.Tx;

import flexjson.JSONSerializer;
import play.i18n.Messages;
import play.libs.F.Promise;
import play.mvc.Result;
import play.mvc.Security;

@ErrorMessage
@Tx
@Security.Authenticated(Secured.class)
public class AccountController extends AbstractController
{

//	private static String KEY = Play.application().configuration().getString("product.digest.key", "");
//	private static String IV = Play.application().configuration().getString("product.digest.iv", "");
//
//	private static String SALT = Play.application().configuration().getString("product.digest.salt", "");

	
	public  Promise<Result> getAccount(final int accountId) throws APIException
	{
		return accountAction(accountId, new PromiseCallback() {
			@Override
			public Promise<Result> execute() throws APIException
			{
				Account a = Account.findById(accountId, true);

				//TODO hacer un serializer como la gente
				JSONSerializer serializer = new JSONSerializer()
						.include("tags", "invites", "gateways.name", "gateways.gatewayId", "users.firstName", "users.id", "users.lastName", "users.emailAddress", "users.phone", "users.permissions.*", "oem.*", "owner.accounts.id", "owner.accounts.owner.firstName", "owner.accounts.owner.lastName",
								"owner.accounts.companyName")
						.exclude("*.class", "tags.possibleAlertMetadatas", "gateways.*", "users.*", "owner.accounts.kabaPassword", "kabaPassword").prettyPrint(true);

				return json(serializer.serialize(a));
			}
		});
	}

//	@ValidateJsonPost(fields = { "productId", "productType" })
//	public  Promise<Result> generateProductKey() throws APIException
//	{
//		try
//		{
//
//			JsonNode request = request().body().asJson();
//
//			JSONObject json = new JSONObject();
//			json.put("productId", request.get("productId").asInt());
//			json.put("productType", request.get("productType").asText());
//
//			String toEncrypt = json.toString() + SALT;
//
//			return Promise.<Result> pure(ok(Crypto.encrypt(toEncrypt, true, KEY, IV)));
//		} catch (JSONException e)
//		{
//			throw APIException.raise(APIErrors.WRONG_FORMAT);
//		}
//	}

	

	

	

	public  Promise<Result> removeUser(int accountId, int userId) throws APIException
	{
		Account a = Account.findById(accountId, true);

		User u = User.findById(userId, true);
		
		removeAllAccountPermissions(u, a);
		
		a.getUsers().remove(u);
		a.update();
		
		u.update();

		return Promise.<Result> pure(ok());
	}

	
	
	public  Promise<Result> addUsersToAccount(final int accountId) throws APIException
	{
		return accountAction(accountId, new PromiseCallback() {
			
			@Override
			public Promise<Result> execute() throws APIException
			{
				JsonNode emails = request().body().asJson();
				
				Account a = Account.findById(accountId, true);
				
				for (JsonNode email : emails)
				{
					User u = User.findByEmailAddress(email.asText());
	
					if (u == null)
						continue;
	
					if (a.getUsers().contains(u))
						continue;
					
					a.getUsers().add(u);
	
					for (PermissionNames permissionNames : AccountAccessLevel.VIEWER.permissions)
					{
						Permission permission = new Permission();
						permission.setPermissionId(MessageFormat.format(permissionNames.pattern, String.valueOf(a.getId())));
						u.getPermissions().add(permission);
					}
					Permission permission = new Permission();
					permission.setPermissionId(MessageFormat.format("access-level-account_{0}:{1}", String.valueOf(a.getId()), AccountAccessLevel.VIEWER.toString()));
					u.getPermissions().add(permission);
	
					u.update();
				}

				a.update();
				
				return Promise.<Result>pure(ok());
			}
		});
	}
	
	public  Promise<Result> removeUsersFromAccount(final int accountId) throws APIException
	{
		return accountAction(accountId, new PromiseCallback() {
			
			@Override
			public Promise<Result> execute() throws APIException
			{
				JsonNode emails = request().body().asJson();
				
				Account a = Account.findById(accountId, true);
				
				for (JsonNode email : emails)
				{
					User u = User.findByEmailAddress(email.asText());
	
					if (u == null)
						continue;
	
					if (!a.getUsers().contains(u))
						continue;
					
					a.getUsers().remove(u);
					
					removeAllAccountPermissions(u, a);
					
					u.update();
				}

				a.update();
				
				return Promise.<Result>pure(ok());
			}
		});
	}
	
	
	
	private  void removeAllAccountPermissions(User u, Account account)
	{
		ArrayList<Permission> toRemove = new ArrayList<>();
		for(Permission p : u.getPermissions())
		{
			if(p.getPermissionId().contains("account_"+account.getId()))
			{
				toRemove.add(p);
			}
		}
		
		for(Permission p : toRemove)
		{
			u.getPermissions().remove(p);
			p.delete();
		}
		
	}

	
	

	

	private enum ACLType {
		account
	}

	private enum ACLAction {
		set, unset
	}

	public  Promise<Result> setAcl(int accountId, int userId) throws APIException
	{
		final User user = User.findById(userId, true);

		final Account account = Account.findById(accountId, true);

		if (!account.isUserInAccount(user))
			throw APIException.raise(APIErrors.USER_NOT_PART_OF_ACCOUNT);

		JsonNode aclJson = request().body().asJson();
		final String aclName = aclJson.has("acl") ? aclJson.get("acl").asText() : null;
		final String aclAction = aclJson.has("action") ? aclJson.get("action").asText() : "set";
		final String aclTypeString = aclJson.has("type") ? aclJson.get("type").asText() : null;
		final Integer aclId = aclJson.has("id") ? aclJson.get("id").asInt() : null;

		ACLType type = com.bluedot.commons.utils.EnumUtils.valueOf(aclTypeString, ACLType.class);

		if (type == null)
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE.withParams("aclType"));

		ACLAction action = com.bluedot.commons.utils.EnumUtils.valueOf(aclAction, ACLAction.class);

		if (action == null)
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE.withParams("aclAction"));

		switch (type)
		{
		
		case account:

			accountAction(accountId, new PromiseCallback() {

				@Override
				public Promise<Result> execute() throws APIException
				{

					setAccountACL(account, aclId, aclName, aclAction, user);

					return Promise.<Result> pure(ok());
				}
			});

			break;

		}

		user.update();

		return Promise.<Result> pure(ok());
	}

	

	

	private  void setAccountACL(Account account, int aclId, final String aclName, final String aclAction, final User user) throws APIException
	{

		/*
		 * ACL CONTROL
		 */
		if (aclName == null)
			throw APIException.raise(APIErrors.MISSING_PARAMETER.withParams("acl"));
		final AccountAccessLevel accountAcl = com.bluedot.commons.utils.EnumUtils.valueOf(aclName, AccountAccessLevel.class);
		if (accountAcl == null)
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE.withParams("acl"));

		ArrayList<Permission> toRemove = new ArrayList<>();
		for (Permission permission : user.getPermissions())
		{
			if (permission.getPermissionId().contains("account_" + String.valueOf(account.getId())))
			{
				permission.delete();
				toRemove.add(permission);
			}
		}

		user.getPermissions().removeAll(toRemove);

		if ("set".equals(aclAction))
		{

			MicroControllersFactory microControllersFactory = new MicroControllerFactoryBuilder().getMicroControllersFactory();

			microControllersFactory.getAccountController().addPermissionsToUser(user, accountAcl.permissions, "access-level-account_{0}:{1}", String.valueOf(account.getId()), aclName);

		}

	}

	
	public  Promise<Result> getSettings(final int accountId) throws APIException
	{
		return accountAction(accountId, new PromiseCallback() {
			@Override
			public Promise<Result> execute() throws APIException
			{
				Account account = Account.findById(accountId, true);

				JSONObject result;
				try
				{

					result = JSONSerializerProvider.getSettingsSerializer().objectToJson(account.getSettings());
				} catch (JSONException e)
				{
					throw APIException.raise(APIErrors.BAD_JSON).setDetailMessage("Converting Settings into JSON");
				}
					
				return json(result.toString());
				
			}
		});
	}
	
	public  Promise<Result> getUserSettings(final int accountId, final int userId) throws APIException
	{
		return accountAction(accountId, new PromiseCallback() {
			@Override
			public Promise<Result> execute() throws APIException
			{
				Account account = Account.findById(accountId, true);
				
				User user = User.findById(userId, true);
				
				if (!account.isUserInAccount(user))
					throw APIException.raise(APIErrors.USER_NOT_PART_OF_ACCOUNT);

				JSONObject result;
				try
				{

					result = JSONSerializerProvider.getSettingsSerializer().objectToJson(user.getSettings());
				} catch (JSONException e)
				{
					throw APIException.raise(APIErrors.BAD_JSON).setDetailMessage("Converting Settings into JSON");
				}
				
				return json(result.toString());
				
			}
		});
	}

	public  Promise<Result> setSettings(final int accountId) throws APIException
	{
		return accountAction(accountId, new PromiseCallback() {
			@Override
			public Promise<Result> execute() throws APIException
			{
				Account account = Account.findById(accountId, true);
				JsonNode settings = request().body().asJson();
				try
				{
					account.getSettings().updateSettings(new JSONObject(settings.toString()));
					account.update();
					return Promise.<Result> pure(ok());
				} catch (Throwable e)
				{
					throw APIException.raise(APIErrors.BAD_JSON).setDetailMessage("Wrong json settings");
				}

			}
		});
	}

	

	public  Promise<Result> listAccounts() throws APIException
	{
		return PermissionValidator.runWithValidation(ctx(), new PromiseCallback() {
			@Override
			public Promise<Result> execute() throws APIException
			{
				String keyword = request().getQueryString("keyword");
				
				if(keyword == null || "".equals(keyword))
					return json("[]");
				
				Collection<Account> accounts = new ArrayList<>();
				accounts = Account.find(keyword);
				
				//TODO cambiar a un serializer como la gente
				JSONSerializer serializer = new JSONSerializer()
						.include("id","accountType", "companyName", "oem.name", "oem.id", "owner.id", "owner.firstName", "owner.lastName", "owner.emailAddress", "owner.phone", "oem.requiresId","kabaUsername", "kabaPassword", "validated", "idInOem")
						.exclude("*").prettyPrint(true);
				return json(serializer.serialize(accounts));
			}
		}, PermissionNames.MASTER_ADMIN, null, null);
	}

	

	@ValidateJsonPost(fields = { "firstName", "lastName" })
	public  Promise<Result> createAccount() throws APIException {
		return PermissionValidator.runWithValidation(ctx(), new PromiseCallback() {

			@Override
			public Promise<Result> execute() throws APIException {
				JsonNode userJson = request().body().asJson();

				final String emailAddress = userJson.findPath("email").textValue();
				final String phone = userJson.findPath("phone").textValue();
				final String password = userJson.findPath("password").textValue();
				final String firstName = userJson.findPath("firstName").textValue();
				final String lastName = userJson.findPath("lastName").asText();
				final String accountTypeString = userJson.has("accountType") ? userJson.get("accountType").asText()
						: null;
				final AccountType accountType = accountTypeString != null ? AccountType.valueOf(accountTypeString)
						: null;
				final String companyName = userJson.has("company") ? userJson.findPath("company").asText() : null;

				final List<Address> addresses = new ArrayList<Address>();

				if (userJson.has("address")) {
					JsonNode addressNode = userJson.get("address");
					Address address = Address.fromJson(addressNode);
					addresses.add(address);
				}

				final MicroControllersFactory microControllersFactory = new MicroControllerFactoryDefault();

				//User sessionUser = Global.getSessionUser(ctx());

				SignUpConfigurator signUpConfigurator = new SignUpConfigurator(false, true, true, false,
						request().host(), null);

				microControllersFactory.getAccountController().signUp(emailAddress, password, firstName, lastName,
						companyName, accountType, addresses, phone, signUpConfigurator);

				return Promise.<Result>pure(created());
			}
		}, PermissionNames.MASTER_ADMIN, null, null);
	}
	
	@ValidateJsonPost(fields = { "firstName", "lastName", "email", "password" })
	public  Promise<Result> createSubAccount(final int accountId) throws APIException
	{
		return accountAction(accountId, new PromiseCallback() {
			@Override
			public Promise<Result> execute() throws APIException
			{
				Account parentAccount = Account.findById(accountId, true);
				
				
				JsonNode userJson = request().body().asJson();

				String emailAddress = userJson.findPath("email").textValue();
				String phone = userJson.findPath("phone").textValue();
				String password = userJson.findPath("password").textValue();
				String firstName = userJson.findPath("firstName").textValue();
				String lastName = userJson.findPath("lastName").asText();
				
				User user = null;
				
				if (User.findByEmailAddress(emailAddress) != null)
				{
					user = User.findByEmailAddress(emailAddress);
					
					if (parentAccount.getUsers().contains(user))
						throw APIException.raise(APIErrors.USER_ALREADY_EXISTS.withParams("emailAddress", emailAddress));
				}
				else
				{
				    user = new User(emailAddress, password, firstName, lastName);
				
					if (parentAccount.getUsers().contains(user))
						throw APIException.raise(APIErrors.USER_ALREADY_EXISTS).setDetailMessage(Messages.get("email_already_taken"));
	
					NotificationChannel nc = new Email("User Email", emailAddress);
					user.getNotificationChannels().add(nc);
					nc.validate(nc.getValidationKey());
					
					if(phone != null && !"".equals(phone))
					{
						NotificationChannel phonenc = new SMS("User phone", phone);
						user.setPhone(phone);
						user.getNotificationChannels().add(phonenc);
						phonenc.validate(phonenc.getValidationKey());
					}
	
					Account account = new Account();
					account.setCreationTimestamp(new Date());
					account.setUuid(UUID.randomUUID().toString());
	
					account.setCompanyName(parentAccount.getCompanyName());
	
					account.setValidated(true);
					account.setValidationDate(new Date());
					account.setAccountType(AccountType.EMISOR_ELECTRONICO);
					
					user.setPermissions(new ArrayList<Permission>());
					
					user.save();
					account.save();
					
					ThreadMan.forceTransactionFlush();
				}

				parentAccount.getUsers().add(user);
				
				for (PermissionNames permissionNames : AccountAccessLevel.VIEWER.permissions)
				{
					Permission permission = new Permission();
					permission.setPermissionId(MessageFormat.format(permissionNames.pattern, String.valueOf(parentAccount.getId())));
					
					user.getPermissions().add(permission);
				}
				Permission permission = new Permission();
				permission.setPermissionId(MessageFormat.format("access-level-account_{0}:{1}", String.valueOf(parentAccount.getId()), AccountAccessLevel.VIEWER.toString()));
				user.getPermissions().add(permission);

				user.update();
				parentAccount.update();

				return Promise.<Result> pure(created());
			}
		});
	}

	public  Promise<Result> editAccount(final int accountId) throws APIException
	{
		return PermissionValidator.runWithValidation(ctx(), new PromiseCallback() {
			@Override
			public Promise<Result> execute() throws APIException
			{
				JsonNode userJson = request().body().asJson();
				Account a = Account.findById(accountId, true);

//				String firstName = userJson.has("firstName") ? userJson.findPath("firstName").textValue() : null;
//				String lastName = userJson.has("lastName") ? userJson.findPath("lastName").asText() : null;
//				String password = userJson.hasNonNull("password") ? userJson.findPath("password").asText() : null;
//				
//				String accountType = userJson.has("accountType") ? userJson.get("accountType").asText() : null;
//				
//				String companyName = userJson.has("company") ? userJson.findPath("company").asText() : null;
//				int oemId = userJson.has("oem") ? userJson.findPath("oem").asInt() : -1;
//
//				String idInOem = userJson.has("idInOem") ? userJson.findPath("idInOem").asText() : null;

//				if (firstName != null)
//					a.getOwner().setFirstName(firstName);
//
//				if (lastName != null)
//					a.getOwner().setLastName(lastName);
//				
//				if (password != null)
//					a.getOwner().setPassword(password);
//				
//				if(accountType != null)
//					a.setAccountType(AccountType.valueOf(accountType));
				
				a.update();

				return Promise.<Result> pure(ok());
			}
		}, PermissionNames.MASTER_ADMIN, null, null);
	}

	public  Promise<Result> deleteAccount(final int accountId) throws APIException
	{
		return PermissionValidator.runWithValidation(ctx(), new PromiseCallback() {
			@Override
			public Promise<Result> execute() throws APIException
			{
				Account a = Account.findById(accountId, true);

				a.delete();

				return Promise.<Result> pure(ok());
			}
		}, PermissionNames.MASTER_ADMIN, null, null);
	}
	
	
	
	
}
