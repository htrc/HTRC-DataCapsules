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
import edu.indiana.d2i.sloan.hyper.DeletePublicKeyCommand;
import edu.indiana.d2i.sloan.hyper.DeleteVMCommand;
import edu.indiana.d2i.sloan.hyper.HypervisorProxy;
import edu.indiana.d2i.sloan.utils.RolePermissionUtils;
import edu.indiana.d2i.sloan.vm.VMRole;
import edu.indiana.d2i.sloan.vm.VMType;
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
import java.util.*;
import java.util.stream.Collectors;

import static edu.indiana.d2i.sloan.Constants.MAX_NO_OF_SHAREES;

@Path("/deletesharees")
public class DeleteVmSharees {
	private static Logger logger = Logger.getLogger(DeleteVmSharees.class);

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
		if (sharees == null) {
			return Response.status(400)
					.entity(new ErrorBean(400, "Invalid sharees input!")).build();
		}

		try {
			if (!RolePermissionUtils.isPermittedCommand(userName, vmId, RolePermissionUtils.API_CMD.DELETE_SHAREES)) {
				return Response.status(400).entity(new ErrorBean(400,
						"User " + userName + " cannot perform task "
								+ RolePermissionUtils.API_CMD.DELETE_SHAREES + " on VM " + vmId)).build();
			}

			VmInfoBean vmInfo = DBOperations.getInstance().getVmInfo(userName, vmId);

			logger.info("User " + userName + " tries to delete sharee/(s) " + sharees + " for vm " + vmId);

			List<String> sharees_list = Arrays.asList(sharees.split(","));

			// return error if any of the sharees is not a role in VM
			List<VmUserRole> vmUserRoles = DBOperations.getInstance().getRolesWithVmid(vmId, true);
			for(String sharee : sharees_list) {
				if( vmUserRoles.stream().filter(role ->
						role.getGuid().equals(sharee)).collect(Collectors.toList()).size() == 0 ) {
					return Response.status(400)
							.entity(new ErrorBean(400, "User " + sharee
									+ " is not a sharee of VM " + vmId)).build();
				}
			}

			// remove users from the Database.
			// This removes users from the users table too, if the user don't have other capsules owned
			DBOperations.getInstance().removeVmSharee(vmId, sharees_list);

			// remove users keys from the data capsule backend
			for(String sharee : sharees_list) {
				String pubKey = DBOperations.getInstance().getUserPubKey(sharee);
				HypervisorProxy.getInstance().addCommand(
						new DeletePublicKeyCommand(vmInfo, sharee, sharee, pubKey));
			}

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
