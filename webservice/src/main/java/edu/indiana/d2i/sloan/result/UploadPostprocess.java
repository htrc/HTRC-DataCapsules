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

import edu.indiana.d2i.sloan.bean.VmUserRole;
import edu.indiana.d2i.sloan.utils.RolePermissionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import edu.indiana.d2i.sloan.Configuration;
import edu.indiana.d2i.sloan.bean.UserResultBean;
import edu.indiana.d2i.sloan.db.DBOperations;
import edu.indiana.d2i.sloan.utils.EmailUtil;
import edu.indiana.d2i.sloan.utils.RetriableTask;

public class UploadPostprocess {
	private static Logger logger = LoggerFactory.getLogger(UploadPostprocess.class);
	public final static UploadPostprocess instance = new UploadPostprocess();
	
	private final Queue<UserResultBean> resultsQueue = new LinkedList<UserResultBean>();
	
	private class ResultDeliverThread implements Runnable {
		private EmailUtil emailUtil = new EmailUtil();
		private final String EMAIL_SUBJECT = "HTRC Data Capsule Result Download URL";
		
		private void notifyUser(final UserResultBean item) throws Exception {
			// generate url
			String url = Configuration.getInstance().getString(
				Configuration.PropertyName.RESULT_DOWNLOAD_URL_PREFIX) + 
				item.getResultId();

			//filter roles who are allowed to view results
			List<VmUserRole> allowedVmUserRoles = RolePermissionUtils.filterPermittedRoles(item.getRoles(),
					item.getVmId(), RolePermissionUtils.API_CMD.VIEW_RESULT);

			for (VmUserRole role : allowedVmUserRoles) {
				// message
				final String content = String.format(
					"Dear User, \n\nThank you for using HTRC Data Capsule! " +
					"You can download your result from the link below. \n%s", url);

				logger.debug(String.format("url: %s, content: %s", url, content));

				// send email
				RetriableTask<Void> r = new RetriableTask<Void>(
					new Callable<Void>() {
						@Override
						public Void call() throws Exception {
							emailUtil.sendEMail(null,role.getEmail(), EMAIL_SUBJECT, content);
							return null;
						}
					},  2000, 3);
				r.call();

				logger.info(String.format("Email has been sent to %s, " +
						"with guid %s, url %s", role.getEmail(), role.getGuid(), url));
			}

			// mark result as notified
			DBOperations.getInstance().updateResultAsNotified(item.getResultId());

		}
		
		public ResultDeliverThread() {
			try {
				List<UserResultBean> resultsUnnotified = DBOperations.getInstance().getResultsUnnotified();
				for (UserResultBean bean : resultsUnnotified) {
					notifyUser(bean);
				}
				logger.info("Deliver " + resultsUnnotified.size() + " result notifications from last run.");
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				throw new RuntimeException(e); // cannot deliver remaining notifications in DB, do start at all
			}
		}
		
		@Override
		public void run() {
			while (true) {
				synchronized (resultsQueue) {
					try {
						if (resultsQueue.isEmpty()) {
							resultsQueue.wait();
						} else {
							final UserResultBean item = resultsQueue.poll();
							notifyUser(item);							
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
	
	public void addPostprocessingItem(UserResultBean userresult) {
		synchronized (resultsQueue) {
			resultsQueue.add(userresult);
			resultsQueue.notify();
		}
	}
}
