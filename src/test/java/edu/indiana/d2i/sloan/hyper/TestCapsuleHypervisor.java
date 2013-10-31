package edu.indiana.d2i.sloan.hyper;

import java.io.IOException;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import com.jcraft.jsch.JSchException;

import edu.indiana.d2i.sloan.bean.VmInfoBean;
import edu.indiana.d2i.sloan.utils.SSHProxy;
import edu.indiana.d2i.sloan.vm.VMMode;
import edu.indiana.d2i.sloan.vm.VMState;

public class TestCapsuleHypervisor {
	private static final String SUCCESSFUL_DESCRIPTION = "test-screen-output";
	private static final VMState SUCCESSFUL_STATE = VMState.RUNNING;
	
	static class FakeSSHProxy extends SSHProxy {		
		public FakeSSHProxy() throws JSchException {

		}
		
		public CmdsExecResult execCmdSync(Commands cmds) throws JSchException,
			IOException {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				throw new IOException(e);
			}
			return new CmdsExecResult(cmds, "testhost", 0, 
				"0 \n" +
				SUCCESSFUL_DESCRIPTION + "\n" +
				"status\t" + SUCCESSFUL_STATE.toString());
		}
		
		public void close() {
			
		}
	}
	
	static class PartialFailedSSHProxy extends SSHProxy {	
		private int timesFailed;
		
		public PartialFailedSSHProxy(int timesFailed) throws JSchException {
			this.timesFailed = timesFailed;
		}
		
		public CmdsExecResult execCmdSync(Commands cmds) throws JSchException,
			IOException {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				throw new IOException(e);
			}
			
			if (timesFailed-- > 0) {
				if (Math.random() > 0.5)
					throw new IOException("fake io exception");
				else 
					throw new JSchException("fake JSchException");
			}
			
			return new CmdsExecResult(cmds, "testhost", 0, 
				"0 \n" +
				SUCCESSFUL_DESCRIPTION + "\n" +
				"status\t" + SUCCESSFUL_STATE.toString());
		}
		
		public void close() {
			
		}
	}
	
	static class FailedSSHProxy extends SSHProxy {		
		public FailedSSHProxy() throws JSchException {

		}
		
		public CmdsExecResult execCmdSync(Commands cmds) throws JSchException,
			IOException {
			if (Math.random() > 0.5)
				throw new IOException("non retriable io exception");
			else 
				throw new JSchException("non retriable JSchException");
		}
		
		public void close() {
			
		}
	}
	
	class FakeCapsuleHypervisor extends CapsuleHypervisor {
		private final SSHProxy proxy;
		
		public FakeCapsuleHypervisor(SSHProxy proxy) {
			this.proxy = proxy;
			CapsuleHypervisor.retryWaitInMs = 100;
			CapsuleHypervisor.maxRetry = 3;
		}
		
		protected SSHProxy establishSShCon(String hostname, int port)
				throws JSchException {
			return proxy;
		}
	}
	
	private IHypervisor hyper = null;
	
	@Before
	public void before() {

	}
	
	private void testCreateVM(IHypervisor hypervisor, int records) throws Exception {
		for (int i = 0; i < records; i++) {
			VmInfoBean vminfo = new VmInfoBean("vmid-" + i, "192.168.0." + i, 
				"/path/to/work/dir-"+i, 
				"/path/to/image", "/path/to/policy", 2000 + i*2, 2000 + i*2 +1, 2, 2048, 
				10, VMMode.NOT_DEFINED, VMState.LAUNCH_PENDING, "ubuntu", 
				"password", "test-image", "test-policy", VMMode.MAINTENANCE);
			
			HypervisorResponse response = hypervisor.createVM(vminfo);
			Assert.assertEquals(0, response.getResponseCode());
			Assert.assertEquals(SUCCESSFUL_STATE, response.getVmState());			
			Assert.assertEquals(SUCCESSFUL_DESCRIPTION, response.getDescription());
		}
	}
	
	@Test(expected = IOException.class)
	public void testCreateVM() throws Exception {
		int records = 3;
		
		// successful
		hyper = new FakeCapsuleHypervisor(new FakeSSHProxy());
		testCreateVM(hyper, records);
		
		// still successful, although retry is required
		hyper = new FakeCapsuleHypervisor(new PartialFailedSSHProxy(2));
		testCreateVM(hyper, 1);
		
		// fail, throw exception
		try {
			hyper = new FakeCapsuleHypervisor(new FailedSSHProxy());
			testCreateVM(hyper, 1);
		} catch (Exception e) {
			throw new IOException(e);
		}
	}
	
	// other hyper calls are similar to the logic of create vm
}
