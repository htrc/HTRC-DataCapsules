package edu.indiana.d2i.sloan.hyper;

import edu.indiana.d2i.sloan.bean.VmInfoBean;

public class LaunchVMCommand extends HypervisorCommand {

	public LaunchVMCommand(VmInfoBean vminfo) {
		super(vminfo);
	}
	
	@Override
	public void execute() throws Exception {
		// update vm status as "launching" ??

		
		// call hypervisor layer
		
		
		// update vm status 
	}

	@Override
	public void cleanupOnFailed() throws Exception {
		
	}
}
