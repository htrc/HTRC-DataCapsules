package edu.indiana.d2i.sloan.hyper;

import edu.indiana.d2i.sloan.bean.VmInfoBean;

public class DeleteVMCommand extends HypervisorCommand {
	
	public DeleteVMCommand(VmInfoBean vminfo) {
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
