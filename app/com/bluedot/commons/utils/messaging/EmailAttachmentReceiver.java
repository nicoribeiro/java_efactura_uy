package com.bluedot.commons.utils.messaging;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

import javax.mail.Address;
import javax.mail.FetchProfile;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.UIDFolder;
import javax.mail.internet.MimeBodyPart;
import javax.mail.search.ComparisonTerm;
import javax.mail.search.ReceivedDateTerm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.mail.imap.IMAPFolder;

/**
 * Descarga mensajes de todas las carpetas de un buzon IMAP y extrae sus adjuntos.
 *
 * @author www.codejava.net
 * @author nicolasribeiro
 *
 */
public class EmailAttachmentReceiver {
	final static Logger logger = LoggerFactory.getLogger(EmailAttachmentReceiver.class);

	/**
	 * Atributos SPECIAL-USE (RFC 6154) de carpetas que no se procesan. \All, \Flagged e \Important
	 * son vistas virtuales (ej: Gmail) que duplican mensajes de otras carpetas.
	 */
	private static final Set<String> SPECIAL_USE_EXCLUIDOS = new HashSet<String>(
			Arrays.asList("\\trash", "\\junk", "\\sent", "\\drafts", "\\all", "\\flagged", "\\important"));

	/**
	 * Nombres de carpetas excluidas para servidores que no informan SPECIAL-USE.
	 */
	private static final Set<String> NOMBRES_EXCLUIDOS = new HashSet<String>(Arrays.asList("trash", "deleted",
			"deleted items", "deleted messages", "papelera", "elementos eliminados", "junk", "junk e-mail",
			"junk email", "spam", "bulk mail", "correo no deseado", "sent", "sent items", "sent mail",
			"sent messages", "enviados", "elementos enviados", "drafts", "borradores"));

	private static final int FETCH_BATCH = 100;

	/**
	 * Sincroniza todas las carpetas del buzon (salvo papelera, spam, enviados y borradores) usando UIDs.
	 * 
	 * Por cada carpeta se procesan solo los mensajes con UID mayor al ultimo procesado. La primera vez
	 * que se ve una carpeta (o si cambia su UIDVALIDITY) se procesan los mensajes recibidos en los
	 * ultimos diasPrimeraSincronizacion dias.
	 * 
	 * @param carpetasExcluidas
	 *            nombres adicionales de carpetas a excluir (case insensitive, nombre completo o ultimo segmento)
	 */
	public void syncAllFolders(String host, String port, String userName, String password,
			Collection<String> carpetasExcluidas, int diasPrimeraSincronizacion, ImapSyncListener listener)
			throws MessagingException {

		String buzon = userName + "@" + host + ":" + port;
		long inicio = System.currentTimeMillis();

		Properties properties = EmailReceiver.getServerProperties("imap", host, port, true);
		Session session = Session.getInstance(properties);
		Store store = session.getStore("imap");

		Set<String> excluidos = new HashSet<String>(NOMBRES_EXCLUIDOS);
		if (carpetasExcluidas != null)
			for (String nombre : carpetasExcluidas)
				excluidos.add(nombre.trim().toLowerCase(Locale.ROOT));

		int carpetasSincronizadas = 0;
		int carpetasExcluidasCount = 0;
		int carpetasConError = 0;
		int mensajesProcesados = 0;
		int mensajesConError = 0;

		logger.info("Inicio sincronizacion IMAP de " + buzon);
		logger.debug("Carpetas adicionales excluidas por configuracion: " + carpetasExcluidas
				+ ", dias primera sincronizacion: " + diasPrimeraSincronizacion);

		try {
			store.connect(userName, password);
			logger.debug("Conectado a " + buzon);

			Folder[] folders = store.getDefaultFolder().list("*");
			logger.info(folders.length + " carpetas en " + buzon);

			for (Folder folder : folders) {
				IMAPFolder imapFolder = (IMAPFolder) folder;
				logger.debug("Carpeta " + folder.getFullName() + " atributos=" + Arrays.toString(imapFolder.getAttributes()));

				if ((folder.getType() & Folder.HOLDS_MESSAGES) == 0) {
					logger.debug("Carpeta " + folder.getFullName() + " omitida: no contiene mensajes");
					continue;
				}

				String motivoExclusion = motivoExclusion(imapFolder, excluidos);
				if (motivoExclusion != null) {
					logger.debug("Carpeta " + folder.getFullName() + " excluida: " + motivoExclusion);
					carpetasExcluidasCount++;
					continue;
				}

				try {
					ResultadoCarpeta resultado = syncFolder(imapFolder, diasPrimeraSincronizacion, listener);
					carpetasSincronizadas++;
					mensajesProcesados += resultado.procesados;
					mensajesConError += resultado.errores;
				} catch (MessagingException e) {
					// Un error en una carpeta no debe impedir procesar las demas
					carpetasConError++;
					logger.error("Error sincronizando carpeta " + folder.getFullName() + " de " + buzon, e);
				}
			}
		} finally {
			if (store.isConnected())
				store.close();
			logger.info("Fin sincronizacion IMAP de " + buzon + ": carpetas sincronizadas=" + carpetasSincronizadas
					+ ", excluidas=" + carpetasExcluidasCount + ", con error=" + carpetasConError
					+ ", mensajes procesados=" + mensajesProcesados + ", mensajes con error=" + mensajesConError
					+ ", duracion=" + (System.currentTimeMillis() - inicio) + "ms");
		}
	}

