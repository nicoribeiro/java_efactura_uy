package com.bluedot.commons.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.NoSuchProviderException;
import javax.mail.Part;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.MimeBodyPart;

import org.apache.commons.lang.StringUtils;

import com.sun.mail.pop3.POP3Message;



 
/**
 * This program demonstrates how to download e-mail messages and save
 * attachments into files on disk.
 *
 * @author www.codejava.net
 *
 */
public class EmailAttachmentReceiver {
    private String saveDirectory;
 
    /**
     * Sets the directory where attached files will be stored.
     * @param dir absolute path of the directory
     */
    public void setSaveDirectory(String dir) {
        this.saveDirectory = dir;
    }
 
    /**
     * Downloads new messages and saves attachments to disk if any.
     * @param host
     * @param port
     * @param userName
     * @param password
     */
    public List<Email> downloadEmail(String host, int port,
            String userName, String password) {
        
    	final String usernameFinal = userName;

		final String passwordFinal = password;
    	
    	List<Email> emails = new LinkedList<Email>();
    	
    	Properties properties = new Properties();
 
        // server setting
        properties.put("mail.pop3.host", host);
        properties.put("mail.pop3.port", port);
 
        // SSL setting
//        properties.setProperty("mail.pop3.socketFactory.class","javax.net.ssl.SSLSocketFactory");
//        properties.setProperty("mail.pop3.socketFactory.fallback", "false");
//        properties.setProperty("mail.pop3.socketFactory.port",String.valueOf(port));
 
       
        
     // Get the default Session object.
     		Session session = Session.getInstance(properties, new javax.mail.Authenticator() {
     			protected PasswordAuthentication getPasswordAuthentication()
     			{
     				return new PasswordAuthentication(usernameFinal, passwordFinal);
     			}
     		});
     		
            // session.setDebug(true);
        
     		 try {
				String packageName="javax.mail.internet.";
				String simpleClassName="MimeMultipart";
				String className=packageName+simpleClassName;
				Class<?> cl=Class.forName(className);
				URL url=cl.getResource(simpleClassName+".class");
				System.out.println("url="+url);
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
     		
     		
        try {
            // connects to the message store
            Store store = session.getStore("pop3");
            store.connect(userName, password);
 
            // opens the inbox folder
            Folder folderInbox = store.getFolder("INBOX");
            folderInbox.open(Folder.READ_ONLY);
 
            // fetches new messages from server
            Message[] arrayMessages = folderInbox.getMessages();
            //TODO cambiar 10 por arrayMessages.length
            for (int i = 0; i < 10; i++) {
            	POP3Message message = (POP3Message)arrayMessages[i];
                Address[] fromAddress = message.getFrom();
                String from="";
                if (fromAddress !=null && fromAddress.length > 0)
                	from = fromAddress[0].toString();
                String subject = message.getSubject();
                Date sentDate = message.getSentDate();
 
                String contentType = message.getContentType();
                String messageContent = "";
 
                // store attachment file name, separated by comma
                String attachFiles = "";
 
                Email email = new Email(from,subject,sentDate,messageContent);
                
//                List<File> attachments = getAttachments(message);
                
                if (contentType.contains("multipart")) {
                    // content may contain attachments
                    Multipart multiPart = (Multipart) message.getContent();
                    int numberOfParts = multiPart.getCount();
                    for (int partCount = 0; partCount < numberOfParts; partCount++) {
                        MimeBodyPart part = (MimeBodyPart) multiPart.getBodyPart(partCount);
                        if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
                            // this part is attachment
                            String fileName = part.getFileName();
                            attachFiles += fileName + ", ";
                            ByteArrayOutputStream bos = new ByteArrayOutputStream();
                            part.getDataHandler().writeTo(bos);

                            String decodedContent = bos.toString();
                            	email.getAttachments().put(fileName, decodedContent);
                            
//                            part.saveFile(saveDirectory + File.separator + fileName);
                        } else {
                            // this part may be the message content
                            messageContent = part.getContent().toString();
                        }
                    }
 
                    if (attachFiles.length() > 1) {
                        attachFiles = attachFiles.substring(0, attachFiles.length() - 2);
                    }
                } else if (contentType.contains("text/plain")
                        || contentType.contains("text/html")) {
                    Object content = message.getContent();
                    if (content != null) {
                        messageContent = content.toString();
                    }
                }
                
                
                
                
                emails.add(email);
 
                // print out details of each message
                System.out.println("Message #" + (i + 1) + ":");
                System.out.println("\t From: " + from);
                System.out.println("\t Subject: " + subject);
                System.out.println("\t Sent Date: " + sentDate);
                System.out.println("\t Message: " + messageContent);
                System.out.println("\t Attachments: " + attachFiles);
                
            }
 
            // disconnect
            folderInbox.close(false);
            store.close();
        } catch (NoSuchProviderException ex) {
            System.out.println("No provider for pop3.");
            ex.printStackTrace();
        } catch (MessagingException ex) {
            System.out.println("Could not connect to the message store");
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        
        return emails;
    }
 
    /**
     * Runs this program with Gmail POP3 server
     */
    public static void main(String[] args) {
        String host = "pop.gmail.com";
        int port = 995;
        String userName = "your_email";
        String password = "your_password";
 
        String saveDirectory = "E:/Attachment";
 
        EmailAttachmentReceiver receiver = new EmailAttachmentReceiver();
        receiver.setSaveDirectory(saveDirectory);
        receiver.downloadEmail(host, port, userName, password);
 
    }
    
    
    private static List<File> getAttachments(Message message) throws FileNotFoundException, MessagingException, IOException{
    	List<File> attachments = new ArrayList<File>();
    	
    	System.out.println("#####################################################");
    	System.out.println(message.getContent());
    	System.out.println("#####################################################");
    	
    	    if (!(message.getContent() instanceof Multipart))
    	    	return attachments;
    	    
    	    Multipart multipart = (Multipart) message.getContent();
    	    
    	    // System.out.println(multipart.getCount());

    	    for (int i = 0; i < multipart.getCount(); i++) {
    	        BodyPart bodyPart = multipart.getBodyPart(i);
    	        if(!Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition()) &&
    	               !StringUtils.isNotBlank(bodyPart.getFileName())) {
    	            continue; // dealing with attachments only
    	        } 
    	        InputStream is = bodyPart.getInputStream();
    	        File f = new File("/tmp/" + bodyPart.getFileName());
    	        FileOutputStream fos = new FileOutputStream(f);
    	        byte[] buf = new byte[4096];
    	        int bytesRead;
    	        while((bytesRead = is.read(buf))!=-1) {
    	            fos.write(buf, 0, bytesRead);
    	        }
    	        fos.close();
    	        attachments.add(f);
    	    }
    	
    	return attachments;
    }
    
}
