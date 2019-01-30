/*******************************************************************************
 * Copyright 2018 The Trustees of Indiana University
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

import edu.indiana.d2i.sloan.bean.ErrorBean;
import edu.indiana.d2i.sloan.bean.VmInfoBean;
import edu.indiana.d2i.sloan.bean.VmUserRole;
import edu.indiana.d2i.sloan.db.DBOperations;
import edu.indiana.d2i.sloan.exception.NoItemIsFoundInDBException;
import edu.indiana.d2i.sloan.hyper.AddVmShareesCommand;
import edu.indiana.d2i.sloan.hyper.HypervisorProxy;
import edu.indiana.d2i.sloan.hyper.UpdatePublicKeyCommand;
import edu.indiana.d2i.sloan.utils.RolePermissionUtils;
import edu.indiana.d2i.sloan.vm.VMRole;
import edu.indiana.d2i.sloan.vm.VMState;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

@Path("/addsharees")
public class AddVmSharees {
	private static Logger logger = Logger.getLogger(AddVmSharees.class);

	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public Response addSharees(@FormParam("vmId") String vmId,
			@FormParam("sharees") List<String> sharees,
			@Context HttpHeaders httpHeaders,
			@Context HttpServletRequest httpServletRequest) {		
		String userName = httpServletRequest.getHeader(Constants.USER_NAME);

		if (userName == null) {
			logger.error("Username is not present in http header.");
			return Response
					.status(400)
					.entity(new ErrorBean(400,
							"Username is not present in http header.")).build();
		}

		try {
			VmUserRole role = DBOperations.getInstance().getUserRoleWithVmid(userName, vmId);
			if (!RolePermissionUtils.isPermittedCommand(role.getRole(), RolePermissionUtils.API_CMD.ADD_SHAREES)) {
				String msg = "User " + userName + " with role " + role.getRole() + " cannot perform task "
						+ RolePermissionUtils.API_CMD.ADD_SHAREES + " on VM " + vmId;
				logger.error(msg);
				return Response.status(400).entity(new ErrorBean(400, msg)).build();
			}

			VmInfoBean vmInfo = DBOperations.getInstance().getVmInfo(userName, vmId);

			logger.info("User " + userName + " tries to add " + sharees + " as sharees for vm " + vmId);

			//TODO-UN how to get usernames, should add to users table ??
			for(String sharee : sharees) {
				//DBOperations.getInstance().insertUserIfNotExists(userName, userEmail);
				//DBOperations.getInstance().insertUserIfNotExists(operator, operatorEmail);

				//add to map table
			}

			//TODO-UN send the keys?? or shouldn't send right now? only after tou is accepted?
			HypervisorProxy.getInstance().addCommand(
					new AddVmShareesCommand(vmInfo, userName, userName, sharees));

			return Response.status(200).build();
		} catch (NoItemIsFoundInDBException e) {
			logger.error(e.getMessage(), e);
			return Response
					.status(400)
					.entity(new ErrorBean(400, "Cannot find VM " + vmId
							+ " associated with username " + userName)).build();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return Response.status(500)
					.entity(new ErrorBean(500, e.getMessage())).build();
		}
	}
}
