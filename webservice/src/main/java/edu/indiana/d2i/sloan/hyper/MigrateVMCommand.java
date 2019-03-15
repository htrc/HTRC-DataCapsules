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
import edu.indiana.d2i.sloan.vm.*;
import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.Callable;

public class MigrateVMCommand extends HypervisorCommand {
	private static Logger logger = Logger.getLogger(MigrateVMCommand.class);
	private String operator;
	private VMPorts vmports;

	public MigrateVMCommand(VmInfoBean vminfo, String operator, VMPorts vmports) throws Exception {
		super(vminfo);
		this.operator = operator;
		this.vmports = vmports;
	}

	@Override
	public void execute() throws Exception {
		HypervisorResponse resp = hypervisor.migrateVM(vminfo, vmports);
		logger.info(resp);

		if (resp.getResponseCode() != 0) {
			throw new ScriptCmdErrorException(String.format(
					"Failed to excute command:\n%s ", resp));
		}

		/*
			If migration is successful, updates vms table with new ports and host values, updates VM state to shutdown,
			and release previous ports.
		 */
		RetriableTask<Void> r = new RetriableTask<Void>(
			new Callable<Void>() {
				@Override
				public Void call() throws Exception {

					DBOperations.getInstance().updateVmHostAndPorts(vminfo.getVmid(), vmports);

					VMStateManager.getInstance().transitTo(vminfo.getVmid(),
							vminfo.getVmstate(), VMState.SHUTDOWN, operator);

						if (logger.isDebugEnabled()) {
							logger.debug(String.format(
									"Going to update VM's (vmid = %s) host, VNC port and SSH port to %s, %d and %d",
									vminfo.getVmid(), vmports.publicip, vmports.vncport, vmports.sshport));
						}

						PortsPool.getInstance().release(vminfo.getVmid(),
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
			If migration fails, update VM's state to Erro
			Do not release the newly allocated ports of the target host, both old and new ports will be not available
			until its deleted manually and DELETE /deletevm API is called
		 */
		RetriableTask<Void> r = new RetriableTask<Void>(
			new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					VMPorts current_port = new VMPorts(vminfo.getPublicip(), vminfo.getSshport(), vminfo.getVncport());
					logger.error("Error occurred while migrating VM " + vminfo.getVmid() + " from VMPort("
							+ current_port.toString() + ") to VMPort(" + vmports.toString() + ")");

					VMStateManager.getInstance().transitTo(vminfo.getVmid(),
							vminfo.getVmstate(), VMState.ERROR, operator);

					//PortsPool.getInstance().release(vmports);

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
