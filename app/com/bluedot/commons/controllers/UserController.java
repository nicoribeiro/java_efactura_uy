package com.bluedot.commons.controllers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.commons.error.ErrorMessage;
import com.bluedot.commons.notificationChannels.NotificationChannel;
import com.bluedot.commons.notificationChannels.NotificationRecord;
import com.bluedot.commons.notificationChannels.SMS;
import com.bluedot.commons.security.Account;
import com.bluedot.commons.security.Address;
import com.bluedot.commons.security.Credential;
import com.bluedot.commons.security.Permission;
import com.bluedot.commons.security.PermissionNames;
import com.bluedot.commons.security.PermissionValidator;
import com.bluedot.commons.security.PromiseCallback;
import com.bluedot.commons.security.Secured;
import com.bluedot.commons.security.User;
import com.bluedot.commons.security.User.Role;
import com.bluedot.commons.security.ValidateJsonPost;
import com.fasterxml.jackson.databind.JsonNode;
import com.play4jpa.jpa.db.Tx;

import flexjson.JSONSerializer;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.Security;

@Tx
@ErrorMessage
@Security.Authenticated(Secured.class)
public class UserController extends AbstractController
{

	public  CompletionStage<Result> listUsers() throws APIException
	{
		return PermissionValidator.runIfHasRole(ctx(), new PromiseCallback() {

			@Override
			public CompletionStage<Result> execute() throws APIException
			{

				List<User> users = User.findAll();

				JSONSerializer serializer = new JSONSerializer().include("emailAddress", "firstName", "lastName", "gender", "addresses", "id").exclude("*").prettyPrint(true);

				return json(serializer.serialize(users));

			}
		}, Role.ADMIN);
	}

	@BodyParser.Of(BodyParser.Json.class)
	@ValidateJsonPost(fields = { "emailAddress", "password", "firstName", "lastName", "accountId" })
	public  CompletionStage<Result> createUser() throws APIException
	{
		JsonNode userJson = request().body().asJson();

		final int accountId = userJson.findPath("accountId").asInt();

		return accountAction(accountId, new PromiseCallback() {
			@Override
			public CompletionStage<Result> execute() throws APIException
			{

				JsonNode userJson = request().body().asJson();

				Account account = Account.findById(accountId, true);

				String emailAddress = userJson.findPath("emailAddress").textValue();
				String password = userJson.findPath("password").textValue();
				String firstName = userJson.findPath("firstName").textValue();
				String lastName = userJson.findPath("lastName").asText();

				User user = new User(emailAddress, password, firstName, lastName);

				account.getUsers().add(user);
				account.update();

				return CompletableFuture.completedFuture(created());
			}
		});
	}
	
	
//	@BodyParser.Of(BodyParser.Json.class)
//	@ValidateJsonPost(fields = { "phone" })
//	public  CompletionStage<Result> overrideContactInfo(final String email) throws APIException
//	{
//		Account sessionUserAccount = Global.getSessionUser(ctx()).getMasterAccount();
//		
//		final User user = User.findByEmailAddress(email);
//		
//		if(Global.getSessionUser(ctx()).getRole() != Role.ADMIN && !sessionUserAccount.isUserInAccount(user))
//			throw APIException.raise(APIErrors.USER_NOT_PART_OF_ACCOUNT);
//		
//		return accountAction(sessionUserAccount.getId(), new PromiseCallback() {
//			
//			@Override
//			public CompletionStage<Result> execute() throws APIException
//			{
//				JsonNode userJson = request().body().asJson();
//
//				String phone = userJson.findPath("phone").asText();
//				
//				if (user==null)
//					throw APIException.raise(APIErrors.USER_NOT_FOUND);
//				
//				user.setPhone(phone);
//				
//				List<SMS> smsList = user.getSMSNotificationChannel();
//				
//				for (Iterator<SMS> iterator = smsList.iterator(); iterator.hasNext();)
//				{
//					SMS sms = iterator.next();
//					
//					if(sms.getPhone().equals(phone))
//						return CompletableFuture.completedFuture(ok());
//					
//					for(NotificationRecord nr : sms.getNotificationRecords()){
//						nr.setNotificationChannel(null);
//						nr.update();
//					}
//					
//					user.getNotificationChannels().remove(sms);
//					sms.delete();
//				}
//				
//				SMS sms = new SMS("User phone", phone);
//				
//				sms.validate(sms.getValidationKey());
//				
//				user.getNotificationChannels().add(sms);
//				
//				sms.save();
//				
//				user.update();
//				
//				return CompletableFuture.completedFuture(ok());
//			}
//		}, PermissionNames.EDIT_ACCOUNT_SETTINGS);
//	}
	
//	public  CompletionStage<Result> overrideEmail(final int userId) throws APIException
//	{
//		Account sessionUserAccount = Global.getSessionUser(ctx()).getMasterAccount();
//		
//		final User user = User.findById(userId);
//		
//		if(Global.getSessionUser(ctx()).getRole() != Role.ADMIN && !sessionUserAccount.isUserInAccount(user))
//			throw APIException.raise(APIErrors.USER_NOT_PART_OF_ACCOUNT);
//		
//		return accountAction(sessionUserAccount.getId(), new PromiseCallback() {
//			
//			@Override
//			public CompletionStage<Result> execute() throws APIException
//			{
//				JsonNode userJson = request().body().asJson();
//
//				String email = userJson.findPath("email").asText();
//				
//				User user = User.findById(userId);
//				
//				if (user==null)
//					throw APIException.raise(APIErrors.USER_NOT_FOUND);
//				
//				if(user.getMasterAccount().getAccountType() != AccountType.ACCOUNT_USER)
//					throw APIException.raise(APIErrors.AUTHORIZATION_FAILED).setDetailMessage("You don't have rights to change the email address of this account");
//				
//				user.setEmailAddress(email);
//				
//				Email emailChannel = user.getEmailNotificationChannel();
//				
//				if(emailChannel.getEmail().equals(email))
//					return CompletableFuture.completedFuture(ok());
//				
//				emailChannel.setEmail(email);
//				
//				emailChannel.update();
//				
//				user.update();
//				
//				return CompletableFuture.completedFuture(ok());
//			}
//		}, PermissionNames.EDIT_ACCOUNT_SETTINGS);
//	}
	
