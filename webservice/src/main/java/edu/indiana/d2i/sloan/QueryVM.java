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

import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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

import edu.indiana.d2i.sloan.bean.*;
import edu.indiana.d2i.sloan.utils.RolePermissionUtils;
import edu.indiana.d2i.sloan.vm.VMMode;
import edu.indiana.d2i.sloan.vm.VMState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import edu.indiana.d2i.sloan.db.DBOperations;
import edu.indiana.d2i.sloan.exception.NoItemIsFoundInDBException;
import edu.indiana.d2i.sloan.hyper.HypervisorProxy;
import edu.indiana.d2i.sloan.hyper.QueryVMCommand;
import edu.indiana.d2i.sloan.vm.VMStateManager;

@Path("/show")
public class QueryVM {
	private static Logger logger = LoggerFactory.getLogger(QueryVM.class);

	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public Response queryVMs(@FormParam("vmid") String vmid,
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

		// read from db and return first
		// at the same time, send query to hypervisor async
		// it might be overwhelmed by other users' requests
		// ws periodically updates VM status??

		try {
			//DBOperations.getInstance().insertUserIfNotExists(userName, userEmail);
			//DBOperations.getInstance().insertUserIfNotExists(operator, operatorEmail);
			boolean pub_key_exists = false;
			boolean tou = false;
			try {
				pub_key_exists = DBOperations.getInstance().getUserPubKey(userName) == null ? false : true;
				tou = DBOperations.getInstance().getUserTOU(userName);
			} catch (NoItemIsFoundInDBException e) {
				logger.debug("Cannot retrieve public key or TOU of user since '" + userName + "' is not in the database");
			}

			List<VmStatusBean> status = new ArrayList<VmStatusBean>();
			List<VmInfoBean> vmInfoList = new ArrayList<VmInfoBean>();
			if (vmid == null) {
				vmInfoList = DBOperations.getInstance().getVmInfo(userName);
				for (VmInfoBean vminfo : vmInfoList) {
					VmUserRole vmUserRole = DBOperations.getInstance().getUserRoleWithVmid(userName, vminfo.getVmid());
					if (!RolePermissionUtils.isPermittedCommand(userName, vminfo.getVmid(), RolePermissionUtils.API_CMD.QUERY_VM)) {
						vminfo = new VmInfoBean(vminfo.getVmid(), vminfo.getRoles(), vminfo.isFull_access()
								, vminfo.getCreated_at());
					}
					status.add(new VmStatusBean(vminfo, pub_key_exists, tou, vmUserRole));
				}
			} else {
				VmUserRole vmUserRole = DBOperations.getInstance().getUserRoleWithVmid(userName, vmid);
				VmInfoBean vminfo = DBOperations.getInstance().getVmInfo(userName, vmid);
				if (!RolePermissionUtils.isPermittedCommand(userName, vmid, RolePermissionUtils.API_CMD.QUERY_VM)) {
					vminfo = new VmInfoBean(vmid, vminfo.getRoles(), vminfo.isFull_access(), vminfo.getCreated_at());
				}
				vmInfoList.add(vminfo);
				status.add(new VmStatusBean(vminfo, pub_key_exists, tou, vmUserRole));
			}

			logger.info("User " + userName + " tries to query VM " + vmInfoList.toString());
			
			for (VmInfoBean vminfo : vmInfoList) {
				// query the back-end script only when vm state is not in pending
				if (!VMStateManager.isPendingState(vminfo.getVmstate())) {
					HypervisorProxy.getInstance().addCommand(
							new QueryVMCommand(vminfo, userName));
				}
			}

			return Response.status(200).entity(new QueryVmResponseBean(status)).build();
		} catch (NoItemIsFoundInDBException e) {
			logger.error(e.getMessage(), e);
			String msg = (vmid == null) ? 
				"Cannot find VMs with username " + userName: 
				"Cannot find VMs " + vmid + " with username " + userName;
			return Response
					.status(400)
					.entity(new ErrorBean(400, msg)).build();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return Response.status(500)
					.entity(new ErrorBean(500, e.getMessage())).build();
		}
	}
}
