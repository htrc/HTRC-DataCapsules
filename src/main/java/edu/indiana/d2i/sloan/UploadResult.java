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

import java.io.InputStream;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;

import com.sun.jersey.multipart.FormDataParam;

import edu.indiana.d2i.sloan.bean.ErrorBean;
import edu.indiana.d2i.sloan.bean.UserBean;
import edu.indiana.d2i.sloan.db.DBOperations;
import edu.indiana.d2i.sloan.result.UploadPostprocess;

/**
 * For internal use only!
 *
 */
@Path("/upload")
public class UploadResult {
	private static Logger logger = Logger.getLogger(UploadResult.class);
	
	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response getResourcePost(
		@FormDataParam("vmid") String vmid, @FormDataParam("file") InputStream input,
		@Context HttpHeaders httpHeaders,
		@Context HttpServletRequest httpServletRequest) {
		if (vmid == null || input == null) {
			logger.error("vmid or input stream is null!");
			return Response.status(400).entity(
				new ErrorBean(400, "vmid and file cannot be empty!")).build();
		}
		
		try {
			// check upload file type ??			
			
			// check if vmid is associated with a user in uservm table
			UserBean userbean = DBOperations.getInstance().getUserWithVmid(vmid);	
			logger.info("Upload result for " + vmid + ", " + userbean);
			
			// write to DB
			String randomid = UUID.randomUUID().toString();
			DBOperations.getInstance().insertResult(vmid, randomid, input);
			
			// add to post-process
			UploadPostprocess.instance.addPostprocessingItem(userbean, randomid);
			
			return Response.status(200).build();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return Response.status(500)
					.entity(new ErrorBean(500, e.getMessage())).build();
		}		
	}
}
