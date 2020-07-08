/*******************************************************************************
 * Copyright 2019 The Trustees of Indiana University
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
import edu.indiana.d2i.sloan.utils.RolePermissionUtils;
import edu.indiana.d2i.sloan.vm.VMRole;
import edu.indiana.d2i.sloan.vm.VMState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;

@Path("/managecontroller")
public class ManageController {
	private static Logger logger = LoggerFactory.getLogger(ManageController.class);
	private static final String DELETE = "DELETE";

	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public Response manageControllerRole(@FormParam("vmId") String vmId,
			@FormParam("controller") String controller,
			@FormParam("action") String action,
			@Context HttpHeaders httpHeaders,
			@Context HttpServletRequest httpServletRequest) {		
		String userName = httpServletRequest.getHeader(Constants.USER_NAME);

		if (userName == null || controller == null) {
			logger.error("Username/Controller is not present in http header.");
			return Response.status(400).entity(new ErrorBean(400,
							"Username/Controller is not present in http header.")).build();
		}

		if(action == null || (!action.equals(RolePermissionUtils.CNTR_ACTION.REVOKE.getName())
				&& !action.equals(RolePermissionUtils.CNTR_ACTION.DELEGATE.getName()))) {
			String message = "Action should be either '" + RolePermissionUtils.CNTR_ACTION.DELEGATE + "' or '" +
					RolePermissionUtils.CNTR_ACTION.REVOKE + "'!";
			logger.error(message);
			return Response.status(400).entity(new ErrorBean(400, message)).build();
		}

		try {
			VmInfoBean vmInfo = DBOperations.getInstance().getVmInfo(userName, vmId);

			// don't allow to manage controller if capsule is in delete* or error state
			if (vmInfo.getVmstate() == VMState.ERROR
					|| vmInfo.getVmstate().name().contains(DELETE)){
				return Response.status(Response.Status.BAD_REQUEST)
						.entity(new ErrorBean(400, "Cannot manage controller when capsule is in "
								+ VMState.ERROR + " or " + DELETE + "* state!")).build();
			}

			VmUserRole owner_role = DBOperations.getInstance().getUserRoleWithVmid(userName, vmId);
			VmUserRole controller_role = DBOperations.getInstance().getUserRoleWithVmid(controller, vmId);

			Map<VMRole, VMRole> roleMap = RolePermissionUtils.getValidCntrlAction(vmId, owner_role, controller_role,
					RolePermissionUtils.CNTR_ACTION.valueOf(action));
			if (roleMap == null) {
				String message = "User " + userName + " cannot " + action.toLowerCase() + " control on collaborator "
						+ controller + " in VM " + vmId;
				logger.error(message);
				return Response.status(400).entity(new ErrorBean(400, message)).build();
			}

			VMRole owner_target_role = roleMap.keySet().stream().findFirst().get();
			VMRole sharee_target_role = roleMap.get(owner_target_role);

			logger.info("User " + userName + "("+ owner_role.getRole()+") is trying to "
					+ action + " controller role on " + controller + "("+controller_role.getRole()+"->"
					+ sharee_target_role+") in VM " + vmId + " and become " + owner_target_role);

			DBOperations.getInstance().manageController(vmId, userName, owner_target_role, controller
					, sharee_target_role);

			return Response.status(200).build();
		} catch (NoItemIsFoundInDBException e) {
			logger.error(e.getMessage(), e);
			return Response
					.status(400)
					.entity(new ErrorBean(400, "Cannot find a VM " + vmId
							+ " associated with username " + userName)).build();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return Response.status(500)
					.entity(new ErrorBean(500, e.getMessage())).build();
		}
	}
}
