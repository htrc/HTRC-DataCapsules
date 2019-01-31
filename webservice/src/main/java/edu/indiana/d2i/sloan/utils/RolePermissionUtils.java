package edu.indiana.d2i.sloan.utils;

import edu.indiana.d2i.sloan.bean.VmUserRole;
import edu.indiana.d2i.sloan.db.DBOperations;
import edu.indiana.d2i.sloan.exception.NoItemIsFoundInDBException;
import edu.indiana.d2i.sloan.vm.VMRole;
import edu.indiana.d2i.sloan.vm.VMState;
import org.apache.log4j.Logger;

import java.sql.SQLException;

public class RolePermissionUtils {
    private static Logger logger = Logger.getLogger(RolePermissionUtils.class);

    public enum API_CMD {
        DELETE_VM, LAUNCH_VM, QUERY_VM, MIGRATE_VM, SWITCH_VM, STOP_VM, UPDATE_VM, ADD_SHAREES
    }

    public static boolean isPermittedCommand(String username, String vmid, API_CMD api_cmd)
            throws NoItemIsFoundInDBException, SQLException {
        // check if current state can be transmitted
        VmUserRole vmUserRole = DBOperations.getInstance().getUserRoleWithVmid(username, vmid);
        VMRole role = vmUserRole.getRole();

        boolean isPermitted = false;

        if(!vmUserRole.getTou() || vmUserRole.isFull_access() == false) {
            return isPermitted;
        }

        switch (role) {
            case OWNER_CONTROLLER:
                if (api_cmd == API_CMD.QUERY_VM
                        || api_cmd == API_CMD.DELETE_VM
                        || api_cmd == API_CMD.ADD_SHAREES
                        || api_cmd == API_CMD.UPDATE_VM
                        || api_cmd == API_CMD.LAUNCH_VM
                        || api_cmd == API_CMD.STOP_VM
                        || api_cmd == API_CMD.SWITCH_VM) {
                    isPermitted = true;
                }
                break;

            case OWNER:
                if (api_cmd == API_CMD.QUERY_VM
                        || api_cmd == API_CMD.DELETE_VM
                        || api_cmd == API_CMD.ADD_SHAREES
                        || api_cmd == API_CMD.UPDATE_VM) {
                    isPermitted = true;
                }
                break;

            case CONTROLLER:
                if (api_cmd == API_CMD.QUERY_VM
                        || api_cmd == API_CMD.LAUNCH_VM
                        || api_cmd == API_CMD.STOP_VM
                        || api_cmd == API_CMD.SWITCH_VM) {
                    isPermitted = true;
                }
                break;

            case SHAREE:
                if (api_cmd == API_CMD.QUERY_VM) {
                    isPermitted = true;
                }
                break;

            default :
                logger.error("Unknown vm role " + role);
        }

        return isPermitted;
    }

}
