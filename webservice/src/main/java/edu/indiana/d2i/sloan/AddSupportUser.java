package edu.indiana.d2i.sloan;


import edu.indiana.d2i.sloan.bean.*;
import edu.indiana.d2i.sloan.db.DBOperations;
import edu.indiana.d2i.sloan.exception.NoItemIsFoundInDBException;
import edu.indiana.d2i.sloan.utils.EmailUtil;
import edu.indiana.d2i.sloan.utils.RolePermissionUtils;
import edu.indiana.d2i.sloan.vm.VMRole;
import edu.indiana.d2i.sloan.vm.VMState;
import edu.indiana.d2i.sloan.vm.VMType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Path("/addsupportuser")
public class AddSupportUser {
    private static final Logger logger = LoggerFactory.getLogger(AddSupportUser.class);
    private static final String DELETE = "DELETE";

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addSupportUser(@FormParam("vmId") String vmId,
                                   @Context HttpHeaders httpHeaders,
                                   @Context HttpServletRequest httpServletRequest) {
        String userName = httpServletRequest.getHeader(Constants.USER_NAME);

        String supportUserEmail = Configuration.getInstance().getString(Configuration.PropertyName.SUPPORT_USER_EMAIL);
        String supportUserGuid = Configuration.getInstance().getString(Configuration.PropertyName.SUPPORT_USER_GUID);

        if (userName == null) {
            logger.error("Username is not present in http header.");
            return Response
                    .status(400)
                    .entity(new ErrorBean(400,
                            "Username is not present in http header.")).build();
        }

        try {
            if (!RolePermissionUtils.isPermittedCommand(userName, vmId, RolePermissionUtils.API_CMD.ADD_SUPPORT_USER)) {
                return Response.status(400).entity(new ErrorBean(400,
                        "User " + userName + " cannot add collaborators on VM " + vmId)).build();
            }

            VmInfoBean vmInfo = DBOperations.getInstance().getVmInfo(userName, vmId);

            // don't allow to add support user if capsule is in delete* state
            if (vmInfo.getVmstate().name().contains(DELETE)){
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorBean(400, "Cannot add support user when capsule is in "
                                + DELETE + "* state!")).build();
            }

//            if(vmInfo.getType().equals(VMType.DEMO.getName())) {
//                return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorBean(400,
//                        "Cannot add support user to a " + VMType.DEMO.getName() +" capsule!")).build();
//            }

            // set full_access of the support user as null if not requested for full access already
            // set this to false if VM has requested full access
            Boolean full_access = vmInfo.isFull_access();

            List<VmUserRole> current_roles = DBOperations.getInstance().getRolesWithVmid(vmId, true);

            if(current_roles.stream().anyMatch(role -> role.getGuid().equals(supportUserGuid))) {
                logger.warn("Support User " + supportUserEmail + " is already exists in capsule " + vmId + "!");
                return Response.status(400).entity(new ErrorBean(400,
                        "Support User " + supportUserEmail + " is already exists in capsule " + vmId + "!")).build();
            }

            logger.info("User " + userName + " tries to add HTRC Support User " + supportUserEmail  + " for the vm " + vmId);

            DBOperations.getInstance().insertUserIfNotExists(supportUserGuid, supportUserEmail);  // add to users table
            VmUserRole vmSupportUserRole = new VmUserRole(supportUserEmail, VMRole.SHAREE, true, supportUserGuid, full_access);
            DBOperations.getInstance().addVmSharee(vmId, vmSupportUserRole, "HTRC support user"); // add to uservmmap table

            boolean pub_key_exists = DBOperations.getInstance().getUserPubKey(userName) != null;
            boolean tou = DBOperations.getInstance().getUserTOU(userName);
            VmUserRole vmUserRole = DBOperations.getInstance().getUserRoleWithVmid(userName, vmId);
            List<VmStatusBean> status = new ArrayList<VmStatusBean>();
            vmInfo = DBOperations.getInstance().getVmInfo(userName, vmId);
            status.add(new VmStatusBean(vmInfo, pub_key_exists, tou, vmUserRole));

            // send vminfo back with added guids, AG then sends full_access request email containing all added users
            // if VM's full_access is true or false
            return Response.status(200).entity(new QueryVmResponseBean(status)).build();

            
        } catch (NoItemIsFoundInDBException | SQLException e) {
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
