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
import edu.indiana.d2i.sloan.hyper.HypervisorProxy;
import edu.indiana.d2i.sloan.hyper.UpdatePublicKeyCommand;
import edu.indiana.d2i.sloan.vm.VMMode;
import edu.indiana.d2i.sloan.vm.VMState;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

@Path("/updatevmtype")
public class UpdateVmType {
	private static Logger logger = Logger.getLogger(UpdateVmType.class);

	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public Response queryVMs(@FormParam("type") String type, @FormParam("vmId") String vmId,
			@Context HttpHeaders httpHeaders,
			@Context HttpServletRequest httpServletRequest) {		
		String userName = httpServletRequest.getHeader(Constants.USER_NAME);
		String userEmail = httpServletRequest.getHeader(Constants.USER_EMAIL);
		if (userEmail == null) userEmail = "";

		String operator = httpServletRequest.getHeader(Constants.OPERATOR);
		String operatorEmail = httpServletRequest.getHeader(Constants.OPERATOR_EMAIL);
		if (operator == null) operator = userName;
		if (operatorEmail == null) operatorEmail = "";

		if (userName == null) {
			logger.error("Username is not present in http header.");
			return Response
					.status(400)
					.entity(new ErrorBean(400,
							"Username is not present in http header.")).build();
		}

		try {
			DBOperations.getInstance().insertUserIfNotExists(userName, userEmail);
			DBOperations.getInstance().insertUserIfNotExists(operator, operatorEmail);

			logger.info("User " + userName + " tries to update the VM type");

			DBOperations.getInstance().updateVmType(userName, vmId, type);
			logger.info("Type of the VM " + vmId + " of user '" + userName + "' was updated to type "
					+ type + " in database successfully!");

			return Response.status(200).build();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return Response.status(500)
					.entity(new ErrorBean(500, e.getMessage())).build();
		}
	}
}