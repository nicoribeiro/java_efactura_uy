package com.bluedot.commons.controllers;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.commons.error.ErrorMessage;
import com.bluedot.commons.notificationChannels.Email;
import com.bluedot.commons.notificationChannels.MessagingHelper;
import com.bluedot.commons.notificationChannels.NotificationChannel;
import com.bluedot.commons.notificationChannels.SMS;
import com.bluedot.commons.security.PermissionNames;
import com.bluedot.commons.security.PermissionValidator;
import com.bluedot.commons.security.PromiseCallback;
import com.bluedot.commons.security.Secured;
import com.bluedot.commons.security.User;
import com.bluedot.commons.security.ValidateJsonPost;
import com.fasterxml.jackson.databind.JsonNode;
import com.play4jpa.jpa.db.Tx;

import flexjson.JSONSerializer;
import play.mvc.BodyParser;
import play.mvc.Http.Context;
import play.mvc.Result;
import play.mvc.Security;


@ErrorMessage
@Tx
@Security.Authenticated(Secured.class)
public class NotificationChannelController extends AbstractController
{

	private enum NotificationChannelType {
		SMS, MAIL
	}

	public  CompletionStage<Result> listNotificationChannel(final int userId) throws Throwable
	{

		return PermissionValidator.runWithValidation(Context.current(), new PromiseCallback() {
			@Override
			public CompletionStage<Result> execute() throws APIException
			{
				User user = User.findById(userId, true);

				Collection<NotificationChannel> notificationsChannels = user.getNotificationChannels();

				JSONSerializer serializer = new JSONSerializer().include("email", "phone", "enabled", "validated", "id").exclude("*").prettyPrint(true);

				return CompletableFuture.completedFuture(ok(serializer.deepSerialize(notificationsChannels)));
			}
		}, PermissionNames.ANY);
	}

