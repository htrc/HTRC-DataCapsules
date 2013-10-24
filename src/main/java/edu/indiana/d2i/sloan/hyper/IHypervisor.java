package edu.indiana.d2i.sloan.hyper;

import edu.indiana.d2i.sloan.bean.VmInfoBean;

interface IHypervisor {
	public HypervisorResponse createVM(VmInfoBean vminfo);
	
	public HypervisorResponse launchVM(VmInfoBean vminfo);
	
	public HypervisorResponse queryVM(VmInfoBean vminfo);
	
	public HypervisorResponse switchVM(VmInfoBean vminfo);
	
	public HypervisorResponse stopVM(VmInfoBean vminfo);
	
	public HypervisorResponse delete(VmInfoBean vminfo);
}
