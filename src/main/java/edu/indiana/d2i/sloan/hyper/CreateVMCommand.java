package edu.indiana.d2i.sloan.hyper;


import org.apache.log4j.Logger;

import edu.indiana.d2i.sloan.bean.VmInfoBean;
import edu.indiana.d2i.sloan.exception.RetriableException;
import edu.indiana.d2i.sloan.vm.VMState;
import edu.indiana.d2i.sloan.vm.VMStateManager;

public class CreateVMCommand extends HypervisorCommand {
	private static Logger logger = Logger.getLogger(CreateVMCommand.class);

	public CreateVMCommand(VmInfoBean vminfo) {
		super(vminfo);
	}

	@Override
	public void execute() throws Exception {

		try {
			// call hypervisor layer
			HypervisorResponse resp = hypervisor.createVM(vminfo);

			if (logger.isDebugEnabled()) {
				logger.debug(resp.toString());
			}

		} catch (Exception e) {
			throw new RetriableException(e.getMessage(), e);
		}

		// update vm status and login info
		VMStateManager.getInstance().transitTo(vminfo.getVmid(),
			VMState.BUILDING, VMState.SHUTDOWN);
	}

	@Override
	public void cleanupOnFailed() throws Exception {
		VMStateManager.getInstance().transitTo(vminfo.getVmid(),
				VMState.BUILDING, VMState.ERROR);
	}

	@Override
	public String toString() {
		return "createvm " + vminfo;
	}
}
