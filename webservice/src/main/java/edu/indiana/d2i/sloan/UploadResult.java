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
package edu.indiana.d2i.sloan;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import edu.indiana.d2i.sloan.bean.VmUserRole;
import edu.indiana.d2i.sloan.vm.VMRole;
import org.apache.log4j.Logger;

import com.sun.jersey.multipart.FormDataParam;

import edu.indiana.d2i.sloan.bean.ErrorBean;
import edu.indiana.d2i.sloan.bean.UserBean;
import edu.indiana.d2i.sloan.bean.UserResultBean;
import edu.indiana.d2i.sloan.db.DBOperations;
import edu.indiana.d2i.sloan.result.UploadPostprocess;
import edu.indiana.d2i.sloan.utils.EmailUtil;

/**
 * For internal use only!
 *
 */
@Path("/upload")
public class UploadResult {
	private static Logger logger = Logger.getLogger(UploadResult.class);
	
	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response uploadResult(
		@FormDataParam("vmid") String vmid, @FormDataParam("file") InputStream input,
		@Context HttpHeaders httpHeaders,
		@Context HttpServletRequest httpServletRequest) {
		if (vmid == null || input == null) {
			logger.error("vmid or input stream is null!");
			return Response.status(400).entity(
				new ErrorBean(400, "vmid and file cannot be empty!")).build();
		}
		
		try {
			// check upload file type ??			
			
			// check if vmid is associated with a user in uservm table
			//UserBean userbean = DBOperations.getInstance().getUserWithVmid(vmid); -- now there're multiple users

			//get roles of the vm
			List<VmUserRole> vmUserRoles = DBOperations.getInstance().getRolesWithVmid(vmid, true);

			//get the owner from roles
			List<VmUserRole> owner = vmUserRoles.stream()
					.filter(role -> role.getRole() == VMRole.OWNER_CONTROLLER)
					.collect(Collectors.toList());

			logger.info("Prepare to upload result for " + vmid + ", " + vmUserRoles);
			
			// write to DB
			String randomid = UUID.randomUUID().toString();
			DBOperations.getInstance().insertResult(vmid, randomid, input);			
			
			// add to post-process
			if (!Configuration.getInstance().getBoolean(
				Configuration.PropertyName.RESULT_HUMAN_REVIEW, false)) {
				UploadPostprocess.instance.addPostprocessingItem(new UserResultBean(
						owner.get(0).getUsername(), owner.get(0).getEmail(), randomid, vmUserRoles));
			}	else {
				// send email to the reviewer
				String emails = Configuration.getInstance().getString(
					Configuration.PropertyName.RESULT_HUMAN_REVIEW_EMAIL);
				if (emails == null) {
					logger.error("Reviewers' email addresses are missing.");
				} else {
					String[] addrs = emails.split(";");
					for (String email : addrs) {
						EmailUtil emailUtil = new EmailUtil();
						emailUtil.sendEMail(email, "HTRC Data Capsules Result Review Request", 
							"Dear Reviewer, \nThere is a new result pending for your review. Its id is " + randomid);
					}
				}
			}
			
			logger.info("Upload result for " + vmid + ", " + vmUserRoles + " successfully.");
			
			return Response.status(200).build();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return Response.status(500)
					.entity(new ErrorBean(500, e.getMessage())).build();
		}		
	}
}
