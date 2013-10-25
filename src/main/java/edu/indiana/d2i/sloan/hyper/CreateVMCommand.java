package edu.indiana.d2i.sloan.hyper;

import org.apache.log4j.Logger;

import edu.indiana.d2i.sloan.bean.VmInfoBean;
import edu.indiana.d2i.sloan.exception.RetriableException;
import edu.indiana.d2i.sloan.exception.ScriptCmdErrorException;
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

			HypervisorResponse resp = hypervisor.createVM(vminfo);

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
				VMState.BUILDING, VMState.SHUTDOWN);

		// no need to update mode since web service layer should already set VM
		// mode to NOT_DEFINED
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
