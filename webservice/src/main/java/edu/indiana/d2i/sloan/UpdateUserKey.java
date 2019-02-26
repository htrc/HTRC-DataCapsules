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
import edu.indiana.d2i.sloan.exception.InvalidHostNameException;
import edu.indiana.d2i.sloan.exception.NoItemIsFoundInDBException;
import edu.indiana.d2i.sloan.hyper.HypervisorProxy;
import edu.indiana.d2i.sloan.hyper.QueryVMCommand;
import edu.indiana.d2i.sloan.hyper.UpdatePublicKeyCommand;
import edu.indiana.d2i.sloan.utils.RolePermissionUtils;
import edu.indiana.d2i.sloan.vm.*;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

@Path("/updateuserkey")
public class UpdateUserKey {
	private static Logger logger = Logger.getLogger(UpdateUserKey.class);

	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public Response updateUserKey(@FormParam("pubkey") String pubkey,
			@Context HttpHeaders httpHeaders,
			@Context HttpServletRequest httpServletRequest) {		
		String userName = httpServletRequest.getHeader(Constants.USER_NAME);
		//String userEmail = httpServletRequest.getHeader(Constants.USER_EMAIL);
		//if (userEmail == null) userEmail = "";

		//String operator = httpServletRequest.getHeader(Constants.OPERATOR);
		//String operatorEmail = httpServletRequest.getHeader(Constants.OPERATOR_EMAIL);
		//if (operator == null) operator = userName;
		//if (operatorEmail == null) operatorEmail = "";

		if (userName == null) {
			logger.error("Username is not present in http header.");
			return Response
					.status(400)
					.entity(new ErrorBean(400,
							"Username is not present in http header.")).build();
		}

		try {
			//DBOperations.getInstance().insertUserIfNotExists(userName, userEmail);
			//DBOperations.getInstance().insertUserIfNotExists(operator, operatorEmail);

			logger.info("User " + userName + " tries to update the public key");
			if(DBOperations.getInstance().userExists(userName)) {
				DBOperations.getInstance().updateUserPubKey(userName, pubkey);
				logger.info("Public key of user '" + userName + "' was updated in database successfully!");

				List<VmInfoBean> vmInfoList = new ArrayList<VmInfoBean>();
				vmInfoList = DBOperations.getInstance().getVmInfo(userName);

				for (VmInfoBean vminfo : vmInfoList) {
					if (RolePermissionUtils.isPermittedToUpdateKey(
									userName, vminfo, RolePermissionUtils.API_CMD.UPDATE_SSH_KEY)) {
						HypervisorProxy.getInstance().addCommand(
								new UpdatePublicKeyCommand(vminfo, userName, userName, pubkey));
					}
				}
				return Response.status(200).build();
			} else {
				logger.info("User '" + userName + "' is not in database, hence not updating public key!");
				return Response
						.status(400)
						.entity(new ErrorBean(400,
								"Username '" + userName + "' does not have any capsules created. Public key " +
										"can be added/updated only if the user has created capsules previously.")).build();
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return Response.status(500)
					.entity(new ErrorBean(500, e.getMessage())).build();
		}
	}


	@PUT
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public Response updateUserKeyUtil(@FormParam("vmid") String vmid,
							  @Context HttpHeaders httpHeaders,
							  @Context HttpServletRequest httpServletRequest) {

		try {
			List<VmInfoBean> vmInfoBeans = new ArrayList<VmInfoBean>();
			if(vmid != null ) {
				vmInfoBeans.add(DBOperations.getInstance().getAllVmInfoByID(vmid));
			} else {
				vmInfoBeans = DBOperations.getInstance().getAllVmInfo();
			}

			//Update the VMs with the public keys of all users
			for(VmInfoBean vmInfoBean : vmInfoBeans) {
				String vmId = vmInfoBean.getVmid();
				List<VmUserRole> roles = DBOperations.getInstance().getRolesWithVmid(vmId, true);
				//iterate all the roles
				for(VmUserRole vmUserRole : roles) {
					String guid = vmUserRole.getGuid();
					String pubkey = DBOperations.getInstance().getUserPubKey(guid);
					//if role has a public key in database
					if (pubkey != null) {
						//if user is allowed to update ssh key in VM
						if (RolePermissionUtils.isPermittedCommand(guid, vmId,
								RolePermissionUtils.API_CMD.UPDATE_SSH_KEY)) {
							// add user's public key to VM
							logger.info("Public key of user with GUID " + guid + " is going to be added to VM " + vmId);
							HypervisorProxy.getInstance().addCommand(
									new UpdatePublicKeyCommand(vmInfoBean, guid, guid, pubkey));
						}
					}
				}

			}

			return Response.status(200).build();
		} catch (NoItemIsFoundInDBException e) {
			logger.error(e.getMessage(), e);
			return Response.status(400)
					.entity(new ErrorBean(400, "VM with " + vmid + " cannot be found")).build();
		}  catch (Exception e) {
			logger.error(e.getMessage(), e);
			return Response.status(500)
					.entity(new ErrorBean(500, e.getMessage())).build();
		}

	}
}
