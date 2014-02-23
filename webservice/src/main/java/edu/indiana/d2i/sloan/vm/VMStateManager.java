/*******************************************************************************
 * Copyright 2014 The Trustees of Indiana University
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package edu.indiana.d2i.sloan.vm;

import java.sql.SQLException;

import org.apache.log4j.Logger;

import edu.indiana.d2i.sloan.db.DBOperations;
import edu.indiana.d2i.sloan.exception.NoItemIsFoundInDBException;

public class VMStateManager {
	private static Logger logger = Logger.getLogger(VMStateManager.class);
	private static VMStateManager instance = new VMStateManager();
	private VMStateManager() {

	}

	public static VMStateManager getInstance() {
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