	@BodyParser.Of(BodyParser.Json.class)
	@ValidateJsonPost(fields = { "type", "description" })
	public  CompletionStage<Result> createNotificationChannel(final int userId) throws Throwable
	{

		return PermissionValidator.runWithValidation(Context.current(), new PromiseCallback() {
			@Override
			public CompletionStage<Result> execute() throws APIException
			{
				JsonNode notificationChannelJson = request().body().asJson();

				User user = User.findById(userId, true);

				String description = notificationChannelJson.get("description").asText();

				NotificationChannelType type;
				try
				{
					type = NotificationChannelType.valueOf(notificationChannelJson.get("type").asText());
				} catch (Exception e)
				{
					type = null;
				}

				if (type == null)
					throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE.withParams("type"));

				NotificationChannel notificationChannel = null;

				switch (type)
				{
					case MAIL:
						String email = notificationChannelJson.get("email").asText();
						if (email == null)
							throw APIException.raise(APIErrors.MISSING_PARAMETER.withParams("email"));
		
						for (NotificationChannel notiChannel : user.getNotificationChannels())
						{
							if (notiChannel instanceof Email)
							{
								if (email.equals(((Email) notiChannel).getEmail()))
									throw APIException.raise(APIErrors.EMAIL_EXISTS.withParams(email));
							}
						}
		
						notificationChannel = new Email(description, email);
						break;
					case SMS:
						String phone = notificationChannelJson.get("phone").asText();
						if (phone == null)
							throw APIException.raise(APIErrors.MISSING_PARAMETER.withParams("phone"));
						for (NotificationChannel notiChannel : user.getNotificationChannels())
						{
							if (notiChannel instanceof SMS)
							{
								if (phone.equals(((SMS) notiChannel).getPhone()))
									throw APIException.raise(APIErrors.PHONE_EXISTS.withParams(phone));
							}
						}
		
						notificationChannel = new SMS(description, phone);
						break;
				}

				user.getNotificationChannels().add(notificationChannel);

				notificationChannel.save();

				notificationChannel.sendValidationKey(new MessagingHelper().getValidationHost(request().host()));
				
				return CompletableFuture.completedFuture(created());
			}
		}, PermissionNames.ANY);
	}

	public  CompletionStage<Result> editNotificationChannel(final int userId, final int notificationChannelId) throws Throwable
	{

		return PermissionValidator.runWithValidation(Context.current(), new PromiseCallback() {
			@Override
			public CompletionStage<Result> execute() throws APIException
			{
				JsonNode body = request().body().asJson();
				User user = User.findById(userId, true);

				NotificationChannel notificationChannel = NotificationChannel.findById(notificationChannelId);

				if (!user.getNotificationChannels().contains(notificationChannel))
					throw APIException.raise(APIErrors.UNAUTHORIZED).setDetailMessage("Notification Channel does not belog to you");
			
				if(body.has("enabled"))
				{
					notificationChannel.setEnabled(body.get("enabled").asBoolean());
					notificationChannel.update();
				}
				
				return CompletableFuture.completedFuture(ok());
				
			}
		}, PermissionNames.ANY);
	}

	public  CompletionStage<Result> deleteNotificationChannel(final int userId, final int notificationChannelId) throws Throwable
	{

		return PermissionValidator.runWithValidation(Context.current(), new PromiseCallback() {
			@Override
			public CompletionStage<Result> execute() throws APIException
			{

				User user = User.findById(userId, true);

				NotificationChannel notificationChannel = NotificationChannel.findById(notificationChannelId);

				if (!user.getNotificationChannels().contains(notificationChannel))
					throw APIException.raise(APIErrors.UNAUTHORIZED).setDetailMessage("Notification Channel does not belog to you");

				user.getNotificationChannels().remove(notificationChannel);

				notificationChannel.delete();

				return CompletableFuture.completedFuture(ok());

			}
		}, PermissionNames.ANY);
	}

	public  CompletionStage<Result> sendKeyNotificationChannel(final int userId, final int notificationChannelId) throws Throwable
	{

		return PermissionValidator.runWithValidation(Context.current(), new PromiseCallback() {
			@Override
			public CompletionStage<Result> execute() throws APIException
			{

				User user = User.findById(userId, true);

				NotificationChannel notificationChannel = NotificationChannel.findById(notificationChannelId);

				if (!user.getNotificationChannels().contains(notificationChannel))
					throw APIException.raise(APIErrors.UNAUTHORIZED).setDetailMessage("Notification Channel does not belog to you");

				notificationChannel.sendValidationKey(new MessagingHelper().getValidationHost(request().host()));

				return CompletableFuture.completedFuture(ok());

			}
		}, PermissionNames.ANY);
	}

	public  CompletionStage<Result> validateNotificationChannel(final int notificationChannelId, final String key) throws Throwable
	{

		return PermissionValidator.runWithValidation(Context.current(), new PromiseCallback() {
			@Override
			public CompletionStage<Result> execute() throws APIException
			{

				NotificationChannel notificationChannel = NotificationChannel.findById(notificationChannelId);

				boolean validated = notificationChannel.validate(key);
				
				notificationChannel.update();

				if (validated)
					return CompletableFuture.completedFuture(ok());
				else
					throw APIException.raise(APIErrors.INVALID_VALIDATION_KEY);

			}
		}, PermissionNames.ANY);
	}

	public  CompletionStage<Result> testNotificationChannel(final int userId, final int notificationChannelId) throws Throwable
	{

		return PermissionValidator.runWithValidation(Context.current(), new PromiseCallback() {
			@Override
			public CompletionStage<Result> execute() throws APIException
			{

				User user = User.findById(userId, true);

				NotificationChannel notificationChannel = NotificationChannel.findById(notificationChannelId);

				if (!user.getNotificationChannels().contains(notificationChannel))
					throw APIException.raise(APIErrors.UNAUTHORIZED).setDetailMessage("Notification Channel does not belog to you");

				notificationChannel.test();

				return CompletableFuture.completedFuture(ok());
			}
		}, PermissionNames.ANY);
	}

}
