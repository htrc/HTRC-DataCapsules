package edu.indiana.d2i.sloan;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;

import edu.indiana.d2i.sloan.bean.ErrorBean;
import edu.indiana.d2i.sloan.bean.UserVmInfoBean;
import edu.indiana.d2i.sloan.bean.VmInfoBean;
import edu.indiana.d2i.sloan.db.DBOperations;

@Path("/listvm")
public class ListVM {
	private static Logger logger = Logger.getLogger(ListVM.class);

	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public Response getResourcePost(@Context HttpHeaders httpHeaders,
			@Context HttpServletRequest httpServletRequest) {

		String userName = httpServletRequest.getHeader(Constants.USER_NAME);

		// check if username exists
		if (userName == null) {
			logger.error("Username is not present in http header.");
			return Response
					.status(500)
					.entity(new ErrorBean(500,
							"Username is not present in http header.")).build();
		}

		try {
			List<VmInfoBean> fullVMInfo = DBOperations.getInstance().getVmInfo(
					userName);

			List<UserVmInfoBean> userVMInfos = new ArrayList<UserVmInfoBean>();
			for (VmInfoBean vminfo : fullVMInfo) {
				userVMInfos.add(UserVmInfoBean.fullVmInfo2UserInfo(vminfo));
			}

			// TODO: need to double check whether python plugin can work on the
			// list
			return Response.status(200).entity(userVMInfos).build();

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return Response.status(500)
					.entity(new ErrorBean(500, e.getMessage())).build();
		}

	}
}
