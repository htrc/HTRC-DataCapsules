package edu.indiana.d2i.sloan.hyper;

import edu.indiana.d2i.sloan.bean.VmInfoBean;

public abstract class HypervisorCommand {
	protected IHypervisor hypervisor = null;
	protected VmInfoBean vminfo = null;
	
	public HypervisorCommand() {
		this.hypervisor = HypervisorFactory.createHypervisor();
	}
	
	public HypervisorCommand(VmInfoBean vminfo) {
		super();
		this.vminfo = vminfo;
	}
	
	public abstract void execute() throws Exception;
	public abstract void cleanupOnFailed() throws Exception;
}
