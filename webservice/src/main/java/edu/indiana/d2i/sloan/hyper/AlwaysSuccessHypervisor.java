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

import java.util.HashMap;

import edu.indiana.d2i.sloan.bean.VmInfoBean;
import edu.indiana.d2i.sloan.vm.VMState;

/**
 * 
 * AlwaysSuccessHypervisor for testing purpose
 * 
 */
public class AlwaysSuccessHypervisor implements IHypervisor {

	private static HypervisorResponse genFakeResponse(VMState state) {
		return HypervisorResponse.createTestHypervisorResp(
				"Always success command",
				"Always success host", 0, "success description", 
				state, new HashMap<String, String>());
	}

	private AlwaysSuccessHypervisor() {
		
	}
	
	@Override
	public HypervisorResponse createVM(VmInfoBean vminfo) throws Exception {
		return genFakeResponse(VMState.SHUTDOWN);
	}

	@Override
	public HypervisorResponse launchVM(VmInfoBean vminfo, String pubKey) throws Exception {
		return genFakeResponse(VMState.RUNNING);
	}

	@Override
	public HypervisorResponse queryVM(VmInfoBean vminfo) throws Exception {
		return genFakeResponse(VMState.RUNNING);
	}

	@Override
	public HypervisorResponse switchVM(VmInfoBean vminfo, String pubKey) throws Exception {
		return genFakeResponse(VMState.RUNNING);
	}

	@Override
	public HypervisorResponse stopVM(VmInfoBean vminfo) throws Exception {
		return genFakeResponse(VMState.SHUTDOWN);
	}

	@Override
	public HypervisorResponse delete(VmInfoBean vminfo) throws Exception {
		return genFakeResponse(VMState.SHUTDOWN);
	}

	@Override
	public HypervisorResponse updatePubKey(VmInfoBean vminfo, String pubKey) throws Exception {
		return genFakeResponse(VMState.SHUTDOWN);
	}

	@Override
	public HypervisorResponse migrateVM(VmInfoBean vminfo, String host, int vncport, int sshport) throws Exception {
		return genFakeResponse(VMState.SHUTDOWN);
	}

}
