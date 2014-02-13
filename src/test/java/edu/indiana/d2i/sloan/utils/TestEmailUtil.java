package edu.indiana.d2i.sloan.utils;

import org.junit.Test;

public class TestEmailUtil {
	private final EmailUtil emailUtil = new EmailUtil();
	
	@Test
	public void testSendMessage() {
		String appellation = "Jiaan";
		String emailAddr = "jiaazeng@indiana.edu";
		String subject = "Testing subject";
		String content = "Hello! This is a test message.";
		emailUtil.sendEMail(emailAddr, subject, content);
	}
}
