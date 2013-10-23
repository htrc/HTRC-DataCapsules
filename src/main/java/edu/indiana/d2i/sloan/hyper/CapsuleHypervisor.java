package edu.indiana.d2i.sloan.hyper;

import edu.indiana.d2i.sloan.bean.VmInfoBean;


class CapsuleHypervisor implements IHypervisor {

	// singleton?? It depends on how the ssh lib is implemented!
	
	@Override
	public HypervisorResponse createVM(VmInfoBean vminfo) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HypervisorResponse launchVM(VmInfoBean vminfo) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HypervisorResponse queryVM(VmInfoBean vminfo) {
		// TODO Auto-generated method stub
		return null;
	}	
}
