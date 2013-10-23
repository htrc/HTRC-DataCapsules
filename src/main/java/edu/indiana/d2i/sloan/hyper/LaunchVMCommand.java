package edu.indiana.d2i.sloan.hyper;

public class LaunchVMCommand extends HypervisorCommand {
	private final String vmid;

	public LaunchVMCommand(String vmid) {
		super();
		this.vmid = vmid;
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
