package edu.indiana.d2i.sloan.hyper;


import edu.indiana.d2i.sloan.bean.VmInfoBean;

public class QueryVMCommand extends HypervisorCommand {
	
	public QueryVMCommand(VmInfoBean vminfo) {
//		super(vminfo);
	}
	
	@Override
	public void execute() throws Exception {
		
	}

	@Override
	public void cleanupOnFailed() throws Exception {
		// TODO Auto-generated method stub
		
	}

}
