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
import edu.indiana.d2i.sloan.vm.VMState;
import edu.indiana.d2i.sloan.vm.VMStateManager;

@Path("/launchvm")
public class LaunchVM {
	private static Logger logger = Logger.getLogger(LaunchVM.class);

	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public Response getResourcePost(@FormParam("vmid") String vmid,
			@Context HttpHeaders httpHeaders,
			@Context HttpServletRequest httpServletRequest) {
		String userName = null;
		
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
			VmInfoBean vmInfo = DBOperations.getInstance().getVmInfo(userName, vmid);
			if (!VMStateManager.getInstance().transitTo(userName, vmid, 
				vmInfo.getVmstate(), VMState.LAUNCHING)) {
				return Response
					.status(400)
					.entity(new ErrorBean(400,
						"Cannot launch VM " + vmid + " when it is " + vmInfo.getVmstate()))
					.build();
			}
			
			// nonblocking call to hypervisor
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
