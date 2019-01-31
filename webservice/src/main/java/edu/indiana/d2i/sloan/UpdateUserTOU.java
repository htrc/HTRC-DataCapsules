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
import edu.indiana.d2i.sloan.hyper.UpdatePublicKeyCommand;
import edu.indiana.d2i.sloan.vm.VMMode;
import edu.indiana.d2i.sloan.vm.VMState;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Path("/updateusertou")
public class UpdateUserTOU {
	private static Logger logger = Logger.getLogger(UpdateUserTOU.class);

	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public Response updateUserTou(@FormParam("tou") boolean tou,
			@FormParam("vmId") String vmId,
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
			if(!DBOperations.getInstance().userExists(userName)) {
				logger.info("User '" + userName + "' is not in database, hence not updating TOU!");
				return Response.status(400).entity(new ErrorBean(400,
						"User '" + userName + "' is not in database, hence not updating TOU!")).build();
			}

			logger.info("User " + userName + " tries to update the TOU to " + tou);

			if(vmId != null) {
				DBOperations.getInstance().getVmInfo(userName, vmId); // check if user had VM with vmid
				DBOperations.getInstance().updateVmUserTOU(userName, vmId, tou); // update tou in uservmmap for vmid
			} else {
				DBOperations.getInstance().updateUserTOU(userName, tou);
				logger.info("TOU agreement of user '" + userName + "' was updated in database successfully!");
			}

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

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getUserTou(@Context HttpHeaders httpHeaders,
			@Context HttpServletRequest httpServletRequest) {
		String userName = httpServletRequest.getHeader(Constants.USER_NAME);

		if (userName == null) {
			logger.error("Username is not present in http header.");
			return Response.status(400).entity(new ErrorBean(400,
							"Username is not present in http header.")).build();
		}

		try {
			Boolean tou = DBOperations.getInstance().getUserTOU(userName);
			logger.debug("TOU agreement of user '" + userName + "' was retrieved successfully!");
			return Response.status(200).entity(new JSONObject().put("tou", tou)).build();
		} catch (NoItemIsFoundInDBException e) {
			logger.debug("User with username '" + userName + "' does not exist in DC-API database!");
			return Response.status(400).entity(new ErrorBean(400,
					"User with username '" + userName + "' does not exist in DC-API database!")).build();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return Response.status(500)
					.entity(new ErrorBean(500, e.getMessage())).build();
		}
	}
}
