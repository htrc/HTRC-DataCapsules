package edu.indiana.d2i.sloan.utils;

import edu.indiana.d2i.sloan.bean.VmInfoBean;
import edu.indiana.d2i.sloan.bean.VmUserRole;
import edu.indiana.d2i.sloan.db.DBOperations;
import edu.indiana.d2i.sloan.exception.NoItemIsFoundInDBException;
import edu.indiana.d2i.sloan.vm.VMRole;
import edu.indiana.d2i.sloan.vm.VMState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RolePermissionUtils {
    private static Logger logger = LoggerFactory.getLogger(RolePermissionUtils.class);
    private static final String DELETE = "DELETE";

    public enum API_CMD {
        DELETE_VM, LAUNCH_VM, QUERY_VM, MIGRATE_VM, SWITCH_VM, STOP_VM, UPDATE_VM, ADD_SHAREES, UPDATE_SSH_KEY,
        DELETE_SHAREES, OBTAIN_CONTROLLER, VIEW_RESULT, EXIT_SHAREE, ADD_SUPPORT_USER
    }

    public enum CNTR_ACTION {
        DELEGATE("DELEGATE"),
        REVOKE("REVOKE");
        private final String name;
        CNTR_ACTION(String name) {
            this.name = name;
        }
        public String getName() {
            return this.name;
        }
    }

    public static boolean isPermittedCommand(String username, String vmid, API_CMD api_cmd)
            throws NoItemIsFoundInDBException, SQLException {

        VmUserRole vmUserRole = DBOperations.getInstance().getUserRoleWithVmid(username, vmid);
        VMRole user_role = vmUserRole.getRole();
        VmUserRole owner = DBOperations.getInstance().getOwnerOfVM(vmid);

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
                        || api_cmd == API_CMD.DELETE_SHAREES
                        || api_cmd == API_CMD.UPDATE_VM
                        || api_cmd == API_CMD.LAUNCH_VM
                        || api_cmd == API_CMD.STOP_VM
                        || api_cmd == API_CMD.SWITCH_VM
                        || api_cmd == API_CMD.MIGRATE_VM
                        || api_cmd == API_CMD.UPDATE_SSH_KEY
                        || api_cmd == API_CMD.VIEW_RESULT
                        || api_cmd == API_CMD.ADD_SUPPORT_USER) {
                    isPermitted = true;
                }
                break;

            case OWNER:
                if (api_cmd == API_CMD.QUERY_VM
                        || api_cmd == API_CMD.DELETE_VM
                        || api_cmd == API_CMD.ADD_SHAREES
                        || api_cmd == API_CMD.DELETE_SHAREES
                        || api_cmd == API_CMD.UPDATE_VM
                        || api_cmd == API_CMD.MIGRATE_VM
                        || api_cmd == API_CMD.UPDATE_SSH_KEY
                        || api_cmd == API_CMD.OBTAIN_CONTROLLER
                        || api_cmd == API_CMD.VIEW_RESULT
                        || api_cmd == API_CMD.ADD_SUPPORT_USER) {
                    isPermitted = true;
                }
                break;

            case CONTROLLER:
                if (api_cmd == API_CMD.QUERY_VM
                        || api_cmd == API_CMD.LAUNCH_VM
                        || api_cmd == API_CMD.STOP_VM
                        || api_cmd == API_CMD.SWITCH_VM
                        || api_cmd == API_CMD.UPDATE_SSH_KEY
                        || api_cmd == API_CMD.VIEW_RESULT
                        || api_cmd == API_CMD.ADD_SUPPORT_USER) {
                    isPermitted = true;
                }
                break;

            case SHAREE:
                if (api_cmd == API_CMD.QUERY_VM
                        || api_cmd == API_CMD.UPDATE_SSH_KEY
                        || api_cmd == API_CMD.OBTAIN_CONTROLLER
                        || api_cmd == API_CMD.VIEW_RESULT
                        || api_cmd == API_CMD.EXIT_SHAREE
                        || api_cmd == API_CMD.ADD_SUPPORT_USER) {
                    isPermitted = true;
                }
                break;

            default :
                logger.error("Unknown vm role " + user_role);
        }

        return isPermitted;
    }

    public static Map<VMRole, VMRole> getValidCntrlAction(String vmid, VmUserRole owner,
                                                         VmUserRole user, CNTR_ACTION action)
            throws NoItemIsFoundInDBException, SQLException {

        // check if owner can perform controller action on user,
        // and if so, return the map of resulting roles for owner and user
        Map<VMRole, VMRole> roles_map = null;

        if(action  == CNTR_ACTION.DELEGATE) {
            // owner-controller delegates controller to a sharee and becomes owner, sharee becomes controller
            if(owner.getRole() == VMRole.OWNER_CONTROLLER
                    && user.getRole() == VMRole.SHAREE
                    && isPermittedCommand(user.getGuid(), vmid, API_CMD.OBTAIN_CONTROLLER)) {
                roles_map = new HashMap<>();
                roles_map.put(VMRole.OWNER, VMRole.CONTROLLER);
            }

            // controller delegates controller to sharee and becomes sharee, sharee becomes controller
            if(owner.getRole() == VMRole.CONTROLLER
                    && user.getRole() == VMRole.SHAREE
                    && isPermittedCommand(user.getGuid(), vmid, API_CMD.OBTAIN_CONTROLLER)) {
                roles_map = new HashMap<>();
                roles_map.put(VMRole.SHAREE, VMRole.CONTROLLER);
            }

            // controller delegates controller to owner and becomes sharee, owner becomes owner-controller
            if(owner.getRole() == VMRole.CONTROLLER
                    && user.getRole() == VMRole.OWNER
                    && isPermittedCommand(user.getGuid(), vmid, API_CMD.OBTAIN_CONTROLLER)) {
                roles_map = new HashMap<>();
                roles_map.put(VMRole.SHAREE, VMRole.OWNER_CONTROLLER);
            }

        } else if (action == CNTR_ACTION.REVOKE) {
            // owner revokes controller role from controller and becomes owner-controller, controller becomes sharee
            if(owner.getRole() == VMRole.OWNER
                    && user.getRole() == VMRole.CONTROLLER) {
                roles_map = new HashMap<>();
                roles_map.put(VMRole.OWNER_CONTROLLER, VMRole.SHAREE);
            }
        }

        return roles_map;
    }

    public static boolean isPermittedToUpdateKey(String username, VmInfoBean vminfo, API_CMD api_cmd)
            throws SQLException {
        // update the public key of VMs that are not in ERROR or DELETE* state
        // && accepted tou && pubkey is not null
        // && does have full_access for VM's which are already granted full_access

        if(vminfo.getVmstate() == VMState.ERROR || vminfo.getVmstate().name().contains(DELETE)) {
            return false;
        }

        try {
            String pubkey = DBOperations.getInstance().getUserPubKey(username);
            if (pubkey == null)
                return false;

            return isPermittedCommand(username, vminfo.getVmid(), api_cmd);
        } catch (UnsupportedEncodingException e) {
            logger.debug("No public key in DB for user " + username);
        } catch (NoItemIsFoundInDBException e) {
            logger.debug("VM " + vminfo.getVmid() +  " is not associated with user " + username);
        }

        return false;
    }

    public static List<VmUserRole> filterPermittedRoles(List<VmUserRole> vmUserRoles, String vmid, API_CMD api_cmd)
            throws NoItemIsFoundInDBException, SQLException {
        List<VmUserRole> filteredList = new ArrayList<VmUserRole>();
        for (VmUserRole vmUserRole : vmUserRoles) {
            if (isPermittedCommand(vmUserRole.getGuid(), vmid, api_cmd)) {
                filteredList.add(vmUserRole);
            }
        }
        return filteredList;
    }
}
