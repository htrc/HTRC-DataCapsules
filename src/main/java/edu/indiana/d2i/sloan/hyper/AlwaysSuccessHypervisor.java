package edu.indiana.d2i.sloan.hyper;

import edu.indiana.d2i.sloan.bean.VmInfoBean;

/**
 * 
 * AlwaysSuccessHypervisor for testing purpose
 * 
 */
public class AlwaysSuccessHypervisor implements IHypervisor {

	private static HypervisorResponse genFakeResponse() {
		return new HypervisorResponse("Always success command",
				"Always success host", 0, "success description");

	}

	private AlwaysSuccessHypervisor() {
		
	}
	
	@Override
	public HypervisorResponse createVM(VmInfoBean vminfo) throws Exception {
		return genFakeResponse();
	}

	@Override
	public HypervisorResponse launchVM(VmInfoBean vminfo) throws Exception {
		return genFakeResponse();
	}

	@Override
	public HypervisorResponse queryVM(VmInfoBean vminfo) throws Exception {
		return genFakeResponse();
	}

	@Override
	public HypervisorResponse switchVM(VmInfoBean vminfo) throws Exception {
		return genFakeResponse();
	}

	@Override
	public HypervisorResponse stopVM(VmInfoBean vminfo) throws Exception {
		return genFakeResponse();
	}

	@Override
	public HypervisorResponse delete(VmInfoBean vminfo) throws Exception {
		return genFakeResponse();
	}

}
