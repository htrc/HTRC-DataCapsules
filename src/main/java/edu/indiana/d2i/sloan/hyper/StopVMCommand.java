package edu.indiana.d2i.sloan.hyper;

import org.apache.log4j.Logger;

import edu.indiana.d2i.sloan.bean.VmInfoBean;
import edu.indiana.d2i.sloan.exception.RetriableException;
import edu.indiana.d2i.sloan.exception.ScriptCmdErrorException;
import edu.indiana.d2i.sloan.vm.VMState;
import edu.indiana.d2i.sloan.vm.VMStateManager;

public class StopVMCommand extends HypervisorCommand {
	private static Logger logger = Logger.getLogger(StopVMCommand.class);

	public StopVMCommand(VmInfoBean vminfo) {
		super(vminfo);
	}

	@Override
	public void execute() throws Exception {

		try {

			HypervisorResponse resp = hypervisor.stopVM(vminfo);

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

		VMStateManager.getInstance().transitTo(vminfo.getVmid(),
				VMState.SHUTTINGDOWN, VMState.SHUTDOWN);

	}

	@Override
	public void cleanupOnFailed() throws Exception {
		VMStateManager.getInstance().transitTo(vminfo.getVmid(),
				VMState.SHUTTINGDOWN, VMState.ERROR);

	}

	@Override
	public String toString() {
		return "stopvm " + vminfo;
	}
}
