package edu.indiana.d2i.sloan.hyper;

interface IHypervisor {
	public HypervisorResponse createVM(String pathToCommonImage, String vmWorkingDir,
		int vcpu, int memory, int vncPort, int sshPort, int volumeSizeInGB);
	
	public HypervisorResponse launchVM(String vmWorkingDir, String mode, String policyFile);
	
	public HypervisorResponse queryVM(String vmWorkingDir);
	
}
