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

import org.apache.log4j.Logger;

import edu.indiana.d2i.sloan.bean.ErrorBean;
import edu.indiana.d2i.sloan.bean.QueryVmResponseBean;
import edu.indiana.d2i.sloan.bean.VmInfoBean;
import edu.indiana.d2i.sloan.bean.VmStatusBean;
import edu.indiana.d2i.sloan.db.DBOperations;
import edu.indiana.d2i.sloan.exception.NoItemIsFoundInDBException;
import edu.indiana.d2i.sloan.hyper.HypervisorProxy;
import edu.indiana.d2i.sloan.hyper.QueryVMCommand;
import edu.indiana.d2i.sloan.vm.VMState;
import edu.indiana.d2i.sloan.vm.VMStateManager;

@Path("/show")
public class QueryVM {
	private static Logger logger = Logger.getLogger(QueryVM.class);

	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public Response getResourcePost(@FormParam("vmid") String vmid,
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
					.status(500)
					.entity(new ErrorBean(500,
							"Username is not present in http header.")).build();
		}

		// read from db and return first
		// at the same time, send query to hypervisor async
		// it might be overwhelmed by other users' requests
		// ws periodically updates VM status??

		try {
			DBOperations.getInstance().insertUserIfNotExists(userName, userEmail);
			DBOperations.getInstance().insertUserIfNotExists(operator, operatorEmail);

			List<VmStatusBean> status = new ArrayList<VmStatusBean>();
			List<VmInfoBean> vmInfoList = new ArrayList<VmInfoBean>();
			if (vmid == null) {
				vmInfoList = DBOperations.getInstance().getVmInfo(userName);
				for (VmInfoBean vminfo : vmInfoList) {
					status.add(new VmStatusBean(vminfo));
				}
			} else {
				VmInfoBean vminfo = DBOperations.getInstance().getVmInfo(
						userName, vmid);
				vmInfoList.add(vminfo);
				status.add(new VmStatusBean(vminfo));
			}

			logger.info("User " + userName + " tries to query VM " + vmInfoList.toString());
			
			for (VmInfoBean vminfo : vmInfoList) {
				// query the back-end script only when vm state is not in pending
				if (!VMStateManager.isPendingState(vminfo.getVmstate())) {
					HypervisorProxy.getInstance().addCommand(
							new QueryVMCommand(vminfo, operator));
				}
			}

			return Response.status(200).entity(new QueryVmResponseBean(status))
					.build();
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
