package edu.indiana.d2i.sloan;

import edu.indiana.d2i.sloan.bean.ErrorBean;
import edu.indiana.d2i.sloan.db.DBOperations;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.SQLException;

@Path("/updateasnotified")
public class UpdateAsNotified {
    private static Logger logger = Logger.getLogger(LaunchVM.class);

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getResourcePost(
            @FormParam("resultid") String resultid,
            @Context HttpHeaders httpHeaders,
            @Context HttpServletRequest httpServletRequest) {

            if(resultid == null){
                return Response.status(400).entity(new ErrorBean(400, "Resultid is null")).build();
            }

            String userName = httpServletRequest.getHeader(Constants.USER_NAME);
            String userEmail = httpServletRequest.getHeader(Constants.USER_EMAIL);
            //String resultid = httpServletRequest.getHeader("resultid");
            System.out.println(resultid);
        //return Response.status(200).entity(resultid).build();
            try {
                DBOperations.getInstance().updateResultAsNotified(resultid);
                return Response.status(200).build();

            } catch (SQLException e) {
                logger.error(e.getMessage(),e);
                return Response.status(500).entity(new ErrorBean(500,e.getMessage())).build();
            }


    }

}