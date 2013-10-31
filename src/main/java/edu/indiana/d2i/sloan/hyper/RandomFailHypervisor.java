package edu.indiana.d2i.sloan.hyper;

import java.util.HashMap;
import java.util.Random;

import edu.indiana.d2i.sloan.Configuration;
import edu.indiana.d2i.sloan.bean.VmInfoBean;
import edu.indiana.d2i.sloan.exception.RandomFailException;
import edu.indiana.d2i.sloan.vm.VMState;

public class RandomFailHypervisor implements IHypervisor {

	private static float RANDOM_FAIL_PROB;
	private static String DEFAULT_RANDOM_FAIL_PROB = "0.5f";

	/* when failed, the probability of failure due to exception */
	private static float RANDOM_EXP_PROB;
	private static String DEFAULT_RANDOM_EXP_PROB = "0.5f";

	private static int DEFAULT_SCRIPT_ERR_CODE = -1;

	private static Random rand;

	private RandomFailHypervisor() {
		
	}
	
	static {
		RANDOM_FAIL_PROB = Float.parseFloat(Configuration.getInstance()
				.getString(
						Configuration.PropertyName.RFHYPER_RANDOM_FAIL_PROB,
						DEFAULT_RANDOM_FAIL_PROB));

		RANDOM_EXP_PROB = Float.parseFloat(Configuration.getInstance()
				.getString(
						Configuration.PropertyName.RFHYPER_RANDOM_EXP_PROB,
						DEFAULT_RANDOM_EXP_PROB));

		rand = new Random(System.currentTimeMillis());
	}

	private static HypervisorResponse genFakeResponse(boolean fail) {

		if (fail) {
			return HypervisorResponse.createTestHypervisorResp(
					"Random fail command",
					"Random fail host", DEFAULT_SCRIPT_ERR_CODE,
					"Random fail description", 
					VMState.ERROR, new HashMap<String, String>());
		} else {
			return HypervisorResponse.createTestHypervisorResp(
					"Random success command",
					"Random success host", 0, "Random success description", 
					VMState.RUNNING, new HashMap<String, String>());
		}

	}

	private HypervisorResponse simulateRandProcess() throws RandomFailException {

		if (rand.nextFloat() < RANDOM_FAIL_PROB) {
			// failure
			if (rand.nextFloat() < RANDOM_EXP_PROB) {
				// failure due to exception
				throw new RandomFailException();
			} else {
				// failure due to non-zero return code
				return genFakeResponse(true);
			}
		} else {
			// success
			return genFakeResponse(false);
		}
	}

	@Override
	public HypervisorResponse createVM(VmInfoBean vminfo) throws Exception {
		return simulateRandProcess();
	}

	@Override
	public HypervisorResponse launchVM(VmInfoBean vminfo) throws Exception {
		return simulateRandProcess();
	}

	@Override
	public HypervisorResponse queryVM(VmInfoBean vminfo) throws Exception {
		return simulateRandProcess();
	}

	@Override
	public HypervisorResponse switchVM(VmInfoBean vminfo) throws Exception {
		return simulateRandProcess();
	}

	@Override
	public HypervisorResponse stopVM(VmInfoBean vminfo) throws Exception {
		return simulateRandProcess();
	}

	@Override
	public HypervisorResponse delete(VmInfoBean vminfo) throws Exception {
		return simulateRandProcess();
	}

}
