package edu.indiana.d2i.sloan.utils;

import edu.indiana.d2i.sloan.bean.VmUserRole;
import edu.indiana.d2i.sloan.db.DBOperations;
import edu.indiana.d2i.sloan.exception.NoItemIsFoundInDBException;
import edu.indiana.d2i.sloan.vm.VMRole;
import edu.indiana.d2i.sloan.vm.VMState;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class RolePermissionUtils {
    private static Logger logger = Logger.getLogger(RolePermissionUtils.class);

    public enum API_CMD {
        DELETE_VM, LAUNCH_VM, QUERY_VM, MIGRATE_VM, SWITCH_VM, STOP_VM, UPDATE_VM, ADD_SHAREES, UPDATE_SSH_KEY
    }

    public static boolean isPermittedCommand(String username, String vmid, API_CMD api_cmd)
            throws NoItemIsFoundInDBException, SQLException {

        VmUserRole vmUserRole = DBOperations.getInstance().getUserRoleWithVmid(username, vmid);
        VMRole user_role = vmUserRole.getRole();
        List<VmUserRole> roles = DBOperations.getInstance().getRolesWithVmid(vmid, true);
        VmUserRole owner = roles.stream()
                .filter(role -> role.getRole().equals(VMRole.OWNER_CONTROLLER) || role.getRole().equals(VMRole.OWNER))
                .collect(Collectors.toList()).get(0);

        boolean isPermitted = false;

        if(!vmUserRole.getTou() // if user has not yet accepted tou
                // if the VM has full access but user's full access request is not yet granted
                || ((owner.isFull_access() != null && owner.isFull_access())
                        && (vmUserRole.isFull_access() == null || !vmUserRole.isFull_access()))) {
            return isPermitted;
        }

        switch (user_role) {
            case OWNER_CONTROLLER:
                if (api_cmd == API_CMD.QUERY_VM
                        || api_cmd == API_CMD.DELETE_VM
                        || api_cmd == API_CMD.ADD_SHAREES
                        || api_cmd == API_CMD.UPDATE_VM
                        || api_cmd == API_CMD.LAUNCH_VM
                        || api_cmd == API_CMD.STOP_VM
                        || api_cmd == API_CMD.SWITCH_VM
                        || api_cmd == API_CMD.UPDATE_SSH_KEY) {
                    isPermitted = true;
                }
                break;

            case OWNER:
                if (api_cmd == API_CMD.QUERY_VM
                        || api_cmd == API_CMD.DELETE_VM
                        || api_cmd == API_CMD.ADD_SHAREES
                        || api_cmd == API_CMD.UPDATE_VM
                        || api_cmd == API_CMD.UPDATE_SSH_KEY) {
                    isPermitted = true;
                }
                break;

            case CONTROLLER:
                if (api_cmd == API_CMD.QUERY_VM
                        || api_cmd == API_CMD.LAUNCH_VM
                        || api_cmd == API_CMD.STOP_VM
                        || api_cmd == API_CMD.SWITCH_VM
                        || api_cmd == API_CMD.UPDATE_SSH_KEY) {
                    isPermitted = true;
                }
                break;

            case SHAREE:
                if (api_cmd == API_CMD.QUERY_VM
                        || api_cmd == API_CMD.UPDATE_SSH_KEY) {
                    isPermitted = true;
                }
                break;

            default :
                logger.error("Unknown vm role " + user_role);
        }

        return isPermitted;
    }

}
