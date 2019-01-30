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
import edu.indiana.d2i.sloan.hyper.HypervisorProxy;
import edu.indiana.d2i.sloan.hyper.UpdatePublicKeyCommand;
import edu.indiana.d2i.sloan.utils.RolePermissionUtils;
import edu.indiana.d2i.sloan.vm.VMMode;
import edu.indiana.d2i.sloan.vm.VMState;
import edu.indiana.d2i.sloan.vm.VMType;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

@Path("/updatevm")
public class UpdateVm {
	private static Logger logger = Logger.getLogger(UpdateVm.class);

	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public Response queryVMs(@FormParam("vmId") String vmId,
			@FormParam("type") String type,
			@FormParam("consent") Boolean consent,
			@FormParam("full_access") Boolean full_access,
			@FormParam("title") String title,
			@FormParam("desc_nature") String desc_nature,
			@FormParam("desc_requirement") String desc_requirement,
			@FormParam("desc_links") String desc_links,
			@FormParam("desc_outside_data") String desc_outside_data,
			@FormParam("rr_data_files") String rr_data_files,
			@FormParam("rr_result_usage") String rr_result_usage,
			@Context HttpHeaders httpHeaders,
			@Context HttpServletRequest httpServletRequest) {		
		String userName = httpServletRequest.getHeader(Constants.USER_NAME);
		/*String userEmail = httpServletRequest.getHeader(Constants.USER_EMAIL);
		if (userEmail == null) userEmail = "";

		String operator = httpServletRequest.getHeader(Constants.OPERATOR);
		String operatorEmail = httpServletRequest.getHeader(Constants.OPERATOR_EMAIL);
		if (operator == null) operator = userName;
		if (operatorEmail == null) operatorEmail = "";*/

		if (userName == null) {
			logger.error("Username is not present in http header.");
			return Response
					.status(400)
					.entity(new ErrorBean(400,
							"Username is not present in http header.")).build();
		}

		try {
			VmUserRole role = DBOperations.getInstance().getUserRoleWithVmid(userName, vmId);
			if (!RolePermissionUtils.isPermittedCommand(role.getRole(), RolePermissionUtils.API_CMD.UPDATE_VM)) {
				String msg = "User " + userName + " with role " + role.getRole() + " cannot perform task "
						+ RolePermissionUtils.API_CMD.UPDATE_VM + " on VM " + vmId;
				logger.error(msg);
				return Response.status(400).entity(new ErrorBean(400, msg)).build();
			}

			//DBOperations.getInstance().insertUserIfNotExists(userName, userEmail);
			//DBOperations.getInstance().insertUserIfNotExists(operator, operatorEmail);

			logger.info("User " + userName + " tries to update the VM");
			VmInfoBean vmInfo = DBOperations.getInstance().getVmInfo(userName, vmId);

			if(!vmInfo.getType().equals(VMType.RESEARCH.getName())) {
				return Response.status(Response.Status.BAD_REQUEST)
						.entity(new ErrorBean(400, "Only a " + VMType.RESEARCH.getName() +
								" capsule can be converted to a " + VMType.RESEARCH_FULL.getName() + " capsule!"))
						.build();
			}

			if(type.equals(VMType.RESEARCH_FULL.getName())) {
				if(vmInfo.isFull_access() == null) {
					return Response.status(Response.Status.BAD_REQUEST)
							.entity(new ErrorBean(400, "User has not requested full access for " +
									"this capsule!")).build();
				} else if(vmInfo.isFull_access() == true) {
					return Response.status(Response.Status.BAD_REQUEST)
							.entity(new ErrorBean(400, "This capsule is already a " +
									VMType.RESEARCH_FULL.getName() + " capsule!")).build();
				}

				if(full_access == true) {
					DBOperations.getInstance().updateVmType(vmId, type, full_access);
				} else {
					DBOperations.getInstance().updateVmType(vmId, VMType.RESEARCH.getName(), null);
				}

				logger.info("VM " + vmId + " of user '" + userName + "' was updated (type "
						+ type + ") in database successfully!");
				return Response.status(200).build();
			}

			if(!type.equals(VMType.RESEARCH.getName())) {
				return Response.status(Response.Status.BAD_REQUEST)
						.entity(new ErrorBean(400, "Invalid capsule conversion type : " + type))
						.build();
			}
			if(vmInfo.isFull_access()!= null && vmInfo.isFull_access() == false) {
				return Response.status(Response.Status.BAD_REQUEST)
						.entity(new ErrorBean(400, "You have already requested to convert this " +
								"capsule to a " + VMType.RESEARCH_FULL.getName() + " capsule!"))
						.build();
			}  else if(vmInfo.isFull_access()!= null && vmInfo.isFull_access() == true) {
				return Response.status(Response.Status.BAD_REQUEST)
						.entity(new ErrorBean(400, "This capsule is already a " +
								VMType.RESEARCH_FULL.getName() + " capsule!"))
						.build();
			} else if(!(vmInfo.getVmstate().equals(VMState.SHUTDOWN)) &&
					!(vmInfo.getVmstate().equals(VMState.RUNNING) && vmInfo.getVmmode().equals(VMMode.MAINTENANCE))) {
				return Response.status(Response.Status.BAD_REQUEST)
						.entity(new ErrorBean(400, "A Capsule can be converted to a " +
								VMType.RESEARCH_FULL.getName() + " Capsule " +
								"only when in \"" + VMState.SHUTDOWN + "\" state or in " +
								" \"" + VMState.RUNNING + "\" state and \"" + VMMode.MAINTENANCE + "\" mode. " +
								"Please make sure that the Capsule is in the " +
								"right mode/state before trying to convert to a " +
								VMType.RESEARCH_FULL.getName() + " capsule"))
						.build();
			}

			DBOperations.getInstance().updateVm(vmId, type, title, consent, desc_nature, desc_requirement, desc_links,
					desc_outside_data, rr_data_files, rr_result_usage, full_access);
			logger.info("VM " + vmId + " of user '" + userName + "' was updated (type "
					+ type + ") in database successfully!");

			return Response.status(200).build();
		} catch (NoItemIsFoundInDBException e) {
			logger.error(e.getMessage(), e);
			return Response
					.status(400)
					.entity(new ErrorBean(400, "VM " + vmId
							+ " is not associated with user " + userName))
					.build();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return Response.status(500)
					.entity(new ErrorBean(500, e.getMessage())).build();
		}
	}
}
