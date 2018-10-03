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

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;

import edu.indiana.d2i.sloan.bean.ErrorBean;
import edu.indiana.d2i.sloan.bean.VmInfoBean;
import edu.indiana.d2i.sloan.db.DBOperations;
import edu.indiana.d2i.sloan.exception.NoItemIsFoundInDBException;
import edu.indiana.d2i.sloan.hyper.HypervisorProxy;
import edu.indiana.d2i.sloan.hyper.LaunchVMCommand;
import edu.indiana.d2i.sloan.vm.VMMode;
import edu.indiana.d2i.sloan.vm.VMState;
import edu.indiana.d2i.sloan.vm.VMStateManager;

@Path("/launchvm")
public class LaunchVM {
	private static Logger logger = Logger.getLogger(LaunchVM.class);

	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public Response launchVM(
			@FormParam("vmid") String vmid,
			@Context HttpHeaders httpHeaders,
			@Context HttpServletRequest httpServletRequest) {
		String userName = httpServletRequest.getHeader(Constants.USER_NAME);
		/*String userEmail = httpServletRequest.getHeader(Constants.USER_EMAIL);
		if (userEmail == null) userEmail = "";

		String operator = httpServletRequest.getHeader(Constants.OPERATOR);
		String operatorEmail = httpServletRequest.getHeader(Constants.OPERATOR_EMAIL);
		if (operator == null) operator = userName;
		if (operatorEmail == null) operatorEmail = "";*/

		if (userName == null) {
			logger.error("Username is not present in http header.");
			return Response
					.status(500)
					.entity(new ErrorBean(500,
							"Username is not present in http header.")).build();
		}
		
		if (vmid == null) {
			return Response
				.status(400)
				.entity(new ErrorBean(400,
					"vmid cannot be empty!"))
				.build();
		}		
		
		// how to avoid multiple launching requests for the same vm
		// check and update?
		
		// launch can only start from shutdown
		try {		
			//DBOperations.getInstance().insertUserIfNotExists(userName, userEmail);
			//DBOperations.getInstance().insertUserIfNotExists(operator, operatorEmail);

			String pubkey = "";
			if(DBOperations.getInstance().getUserPubKey(userName) != null ) {
				pubkey = DBOperations.getInstance().getUserPubKey(userName);
			}

			VmInfoBean vmInfo = DBOperations.getInstance().getVmInfo(userName, vmid);
			if (VMStateManager.isPendingState(vmInfo.getVmstate()) ||
				!VMStateManager.getInstance().transitTo(vmid, 
				vmInfo.getVmstate(), VMState.LAUNCH_PENDING, userName)) {
				return Response
					.status(400)
					.entity(new ErrorBean(400,
						"Cannot launch VM " + vmid + " when it is " + vmInfo.getVmstate()))
					.build();
			}
			
			logger.info("User " + userName + " tries to launch VM " + vmInfo.getVmid());
			
			// nonblocking call to hypervisor
			vmInfo.setVmState(VMState.LAUNCH_PENDING);
			vmInfo.setRequestedVMMode(VMMode.MAINTENANCE);
			vmInfo.setPolicypath(Configuration.getInstance().getString(
				Configuration.PropertyName.MAINTENANCE_FIREWALL_POLICY));
			
			HypervisorProxy.getInstance().addCommand(new LaunchVMCommand(vmInfo, userName, pubkey));

			return Response.status(200).build();
		} catch (NoItemIsFoundInDBException e) {
			logger.error(e.getMessage(), e);
			return Response
					.status(400)
					.entity(new ErrorBean(400,
							"VM " + vmid + " is not associated with user " + userName))
					.build();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return Response.status(500)
					.entity(new ErrorBean(500, e.getMessage())).build();
		}
	}
}
