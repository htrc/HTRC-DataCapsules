/*******************************************************************************
 * Copyright 2014 The Trustees of Indiana University
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package edu.indiana.d2i.sloan.hyper;

import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import edu.indiana.d2i.sloan.bean.VmInfoBean;
import edu.indiana.d2i.sloan.db.DBOperations;
import edu.indiana.d2i.sloan.exception.ScriptCmdErrorException;
import edu.indiana.d2i.sloan.utils.RetriableTask;
import edu.indiana.d2i.sloan.vm.VMMode;
import edu.indiana.d2i.sloan.vm.VMState;
import edu.indiana.d2i.sloan.vm.VMStateManager;

public class SwitchVMCommand extends HypervisorCommand {

	private static Logger logger = LoggerFactory.getLogger(SwitchVMCommand.class);
	private String operator;

	public SwitchVMCommand(VmInfoBean vminfo, String operator) throws Exception {
		super(vminfo);
		this.operator = operator;
	}

	@Override
	public void execute() throws Exception {
		HypervisorResponse resp = hypervisor.switchVM(vminfo);
		logger.info(resp.toString());

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
							vminfo.getVmstate(), VMState.RUNNING, operator);

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

					DBOperations.getInstance().updateVMMode(vminfo.getVmid(), targetMode, operator);
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
					logger.error("Failed to switch mode of VM '" + vminfo.getVmid() + "!");
					VMStateManager.getInstance().transitTo(vminfo.getVmid(),
							vminfo.getVmstate(), VMState.ERROR, operator);
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
