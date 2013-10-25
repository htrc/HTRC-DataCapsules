package edu.indiana.d2i.sloan.hyper;

import org.apache.log4j.Logger;

import edu.indiana.d2i.sloan.bean.VmInfoBean;
import edu.indiana.d2i.sloan.db.DBOperations;
import edu.indiana.d2i.sloan.exception.RetriableException;
import edu.indiana.d2i.sloan.exception.ScriptCmdErrorException;
import edu.indiana.d2i.sloan.vm.VMMode;
import edu.indiana.d2i.sloan.vm.VMState;
import edu.indiana.d2i.sloan.vm.VMStateManager;

public class SwitchVMCommand extends HypervisorCommand {

	private static Logger logger = Logger.getLogger(SwitchVMCommand.class);

	public SwitchVMCommand(VmInfoBean vminfo) {
		super(vminfo);
	}

	@Override
	public void execute() throws Exception {
		try {

			HypervisorResponse resp = hypervisor.switchVM(vminfo);

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
				vminfo.getVmstate(), VMState.RUNNING);

		// update mode

		assert vminfo.getVmmode().equals(VMMode.SECURE)
				|| vminfo.getVmmode().equals(VMMode.MAINTENANCE);

		VMMode targetMode = vminfo.getVmmode().equals(VMMode.SECURE)
				? VMMode.MAINTENANCE
				: VMMode.SECURE;

		if (logger.isDebugEnabled()) {
			logger.debug(String.format(
					"Going to update VM mode in DB from %s to %s",
					vminfo.getVmmode(), targetMode));
		}

		DBOperations.getInstance().updateVMMode(vminfo.getVmid(), targetMode);

	}

	@Override
	public void cleanupOnFailed() throws Exception {
		VMStateManager.getInstance().transitTo(vminfo.getVmid(),
				vminfo.getVmstate(), VMState.ERROR);

		// TODO: should we also update VM mode to NOT_DEFINED ?
	}

	@Override
	public String toString() {
		return "switchvm " + vminfo;
	}
}
