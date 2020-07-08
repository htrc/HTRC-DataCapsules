/*******************************************************************************
 * Copyright 2017 The Trustees of Indiana University
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
import edu.indiana.d2i.sloan.exception.NoResultFileFoundException;
import edu.indiana.d2i.sloan.utils.ResultUtils;
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

@Path("/retrieveresultfile")
public class RetrieveResultFile {
	private static Logger logger = LoggerFactory.getLogger(RetrieveResultFile.class);

	@GET
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response retrieveResultFile(
			@QueryParam("randomid") String randomid,
			@QueryParam("filename") String filename,
			@Context HttpHeaders httpHeaders,
			@Context HttpServletRequest httpServletRequest)
	{

		if (randomid == null) {
			logger.error("Invalid download URL! randomid is null");
			return Response.status(400).entity("Invalid download URL!").build();
		}

		if (filename==null || filename.length()==0){
			filename = "result-" + randomid;
		}

		try {

			ResultBean result = DBOperations.getInstance().getResult(randomid);
			logger.info("Result with " + randomid + " is being downloaded.");

			return Response.ok(IOUtils.toByteArray(ResultUtils.getResultFile(randomid)))
					.type("application/zip")
					.header("Content-Disposition", "attachment; filename=\"" + filename + ".zip\"")
					.build();

		} catch (NoItemIsFoundInDBException | NoResultFileFoundException e) {
			logger.error("No Result with id " + randomid + " is found!", e);
			return Response.status(404).entity("No Result with id " + randomid + " is found!").build();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return Response.status(500).entity("Internal error - " + e.getMessage()).build();
		}
	}
}
