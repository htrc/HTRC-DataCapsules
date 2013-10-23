package edu.indiana.d2i.sloan.hyper;

public class QueryVMCommand extends HypervisorCommand {
	private final String vmid; 
	private final String userName;
	
	public QueryVMCommand(String userName, String vmid) {
		super();
		this.vmid = vmid;
		this.userName = userName;
	}
	
	@Override
	public void execute() throws Exception {
		
	}

	@Override
	public void cleanupOnFailed() throws Exception {
		// TODO Auto-generated method stub
		
	}

}
