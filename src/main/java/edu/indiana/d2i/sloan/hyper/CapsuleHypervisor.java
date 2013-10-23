package edu.indiana.d2i.sloan.hyper;

class CapsuleHypervisor implements IHypervisor {

	// singleton?? It depends on how the ssh lib is implemented!
	
	@Override
	public HypervisorResponse createVM(String pathToCommonImage,
			String vmWorkingDir, int vcpu, int memory, int vncPort,
			int sshPort, int volumeSizeInGB) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HypervisorResponse launchVM(String vmWorkingDir, String mode,
			String policyFile) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HypervisorResponse queryVM(String vmWorkingDir) {
		// TODO Auto-generated method stub
		return null;
	}	
}
