/*******************************************************************************
 * Copyright 2014 The Trustees of Indiana University
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
import edu.indiana.d2i.sloan.bean.ResultBean;
import edu.indiana.d2i.sloan.bean.ResultInfoBean;
import edu.indiana.d2i.sloan.bean.ResultInfoResponseBean;
import edu.indiana.d2i.sloan.db.DBOperations;
import edu.indiana.d2i.sloan.exception.NoItemIsFoundInDBException;
import edu.indiana.d2i.sloan.exception.ResultExpireException;
import edu.indiana.d2i.sloan.utils.ResultUtils;
import edu.indiana.d2i.sloan.utils.RolePermissionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

/**
 * param:
 *   resultid: identifier of a specific entry
 *
 *
 *   return: data field of the given resultid
 */

@Path("/getvmresults")
public class GetVMResults {
	private static Logger logger = Logger.getLogger(GetVMResults.class);

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response downloadResult(
			@QueryParam("vmid") String vmid,
			@Context HttpHeaders httpHeaders,
			@Context HttpServletRequest httpServletRequest)
	{
		String userName = httpServletRequest.getHeader(Constants.USER_NAME);
		if (userName == null) {
			logger.error("Username is not present in http header.");
			return Response
					.status(500)
					.entity(new ErrorBean(500,
							"Username is not present in http header.")).build();
		}

		if (vmid == null) {
			logger.error("VM ID is not present in http header.");
			return Response.status(400).entity( "VM ID is not present in http header!").build();
		}

		try {
			//check if this user has permission to view results
			if (!RolePermissionUtils.isPermittedCommand(userName, vmid, RolePermissionUtils.API_CMD.VIEW_RESULT)) {
				String errorMsg = "User " + userName + " is not permitted to view results in VM " + vmid + "!";
				logger.warn(errorMsg);
				return Response.status(400).entity(new ErrorBean(400, errorMsg)).build();
			}

			List<ResultInfoBean> res = DBOperations.getInstance().getVMResults(vmid);
			return Response.status(200).entity(new ResultInfoResponseBean(res)).build();

		} catch (NoItemIsFoundInDBException e) {
			logger.error("Cannot find VM " + vmid + " with username " + userName, e);
			return Response.status(404).entity("Cannot find VM " + vmid + " with username " + userName).build();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return Response.status(500).entity("Internal error - " + e.getMessage()).build();
		}
	}

}
