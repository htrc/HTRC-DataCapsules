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

import edu.indiana.d2i.sloan.bean.VmInfoBean;
import edu.indiana.d2i.sloan.db.DBOperations;
import edu.indiana.d2i.sloan.exception.ScriptCmdErrorException;
import edu.indiana.d2i.sloan.utils.RetriableTask;
import edu.indiana.d2i.sloan.vm.VMMode;
import edu.indiana.d2i.sloan.vm.VMState;
import edu.indiana.d2i.sloan.vm.VMStateManager;
import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.Callable;

public class MigrateVMCommand extends HypervisorCommand {
	private static Logger logger = Logger.getLogger(MigrateVMCommand.class);
	private String operator, host;
	private int sshport, vncport;

	public MigrateVMCommand(VmInfoBean vminfo, String operator, String host, int vncport, int sshport) throws Exception {
		super(vminfo);
		this.operator = operator;
		this.host = host;
		this.vncport = vncport;
		this.sshport = sshport;
	}

	@Override
	public void execute() throws Exception {
		HypervisorResponse resp = hypervisor.migrateVM(vminfo, host, vncport, sshport);
		logger.info(resp);

		if (resp.getResponseCode() != 0) {
			throw new ScriptCmdErrorException(String.format(
					"Failed to excute command:\n%s ", resp));
		}
		
		RetriableTask<Void> r = new RetriableTask<Void>(
			new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					VMStateManager.getInstance().transitTo(vminfo.getVmid(),
							vminfo.getVmstate(), VMState.SHUTDOWN, operator);

						// TODO update mode ??
						assert vminfo.getVmmode().equals(VMMode.MAINTENANCE)
								|| vminfo.getVmmode().equals(VMMode.SECURE);

						//TODO update new host and 2 ports
						/*if (logger.isDebugEnabled()) {
							logger.debug(String.format(
									"Going to update VM (vmid = %s) mode in DB from %s to %s",
									vminfo.getVmid(), vminfo.getVmmode(), VMMode.NOT_DEFINED));
						}

						DBOperations.getInstance().updateVMMode(vminfo.getVmid(),
								VMMode.NOT_DEFINED, operator);*/
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
							vminfo.getVmstate(), VMState.ERROR, operator);
					return null;
				}
			},  1000, 3, 
			new HashSet<String>(Arrays.asList(java.sql.SQLException.class.getName())));
		r.call();
	}

	@Override
	public String toString() {
		return "migratevm " + vminfo;
	}
}
