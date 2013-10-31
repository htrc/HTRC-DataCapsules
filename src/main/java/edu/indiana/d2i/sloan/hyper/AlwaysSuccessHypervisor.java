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
	public HypervisorResponse launchVM(VmInfoBean vminfo) throws Exception {
		return genFakeResponse(VMState.RUNNING);
	}

	@Override
	public HypervisorResponse queryVM(VmInfoBean vminfo) throws Exception {
		return genFakeResponse(VMState.RUNNING);
	}

	@Override
	public HypervisorResponse switchVM(VmInfoBean vminfo) throws Exception {
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

}
