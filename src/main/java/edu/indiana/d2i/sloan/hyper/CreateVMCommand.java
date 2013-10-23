package edu.indiana.d2i.sloan.hyper;

import edu.indiana.d2i.sloan.bean.VmRequestBean;
import edu.indiana.d2i.sloan.vm.VMState;
import edu.indiana.d2i.sloan.vm.VMStateManager;

public class CreateVMCommand extends HypervisorCommand {
	private final VmRequestBean vmreq;

	public CreateVMCommand(VmRequestBean vmreq) {
		this.vmreq = vmreq;
	}

	@Override
	public void execute() throws Exception {
		// call hypervisor layer
		Thread.sleep(2000);
		
		// update vm status and login info
		VMStateManager.getInstance().transitTo(vmreq.getUserName(), 
			vmreq.getVmId(), VMState.BUILDING, VMState.SHUTDOWN);
	}

	@Override
	public void cleanupOnFailed() throws Exception {
		VMStateManager.getInstance().transitTo(vmreq.getUserName(), 
			vmreq.getVmId(), VMState.BUILDING, VMState.ERROR);
	}
	
	@Override
	public String toString() {
		return "createvm " + vmreq;
	}


}
