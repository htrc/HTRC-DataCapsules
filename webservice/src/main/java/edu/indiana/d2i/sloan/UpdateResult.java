package edu.indiana.d2i.sloan;

import com.sun.org.apache.regexp.internal.RE;
import edu.indiana.d2i.sloan.bean.*;
import edu.indiana.d2i.sloan.db.DBOperations;
import edu.indiana.d2i.sloan.exception.NoItemIsFoundInDBException;
import edu.indiana.d2i.sloan.exception.NoResultFileFoundException;
import edu.indiana.d2i.sloan.utils.EmailUtil;
import edu.indiana.d2i.sloan.utils.ResultUtils;
import edu.indiana.d2i.sloan.utils.RolePermissionUtils;
import edu.indiana.d2i.sloan.vm.VMRole;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.List;
import java.util.stream.Collectors;

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
    private static Logger logger = LoggerFactory.getLogger(UpdateResult.class);

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateResult(
            @FormParam("resultid") String resultid,
            @FormParam("status") String status,
            @FormParam("comment") String comment,
            @FormParam("reviewer") String reviewer,
            @Context HttpHeaders httpHeaders,
            @Context HttpServletRequest httpServletRequest) {

            if(resultid == null){
                logger.error("result id is null!");
                return Response.status(400).entity(new ErrorBean(400, "Resultid is null")).build();
            }

            if(!status.equals("Rejected") && !status.equals("Released")){
                logger.error("invalid status input");
                return Response.status(400).entity(new ErrorBean(400, "Invalid status update")).build();
            }

            //check whether this record has been updated yet
            //if yes (not pending), return conflict code 409
            try {
                String currStatus = DBOperations.getInstance().getStatus(resultid);
                if(!currStatus.equals("Pending")){
                    logger.error(resultid + " has been already " + currStatus);
                    return Response.status(409).
                            entity(new ErrorBean(409, resultid+" has been already "+currStatus)).build();
                }
            } catch (NoItemIsFoundInDBException e) {
                logger.error("No Result with id " + resultid + " is found!", e);
                return Response.status(404).entity("No Result with id " + resultid + " is found!").build();
            } catch (SQLException e){
                logger.error(e.getMessage(),e);
                return Response.status(500).entity(new ErrorBean(500,e.getMessage())).build();
            }

            try {
                //result table is not directly linked to user table
                //need vm to query for username
                String vmid = DBOperations.getInstance().getVMIDWithResultid(resultid);

                // there are multiple users for a VM
                //UserBean ub = DBOperations.getInstance().getUserWithVmid(vmid);
                //String userEmail = ub.getUserEmail();

                //get roles of the vm
                List<VmUserRole> vmUserRoles = DBOperations.getInstance().getRolesWithVmid(vmid, true);
                //filter roles who are allowed to view results
                List<VmUserRole> allowedVmUserRoles = RolePermissionUtils.filterPermittedRoles(vmUserRoles, vmid,
                        RolePermissionUtils.API_CMD.VIEW_RESULT);

                String reviewer_email = Configuration.getInstance().
                        getString(Configuration.PropertyName.RESULT_HUMAN_REVIEW_EMAIL);

                DBOperations.getInstance().updateResult(resultid, status, comment, reviewer);
                logger.info("Updated results table - ResultID : " + resultid + " status : " + status);

                //Do not send email if rejected
                if(!status.equals("Rejected")) {
                    EmailUtil send_email = new EmailUtil();

                    //constructor fetch properites automatically
                    String download_url = Configuration.getInstance().
                            getString(Configuration.PropertyName.RESULT_DOWNLOAD_URL_PREFIX);
                    String download_addr = download_url +  resultid;

                    long result_expiration_seconds = Configuration.getInstance().getLong(
                            Configuration.PropertyName.RESULT_EXPIRE_IN_SECOND);
                    int result_expiration_days = (int) (result_expiration_seconds/86400);

                    //construct email content for users
                    for (VmUserRole role : allowedVmUserRoles) {
                        String contentUser = "Dear Data Capsule user,\n\n" +
                                "Thank you for using the HTRC Data Capsule! You can download your result from the link below.\n" +
                                download_addr + "\n\n" +
                                "Please note this link is active for " + result_expiration_days + " day(s).";
                        send_email.sendEMail(null,role.getEmail(), "HTRC Data Capsule Result Download URL", contentUser);
                    }
                    logger.info("Download result email sent to users " + allowedVmUserRoles + " - download URL : " + download_addr);

                    //construct email content for reviewer
                    String contentReviewer = String.format("Result \"%s\" has been released to users: %s",
                            resultid, send_email.userListToString(vmUserRoles, allowedVmUserRoles));
                    send_email.sendEMail(null, reviewer_email, "HTRC Data Capsule Result Has Been Successfully Released", contentReviewer);

                }else{
                    EmailUtil send_email = new EmailUtil();

                    //construct email content for user
                    //String contentUser = String.format("Unfortunately, we are not able to approve your request of recent result release." +
                    //       "\nPlease consult htrc for more details");
                    //send_email.sendEMail(userEmail, "HTRC Data Capsule Result Download URL", contentUser);

                    //construct email content for reviewer
                    String contentReviewer = String.format("Result \"%s\" has been rejected from users: %s",
                            resultid, send_email.userListToString(vmUserRoles, allowedVmUserRoles));
                    send_email.sendEMail(null, reviewer_email, "HTRC Data Capsule Result Has Been Rejected", contentReviewer);
                }

                //release and reject are both legal operations
                return Response.status(200).entity("Result " + status + " successfully!").build();

            } catch (SQLException e) {
                logger.error(e.getMessage(),e);
                return Response.status(500).
                        entity(new ErrorBean(500,"Internal error - " + e.getMessage())).build();
            } catch (NoItemIsFoundInDBException e) {
                logger.error("No Result with id " + resultid + " is found!", e);
                return Response.status(404)
                        .entity(new ErrorBean(404,"No Result with id " + resultid + " is found!"))
                        .build();
            }

    }

    /*
        This utility method is used to migrate all the result files saved in DB to a file system location
     */
    @PUT
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateResult(
            @FormParam("dir") String dir,
            @Context HttpHeaders httpHeaders,
            @Context HttpServletRequest httpServletRequest) {

        if(dir == null) {
            dir = Configuration.getInstance().getString(
                    Configuration.PropertyName.RESULT_FILES_DIR, "/tmp");
        }
        logger.info("Trying to migrate all result files to " + dir + " directory!");

        try {
            List<ReviewInfoBean> reviewInfo = DBOperations.getInstance().getReviewData();
            for(ReviewInfoBean reviewInfoBean : reviewInfo) {
                InputStream input = DBOperations.getInstance().getResultInputStream(reviewInfoBean.getResultid());
                if(input == null) {
                    logger.warn("Result file for result ID " + reviewInfoBean.getResultid() + " is NULL. " +
                            "Therefore not saved in file system!");
                    continue;
                }
                ResultUtils.saveResultFileToDir(reviewInfoBean.getResultid(), input, dir);
                logger.info("Result file with ID " + reviewInfoBean.getResultid() + " was written to file system.");
            }
        } catch (Exception e) {
            logger.error("Error occurred while migrating results to file system!", e);
            return Response.status(500).entity(new ErrorBean(500,
                            "Error occurred while migrating results to file system!")).build();
        }

        logger.info("All results successfully migrated to " + dir + " directory!");
        return Response.status(200).entity("All results successfully migrated to " + dir + " directory!").build();
    }


}