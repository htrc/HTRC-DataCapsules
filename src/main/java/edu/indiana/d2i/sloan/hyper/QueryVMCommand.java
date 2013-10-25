package edu.indiana.d2i.sloan.hyper;

import org.apache.log4j.Logger;

import edu.indiana.d2i.sloan.bean.VmInfoBean;
import edu.indiana.d2i.sloan.exception.StateTransitionException;
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

		} catch (Exception e) {
			throw new RetriableException(e.getMessage(), e);
		}

		/**
		 * check whether the transition of VM state in DB to the one reported by
		 * hyperviosr is possible, if not, set state to error.
		 */

		VMState returnedState = VMState.valueOf(resp
				.getAttribute(HypervisorResponse.VM_STATUS_KEY));

		if (VMStateManager
				.isValidTransition(vminfo.getVmstate(), returnedState)) {

			// set state to error
			VMStateManager.getInstance().transitTo(vminfo.getVmid(),
					vminfo.getVmstate(), VMState.ERROR);

			throw new StateTransitionException(
					String.format(
							"VM state transition error, VM state in DB is %s, returned by hyperviosr is %s",
							vminfo.getVmstate().toString(),
							returnedState.toString()));
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
