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
import edu.indiana.d2i.sloan.vm.VMMode;
import edu.indiana.d2i.sloan.vm.VMRole;
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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Path("/updatevm")
public class UpdateVm {
	private static Logger logger = Logger.getLogger(UpdateVm.class);
	private static final String DELETE = "DELETE";

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
			@FormParam("guids") String guids,
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
			if (!RolePermissionUtils.isPermittedCommand(userName, vmId, RolePermissionUtils.API_CMD.UPDATE_VM)) {
				return Response.status(400).entity(new ErrorBean(400,
						"User " + userName + " cannot perform task "
								+ RolePermissionUtils.API_CMD.UPDATE_VM + " on VM " + vmId)).build();
			}

			logger.info("User " + userName + " tries to update the VM");
			VmInfoBean vmInfo = DBOperations.getInstance().getVmInfo(userName, vmId);

			// If Full_access request is processed (granted or rejected)
			if(type.equals(VMType.RESEARCH_FULL.getName())) {

				// fails if the owner's full_access is null, means full_access is not requested or already rejected
				if(vmInfo.isFull_access() == null) {
					return Response.status(Response.Status.BAD_REQUEST)
							.entity(new ErrorBean(400, "User has not requested full access for " +
									"this capsule, or the full access request has already been rejected!")).build();
				}

				// capsule should be RESEARCH if owner's full_access = false and should be RESEARCH_FULL if its true
				if(!(vmInfo.getType().equals(VMType.RESEARCH.getName())  && !vmInfo.isFull_access())
					&& !(vmInfo.getType().equals(VMType.RESEARCH_FULL.getName())  && vmInfo.isFull_access())) {
					return Response.status(Response.Status.BAD_REQUEST)
							.entity(new ErrorBean(400, "Invalid capsule conversion when VM type is "
								+ vmInfo.getType() + " and owner's full access permission is " + vmInfo.isFull_access()))
							.build();
				}

				// don't allow to process full_access if capsule is in delete* or error state
				if (vmInfo.getVmstate() == VMState.ERROR
						|| vmInfo.getVmstate().name().contains(DELETE)){
					return Response.status(Response.Status.BAD_REQUEST)
							.entity(new ErrorBean(400, "Cannot request/grant full access for a capsule which is in "
									+ VMState.ERROR + " or " + DELETE + "* state!"))
							.build();
				}

				List<VmUserRole> vmUserRoles = DBOperations.getInstance().getRolesWithVmid(vmId, true);
				List<String> guid_list = vmUserRoles.stream().map(role -> role.getGuid()).collect(Collectors.toList());
				VmUserRole owner = vmUserRoles.stream()
						.filter(role -> role.getRole().equals(VMRole.OWNER_CONTROLLER) || role.getRole().equals(VMRole.OWNER))
						.collect(Collectors.toList()).get(0);

				// initial full access request shoudn't have a list and subsequent requests should have a list
				if((vmInfo.isFull_access() == false && guids != null)
						|| (vmInfo.isFull_access() == true && guids == null)) {
					return Response.status(Response.Status.BAD_REQUEST)
							.entity(new ErrorBean(400, "Initial full access processing requests should not " +
							"have a list of GUIDs and the subsequent full access processing requests must specify a list " +
							"of GUIDs!")).build();
				}

				// if full_access is granted/rejected to particular users, then update full_access only for them
				List<String> sub_list = null;
				if( guids != null ) {
					sub_list = Arrays.asList(guids.split(","));
					// if the list for subsequent full access processing request containing owners guid, and its not
					// granted, then full access will be denied for all sharees
					if(!sub_list.contains(owner.getGuid()) || full_access == true) {
						guid_list = sub_list;
					}
				}

				if(full_access == true) {
					//update vmtype to RESEARCH-FULL and set full_access=true
					DBOperations.getInstance().updateVmType(vmId, type, full_access, guid_list);
					logger.info("Users " + guid_list + " were accepted for full access in VM " + vmId);
				} else if (full_access == false && vmInfo.isFull_access() == true
						&& !sub_list.contains(owner.getGuid())) {
					//remove sharee from VM
					DBOperations.getInstance().removeVmSharee(vmId, guid_list);
					logger.info("Users " + guid_list + " were removed from VM " + vmId);
					// TODO-UN send notification to the owner
				} else {
					//update vmtype to RESEARCH and set full_access=null
					DBOperations.getInstance().updateVmType(vmId, VMType.RESEARCH.getName(), null, guid_list);
					logger.info("Users " + guid_list + " were rejected for full access in VM " + vmId);
				}

				logger.info("VM " + vmId + " of user '" + userName + "' was updated (type "
						+ type + ") in database successfully!");


				// If VM was already RESEARCH-FULL, sending public keys of users to the hypervisor whose tou is accepted
				// and has a publik key and:
				// 			- full_access is accepted
				// 			- all the users if owner's full access was also rejected this time
				if(vmInfo.isFull_access() == true) {
					List<String> user_keys = new ArrayList<>();
					for (String guid : guid_list) {
						if (RolePermissionUtils.isPermittedToUpdateKey(
										guid, vmInfo, RolePermissionUtils.API_CMD.UPDATE_SSH_KEY)) {
							user_keys.add(DBOperations.getInstance().getUserPubKey(guid));
						}
					}
					if (user_keys.size() != 0)
						HypervisorProxy.getInstance().addCommand(
							new AddVmShareesCommand(vmInfo, userName, userName, user_keys));
				}

				return Response.status(200).build();
			}

			if(!type.equals(VMType.RESEARCH.getName()) || !vmInfo.getType().equals(VMType.RESEARCH.getName())) {
				return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorBean(400,
						"Invalid capsule conversion type : " + vmInfo.getType() + " to " + type)).build();
			}

			// Processing full_access request from AG
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

			// When requesting full access full_access is set to false for all users
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
