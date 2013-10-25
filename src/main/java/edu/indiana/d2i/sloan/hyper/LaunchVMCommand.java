package edu.indiana.d2i.sloan.hyper;

import org.apache.log4j.Logger;

import edu.indiana.d2i.sloan.bean.VmInfoBean;
import edu.indiana.d2i.sloan.db.DBOperations;
import edu.indiana.d2i.sloan.exception.RetriableException;
import edu.indiana.d2i.sloan.exception.ScriptCmdErrorException;
import edu.indiana.d2i.sloan.vm.VMMode;
import edu.indiana.d2i.sloan.vm.VMState;
import edu.indiana.d2i.sloan.vm.VMStateManager;

public class LaunchVMCommand extends HypervisorCommand {
	private static Logger logger = Logger.getLogger(LaunchVMCommand.class);

	public LaunchVMCommand(VmInfoBean vminfo) {
		super(vminfo);
	}

	@Override
	public void execute() throws Exception {

		try {
			HypervisorResponse resp = hypervisor.launchVM(vminfo);

			if (logger.isDebugEnabled()) {
				logger.debug(resp.toString());
			}

			if (resp.getResponseCode() != 0) {
				throw new ScriptCmdErrorException(String.format(
						"Failed to excute command:\n%s ", resp));
			}

		} catch (Exception e) {
			throw new RetriableException(e.getMessage(), e);
		}

		// update state
		VMStateManager.getInstance().transitTo(vminfo.getVmid(),
				VMState.LAUNCHING, VMState.RUNNING);

		// update mode
		assert vminfo.getVmmode().equals(VMMode.NOT_DEFINED);

		if (logger.isDebugEnabled()) {
			logger.debug(String.format(
					"Going to update VM (vmid = %s) mode in DB from %s to %s",
					vminfo.getVmid(), vminfo.getVmmode(),
					vminfo.getRequestedVMMode()));
		}

		DBOperations.getInstance().updateVMMode(vminfo.getVmid(),
				vminfo.getRequestedVMMode());

	}

	@Override
	public void cleanupOnFailed() throws Exception {
		VMStateManager.getInstance().transitTo(vminfo.getVmid(),
				VMState.LAUNCHING, VMState.ERROR);
	}

	@Override
	public String toString() {
		return "launchvm " + vminfo;
	}
}
