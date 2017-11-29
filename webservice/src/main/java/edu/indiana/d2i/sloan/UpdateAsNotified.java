package edu.indiana.d2i.sloan;

import edu.indiana.d2i.sloan.bean.ErrorBean;
import edu.indiana.d2i.sloan.db.DBOperations;
import edu.indiana.d2i.sloan.utils.EmailUtil;
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
            @FormParam("status") String status,
            @FormParam("comment") String comment,
            @FormParam("reviewer") String reviewer,
            @Context HttpHeaders httpHeaders,
            @Context HttpServletRequest httpServletRequest) {

            if(resultid == null){
                return Response.status(400).entity(new ErrorBean(400, "Resultid is null")).build();
            }

            String userName = httpServletRequest.getHeader(Constants.USER_NAME);
            String userEmail = httpServletRequest.getHeader(Constants.USER_EMAIL);
            //String resultid = httpServletRequest.getHeader("resultid");
            System.out.println(resultid);
            try {
                //DBOperations.getInstance().updateResultAsNotified(resultid);
                if(status.equals("released")) {
                    DBOperations.getInstance().updateResultAsReleased(resultid, comment,reviewer);
                }else if(status.equals("rejected")) {
                    DBOperations.getInstance().updateResultAsRejected(resultid, comment,reviewer);
                }

                EmailUtil send_email = new EmailUtil();
                //constructor fetch properites automatically
                String download_addr = String.format(Configuration.PropertyName.RESULT_DOWNLOAD_URL_PREFIX, resultid);

                //construct email content
                String content = String.format("Please download result from the following URL: \n", download_addr);
                send_email.sendEMail("li530@indiana.edu", "HTRC Data Capsule Result Download URL", content);

                return Response.status(200).build();
            } catch (SQLException e) {
                logger.error(e.getMessage(),e);
                return Response.status(500).entity(new ErrorBean(500,e.getMessage())).build();
            }


    }

}