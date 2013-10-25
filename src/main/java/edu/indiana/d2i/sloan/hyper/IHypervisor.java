package edu.indiana.d2i.sloan.hyper;

import edu.indiana.d2i.sloan.bean.VmInfoBean;

interface IHypervisor {
	public HypervisorResponse createVM(VmInfoBean vminfo) throws Exception;

	public HypervisorResponse launchVM(VmInfoBean vminfo) throws Exception;

	public HypervisorResponse queryVM(VmInfoBean vminfo) throws Exception;

	public HypervisorResponse switchVM(VmInfoBean vminfo) throws Exception;

	public HypervisorResponse stopVM(VmInfoBean vminfo) throws Exception;

	public HypervisorResponse delete(VmInfoBean vminfo) throws Exception;
}
