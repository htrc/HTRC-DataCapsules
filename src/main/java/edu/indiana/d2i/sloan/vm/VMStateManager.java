package edu.indiana.d2i.sloan.vm;

import java.sql.SQLException;

import org.apache.log4j.Logger;

import edu.indiana.d2i.sloan.db.DBOperations;
import edu.indiana.d2i.sloan.exception.NoItemIsFoundInDBException;

public class VMStateManager {
	private static Logger logger = Logger.getLogger(VMStateManager.class);
	private static VMStateManager instance = null;
	private VMStateManager() {

	}

	public static synchronized VMStateManager getInstance() {
		if (instance == null) {
			instance = new VMStateManager();
		}
		return instance;
	}
	
	public static boolean isPendingState(VMState state) {
		return (state == VMState.CREATE_PENDING || state == VMState.LAUNCH_PENDING ||
				state == VMState.SHUTDOWN_PENDING || state == VMState.DELETE_PENDING ||
				state == VMState.SWITCH_TO_MAINTENANCE_PENDING || 
				state == VMState.SWITCH_TO_SECURE_PENDING)
				? true: false;
	}

	public static boolean isValidTransition(VMState src, VMState target) {
		// check if current state can be transmitted
		boolean canTrasist = false;
		switch (src) {
			case CREATE_PENDING :
				if (target == VMState.SHUTDOWN_PENDING
						|| target == VMState.SHUTDOWN
						|| target == VMState.DELETE_PENDING) {
					canTrasist = true;
				}
				break;

			case LAUNCH_PENDING:
				if (target == VMState.RUNNING || target == VMState.SHUTDOWN_PENDING
						|| target == VMState.SHUTDOWN
						|| target == VMState.DELETE_PENDING) {
					canTrasist = true;
				}
				break;

			case RUNNING :
				if (target == VMState.SHUTDOWN_PENDING
						|| target == VMState.SHUTDOWN
						|| target == VMState.SWITCH_TO_MAINTENANCE_PENDING
						|| target == VMState.SWITCH_TO_SECURE_PENDING
						|| target == VMState.DELETE_PENDING) {
					canTrasist = true;
				}
				break;

			case SWITCH_TO_MAINTENANCE_PENDING :
				if (target == VMState.RUNNING || target == VMState.SHUTDOWN_PENDING
						|| target == VMState.SHUTDOWN
						|| target == VMState.DELETE_PENDING) {
					canTrasist = true;
				}
				break;

			case SWITCH_TO_SECURE_PENDING :
				if (target == VMState.RUNNING || target == VMState.SHUTDOWN_PENDING
						|| target == VMState.SHUTDOWN
						|| target == VMState.DELETE_PENDING) {
					canTrasist = true;
				}
				break;

			case SHUTDOWN_PENDING :
				if (target == VMState.SHUTDOWN || target == VMState.DELETE_PENDING) {
					canTrasist = true;
				}
				break;

			case SHUTDOWN :
				if (target == VMState.LAUNCH_PENDING || target == VMState.DELETE_PENDING) {
					canTrasist = true;
				}
				break;

			case ERROR :
				if (target == VMState.DELETE_PENDING) {
					canTrasist = true;
				}
				break;
			case DELETE_PENDING :
				break;

			default :
				logger.error("Unknown vm state " + src);
		}

		return canTrasist;
	}

	public synchronized boolean transitTo(String vmid, VMState src,
			VMState target) throws SQLException, NoItemIsFoundInDBException {
		if (target == VMState.ERROR) {
			DBOperations.getInstance().updateVMState(vmid, target);
			return true;
		}

		if (isValidTransition(src, target)) {
			DBOperations.getInstance().updateVMState(vmid, target);
			logger.info("Transit from " + src + " to " + target + " for vm "
					+ vmid);
			return true;
		} else {
			logger.error("Cannot transit from " + src + " to " + target
					+ " for vm " + vmid);
			return false;
		}
	}
}
