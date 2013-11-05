package edu.indiana.d2i.sloan;

import java.util.Map;
import java.util.Map.Entry;

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
import edu.indiana.d2i.sloan.bean.PolicyInfoBean;
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
	public Response getResourcePost(
			@FormParam("vmid") String vmid,
			@FormParam("policyname") String policyname,
			@Context HttpHeaders httpHeaders,
			@Context HttpServletRequest httpServletRequest) {
		String userName = httpServletRequest.getHeader(Constants.USER_NAME);
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
			DBOperations.getInstance().insertUserIfNotExists(userName);
			
			VmInfoBean vmInfo = DBOperations.getInstance().getVmInfo(userName, vmid);
			if (VMStateManager.isPendingState(vmInfo.getVmstate()) ||
				!VMStateManager.getInstance().transitTo(vmid, 
				vmInfo.getVmstate(), VMState.LAUNCH_PENDING)) {
				return Response
					.status(400)
					.entity(new ErrorBean(400,
						"Cannot launch VM " + vmid + " when it is " + vmInfo.getVmstate()))
					.build();
			}
			
			logger.info(userName + " requests to launch VM " + vmInfo.getVmid());
			
			// nonblocking call to hypervisor
			Map<String, PolicyInfoBean> policies = DBOperations.getInstance().getPolicyInfo();
			if (policyname != null) {
				if (!policies.containsKey(policyname)) {
					return Response
						.status(400)
						.entity(new ErrorBean(400,
							"Cannot find policy " + policyname + " in DB"))
						.build();
				}
				vmInfo.setPolicypath(policies.get(policyname).getPath());
			} else {
				Entry<String, PolicyInfoBean> entry = policies.entrySet().iterator().next();
				vmInfo.setPolicypath(entry.getValue().getPath());
			}
			vmInfo.setVmState(VMState.LAUNCH_PENDING);
			vmInfo.setRequestedVMMode(VMMode.MAINTENANCE);
			
			HypervisorProxy.getInstance().addCommand(new LaunchVMCommand(vmInfo));

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
