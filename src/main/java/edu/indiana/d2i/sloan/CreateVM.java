package edu.indiana.d2i.sloan;

import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
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
import edu.indiana.d2i.sloan.bean.CreateVmRequestBean;
import edu.indiana.d2i.sloan.bean.CreateVmResponseBean;
import edu.indiana.d2i.sloan.bean.VmRequestBean;
import edu.indiana.d2i.sloan.db.DBOperations;
import edu.indiana.d2i.sloan.hyper.CreateVMCommand;
import edu.indiana.d2i.sloan.hyper.HypervisorProxy;
import edu.indiana.d2i.sloan.vm.Scheduler;

@Path("/createvm")
public class CreateVM {
	private static Logger logger = Logger.getLogger(CreateVM.class);

	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public Response getResourcePost(
			@FormParam("imagename") String imageName,
			@FormParam("loginpassword") String loginpassword,
			@DefaultValue("1024") @FormParam("memory") int memory,
			@DefaultValue("1") @FormParam("vcpu") int vcpu,
			@Context HttpHeaders httpHeaders,
			@Context HttpServletRequest httpServletRequest) {
		String userName = httpServletRequest.getHeader(Constants.USER_NAME);

		// check input
		if (userName == null) {
			logger.error("Username is not present in http header.");
			return Response.status(500).entity(new ErrorBean(
				500, "Username is not present in http header.")).build();
		}
		
		if (imageName == null || loginpassword == null) {
			logger.error(String.format("image:%s, loginpwd:%s", imageName, loginpassword));
			return Response
				.status(400)
				.entity(new ErrorBean(400, "Image name and login password cannot be empty!"))
				.build();
		}
		
		// TODO: check if image name is valid
		
		logger.info(String.format("User %s requests image %s with vcpu %d, memory %d, " +
			"and login password %s", userName, imageName, vcpu, memory, loginpassword));
		
		try {
			// create a user entry
			DBOperations.getInstance().insertUserIfNotExists(userName);
			
			// vm parameters
			String vmid = UUID.randomUUID().toString();
			int volumeSizeInGB = Integer.valueOf(Configuration.getInstance()
				.getProperty(Configuration.PropertyName.VOLUME_SIZE_IN_GB, 
					Constants.DEFAULT_VOLUME_SIZE_IN_GB));
			CreateVmRequestBean request = new CreateVmRequestBean(userName,
				imageName, vmid, loginpassword, memory, vcpu, volumeSizeInGB);
			
			// check quota
			if (DBOperations.getInstance().quotaExceedsLimit(request)) {
				logger.error("Quota exceeds limit for request " + request);
				return Response.status(400)
					.entity(new ErrorBean(400, "Quota exceeds limit!"))
					.build();
			}
			
			// schedule & update db
			VmRequestBean vminfo = Scheduler.getInstance().schedule(request);
			
			// nonblocking call to hypervisor
			HypervisorProxy.getInstance().addCommand(new CreateVMCommand(vminfo));
			
			return Response.status(200)
					.entity(new CreateVmResponseBean(vmid)).build();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return Response.status(500)
					.entity(new ErrorBean(500, e.getMessage())).build();
		}
	}
}
