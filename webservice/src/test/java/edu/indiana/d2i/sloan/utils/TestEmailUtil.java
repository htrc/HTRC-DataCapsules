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

import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class TestEmailUtil {
	private final EmailUtil emailUtil = new EmailUtil();
	
	@Test
	public void testSendMessage() {
		String emailAddr = "jiaazeng@indiana.edu";
		String subject = "Testing subject";
		String content = "Hello! This is a test message.";
		emailUtil.sendEMail(null,emailAddr, subject, content);
	}
}
