package edu.indiana.d2i.sloan;

import edu.indiana.d2i.sloan.bean.*;
import edu.indiana.d2i.sloan.db.DBOperations;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * Created by liang on 9/21/16.
 */

@Path("/listvms")
public class ListVMs {
    private static Logger logger = Logger.getLogger(ListVMs.class);


    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listVMs(@Context HttpHeaders httpHeaders,
                                    @Context HttpServletRequest httpServletRequest) {
        String userName = httpServletRequest.getHeader(Constants.USER_NAME);

        try {
            List<VmKeyInfoBean> vmKeyInfoBeanList =  DBOperations.getInstance().getVmKeyInfo();
            return Response.status(200).entity(new ListVmKeyInfoResponseBean(vmKeyInfoBeanList)).build();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return Response.status(500)
                    .entity(new ErrorBean(500, e.getMessage())).build();
        }

    }
}
