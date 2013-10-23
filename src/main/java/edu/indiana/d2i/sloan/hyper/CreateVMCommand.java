package edu.indiana.d2i.sloan.hyper;

import edu.indiana.d2i.sloan.bean.VmInfoBean;

public class CreateVMCommand extends HypervisorCommand {
	private final VmInfoBean vminfo;

	public CreateVMCommand(VmInfoBean vminfo) {
		super();
		this.vminfo = vminfo;
	}

	@Override
	public void execute() throws Exception {
		// get vnc port and ssh port
		
		
		// round robin scheduling
		
		
		// call hypervisor layer
		
		
		// update vm status and login info
	}

	@Override
	public void cleanupOnFailed() throws Exception {
		
	}
	
	@Override
	public String toString() {
		return "createvm " + vminfo;
	}


}
