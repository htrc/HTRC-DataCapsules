package edu.indiana.d2i.sloan.hyper;

import java.sql.SQLException;

import org.apache.log4j.Logger;

import edu.indiana.d2i.sloan.bean.VmInfoBean;
import edu.indiana.d2i.sloan.exception.NoItemIsFoundInDBException;
import edu.indiana.d2i.sloan.exception.NonRetryableException;
import edu.indiana.d2i.sloan.exception.RetriableException;
import edu.indiana.d2i.sloan.vm.VMState;
import edu.indiana.d2i.sloan.vm.VMStateManager;

public class CreateVMCommand extends HypervisorCommand {
	private static Logger logger = Logger.getLogger(CreateVMCommand.class);

	public CreateVMCommand(VmInfoBean vminfo) {
		super(vminfo);
	}

	@Override
	public void execute() throws RetriableException, NonRetryableException {

		try {
			// call hypervisor layer
			HypervisorResponse resp = hypervisor.createVM(vminfo);

			if (logger.isDebugEnabled()) {
				logger.debug(resp.toString());
			}

		} catch (Exception e) {
			throw new RetriableException(e.getMessage(), e);
		}

		try {
			// update vm status and login info
			VMStateManager.getInstance().transitTo(vminfo.getVmid(),
					VMState.BUILDING, VMState.SHUTDOWN);
		} catch (NoItemIsFoundInDBException e) {
			throw new NonRetryableException(e.getMessage(), e);
		} catch (SQLException e) {
			throw new NonRetryableException(e.getMessage(), e);
		}
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
