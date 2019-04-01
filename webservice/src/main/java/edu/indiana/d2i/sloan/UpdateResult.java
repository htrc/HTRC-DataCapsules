package edu.indiana.d2i.sloan;

import com.sun.org.apache.regexp.internal.RE;
import edu.indiana.d2i.sloan.bean.ErrorBean;
import edu.indiana.d2i.sloan.bean.UserBean;
import edu.indiana.d2i.sloan.bean.VmUserRole;
import edu.indiana.d2i.sloan.db.DBOperations;
import edu.indiana.d2i.sloan.exception.NoItemIsFoundInDBException;
import edu.indiana.d2i.sloan.utils.EmailUtil;
import edu.indiana.d2i.sloan.utils.RolePermissionUtils;
import edu.indiana.d2i.sloan.vm.VMRole;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.SQLException;
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

                DBOperations.getInstance().updateResult(resultid, status);
                logger.info("Updated results table - ResultID : " + resultid + " status : " + status);

                //Do not send email if rejected
                if(!status.equals("Rejected")) {
                    EmailUtil send_email = new EmailUtil();

                    //constructor fetch properites automatically
                    String download_url = Configuration.getInstance().
                            getString(Configuration.PropertyName.RESULT_DOWNLOAD_URL_PREFIX);
                    String download_addr = download_url +  resultid;

                    //construct email content for users
                    for (VmUserRole role : allowedVmUserRoles) {
                        String contentUser = "Dear Data Capsule user,\n\n" +
                                "Thank you for using the HTRC Data Capsule! You can download your result from the link below.\n" +
                                download_addr + "\n\n" +
                                "Please note this link is active for 24 hours.";
                        send_email.sendEMail(role.getEmail(), "HTRC Data Capsule Result Download URL", contentUser);
                    }
                    logger.info("Download result email sent to users " + allowedVmUserRoles + " - download URL : " + download_addr);

                    //construct email content for reviewer
                    String contentReviewer = String.format("Result \"%s\" has been released to users: %s",
                            resultid, userListToString(allowedVmUserRoles));
                    send_email.sendEMail(reviewer_email, "HTRC Data Capsule Result Has Been Successfully Released", contentReviewer);

                }else{
                    EmailUtil send_email = new EmailUtil();

                    //construct email content for user
                    //String contentUser = String.format("Unfortunately, we are not able to approve your request of recent result release." +
                    //       "\nPlease consult htrc for more details");
                    //send_email.sendEMail(userEmail, "HTRC Data Capsule Result Download URL", contentUser);

                    //construct email content for reviewer
                    String contentReviewer = String.format("Result \"%s\" has been rejected from users: %s",
                            resultid, userListToString(vmUserRoles));
                    send_email.sendEMail(reviewer_email, "HTRC Data Capsule Result Has Been Rejected", contentReviewer);
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

    private String userListToString(List<VmUserRole> vmUserRoles) {
        String str = "\n";
        for(VmUserRole vmUserRole : vmUserRoles) {
            str += "email : " + vmUserRole.getEmail() + ",\t" +
                    "role : " + vmUserRole.getRole() + ",\t" +
                    "TOU accepted : " + vmUserRole.getTou() + "\n";
        }
        return str;
    }

}