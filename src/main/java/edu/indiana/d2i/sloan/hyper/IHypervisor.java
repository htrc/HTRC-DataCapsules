package edu.indiana.d2i.sloan.hyper;

import edu.indiana.d2i.sloan.bean.VmRequestBean;

interface IHypervisor {
	public HypervisorResponse createVM(VmRequestBean vminfo);
	
	public HypervisorResponse launchVM(VmRequestBean vminfo);
	
	public HypervisorResponse queryVM(VmRequestBean vminfo);
	
}