	private static class ResultadoCarpeta {
		int procesados;
		int errores;
	}

	/**
	 * @return motivo por el que se excluye la carpeta, o null si se procesa
	 */
	private String motivoExclusion(IMAPFolder folder, Set<String> excluidos) throws MessagingException {
		for (String atributo : folder.getAttributes())
			if (SPECIAL_USE_EXCLUIDOS.contains(atributo.toLowerCase(Locale.ROOT)))
				return "atributo " + atributo;

		String fullName = folder.getFullName().toLowerCase(Locale.ROOT);
		String name = folder.getName().toLowerCase(Locale.ROOT);
		if (excluidos.contains(fullName) || excluidos.contains(name))
			return "nombre";
		return null;
	}

	private ResultadoCarpeta syncFolder(IMAPFolder folder, int diasPrimeraSincronizacion, ImapSyncListener listener)
			throws MessagingException {
		String folderName = folder.getFullName();
		ResultadoCarpeta resultado = new ResultadoCarpeta();

		folder.open(Folder.READ_ONLY);
		try {
			long uidValidity = folder.getUIDValidity();
			ImapSyncListener.FolderSyncState state = listener.getState(folderName);

			logger.debug("Carpeta " + folderName + ": mensajes=" + folder.getMessageCount() + ", uidValidity="
					+ uidValidity + ", uidNext=" + folder.getUIDNext() + ", estado guardado="
					+ (state == null ? "ninguno" : "uidValidity=" + state.getUidValidity() + " lastUid=" + state.getLastUid()));

			long lastUid;
			if (state != null && state.getUidValidity() == uidValidity) {
				lastUid = state.getLastUid();
			} else {
				if (state != null)
					logger.warn("Cambio el UIDVALIDITY de la carpeta " + folderName + " (" + state.getUidValidity()
							+ " -> " + uidValidity + "), se resincronizan los ultimos " + diasPrimeraSincronizacion + " dias");
				lastUid = uidInicial(folder, diasPrimeraSincronizacion);
				listener.saveState(folderName, uidValidity, lastUid);
				logger.info("Carpeta " + folderName + " sin estado previo: se procesan mensajes de los ultimos "
						+ diasPrimeraSincronizacion + " dias (desde UID " + (lastUid + 1) + ")");
			}

			// Con UID n:* el servidor siempre devuelve al menos el ultimo mensaje, por eso se filtra uid > lastUid
			Message[] candidatos = folder.getMessagesByUID(lastUid + 1, UIDFolder.LASTUID);
			List<Message> nuevos = new LinkedList<Message>();
			for (Message message : candidatos)
				if (message != null && folder.getUID(message) > lastUid)
					nuevos.add(message);

			if (nuevos.isEmpty()) {
				logger.debug("Carpeta " + folderName + ": sin mensajes nuevos (lastUid=" + lastUid + ")");
				// Se registra igual la sincronizacion, para saber que la carpeta se sigue revisando
				listener.saveState(folderName, uidValidity, lastUid);
				return resultado;
			}

			nuevos.sort(new Comparator<Message>() {
				@Override
				public int compare(Message a, Message b) {
					return Integer.compare(a.getMessageNumber(), b.getMessageNumber());
				}
			});

			logger.info("Carpeta " + folderName + ": " + nuevos.size() + " mensajes nuevos (UID > " + lastUid + ")");

			FetchProfile fetchProfile = new FetchProfile();
			fetchProfile.add(FetchProfile.Item.ENVELOPE);
			fetchProfile.add(UIDFolder.FetchProfileItem.UID);

			int n = 0;
			for (int desde = 0; desde < nuevos.size(); desde += FETCH_BATCH) {
				List<Message> lote = nuevos.subList(desde, Math.min(desde + FETCH_BATCH, nuevos.size()));
				logger.debug("Carpeta " + folderName + ": descargando lote de " + lote.size() + " mensajes");
				folder.fetch(lote.toArray(new Message[lote.size()]), fetchProfile);

				for (Message message : lote) {
					long uid = folder.getUID(message);
					n++;
					logger.debug("Carpeta " + folderName + ": mensaje " + n + "/" + nuevos.size() + " UID " + uid);
					try {
						listener.onMessage(folderName, uid, toEmail(message));
						resultado.procesados++;
					} catch (Exception e) {
						// No se reintenta: se loguea y se avanza para no bloquear el buzon con un mensaje malformado
						resultado.errores++;
						logger.error("Error procesando mensaje UID " + uid + " de carpeta " + folderName, e);
					}
					listener.saveState(folderName, uidValidity, uid);
					logger.debug("Carpeta " + folderName + ": lastUid=" + uid + " guardado");
				}
			}

			logger.info("Carpeta " + folderName + ": " + resultado.procesados + " procesados, " + resultado.errores
					+ " con error");
			return resultado;
		} finally {
			if (folder.isOpen())
				folder.close(false);
		}
	}

