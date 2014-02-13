package edu.indiana.d2i.sloan.utils;

import java.util.Properties;

import javax.mail.*;
import javax.mail.internet.*;

import edu.indiana.d2i.sloan.Configuration;

public class EmailUtil {
	private final String sendername;
	private final String password;
	private Properties props = new Properties();

	public EmailUtil() {
		this.sendername = Configuration.getInstance().getString(
				Configuration.PropertyName.EMAIL_SENDERNAME);
		this.password = Configuration.getInstance().getString(
				Configuration.PropertyName.EMAIL_PASSWORD);
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.host", Configuration.getInstance().getString(
				Configuration.PropertyName.EMAIL_SMTP_HOST));
		props.put("mail.smtp.port", Configuration.getInstance().getString(
				Configuration.PropertyName.EMAIL_SMTP_PORT));
	}

	public void sendEMail(String emailAddr, String subject, String content) {
		// TODO: create a session per request, may change this in the future
		Session session = Session.getDefaultInstance(props,
			new javax.mail.Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(sendername, password);
				}
			});
		
		try {			 
			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress(sendername));
			message.setRecipients(Message.RecipientType.TO,
					InternetAddress.parse(emailAddr));
			message.setSubject(subject);
			message.setText(content);
 
			Transport.send(message);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
