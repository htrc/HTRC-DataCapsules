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
import edu.indiana.d2i.sloan.bean.VmInfoBean;
import edu.indiana.d2i.sloan.db.DBOperations;
import edu.indiana.d2i.sloan.exception.InvalidHostNameException;
import edu.indiana.d2i.sloan.exception.NoItemIsFoundInDBException;
import edu.indiana.d2i.sloan.exception.NoResourceAvailableException;
import edu.indiana.d2i.sloan.hyper.HypervisorProxy;
import edu.indiana.d2i.sloan.hyper.MigrateVMCommand;
import edu.indiana.d2i.sloan.vm.PortsPool;
import edu.indiana.d2i.sloan.vm.VMPorts;
import edu.indiana.d2i.sloan.vm.VMState;
import edu.indiana.d2i.sloan.vm.VMStateManager;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/migratevm")
public class MigrateVM {
	private static Logger logger = Logger.getLogger(MigrateVM.class);

	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public Response migrateVM(@FormParam("vmid") String vmid, @FormParam("host") String host,
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
		
		if (vmid == null || host == null) {
			return Response.status(400)
					.entity(new ErrorBean(400, "VM id or host cannot be empty!"))
					.build();
		}

		try {
			//DBOperations.getInstance().insertUserIfNotExists(userName, userEmail);
			//DBOperations.getInstance().insertUserIfNotExists(operator, operatorEmail);

			VmInfoBean vmInfo = DBOperations.getInstance().getVmInfo(userName,
					vmid);
			if (VMStateManager.isPendingState(vmInfo.getVmstate()) ||
				!VMStateManager.getInstance().transitTo(vmid,
					vmInfo.getVmstate(), VMState.MIGRATE_PENDING, userName)) {
				return Response
						.status(400)
						.entity(new ErrorBean(400, "Cannot migrate VM " + vmid
								+ " when it is " + vmInfo.getVmstate()))
						.build();
			}

			VMPorts vmports = PortsPool.getInstance()
					.getMigrationPortPair(new VMPorts(host, vmInfo.getSshport(), vmInfo.getVncport()));
			if(vmports == null) {
				logger.warn("Cannot migrate VM " + vmid + " to " + host + " - No port resource available");
				VMStateManager.getInstance().transitTo(vmid, VMState.MIGRATE_PENDING, VMState.SHUTDOWN, userName);
				return Response
						.status(400)
						.entity(new ErrorBean(400, "Cannot migrate VM " + vmid
								+ " to " + host + " - No port resource available"))
						.build();
			}

			logger.info(userName + " requests to migrate VM " + vmInfo.getVmid());
			vmInfo.setVmState(VMState.MIGRATE_PENDING);
			HypervisorProxy.getInstance().addCommand(new MigrateVMCommand(vmInfo, userName, vmports));
			return Response.status(200).build();

		} catch (InvalidHostNameException e) {
			logger.error(e.getMessage(), e);
			try {
				VMStateManager.getInstance().transitTo(vmid, VMState.MIGRATE_PENDING, VMState.SHUTDOWN, userName);
			} catch (Exception e1){
				logger.error(e1.getMessage(), e1);
			}
			return Response
					.status(400)
					.entity(new ErrorBean(400, e.getMessage())).build();
		} catch (NoItemIsFoundInDBException e) {
			logger.error(e.getMessage(), e);
			return Response
					.status(400)
					.entity(new ErrorBean(400, "Cannot find VM " + vmid
							+ " associated with username " + userName)).build();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return Response.status(500)
					.entity(new ErrorBean(500, e.getMessage())).build();
		}
	}
}
