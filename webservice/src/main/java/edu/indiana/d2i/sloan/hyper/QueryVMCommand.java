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

import org.apache.log4j.Logger;

import edu.indiana.d2i.sloan.bean.VmInfoBean;
import edu.indiana.d2i.sloan.exception.ScriptCmdErrorException;
import edu.indiana.d2i.sloan.utils.RetriableTask;
import edu.indiana.d2i.sloan.vm.VMState;
import edu.indiana.d2i.sloan.vm.VMStateManager;

public class QueryVMCommand extends HypervisorCommand {
	private static Logger logger = Logger.getLogger(QueryVMCommand.class);

	public QueryVMCommand(VmInfoBean vminfo) throws Exception {
		super(vminfo);
	}

	@Override
	public void execute() throws Exception {
		HypervisorResponse resp = hypervisor.queryVM(vminfo);
		logger.info(resp);

		if (resp.getResponseCode() != 0) {
			throw new ScriptCmdErrorException(String.format(
					"Failed to excute command:\n%s ", resp));
		}

		/**
		 * if VM state returned by hypervisor is ERROR, then update VM state in
		 * DB accordingly
		 */
		VMState returnedState = resp.getVmState();
		if (returnedState == VMState.ERROR) {
			RetriableTask<Void> r = new RetriableTask<Void>(
				new Callable<Void>() {
					@Override
					public Void call() throws Exception {
						// set state to error
						VMStateManager.getInstance().transitTo(vminfo.getVmid(),
								vminfo.getVmstate(), VMState.ERROR);
						return null;
					}
				},  1000, 3, 
				new HashSet<String>(Arrays.asList(java.sql.SQLException.class.getName())));
			r.call();
		}
	}

	@Override
	public void cleanupOnFailed() throws Exception {
		// set to error?
		VMStateManager.getInstance().transitTo(vminfo.getVmid(),
				vminfo.getVmstate(), VMState.ERROR);
	}

	@Override
	public String toString() {
		return "queryvm " + vminfo;
	}

}
