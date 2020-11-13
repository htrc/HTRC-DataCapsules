package edu.indiana.d2i.sloan;

import edu.indiana.d2i.sloan.bean.ErrorBean;
import edu.indiana.d2i.sloan.bean.VmInfoBean;
import edu.indiana.d2i.sloan.bean.VmUserRole;
import edu.indiana.d2i.sloan.db.DBOperations;
import edu.indiana.d2i.sloan.exception.NoItemIsFoundInDBException;
import edu.indiana.d2i.sloan.hyper.DeletePublicKeyCommand;
import edu.indiana.d2i.sloan.hyper.HypervisorProxy;
import edu.indiana.d2i.sloan.utils.EmailUtil;
import edu.indiana.d2i.sloan.utils.RolePermissionUtils;
import edu.indiana.d2i.sloan.vm.VMRole;
import edu.indiana.d2i.sloan.vm.VMState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Path("/exitsharee")
public class ExitSharee {
    private static Logger logger = LoggerFactory.getLogger(ExitSharee.class);

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response exitSharee(@FormParam("vmId") String vmId,
                               @Context HttpHeaders httpHeaders,
                               @Context HttpServletRequest httpServletRequest) {
        String userName = httpServletRequest.getHeader(Constants.USER_NAME);

        if (userName == null) {
            logger.error("Username is not present in http header.");
            return Response
                    .status(400)
                    .entity(new ErrorBean(400,
                            "Username is not present in http header.")).build();
        }

        try {
            if (!RolePermissionUtils.isPermittedCommand(userName, vmId, RolePermissionUtils.API_CMD.EXIT_SHAREE)) {
                return Response.status(400).entity(new ErrorBean(400,
                        "User " + userName + " cannot exit from VM " + vmId)).build();
            }

            VmInfoBean vmInfo = DBOperations.getInstance().getVmInfo(userName, vmId);
            // don't allow to Exit sharee if capsule is not in SHUTDOWN or RUNNING state
            if (!(vmInfo.getVmstate() == VMState.SHUTDOWN
                    || vmInfo.getVmstate() == VMState.RUNNING)){
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorBean(400, "Cannot exit from capsule when capsule is not in "
                                + VMState.RUNNING + " or " + VMState.SHUTDOWN)).build();
            }

            logger.info("User " + userName + " tries to exit from capsule " + vmId);

            // remove users keys from the data capsule backend
            String pubKey = DBOperations.getInstance().getUserPubKey(userName);
            HypervisorProxy.getInstance().addCommand(
                    new DeletePublicKeyCommand(vmInfo, userName, userName, pubKey));


            // remove users from the Database.
            // This removes users from the users table too, if the user don't have other capsules owned

            List<String> sharee_list = Collections.singletonList(userName);

            String ownerEmail = null;
            String shareeEmail = null;
            List<VmUserRole> vmUserRoles = DBOperations.getInstance().getRolesWithVmid(vmId, true);
            for(VmUserRole vmUserRole: vmUserRoles){
                if(vmUserRole.getRole()== VMRole.OWNER || vmUserRole.getRole()== VMRole.OWNER_CONTROLLER){
                    ownerEmail = vmUserRole.getEmail();
                }
                if(vmUserRole.getGuid().equals(userName)){
                    shareeEmail = vmUserRole.getEmail();
                }
            }

            DBOperations.getInstance().removeVmSharee(vmId, sharee_list);

            EmailUtil email_util = new EmailUtil();
            String email_body = "Dear Data Capsule owner,\n"
                    + "HTRC user with email " + shareeEmail + " has left the Data Capusle(" + vmId + ") owned by you." +
                    "\nBest" +
                    "\nHTRC Team";
            email_util.sendEMail(null, ownerEmail, "HTRC Data Capsule Collaborator has exited from your Capsule",
                    email_body);
            logger.info("Email notification on sharee("+shareeEmail+") exit is sent to " + ownerEmail);

            return Response.status(200).build();
        } catch (NoItemIsFoundInDBException e) {
            logger.error(e.getMessage(), e);
            return Response
                    .status(400)
                    .entity(new ErrorBean(400, "Cannot find a VM " + vmId
                            + " associated with username " + userName)).build();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return Response.status(500)
                    .entity(new ErrorBean(500, e.getMessage())).build();
        }
    }
}