	public  CompletionStage<Result> changePassword(final int accountId, final int userId) throws APIException
	{
		Account sessionUserAccount = Account.findById(accountId, true);
		
		final User user = User.findById(userId);
		
		if(PermissionValidator.getSessionUser(ctx()).getRole() != Role.ADMIN && !sessionUserAccount.isUserInAccount(user))
			throw APIException.raise(APIErrors.USER_NOT_PART_OF_ACCOUNT);
		
		return accountAction(sessionUserAccount.getId(), new PromiseCallback() {
			
			@Override
			public CompletionStage<Result> execute() throws APIException
			{
				JsonNode userJson = request().body().asJson();

				String password = userJson.findPath("password").asText();
				
				User user = User.findById(userId, true);
				
				
//				if(user.getMasterAccount().getAccountType() != AccountType.ACCOUNT_USER)
//					throw APIException.raise(APIErrors.AUTHORIZATION_FAILED).setDetailMessage("You don't have rights to change the email address of this account");
				
				user.setPassword(password);
				user.update();
				
				return CompletableFuture.completedFuture(ok());
			}
		}, PermissionNames.EDIT_ACCOUNT_SETTINGS);
	}
	
	public  CompletionStage<Result> getAccessLevels(int userId) throws APIException
	{
		final User u = User.findById(userId, true);

		return userAction(u.getId(), new PromiseCallback() {
			@Override
			public CompletionStage<Result> execute() throws APIException
			{

				HashMap<String, String> gateways = new HashMap<>();

				for (Permission p : u.getPermissions())
				{
					if (p.getPermissionId().startsWith("access-level"))
					{
						String acl = p.getPermissionId().split("\\-")[2];
						gateways.put(acl.split("\\:")[0], acl.split("\\:")[1]);
					}
				}

				JSONSerializer serializer = new JSONSerializer();

				return json(serializer.serialize(gateways));
			}

		});
	}

