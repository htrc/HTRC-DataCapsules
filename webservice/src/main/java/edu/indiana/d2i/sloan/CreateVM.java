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

import java.sql.SQLException;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import edu.indiana.d2i.sloan.exception.NoItemIsFoundInDBException;
import edu.indiana.d2i.sloan.vm.PortsPool;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

import edu.indiana.d2i.sloan.bean.ErrorBean;
import edu.indiana.d2i.sloan.bean.CreateVmRequestBean;
import edu.indiana.d2i.sloan.bean.CreateVmResponseBean;
import edu.indiana.d2i.sloan.bean.VmInfoBean;
import edu.indiana.d2i.sloan.db.DBOperations;
import edu.indiana.d2i.sloan.hyper.CreateVMCommand;
import edu.indiana.d2i.sloan.hyper.HypervisorProxy;
import edu.indiana.d2i.sloan.scheduler.SchedulerFactory;

@Path("/createvm")
public class CreateVM {
	private static Logger logger = Logger.getLogger(CreateVM.class);

	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public Response getResourcePost(
			@FormParam("imagename") String imageName,
			@FormParam("loginusername") String loginusername,
			@FormParam("loginpassword") String loginpassword,
			@DefaultValue("1024") @FormParam("memory") int memory,
			@DefaultValue("1") @FormParam("vcpu") int vcpu,
			@Context HttpHeaders httpHeaders,
			@Context HttpServletRequest httpServletRequest) {
		String userName = httpServletRequest.getHeader(Constants.USER_NAME);
		String userEmail = httpServletRequest.getHeader(Constants.USER_EMAIL);
		if (userEmail == null) userEmail = "";

		// check input
		if (userName == null) {
			logger.error("Username is not present in http header.");
			return Response
					.status(500)
					.entity(new ErrorBean(500,
							"Username is not present in http header.")).build();
		}

		if (imageName == null || loginusername == null || loginpassword == null) {
			return Response
					.status(400)
					.entity(new ErrorBean(400,
							"Image name, login username and login password cannot be empty!"))
					.build();
		}

		try {
			DBOperations.getInstance().insertUserIfNotExists(userName, userEmail);

			// check if ports are available

			// check if image name is valid
			String imagePath = DBOperations.getInstance().getImagePath(imageName);
			if (imagePath == null) {
				return Response.status(400).entity(
					new ErrorBean(400, 
						String.format("Image %s does not exist!", imageName)))
						.build();
			}
			
			// check if policy name is valid
			
			int volumeSizeInGB = Integer.valueOf(Configuration.getInstance()
					.getString(Configuration.PropertyName.VOLUME_SIZE_IN_GB,
							Constants.DEFAULT_VOLUME_SIZE_IN_GB));

			// vm parameters
			String vmid = UUID.randomUUID().toString();
			String workDir = FilenameUtils.concat(
				Configuration.getInstance().getString(
					Configuration.PropertyName.DEFAULT_VM_WORKDIR_PREFIX), vmid);
			CreateVmRequestBean request = new CreateVmRequestBean(userName,
					imageName, vmid, loginusername, loginpassword, memory,
					vcpu, volumeSizeInGB, workDir);
			logger.info("User " + userName + " tries to create vm " + request);
			
			// check quota
			if (!DBOperations.getInstance().quotasNotExceedLimit(request)) {
				logger.info("Quota exceeds limit for user " + userName);
				return Response.status(400)
						.entity(new ErrorBean(400, "Quota exceeds limit!"))
						.build();
			}

			// schedule & update db
			VmInfoBean vminfo = SchedulerFactory.getInstance().schedule(request);

			// nonblocking call to hypervisor
			vminfo.setImagePath(imagePath);
			HypervisorProxy.getInstance().addCommand(new CreateVMCommand(vminfo, userName));

			return Response.status(200).entity(new CreateVmResponseBean(vmid)).build();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);

			// if because of port unavailable error, restore user quota
            if (e.getMessage().equals("No port resource available.")) {
				try {
					DBOperations.getInstance().restoreQuota(userName, vcpu, memory, 10);
				} catch (SQLException e1) {
					e1.printStackTrace();
				} catch (NoItemIsFoundInDBException e1) {
					e1.printStackTrace();
				}
			}
			return Response.status(500)
					.entity(new ErrorBean(500, e.getMessage())).build();
		}
	}
}
