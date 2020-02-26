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
import edu.indiana.d2i.sloan.utils.RolePermissionUtils;
import edu.indiana.d2i.sloan.vm.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/migratevm")
public class MigrateVM {
	private static Logger logger = LoggerFactory.getLogger(MigrateVM.class);
	private static final String ADMIN = "admin";

	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public Response migrateVM(@FormParam("vmid") String vmid, @FormParam("host") String host,
			@Context HttpHeaders httpHeaders,
			@Context HttpServletRequest httpServletRequest) {
		String userName = httpServletRequest.getHeader(Constants.USER_NAME);

		//TODO-UN which role can do this?

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
			if (!RolePermissionUtils.isPermittedCommand(userName, vmid, RolePermissionUtils.API_CMD.MIGRATE_VM)) {
				return Response.status(400).entity(new ErrorBean(400,
						"User " + userName + " cannot perform task "
								+ RolePermissionUtils.API_CMD.MIGRATE_VM + " on VM " + vmid)).build();
			}
			//DBOperations.getInstance().insertUserIfNotExists(userName, userEmail);
			//DBOperations.getInstance().insertUserIfNotExists(operator, operatorEmail);

			VmInfoBean vmInfo = DBOperations.getInstance().getVmInfo(userName,
					vmid);

			if(vmInfo.getPublicip().equals(host)) {
				return Response
						.status(400)
						.entity(new ErrorBean(400, "Cannot migrate a VM to the same host!"))
						.build();
			}

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
					.getMigrationPortPair(vmid, new VMPorts(host, vmInfo.getSshport(), vmInfo.getVncport()));
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

	@PUT
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public Response migrateVM(@FormParam("vmid") String vmid, @FormParam("host") String host,
							  @FormParam("sshport") int sshport, @FormParam("vncport") int vncport,
							  @Context HttpHeaders httpHeaders,
							  @Context HttpServletRequest httpServletRequest) {

		String userName = ADMIN;
		if (vmid == null) {
			return Response.status(400)
					.entity(new ErrorBean(400, "VM id cannot be empty!"))
					.build();
		}

		try {
			VMPorts vmport = new VMPorts(host, sshport, vncport);
			logger.info("User " + userName + " tries to update info of vm " + vmid + " in DB as migrated to " + vmport.toString());

			//Update the database(state:SHUTDOWN) and update vms table with new ports and host values
			DBOperations.getInstance().updateVMState(vmid, VMState.SHUTDOWN, userName);
			DBOperations.getInstance().updateVmHostAndPorts(vmid, vmport);

			//Release old ports of the vm from 'ports' table
			List<VMPorts> vmPorts = DBOperations.getInstance().getPortsOfVm(vmid);
			for(VMPorts vmPort : vmPorts) {
				if(vmPort.publicip.equals(host) && (vmPort.sshport == sshport || vmPort.vncport == vncport)) {
					logger.info("User " + userName + " tries to remove ports from PortsPool : " + vmPort.toString());
					PortsPool.getInstance().release(vmid, vmPort);
				}
			}

			return Response.status(200).build();
		} catch (InvalidHostNameException e) {
			logger.error(e.getMessage(), e);
			return Response
					.status(400)
					.entity(new ErrorBean(400, e.getMessage())).build();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return Response.status(500)
					.entity(new ErrorBean(500, e.getMessage())).build();
		}

	}
}
