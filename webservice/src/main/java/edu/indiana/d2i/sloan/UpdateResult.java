package edu.indiana.d2i.sloan;

import com.sun.org.apache.regexp.internal.RE;
import edu.indiana.d2i.sloan.bean.ErrorBean;
import edu.indiana.d2i.sloan.bean.UserBean;
import edu.indiana.d2i.sloan.db.DBOperations;
import edu.indiana.d2i.sloan.exception.NoItemIsFoundInDBException;
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
    public Response updateResult(
            @FormParam("resultid") String resultid,
            @FormParam("status") String status,
            @Context HttpHeaders httpHeaders,
            @Context HttpServletRequest httpServletRequest) {

            if(resultid == null){
                logger.info("result id is null!");
                return Response.status(400).entity(new ErrorBean(400, "Resultid is null")).build();
            }

            if(!status.equals("Rejected") && !status.equals("Released")){
                logger.info("invalid status input");
                return Response.status(400).entity(new ErrorBean(400, "Invalid status update")).build();
            }

            //check whether this record has been updated yet
            //if yes (not pending), return conflict code 409
            try {
                String currStatus = DBOperations.getInstance().getStatus(resultid);
                if(!currStatus.equals("Pending")){
                    logger.info(resultid+" has been "+currStatus);
                    return Response.status(409).
                            entity(new ErrorBean(409, resultid+" has been "+currStatus)).build();
                }
            } catch (SQLException e){
                logger.error(e.getMessage(),e);
                return Response.status(500).entity(new ErrorBean(500,e.getMessage())).build();
            }


            System.out.println(resultid);
            try {
                //result table is not directly linked to user table
                //need vm to query for username
                String vmid = DBOperations.getInstance().getVMIDWithResultid(resultid);
                UserBean ub = DBOperations.getInstance().getUserWithVmid(vmid);
                String userEmail = ub.getUserEmail();
                String reviewer_email = Configuration.getInstance().
                        getString(Configuration.PropertyName.RESULT_HUMAN_REVIEW_EMAIL);

                DBOperations.getInstance().updateResult(resultid, status);

                //Do not send email if rejected
                if(!status.equals("Rejected")) {
                    EmailUtil send_email = new EmailUtil();

                    //constructor fetch properites automatically
                    String download_url = Configuration.getInstance().
                            getString(Configuration.PropertyName.RESULT_DOWNLOAD_URL_PREFIX);
                    String download_addr = String.format(download_url, resultid);

                    //construct email content for user
                    String contentUser = String.format("Please download result from the following URL: \n", download_addr);
                    send_email.sendEMail(userEmail, "HTRC Data Capsule Result Download URL", contentUser);

                    //construct email content for reviewer
                    String contentReviewer = String.format("Result \"%s\" \nhas been released to user \"%s\" \nemail: %s",
                            resultid, ub.getUserName(), userEmail);
                    send_email.sendEMail(reviewer_email, "HTRC Data Capsule Result Has Been Successfully Released", contentReviewer);

                }else{
                    EmailUtil send_email = new EmailUtil();

                    //construct email content for user
                    //String contentUser = String.format("Unfortunately, we are not able to approve your request of recent result release." +
                    //       "\nPlease consult htrc for more details");
                    //send_email.sendEMail(userEmail, "HTRC Data Capsule Result Download URL", contentUser);

                    //construct email content for reviewer
                    String contentReviewer = String.format("Result \"%s\" has been rejected.\nFrom user \"%s\", email: %s",
                            resultid, ub.getUserName(), userEmail);
                    send_email.sendEMail(reviewer_email, "HTRC Data Capsule Result Has Been Rejected", contentReviewer);
                }

                //release and reject are both legal operations
                return Response.status(200).build();

            } catch (SQLException e) {
                logger.error(e.getMessage(),e);
                return Response.status(500).
                        entity(new ErrorBean(500,e.getMessage())).build();
            } catch (NoItemIsFoundInDBException e) {
                logger.error("Invalid " + resultid + " attempts", e);
                return Response.status(404).
                        entity(String.format("No vmid is found for result %s", resultid)).build();
            }

    }

}