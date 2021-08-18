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
import edu.indiana.d2i.sloan.db.DBOperations;
import edu.indiana.d2i.sloan.exception.NoItemIsFoundInDBException;
import edu.indiana.d2i.sloan.hyper.HypervisorProxy;
import edu.indiana.d2i.sloan.hyper.UpdateCustosCredsCommand;
import edu.indiana.d2i.sloan.utils.RolePermissionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/updatecustoscreds")
public class UpdateCustosCreds {
	private static Logger logger = LoggerFactory.getLogger(UpdateCustosCreds.class);

	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public Response updateCustosCredentials(@FormParam("vmid") String vmid,
			@FormParam("custos_client_id") String custos_client_id, @FormParam("custos_client_secret") String custos_client_secret,
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

		if (vmid == null) {
			logger.error("VM ID is not present in the request.");
			return Response.status(400).entity(new ErrorBean(400,
							"VM ID is not present in the request.")).build();
		}

		try {
			logger.info("User " + userName + " tries to update custos credentials in VM " + vmid);
			VmInfoBean vminfo = DBOperations.getInstance().getVmInfo(userName, vmid); // check if user had VM with vmid
			DBOperations.getInstance().updateCustosCredentials(vmid, custos_client_id, custos_client_secret);
			logger.info("Custos credentials of VM '" + vmid + "' was updated in database successfully!");
			if (RolePermissionUtils.isPermittedToUpdateKey(
					userName, vminfo, RolePermissionUtils.API_CMD.UPDATE_CUSTOS_CREDS)) {
				HypervisorProxy.getInstance().addCommand(
						new UpdateCustosCredsCommand(vminfo, userName, custos_client_id, custos_client_secret));
			}
			return Response.status(200).build();
		} catch (NoItemIsFoundInDBException e) {
			logger.error(e.getMessage(), e);
			return Response
					.status(400)
					.entity(new ErrorBean(400, "Cannot find VM " + vmid
							+ " associated with username " + userName)).build();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return Response.status(500)
					.entity(new ErrorBean(500, e.getMessage())).build();
		}
	}
}
