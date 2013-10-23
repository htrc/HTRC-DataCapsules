package edu.indiana.d2i.sloan.hyper;

public abstract class HypervisorCommand {
	protected IHypervisor hypervisor = null;
	
	public HypervisorCommand() {
		this.hypervisor = HypervisorFactory.createHypervisor();
	}
	
	public abstract void execute() throws Exception;
	public abstract void cleanupOnFailed() throws Exception;
}
