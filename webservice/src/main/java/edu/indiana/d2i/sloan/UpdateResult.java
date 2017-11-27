package edu.indiana.d2i.sloan;

import com.sun.org.apache.regexp.internal.RE;
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

/**
 * param:
 *   resultid: identifier of the record to be updated
 *   status: updated record's status (Pending to Released/Rejected)
 *
 *
 * return: null (POST method)
 */

@Path("/updateresult")
public class UpdateResult {
    private static Logger logger = Logger.getLogger(UpdateResult.class);

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getResourcePost(
            @FormParam("resultid") String resultid,
            @FormParam("status") String status,
            @Context HttpHeaders httpHeaders,
            @Context HttpServletRequest httpServletRequest) {

            if(resultid == null){
                return Response.status(204).entity(new ErrorBean(204, "Resultid is null")).build();
            }


            //check whether this record has been updated yet
            //if yes (not pending), return conflict code 409
            try {
                String currStatus = DBOperations.getInstance().getStatus(resultid);
                if(!currStatus.equals("Pending")){
                    return Response.status(409).
                            entity(new ErrorBean(409, resultid+" has been "+currStatus)).build();
                }
            } catch (SQLException e){
                logger.error(e.getMessage(),e);
                return Response.status(500).entity(new ErrorBean(500,e.getMessage())).build();
            }


            System.out.println(resultid);
            try {

                DBOperations.getInstance().updateResult(resultid, status);

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



            //
            //
            //
    }

}