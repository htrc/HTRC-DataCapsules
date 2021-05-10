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

import edu.indiana.d2i.sloan.bean.*;
import edu.indiana.d2i.sloan.db.DBOperations;
import edu.indiana.d2i.sloan.exception.NoItemIsFoundInDBException;
import edu.indiana.d2i.sloan.utils.EmailUtil;
import edu.indiana.d2i.sloan.utils.RolePermissionUtils;
import edu.indiana.d2i.sloan.vm.VMRole;
import edu.indiana.d2i.sloan.vm.VMState;
import edu.indiana.d2i.sloan.vm.VMType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/addsharees")
public class AddVmSharees {
	private static Logger logger = LoggerFactory.getLogger(AddVmSharees.class);
	private static final String DELETE = "DELETE";

	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public Response addSharees(@FormParam("vmId") String vmId,
			@FormParam("sharees") String sharees,
			@FormParam("desc_shared") String desc_shared,
			@Context HttpHeaders httpHeaders,
			@Context HttpServletRequest httpServletRequest) {
		String userName = httpServletRequest.getHeader(Constants.USER_NAME);
		String userEmail = httpServletRequest.getHeader(Constants.USER_EMAIL);

		if (userName == null || userEmail == null) {
			logger.error("Username/E-mail is not present in http header.");
			return Response
					.status(400)
					.entity(new ErrorBean(400,
							"Username/E-mail is not present in http header.")).build();
		}

		Map<String, String> sharees_map = new HashMap<>();
		try {
			JSONArray sharees_array = new JSONArray(sharees);
			for(int i = 0 ; i < sharees_array.length() ; i++) {
				JSONObject sharee = sharees_array.getJSONObject(i);
				sharees_map.put(sharee.getString("guid"), sharee.getString("email"));
			}
		} catch (JSONException e) {
			logger.error("Invalid JSON array of JSON Objects to represent collaborators.");
			return Response.status(400).entity(new ErrorBean(400,
							"Invalid JSON array of JSON Objects to represent collaborators.")).build();
		}

		try {
			if (!RolePermissionUtils.isPermittedCommand(userName, vmId, RolePermissionUtils.API_CMD.ADD_SHAREES)) {
				return Response.status(400).entity(new ErrorBean(400,
						"User " + userName + " cannot add collaborators on VM " + vmId)).build();
			}

			VmInfoBean vmInfo = DBOperations.getInstance().getVmInfo(userName, vmId);

			// don't allow to add sharees if capsule is in delete* or error state
			if (vmInfo.getVmstate() == VMState.ERROR
					|| vmInfo.getVmstate().name().contains(DELETE)){
				return Response.status(Response.Status.BAD_REQUEST)
						.entity(new ErrorBean(400, "Cannot add collaborators when capsule is in "
								+ VMState.ERROR + " or " + DELETE + "* state!")).build();
			}

			if(vmInfo.getType().equals(VMType.DEMO.getName())) {
				return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorBean(400,
						"Cannot add collaborators to a " + VMType.DEMO.getName() +" capsule!")).build();
			}

			// cannot add sharees when the capsule's full access request is pending
			if(vmInfo.isFull_access() != null && vmInfo.isFull_access() == false) {
				return Response.status(400).entity(new ErrorBean(400,
						"Cannot add collaborators when capsule's full access request is pending!")).build();
			}

			if(vmInfo.isFull_access() != null && vmInfo.isFull_access() == true && desc_shared == null) {
				return Response.status(400).entity(new ErrorBean(400,
						"desc_shared should not be null when requesting full access for a "
								+ VMType.RESEARCH_FULL.getName() + " capsule!")).build();
			}
			desc_shared = vmInfo.isFull_access() != null && vmInfo.isFull_access() == true ? desc_shared : null;

			// set full_access of the sharees as null if not requested for full access already
			// set this to false if VM has requested full access
			Boolean full_access = vmInfo.isFull_access() == null ? null : false;

			logger.info("User " + userName + " tries to add " + sharees_map + " as collaborators for vm " + vmId);

			// do not allow to add more than MAX_NO_OF_SHAREES=5 sharees
			List<VmUserRole> current_roles = DBOperations.getInstance().getRolesWithVmid(vmId, true);
			int maxNoOfSharees = Configuration.getInstance().getInt(Configuration.PropertyName.MAX_NO_OF_SHAREES,Constants.DEFAULT_MAX_NO_OF_SHAREES);
			if(current_roles.size() + sharees_map.size() > maxNoOfSharees) {
				return Response.status(400).entity(new ErrorBean(400,
						"A Data Capsule cannot have more than " + maxNoOfSharees + " collaborators!")).build();
			}

			for(String guid : sharees_map.keySet()) { // for each user
				if(current_roles.stream().filter(
						role -> role.getGuid().equals(guid)).collect(Collectors.toList()).size() > 0) {
					logger.warn("Collaborator " + guid + " already exists in capsule " + vmId + "!");
					continue; // skip adding existing users
				}
				DBOperations.getInstance().insertUserIfNotExists(guid, sharees_map.get(guid));  // add to users table
				VmUserRole vmUserRole = new VmUserRole(sharees_map.get(guid), VMRole.SHAREE, false, guid, full_access);
				DBOperations.getInstance().addVmSharee(vmId, vmUserRole, desc_shared); // add to uservmmap table

				// send email to user if adding to a RESEARCH capsule
				if(vmInfo.isFull_access() == null) {
					EmailUtil email_util = new EmailUtil();
					String email_body = "Dear Data Capsule user,\n"
							+ "HTRC user with email " + userEmail + " has shared their Data Capusle(" + vmId + ") with you." +
							"\nYou will be able to access this Data Capsule once you accept the TOU agreement.";
					email_util.sendEMail(null,sharees_map.get(guid), "A HTRC Data Capsule Has Been Shared With You",
							email_body);
					logger.info("Email notification on shared capsule sent to " + sharees_map.get(guid));
				}
			}

			boolean pub_key_exists = DBOperations.getInstance().getUserPubKey(userName) == null ? false : true;
			boolean tou = DBOperations.getInstance().getUserTOU(userName);
			VmUserRole vmUserRole = DBOperations.getInstance().getUserRoleWithVmid(userName, vmId);
			List<VmStatusBean> status = new ArrayList<VmStatusBean>();
			vmInfo = DBOperations.getInstance().getVmInfo(userName, vmId);
			status.add(new VmStatusBean(vmInfo, pub_key_exists, tou, vmUserRole));

			// send vminfo back with added guids, AG then sends full_access request email containing all added users
			// if VM's full_access is true or false
			return Response.status(200).entity(new QueryVmResponseBean(status)).build();

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
