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
import edu.indiana.d2i.sloan.exception.NoItemIsFoundInDBException;
import edu.indiana.d2i.sloan.hyper.HypervisorProxy;
import edu.indiana.d2i.sloan.hyper.StopVMCommand;
import edu.indiana.d2i.sloan.vm.VMState;
import edu.indiana.d2i.sloan.vm.VMStateManager;

@Path("/stopvm")
public class StopVM {
	private static Logger logger = Logger.getLogger(StopVM.class);
	
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
							"VM id cannot be empty!"))
					.build();
		}
		
		try {
			if (!VMStateManager.getInstance().transitTo(userName, vmid, VMState.SHUTTINGDOWN)) {
				return Response
					.status(400)
					.entity(new ErrorBean(400, "Cannot stop " + vmid))
					.build();
			}
			
			HypervisorProxy.getInstance().addCommand(new StopVMCommand(userName, vmid));	
			
			return Response.status(200).build();			
		} catch (NoItemIsFoundInDBException e) {
			logger.error(e.getMessage(), e);
			return Response
					.status(400)
					.entity(new ErrorBean(400,
							"Cannot find VM " + vmid + " with username " + userName))
					.build();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return Response.status(500)
					.entity(new ErrorBean(500, e.getMessage())).build();
		}
	}
}