	/**
	 * UID a partir del cual empezar en una carpeta que no tiene estado: el anterior al primer mensaje
	 * recibido en los ultimos dias, o el ultimo UID de la carpeta si no hay mensajes recientes.
	 */
	private long uidInicial(IMAPFolder folder, int dias) throws MessagingException {
		Calendar desde = Calendar.getInstance();
		desde.add(Calendar.DAY_OF_MONTH, -dias);

		Message[] recientes = folder.search(new ReceivedDateTerm(ComparisonTerm.GE, desde.getTime()));
		logger.debug("Carpeta " + folder.getFullName() + ": " + recientes.length + " mensajes recibidos desde "
				+ desde.getTime());

		if (recientes.length > 0) {
			long minUid = Long.MAX_VALUE;
			for (Message message : recientes)
				minUid = Math.min(minUid, folder.getUID(message));
			return minUid - 1;
		}

		int messageCount = folder.getMessageCount();
		if (messageCount == 0)
			return Math.max(folder.getUIDNext() - 1, 0);
		return folder.getUID(folder.getMessage(messageCount));
	}

	private Email toEmail(Message message) throws MessagingException, IOException {
		Address[] fromAddress = message.getFrom();
		String from = "";
		if (fromAddress != null && fromAddress.length > 0)
			from = fromAddress[0].toString();

		String subject = message.getSubject();
		Date sentDate = message.getSentDate();
		String messageId = message instanceof javax.mail.internet.MimeMessage
				? ((javax.mail.internet.MimeMessage) message).getMessageID()
				: null;

		String messageContent = "";

		Email email = new Email(messageId, from, subject, sentDate, messageContent);

		messageContent = extractAttachments(message, messageContent, email);

		logger.debug("\t Message id: " + messageId);
		logger.debug("\t From: " + from);
		logger.debug("\t Subject: " + subject);
		logger.debug("\t Sent Date: " + sentDate);
		logger.debug("\t Message: " + (messageContent.length() > 500 ? messageContent.substring(0, 500) + "..." : messageContent));
		logger.debug("\t #Attachments: " + email.getAttachments().size());
		for (Attachment attachment : email.getAttachments())
			logger.debug("\t Attachment: " + attachment.getName());

		return email;
	}

