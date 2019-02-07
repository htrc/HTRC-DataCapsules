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
import edu.indiana.d2i.sloan.db.DBOperations;
import edu.indiana.d2i.sloan.exception.ScriptCmdErrorException;
import edu.indiana.d2i.sloan.utils.RetriableTask;
import edu.indiana.d2i.sloan.vm.VMState;
import edu.indiana.d2i.sloan.vm.VMStateManager;
import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.Callable;

public class UpdatePublicKeyCommand extends HypervisorCommand {
	private static Logger logger = Logger.getLogger(UpdatePublicKeyCommand.class);
	private final String publicKey;
	private final String username;
	private final String operator;

	public UpdatePublicKeyCommand(VmInfoBean vminfo, String username, String operator, String publicKey) throws Exception {
		super(vminfo);
		this.publicKey = publicKey;
		this.username = username;
		this.operator = operator;
	}

	@Override
	public void execute() throws Exception {
		HypervisorResponse resp = hypervisor.updatePubKey(vminfo, publicKey);
		logger.info(resp);

		if (resp.getResponseCode() != 0) {
			throw new ScriptCmdErrorException(String.format(
					"Failed to excute command:\n%s ", resp));
		}

		// update state
		RetriableTask<Void> r = new RetriableTask<Void>(
				new Callable<Void>() {
					@Override
					public Void call() throws Exception {
						logger.info("Public key of user '" + username + "' was updated in data capsule "
								+ vminfo.getVmid() + " successfully!");
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
						logger.info("Failed to update public key of user '" + username + "'in " + vminfo.getVmid() + "!");
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
		return "UpdatePublicKey " + vminfo;
	}
}
