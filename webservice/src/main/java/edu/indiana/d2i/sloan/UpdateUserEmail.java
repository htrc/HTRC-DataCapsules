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
import edu.indiana.d2i.sloan.db.DBOperations;
import org.apache.log4j.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/updateuseremail")
public class UpdateUserEmail {
	private static Logger logger = Logger.getLogger(UpdateUserEmail.class);

	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public Response updateUserEmail(@Context HttpHeaders httpHeaders, @Context HttpServletRequest httpServletRequest) {
		String userName = httpServletRequest.getHeader(Constants.USER_NAME);
		String userEmail = httpServletRequest.getHeader(Constants.USER_EMAIL);
		if (userEmail == null) {
			logger.error("Email is not present in http header.");
			return Response
					.status(400)
					.entity(new ErrorBean(400,
							"User's email is not present in http header.")).build();
		}

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

			logger.info("User " + userName + " tries to update the email to " + userEmail);
			if(DBOperations.getInstance().userExists(userName)) {
				DBOperations.getInstance().updateUserEmail(userName, userEmail);
				logger.info("Email of user '" + userName + "' was updated in database successfully!");
                return Response.status(200).build();
			} else {
				logger.info("User '" + userName + "' is not in database, hence not updating email!");
                return Response
                        .status(400)
                        .entity(new ErrorBean(400,
                                "User '" + userName + "' is not in database, hence not updating email!")).build();
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return Response.status(500)
					.entity(new ErrorBean(500, e.getMessage())).build();
		}
	}
}