	public  CompletionStage<Result> getUser(String emailAddress) throws APIException
	{
		final User u = User.findByEmailAddress(emailAddress, true);

		return userAction(u.getId(), new PromiseCallback() {
			@Override
			public CompletionStage<Result> execute() throws APIException
			{

				JSONSerializer serializer = new JSONSerializer()
						.include("masterAccount", "masterAccount.users", "masterAccount.users.permissions", "masterAccount.tags", "masterAccount.invites", "masterAccount.gateways.name",
								"masterAccount.gateways.gatewayId", "permissions", "addresses", "notificationChannels", "credentials", "masterAccount.oem.id", "masterAccount.oem.code", "accounts.id",
								"accounts.owner.firstName", "accounts.owner.lastName", "accounts.companyName", "accounts.owner.emailAddress", "accounts.owner.phone")
						.exclude("*.class", "notificationChannels.validationKey", "masterAccount.gateways.*", "masterAccount.users.masterAccount", "accounts.*", "settings","masterAccount.settings").prettyPrint(true);

				return json(serializer.serialize(u));

			}

		});
	}

	@BodyParser.Of(BodyParser.Json.class)
	public  CompletionStage<Result> updateUser(int userId) throws APIException
	{
		final User u = User.findById(userId, true);

		return userAction(u.getId(), new PromiseCallback() {
			@Override
			public CompletionStage<Result> execute() throws APIException
			{

				JsonNode userJson = request().body().asJson();
				String emailAddress = userJson.has("emailAddress") ? userJson.findPath("emailAddress").textValue() : null;
				String password = userJson.has("password") ? userJson.findPath("password").textValue() : null;
				String firstName = userJson.has("firstName") ? userJson.findPath("firstName").textValue() : null;
				String lastName = userJson.has("lastName") ? userJson.findPath("lastName").asText() : null;
				String phone = userJson.has("phone") ? userJson.findPath("phone").asText() : null;
				JsonNode address = userJson.has("address") ? userJson.findPath("address") : null;

				if (emailAddress != null)
					u.setEmailAddress(emailAddress);
				if (password != null)
					u.setPassword(password);
				if (firstName != null)
					u.setFirstName(firstName);
				if (lastName != null)
					u.setLastName(lastName);
				if (phone != null)
				{
					u.setPhone(phone);
					NotificationChannel nc = new SMS("User phone", phone);

					List<NotificationChannel> toRemove = new ArrayList<>();

					for (NotificationChannel n : u.getNotificationChannels())
					{
						if (n instanceof SMS)
							toRemove.add(n);
					}

					for (NotificationChannel n : toRemove)
					{
						for (NotificationRecord nr : n.getNotificationRecords())
						{
							nr.setNotificationChannel(null);
							nr.update();
						}

						u.getNotificationChannels().remove(n);
						n.delete();
					}

					u.getNotificationChannels().add(nc);
				}

				if (address != null)
				{
					Address a;
					boolean isNew = false;
					if (u.getAddresses().size() > 0)
					{
						a = u.getAddresses().get(0);
					} else
					{
						a = new Address();
						isNew = true;
					}
					a.setStreetLine1(address.findPath("streetLine1").asText());
					a.setStreetLine2(address.findPath("streetLine2").asText());
					a.setState(address.findPath("state").asText());
					a.setCity(address.findPath("city").asText());
					a.setCountry(address.findPath("country").asText());
					a.setZipcode(address.findPath("zipcode").asText());

					if (isNew)
						u.getAddresses().add(a);
				}

				u.update();

				return CompletableFuture.completedFuture(ok());
			}

		});
	}

	public  CompletionStage<Result> deleteUser(int userId) throws APIException
	{

		final User u = User.findById(userId, true);
		
		return PermissionValidator.runIfHasRole(ctx(), new PromiseCallback() {

			@Override
			public CompletionStage<Result> execute() throws APIException
			{
				u.delete();

				return CompletableFuture.completedFuture(ok());
			}
		}, Role.ADMIN);
	}

	public  CompletionStage<Result> regenerateCredentials(int userId) throws APIException
	{
		final User u = User.findById(userId, true);

		return userAction(u.getId(), new PromiseCallback() {
			@Override
			public CompletionStage<Result> execute() throws APIException
			{

				u.getCredentials().revoke();
				u.setCredentials(Credential.generateCredential());
				u.update();

				JSONSerializer serializer = new JSONSerializer().exclude("*.class").prettyPrint(true);

				return json(serializer.serialize(u.getCredentials()));
			}

		});
	}

//	public  CompletionStage<Result> getMotd() throws APIException
//	{
//		JSONSerializer serializer = new JSONSerializer().exclude("class");
//		List<Motd> motds = Motd.getTodaysMessages();
//		return json(serializer.serialize(motds));
//	}

}
