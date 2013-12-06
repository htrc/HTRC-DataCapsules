package edu.indiana.d2i.sloan.hyper;

import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import edu.indiana.d2i.sloan.bean.VmInfoBean;
import edu.indiana.d2i.sloan.db.DBOperations;
import edu.indiana.d2i.sloan.exception.ScriptCmdErrorException;
import edu.indiana.d2i.sloan.utils.RetriableTask;
import edu.indiana.d2i.sloan.vm.VMMode;
import edu.indiana.d2i.sloan.vm.VMState;
import edu.indiana.d2i.sloan.vm.VMStateManager;

public class SwitchVMCommand extends HypervisorCommand {

	private static Logger logger = Logger.getLogger(SwitchVMCommand.class);

	public SwitchVMCommand(VmInfoBean vminfo) throws Exception {
		super(vminfo);
	}

	@Override
	public void execute() throws Exception {
		HypervisorResponse resp = hypervisor.switchVM(vminfo);
		logger.info(resp);

		if (resp.getResponseCode() != 0) {
			throw new ScriptCmdErrorException(String.format(
					"Failed to excute command:\n%s ", resp));
		}

		RetriableTask<Void> r = new RetriableTask<Void>(
			new Callable<Void>() {
				@Override
				public Void call() throws Exception {
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
								"Going to update VM (vmid = %s) mode in DB from %s to %s",
								vminfo.getVmid(), vminfo.getVmmode(), targetMode));
					}

					DBOperations.getInstance().updateVMMode(vminfo.getVmid(), targetMode);
					return null;
				}
			},  1000, 3, 
			new HashSet<String>(Arrays.asList(java.sql.SQLException.class.getName())));
		r.call();
	}

	@Override
	public void cleanupOnFailed() throws Exception {
		RetriableTask<Void> r = new RetriableTask<Void>(
			new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					VMStateManager.getInstance().transitTo(vminfo.getVmid(),
							vminfo.getVmstate(), VMState.ERROR);
					return null;
				}
			},  1000, 3, 
			new HashSet<String>(Arrays.asList(java.sql.SQLException.class.getName())));
		r.call();

		// TODO: should we also update VM mode to NOT_DEFINED ?
	}

	@Override
	public String toString() {
		return "switchvm " + vminfo;
	}
}