	/**
	 * @param message
	 * @param messageContent
	 * @param email
	 * @return
	 * @throws IOException
	 * @throws MessagingException
	 */
	private String extractAttachments(Part message, String messageContent, Email email)
			throws IOException, MessagingException {
		String contentType = message.getContentType();
		
		if (contentType.contains("multipart")) {
			Multipart multiPart = (Multipart) message.getContent();
			int numberOfParts = multiPart.getCount();
			
			for (int partCount = 0; partCount < numberOfParts; partCount++) {
				MimeBodyPart part = (MimeBodyPart) multiPart.getBodyPart(partCount);
				logger.info("Content-Type: " + part.getContentType());
				if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
					/*
					 *  This part is attachment
					 */
					messagePartToAttachment(email, part);
	
				} else {
					if (part.getContentType().toLowerCase().contains("multipart")) {
						extractAttachments((Multipart) part.getContent(), messageContent, email);
					}else
						if (part.getContentType().toLowerCase().contains("text/plain") || part.getContentType().toLowerCase().contains("text/html"))
							/*
							 * This part may be the message content
							 */
							messageContent = messageContent.concat(part.getContent().toString());
						else
							/*
							 * This is an attachment but not in the ATTACHMENT section
							 */
							messagePartToAttachment(email, part);
				}
			}
		}else {
			
			if (contentType.toLowerCase().contains("text/plain") || contentType.toLowerCase().contains("text/html")) {
				Object content = message.getContent();
				if (content != null) {
					/*
					 * This part may be the message content
					 */
					messageContent = messageContent.concat(message.getContent().toString());
				}
			}else {
				messagePartToAttachment(email, message);
			}
			
			
		}
			
		return messageContent;
	}

	private void extractAttachments(Multipart multiPart, String messageContent, Email email) throws MessagingException, IOException {
		
		
		int numberOfParts = multiPart.getCount();
		
		for (int partCount = 0; partCount < numberOfParts; partCount++) {
			MimeBodyPart part = (MimeBodyPart) multiPart.getBodyPart(partCount);
			logger.info("Content-Type: " + part.getContentType());
			if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
				/*
				 *  This part is attachment
				 */
				messagePartToAttachment(email, part);

			} else {
				if (part.getContentType().toLowerCase().contains("multipart")) {
					extractAttachments((Multipart) part.getContent(), messageContent, email);
				}else
					if (part.getContentType().toLowerCase().contains("text/plain") || part.getContentType().toLowerCase().contains("text/html"))
						/*
						 * This part may be the message content
						 */
						messageContent = messageContent.concat(part.getContent().toString());
					else
						/*
						 * This is an attachment but not in the ATTACHMENT section
						 */
						messagePartToAttachment(email, part);
			}
		}
		
		
	}

	/**
	 * @param email
	 * @param part
	 * @throws MessagingException
	 * @throws IOException
	 */
	private void messagePartToAttachment(Email email, Part part) throws MessagingException, IOException {
		String fileName = part.getFileName();
//							attachFiles += fileName + ", ";
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		part.getDataHandler().writeTo(bos);

		String decodedContent = bos.toString();
		Attachment attachment = new Attachment();
		attachment.setAttachmentType(AttachmentType.TEXT);
		attachment.setPayload(decodedContent);
		attachment.setName(fileName);
		email.getAttachments().add(attachment);
	}

}
