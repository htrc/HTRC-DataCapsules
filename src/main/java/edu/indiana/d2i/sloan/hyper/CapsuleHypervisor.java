package edu.indiana.d2i.sloan.hyper;

import edu.indiana.d2i.sloan.bean.VmRequestBean;

class CapsuleHypervisor implements IHypervisor {

	// singleton?? It depends on how the ssh lib is implemented!
	
	@Override
	public HypervisorResponse createVM(VmRequestBean vminfo) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HypervisorResponse launchVM(VmRequestBean vminfo) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HypervisorResponse queryVM(VmRequestBean vminfo) {
		// TODO Auto-generated method stub
		return null;
	}	
}
