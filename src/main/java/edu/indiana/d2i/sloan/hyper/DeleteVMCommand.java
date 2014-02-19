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
import edu.indiana.d2i.sloan.db.DBOperations;
import edu.indiana.d2i.sloan.exception.ScriptCmdErrorException;
import edu.indiana.d2i.sloan.utils.RetriableTask;

public class DeleteVMCommand extends HypervisorCommand {
	private static Logger logger = Logger.getLogger(DeleteVMCommand.class);
	private final String username;

	public DeleteVMCommand(String username, VmInfoBean vminfo) throws Exception {
		super(vminfo);
		this.username = username;
	}

	@Override
	public void execute() throws Exception {
		// Update DB first, then call hypervisor. Such order guarantee that user will always
		// see VM is removed although it might not be the case in the backend.
		// No need to update VM' state and mode since it is going to be deleted.
		/* Also restore user quota after deleting the VM */
		RetriableTask<Void> r = new RetriableTask<Void>(
			new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					DBOperations.getInstance().deleteVMs(username, vminfo);
					return null;
				}
			},  1000, 3, 
			new HashSet<String>(Arrays.asList(java.sql.SQLException.class.getName())));
		r.call();
		
		HypervisorResponse resp = hypervisor.delete(vminfo);
		logger.info(resp);
		if (resp.getResponseCode() != 0) {
			throw new ScriptCmdErrorException(String.format(
					"Failed to excute command:\n%s ", resp));
		}
	}

	@Override
	public void cleanupOnFailed() throws Exception {
		// don't mark error here because we don't want to indicate user there is an error
//		RetriableTask<Void> r = new RetriableTask<Void>(
//			new Callable<Void>() {
//				@Override
//				public Void call() throws Exception {
//					VMStateManager.getInstance().transitTo(vminfo.getVmid(),
//							vminfo.getVmstate(), VMState.ERROR);
//					return null;
//				}
//			},  1000, 3, 
//			new HashSet<String>(Arrays.asList(java.sql.SQLException.class.getName())));
//		r.call();
	}

	@Override
	public String toString() {
		return "deletevm " + vminfo;
	}
}
