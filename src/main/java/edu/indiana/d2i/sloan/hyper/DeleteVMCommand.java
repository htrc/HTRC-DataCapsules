package edu.indiana.d2i.sloan.hyper;

import org.apache.log4j.Logger;

import edu.indiana.d2i.sloan.bean.VmInfoBean;
import edu.indiana.d2i.sloan.exception.RetriableException;
import edu.indiana.d2i.sloan.vm.VMState;
import edu.indiana.d2i.sloan.vm.VMStateManager;

public class DeleteVMCommand extends HypervisorCommand {
	private static Logger logger = Logger.getLogger(DeleteVMCommand.class);

	public DeleteVMCommand(VmInfoBean vminfo) {
		super(vminfo);
	}

	@Override
	public void execute() throws Exception {
		try {
			HypervisorResponse resp = hypervisor.delete(vminfo);

			if (logger.isDebugEnabled()) {
				logger.debug(resp.toString());
			}

		} catch (Exception e) {
			throw new RetriableException(e.getMessage(), e);
		}

		// no need to update VM' state since it is deleted
	}

	@Override
	public void cleanupOnFailed() throws Exception {
		VMStateManager.getInstance().transitTo(vminfo.getVmid(),
				VMState.DELETING, VMState.ERROR);

	}

	@Override
	public String toString() {
		return "deletevm " + vminfo;
	}
}
