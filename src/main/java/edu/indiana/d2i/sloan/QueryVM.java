package edu.indiana.d2i.sloan;

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
import edu.indiana.d2i.sloan.bean.VmStatusBean;
import edu.indiana.d2i.sloan.db.DBOperations;
import edu.indiana.d2i.sloan.exception.NoItemIsFoundInDBException;
import edu.indiana.d2i.sloan.hyper.HypervisorProxy;
import edu.indiana.d2i.sloan.hyper.QueryVMCommand;

@Path("/queryvm")
public class QueryVM {
	private static Logger logger = Logger.getLogger(QueryVM.class);
	
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public Response getResourcePost(@FormParam("vmid") String vmid,
			@Context HttpHeaders httpHeaders,
			@Context HttpServletRequest httpServletRequest) {
		String userName = null;
		
		// read from db and return first
		// at the same time, send query to hypervisor async
		// it might be overwhelmed by other users' requests
		// ws periodically updates VM status??
		
		try {		
			List<VmStatusBean> status = null;
			if (vmid == null) {
				status = DBOperations.getInstance().getVmStatus(userName);
			} else {
				status = DBOperations.getInstance().getVmStatus(userName, vmid);
			}
			
			HypervisorProxy.getInstance().addCommand(new QueryVMCommand(
				userName, vmid /* could be null */));
			
			return Response.status(200)
				.entity(new QueryVmResponseBean(status)).build();
		} catch (NoItemIsFoundInDBException e) {
			logger.error(e.getMessage(), e);
			return Response
					.status(400)
					.entity(new ErrorBean(400,
							"Cannot find VMs " + vmid + " with username " + userName))
					.build();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return Response.status(500)
					.entity(new ErrorBean(500, e.getMessage())).build();
		}
	}
}
