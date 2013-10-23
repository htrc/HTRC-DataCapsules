package edu.indiana.d2i.sloan.hyper;

import edu.indiana.d2i.sloan.bean.VmInfoBean;

public class StopVMCommand extends HypervisorCommand {

	public StopVMCommand(VmInfoBean vminfo) {
		super(vminfo);
	}
	
	@Override
	public void execute() throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void cleanupOnFailed() throws Exception {
		// TODO Auto-generated method stub
		
	}

}
