/*******************************************************************************
 * Copyright 2014 The Trustees of Indiana University
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package edu.indiana.d2i.sloan.utils;

import java.util.Properties;

import javax.mail.*;
import javax.mail.internet.*;

import edu.indiana.d2i.sloan.Configuration;

public class EmailUtil {
	private final String sendername;
	private final String senderAddr;
	private final String password;
	private Properties props = new Properties();

	public EmailUtil() {
		this.sendername = Configuration.getInstance().getString(
				Configuration.PropertyName.EMAIL_SENDERNAME);
		this.password = Configuration.getInstance().getString(
				Configuration.PropertyName.EMAIL_PASSWORD);
		this.senderAddr = Configuration.getInstance().getString(Configuration.PropertyName.EMAIL_SENDER_ADDR);
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.host", Configuration.getInstance().getString(
				Configuration.PropertyName.EMAIL_SMTP_HOST));
		props.put("mail.smtp.socketFactory.port", Configuration.getInstance().getString(
				Configuration.PropertyName.EMAIL_SMTP_PORT));
		props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
		
		
		System.out.println(props.toString());
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
			message.setFrom(new InternetAddress(senderAddr));
			message.setRecipients(Message.RecipientType.TO,
					InternetAddress.parse(emailAddr));
			message.setSubject(subject);
			message.setText(content);
 
			Transport.send(message);
		} catch (Exception e) {		
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
}
