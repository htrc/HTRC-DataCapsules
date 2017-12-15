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

import edu.indiana.d2i.sloan.bean.ResultBean;
import edu.indiana.d2i.sloan.db.DBOperations;
import edu.indiana.d2i.sloan.exception.NoItemIsFoundInDBException;
import edu.indiana.d2i.sloan.exception.ResultExpireException;
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

/**
 * param:
 *   resultid: identifier of a specific entry
 *
 *
 *   return: data field of the given resultid
 */

@Path("/download")
public class DownloadResult {
	private static Logger logger = Logger.getLogger(DownloadResult.class);

	@GET
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response downloadResult(
			@QueryParam("randomid") String randomid,
			@QueryParam("filename") String filename,
			@Context HttpHeaders httpHeaders,
			@Context HttpServletRequest httpServletRequest)
	{


		if (randomid == null) {
			logger.error("Invalid download URL! randomid is null");
			return Response.status(400).entity( "Invalid download URL!").build();
		}

		if (filename==null || filename.length()==0){
			filename = String.format("result-", randomid, ".txt");
		}

		try {
			String currStatus = DBOperations.getInstance().getStatus(randomid);
			if (currStatus.equals("Pending") || currStatus.equals("Rejected")) {
				return Response.status(400)
						.entity( "Cannot download results! Result with id " + randomid + " is " + currStatus).build();
			}

			ResultBean result = DBOperations.getInstance().getResult(randomid);

			// check if result expires
			long currentT = new java.util.Date().getTime();
			long startT = result.getStartdate().getTime();
			long span = Configuration.getInstance().getLong(
					Configuration.PropertyName.RESULT_EXPIRE_IN_SECOND);
			if (span > 0 && ((currentT-startT)/1000) > span)
				throw new ResultExpireException(randomid + " has been expired!");

			logger.info("Result with " + randomid + " is being downloaded.");
			return Response.ok(IOUtils.toByteArray(result.getInputstream()))
					.type("application/zip")
					.header("Content-Disposition", "attachment; filename=\"" + filename + ".zip\"")
					.build();

		} catch (NoItemIsFoundInDBException e) {
			logger.error("No Result with id " + randomid + " is found!", e);
			return Response.status(404).entity("No Result with id " + randomid + " is found!").build();
		} catch (ResultExpireException e) {
			logger.error(e.getMessage(), e);
			return Response.status(400).entity("Result with id " + randomid + " has been expired!").build();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return Response.status(500).entity("Internal error - " + e.getMessage()).build();
		}
	}

}
