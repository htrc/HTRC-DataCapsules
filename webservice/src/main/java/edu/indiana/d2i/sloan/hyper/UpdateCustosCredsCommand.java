/*******************************************************************************
 * Copyright 2018 The Trustees of Indiana University
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
import edu.indiana.d2i.sloan.exception.ScriptCmdErrorException;
import edu.indiana.d2i.sloan.utils.RetriableTask;
import edu.indiana.d2i.sloan.vm.VMState;
import edu.indiana.d2i.sloan.vm.VMStateManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.Callable;

public class UpdateCustosCredsCommand extends HypervisorCommand {
	private static Logger logger = LoggerFactory.getLogger(UpdateCustosCredsCommand.class);
	private final String username;
	private final String custos_client_id;
	private final String custos_client_secret;

	public UpdateCustosCredsCommand(VmInfoBean vminfo, String username, String custos_client_id, String custos_client_secret) throws Exception {
		super(vminfo);
		this.username = username;
		this.custos_client_id = custos_client_id;
		this.custos_client_secret = custos_client_secret;
	}

	@Override
	public void execute() throws Exception {
		HypervisorResponse resp = hypervisor.updateCustosCreds(vminfo, custos_client_id, custos_client_secret);
		logger.info(resp.toString());

		if (resp.getResponseCode() != 0) {
			throw new ScriptCmdErrorException(String.format(
					"Failed to excute command:\n%s ", resp));
		}

		// update state
		RetriableTask<Void> r = new RetriableTask<Void>(
				new Callable<Void>() {
					@Override
					public Void call() throws Exception {
						logger.info("Custos credentials of VM '" + vminfo.getVmid() + "' was updated successfully!");
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
						logger.error("Failed to update credentials of VM '" + vminfo.getVmid() + "'!");
						VMStateManager.getInstance().transitTo(vminfo.getVmid(), vminfo.getVmstate(), VMState.ERROR, username);
						return null;
					}
				},  1000, 3,
				new HashSet<String>(Arrays.asList(java.sql.SQLException.class.getName())));
		r.call();
	}

	@Override
	public String toString() {
		return "UpdateCustosCreds " + vminfo;
	}
}
