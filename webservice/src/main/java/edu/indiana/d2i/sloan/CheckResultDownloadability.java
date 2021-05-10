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
import edu.indiana.d2i.sloan.db.DBOperations;
import edu.indiana.d2i.sloan.exception.NoItemIsFoundInDBException;
import edu.indiana.d2i.sloan.exception.ResultExpireException;
import edu.indiana.d2i.sloan.utils.ResultUtils;
import edu.indiana.d2i.sloan.utils.RolePermissionUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * param:
 *   resultid: identifier of a specific entry
 *
 *
 *   return: data field of the given resultid
 */

@Path("/checkdownloadability")
public class CheckResultDownloadability {
	private static Logger logger = LoggerFactory.getLogger(CheckResultDownloadability.class);

	@GET
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response CheckResultDownloadability(
			@QueryParam("randomid") String randomid,
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

		if (randomid == null) {
			logger.error("Invalid download URL! randomid is null");
			return Response.status(400).entity( "Invalid download URL!").build();
		}

		try {
			ResultInfoBean resultInfo = DBOperations.getInstance().getResultInfo(randomid);

			//check if this user has permission to view results
			if (!RolePermissionUtils.isPermittedCommand(userName, resultInfo.getVmid(), RolePermissionUtils.API_CMD.VIEW_RESULT)) {
				String errorMsg = "User " + userName + " is not permitted to download result " + randomid + " in VM "
						+ resultInfo.getVmid() + "!";
				logger.warn(errorMsg);
				throw new NoItemIsFoundInDBException(errorMsg);
			}

			//check if result is released
			String currStatus = resultInfo.getStatus();
			if (currStatus.equals("Pending") || currStatus.equals("Rejected")) {
				return Response.status(400)
						.entity( "Cannot download results! Result with id " + randomid + " is " + currStatus).build();
			}

			// check if result expires
			ResultBean result = DBOperations.getInstance().getResult(randomid);
			long currentT = new java.util.Date().getTime();
			long startT = result.getStartdate().getTime();
			long span = Configuration.getInstance().getLong(
					Configuration.PropertyName.RESULT_EXPIRE_IN_SECOND);
			if (span > 0 && ((currentT-startT)/1000) > span)
				throw new ResultExpireException(randomid + " has been expired!");

			logger.info("Result with " + randomid + " is being downloaded.");
			return Response.status(200).build();

		} catch (NoItemIsFoundInDBException e) {
			logger.error("No Result with id " + randomid + " is found for the user " + userName + " !", e);
			return Response.status(404).entity("No Result with id " + randomid + " is found for the user!").build();
		} catch (ResultExpireException e) {
			logger.error(e.getMessage(), e);
			return Response.status(400).entity("Result with id " + randomid + " has been expired!").build();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return Response.status(500).entity("Internal error - " + e.getMessage()).build();
		}
	}

}
