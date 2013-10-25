package edu.indiana.d2i.sloan.hyper;

import org.apache.log4j.Logger;

import edu.indiana.d2i.sloan.bean.VmInfoBean;
import edu.indiana.d2i.sloan.exception.ScriptCmdErrorException;
import edu.indiana.d2i.sloan.exception.RetriableException;
import edu.indiana.d2i.sloan.vm.VMState;
import edu.indiana.d2i.sloan.vm.VMStateManager;

public class QueryVMCommand extends HypervisorCommand {
	private static Logger logger = Logger.getLogger(QueryVMCommand.class);

	public QueryVMCommand(VmInfoBean vminfo) {
		super(vminfo);
	}

	@Override
	public void execute() throws Exception {

		HypervisorResponse resp = null;
		try {
			resp = hypervisor.queryVM(vminfo);

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

		/**
		 * if VM state returned by hypervisor is ERROR, then update VM state in
		 * DB accordingly
		 */
		VMState returnedState = VMState.valueOf(resp.getAttribute(
				HypervisorResponse.VM_STATUS_KEY).toUpperCase());

		if (returnedState.equals(VMState.ERROR)) {

			// set state to error
			VMStateManager.getInstance().transitTo(vminfo.getVmid(),
					vminfo.getVmstate(), VMState.ERROR);

		}
	}

	@Override
	public void cleanupOnFailed() throws Exception {
		VMStateManager.getInstance().transitTo(vminfo.getVmid(),
				vminfo.getVmstate(), VMState.ERROR);
	}

	@Override
	public String toString() {
		return "queryvm " + vminfo;
	}

}
