package edu.indiana.d2i.sloan.vm;

import java.sql.SQLException;
import java.util.List;

import org.apache.log4j.Logger;

import edu.indiana.d2i.sloan.bean.VmStatusBean;
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
	
	public synchronized boolean transitTo(String userName, String vmid, VMState target) 
		throws SQLException, NoItemIsFoundInDBException {
		// get current state from db
		List<VmStatusBean> status = DBOperations.getInstance().getVmStatus(userName, vmid);
		VMState current = VMState.valueOf(status.get(0).getState());
		
		if (target == VMState.ERROR) {
			DBOperations.getInstance().updateVMStatus(vmid, target);
			return true;
		}
		
		// check if current state can be transmitted
		boolean canTrasist = false;
		switch(current) {
			case BUILDING :
				if (target == VMState.SHUTTINGDOWN || 
					target == VMState.SHUTDOWN || target == VMState.DELETING) {
					canTrasist = true;
				} 
				break;
				
			case LAUNCHING :
				if (target == VMState.RUNNING || target == VMState.SHUTTINGDOWN || 
				    target == VMState.SHUTDOWN || target == VMState.DELETING) {
					canTrasist = true;
				} 
				break;
				
			case RUNNING:
				if (target == VMState.SHUTTINGDOWN || target == VMState.SHUTDOWN || 
					target == VMState.SWITCHING_TO_MAINTENANCE || target == VMState.SWITCHING_TO_SECURITY || 
					target == VMState.DELETING) {
					canTrasist = true;
				} 
				break;
				
			case SWITCHING_TO_MAINTENANCE:
				if (target == VMState.RUNNING || target == VMState.SHUTTINGDOWN || 
					target == VMState.SHUTDOWN || target == VMState.DELETING) {
					canTrasist = true;
				} 
				break;
				
			case SWITCHING_TO_SECURITY:
				if (target == VMState.RUNNING || target == VMState.SHUTTINGDOWN || 
					target == VMState.SHUTDOWN || target == VMState.DELETING) {
					canTrasist = true;
				} 
				break;
				
			case SHUTTINGDOWN:
				if (target == VMState.SHUTDOWN || target == VMState.DELETING) {
					canTrasist = true;
				} 
				break;
				
			case SHUTDOWN:
				if (target == VMState.LAUNCHING || target == VMState.DELETING) {
					canTrasist = true;
				} 
				break;
			
			case ERROR:
				if (target == VMState.DELETING) {
					canTrasist = true;
				}
				break;
			case DELETING:
				break;
				
			default:     
				logger.error("Unknown vm state " + current);
		}
		
		if (canTrasist) {
			DBOperations.getInstance().updateVMStatus(vmid, target);
			logger.info("Transit from " + current + " to " + target + " for vm " + vmid);
			return true;
		} else {
			logger.error("Cannot transit from " + current + " to " + target + " for vm " + vmid);
			return false;
		}
	}
}
