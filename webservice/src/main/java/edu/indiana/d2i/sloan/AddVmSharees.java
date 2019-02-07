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
import edu.indiana.d2i.sloan.utils.RolePermissionUtils;
import edu.indiana.d2i.sloan.vm.VMRole;
import org.apache.log4j.Logger;
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

@Path("/addsharees")
public class AddVmSharees {
	private static Logger logger = Logger.getLogger(AddVmSharees.class);

	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public Response addSharees(@FormParam("vmId") String vmId,
			@FormParam("sharees") String sharees,
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

		Map<String, String> sharees_map = new HashMap<>();
		try {
			JSONArray sharees_array = new JSONArray(sharees);
			for(int i = 0 ; i < sharees_array.length() ; i++) {
				JSONObject sharee = sharees_array.getJSONObject(i);
				sharees_map.put(sharee.getString("guid"), sharee.getString("email"));
			}
		} catch (JSONException e) {
			logger.error("Invalid JSON array of JSON Objects to represent sharees.");
			return Response.status(400).entity(new ErrorBean(400,
							"Invalid JSON array of JSON Objects to represent sharees.")).build();
		}

		try {
			if (!RolePermissionUtils.isPermittedCommand(userName, vmId, RolePermissionUtils.API_CMD.ADD_SHAREES)) {
				return Response.status(400).entity(new ErrorBean(400,
						"User " + userName + " cannot perform task "
								+ RolePermissionUtils.API_CMD.ADD_SHAREES + " on VM " + vmId)).build();
			}

			VmInfoBean vmInfo = DBOperations.getInstance().getVmInfo(userName, vmId);

			// cannot add sharees when the capsule's full access request is pending
			if(vmInfo.isFull_access() != null && vmInfo.isFull_access() == false) {
				return Response.status(400).entity(new ErrorBean(400,
						"Cannot add sharees when capsule's full access request is pending!" + vmId)).build();
			}

			// set full_access of the sharees as null if not requested for full access already
			// set this to false if VM has requested full access
			Boolean full_access = vmInfo.isFull_access() == null ? null : false;

			logger.info("User " + userName + " tries to add " + sharees_map + " as sharees for vm " + vmId);

			for(String guid : sharees_map.keySet()) { // for each user
				DBOperations.getInstance().insertUserIfNotExists(guid, sharees_map.get(guid));  // add to users table
				VmUserRole vmUserRole = new VmUserRole(sharees_map.get(guid), VMRole.SHAREE, false, guid, full_access);
				DBOperations.getInstance().addVmSharee(vmId, vmUserRole); // add to uservmmap table
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
					.entity(new ErrorBean(400, "Cannot find VM " + vmId
							+ " associated with username " + userName)).build();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return Response.status(500)
					.entity(new ErrorBean(500, e.getMessage())).build();
		}
	}
}
