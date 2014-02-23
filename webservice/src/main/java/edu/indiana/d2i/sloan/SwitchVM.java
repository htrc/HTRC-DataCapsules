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
import edu.indiana.d2i.sloan.hyper.SwitchVMCommand;
import edu.indiana.d2i.sloan.vm.VMMode;
import edu.indiana.d2i.sloan.vm.VMState;
import edu.indiana.d2i.sloan.vm.VMStateManager;

@Path("/switchvm")
public class SwitchVM {
	private static Logger logger = Logger.getLogger(SwitchVM.class);

	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public Response getResourcePost(
			@FormParam("vmid") String vmid,
			@FormParam("mode") String mode, 
			@Context HttpHeaders httpHeaders,
			@Context HttpServletRequest httpServletRequest) {
		String userName = httpServletRequest.getHeader(Constants.USER_NAME);
		String userEmail = httpServletRequest.getHeader(Constants.USER_EMAIL);
		if (userEmail == null) userEmail = "";

		// check if username exists
		if (userName == null) {
			logger.error("Username is not present in http header.");
			return Response
					.status(500)
					.entity(new ErrorBean(500,
							"Username is not present in http header.")).build();
		}
		
		if (vmid == null) {
			return Response.status(400)
					.entity(new ErrorBean(400, "VM id cannot be empty!"))
					.build();
		}

		// check if mode is valid, return 400 error immediately if invalid
		VMMode target = toVMMode(mode);
		if (target == null) {
			return Response
					.status(400)
					.entity(new ErrorBean(
							400,
							String.format(
									"Input mode parameter %s is invalid, can only be %s or %s",
									mode, VMMode.MAINTENANCE, VMMode.SECURE)))
					.build();
		}

		try {
			DBOperations.getInstance().insertUserIfNotExists(userName, userEmail);
			
			VmInfoBean vmInfo;
			vmInfo = DBOperations.getInstance().getVmInfo(userName, vmid);

			// if already in the target mode, then no need to switch
			if (target.equals(vmInfo.getVmmode())) {
				String msg = String.format(
						"VM %s is already in mode %s, no need to switch",
						vmid, vmInfo.getVmmode());
				logger.info(msg);
				return Response
						.status(400)
						.entity(new ErrorBean(400, msg)).build();
			}

			// VM can only start from "running" state
			if (VMStateManager.isPendingState(vmInfo.getVmstate()) ||
				!VMStateManager.getInstance().transitTo(
					vmid, vmInfo.getVmstate(),
					(VMMode.MAINTENANCE.equals(target)
							? VMState.SWITCH_TO_MAINTENANCE_PENDING
							: VMState.SWITCH_TO_SECURE_PENDING))) {
				return Response
						.status(400)
						.entity(new ErrorBean(400, "Cannot switch VM " + vmid
								+ " when it is " + vmInfo.getVmstate()))
						.build();
			}

			logger.info("User " + userName + " tries to switch VM " + vmInfo.getVmid());
			
			// nonblocking call to hypervisor
			String policypath = (target.equals(VMMode.MAINTENANCE)) ? 
				Configuration.getInstance().getString(
					Configuration.PropertyName.MAINTENANCE_FIREWALL_POLICY): 
				Configuration.getInstance().getString(
					Configuration.PropertyName.SECURE_FIREWALL_POLICY);
			vmInfo.setVmState((VMMode.MAINTENANCE.equals(target)
					? VMState.SWITCH_TO_MAINTENANCE_PENDING
					: VMState.SWITCH_TO_SECURE_PENDING));
			vmInfo.setRequestedVMMode(target);
			vmInfo.setPolicypath(policypath);

			HypervisorProxy.getInstance().addCommand(
					new SwitchVMCommand(vmInfo));

			return Response.status(200).build();
		} catch (NoItemIsFoundInDBException e) {
			logger.error(e.getMessage(), e);
			return Response
					.status(400)
					.entity(new ErrorBean(400, "VM " + vmid
							+ " is not associated with user " + userName))
					.build();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return Response.status(500)
					.entity(new ErrorBean(500, e.getMessage())).build();
		}

	}
	/**
	 * null indicates error
	 * 
	 * @param mode
	 * @return
	 */
	private static VMMode toVMMode(String mode) {
		if (VMMode.MAINTENANCE.toString().equalsIgnoreCase(mode))
			return VMMode.MAINTENANCE;

		if (VMMode.SECURE.toString().equalsIgnoreCase(mode))
			return VMMode.SECURE;

		return null;
	}
}
