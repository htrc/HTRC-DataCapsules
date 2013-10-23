package edu.indiana.d2i.sloan.hyper;

class HypervisorFactory {
	public static IHypervisor createHypervisor() {
		return new CapsuleHypervisor();
	}
}
