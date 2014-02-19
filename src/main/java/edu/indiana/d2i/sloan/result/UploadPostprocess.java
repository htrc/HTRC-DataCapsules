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
package edu.indiana.d2i.sloan.result;

import java.util.*;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import edu.indiana.d2i.sloan.Configuration;
import edu.indiana.d2i.sloan.bean.UserBean;
import edu.indiana.d2i.sloan.utils.EmailUtil;
import edu.indiana.d2i.sloan.utils.RetriableTask;

public class UploadPostprocess {
	private static Logger logger = Logger.getLogger(UploadPostprocess.class);
	public final static UploadPostprocess instance = new UploadPostprocess();
	
	private final Queue<UserResult> resultsQueue = new LinkedList<UserResult>();
	
	private class UserResult {
		public final String appellation;
		public final String useremail;
		public final String resultid;
		
		public UserResult(String appellation, String useremail, String itemid) {
			this.appellation = appellation;
			this.useremail = useremail;
			this.resultid = itemid;
		}
	}
	
	private class ResultDeliverThread implements Runnable {
		private EmailUtil emailUtil = new EmailUtil();
		private final String EMAIL_SUBJECT = "HTRC Data Capsule Result Download URL";
		
		@Override
		public void run() {
			while (true) {
				synchronized (resultsQueue) {
					try {
						if (resultsQueue.isEmpty()) {
							resultsQueue.wait();
						} else {
							final UserResult item = resultsQueue.poll();
							String url = Configuration.getInstance().getString(
								Configuration.PropertyName.RESULT_DOWNLOAD_URL_PREFIX) + 
								item.resultid;
							final String content = String.format(
								"Dear %s, \n\nThank you for using HTRC Data Capsule! " +
								"You can download your result from the link below. \n%s", 
								item.appellation, url);
							
							logger.debug(String.format("url: %s, content: %s", url, content));
							
							RetriableTask<Void> r = new RetriableTask<Void>(
								new Callable<Void>() {
									@Override
									public Void call() throws Exception {
										emailUtil.sendEMail(item.useremail, EMAIL_SUBJECT, content);
										return null;
									}
								},  2000, 3);
							r.call();
							
							logger.info(String.format("Email has been sent to %s, " +
								"with username %s, url %s", item.useremail, item.appellation, url));
						}
					} catch (Exception e) {
						logger.error(e.getMessage(), e);
					}
				}
			}
		}		
	}
	
	private class ResultAvoidThread implements Runnable {

		@Override
		public void run() {
			
		}		
	}
	
	private UploadPostprocess() {
		Thread deliver = new Thread(new ResultDeliverThread());
		deliver.setDaemon(true);
		deliver.start();
		
		
	}
	
	public void addPostprocessingItem(UserBean user, String itemId) {
		synchronized (resultsQueue) {
			resultsQueue.add(new UserResult(user.getUserName(), user.getUserEmail(), itemId));
			resultsQueue.notify();
		}
	}
}
