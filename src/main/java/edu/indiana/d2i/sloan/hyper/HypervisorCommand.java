package edu.indiana.d2i.sloan.hyper;

import edu.indiana.d2i.sloan.bean.VmInfoBean;

public abstract class HypervisorCommand {
	protected final IHypervisor hypervisor;
	protected final VmInfoBean vminfo;

	public HypervisorCommand(VmInfoBean vminfo) throws Exception {
		this.hypervisor = HypervisorFactory.createHypervisor();
		this.vminfo = vminfo;
	}

	public abstract void execute() throws Exception;
	public abstract void cleanupOnFailed() throws Exception;
}
