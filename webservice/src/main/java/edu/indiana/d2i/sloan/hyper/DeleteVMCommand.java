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

import edu.indiana.d2i.sloan.vm.PortsPool;
import edu.indiana.d2i.sloan.vm.VMPorts;
import edu.indiana.d2i.sloan.vm.VMState;
import edu.indiana.d2i.sloan.vm.VMStateManager;
import org.apache.log4j.Logger;

import edu.indiana.d2i.sloan.bean.VmInfoBean;
import edu.indiana.d2i.sloan.db.DBOperations;
import edu.indiana.d2i.sloan.exception.ScriptCmdErrorException;
import edu.indiana.d2i.sloan.utils.RetriableTask;

public class DeleteVMCommand extends HypervisorCommand {
	private static Logger logger = Logger.getLogger(DeleteVMCommand.class);
	private final String username;
	private final String operator;

	public DeleteVMCommand(String username, String operator, VmInfoBean vminfo) throws Exception {
		super(vminfo);
		this.username = username;
		this.operator = operator;
	}

	@Override
	public void execute() throws Exception {
		/*
		First call hypervisor and update DB table's(vms and vmactivity) state to DELETED upon successful hypervisor
		execution. Also restore user quota and release ports when the hypervisor call is successful.
		Does not update VmMode in the vms table
		 */
		HypervisorResponse resp = hypervisor.delete(vminfo);
		logger.info(resp);
		if (resp.getResponseCode() != 0) {
			throw new ScriptCmdErrorException(String.format(
					"Failed to excute command:\n%s ", resp));
		}

		RetriableTask<Void> r = new RetriableTask<Void>(
			new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					DBOperations.getInstance().deleteVMs(username, operator, vminfo);

					//remove ports allocated in PortsPool upon successful deletion
					PortsPool.getInstance().release(
							new VMPorts(vminfo.getPublicip(), vminfo.getSshport(), vminfo.getVncport()));

					return null;
				}
			},  1000, 3, 
			new HashSet<String>(Arrays.asList(java.sql.SQLException.class.getName())));
		r.call();
		

	}

	@Override
	public void cleanupOnFailed() throws Exception {
		/*
		   Update DB table's(vms and vmactivity) state to DELETE_ERROR upon failed hypervisor execution.
		   Also restore user quota when the hypervisor call is failed. Not releasing ports if there is an error.
		   Failed VMs will not be listed/shown to users and all API calls will return 'NoItemsFound' error for those VMs
		*/
		RetriableTask<Void> r = new RetriableTask<Void>(
			new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					VMStateManager.getInstance().transitTo(vminfo.getVmid(),
							vminfo.getVmstate(), VMState.DELETE_ERROR, operator);

					DBOperations.getInstance().restoreQuota(username,
							vminfo.getNumCPUs(), vminfo.getMemorySizeInMB(), vminfo.getVolumeSizeInGB());

					return null;
				}
			},  1000, 3,
			new HashSet<String>(Arrays.asList(java.sql.SQLException.class.getName())));
		r.call();
	}

	@Override
	public String toString() {
		return "deletevm " + vminfo;
	}
}
