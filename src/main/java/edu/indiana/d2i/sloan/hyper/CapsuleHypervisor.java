package edu.indiana.d2i.sloan.hyper;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;

import edu.indiana.d2i.sloan.Configuration;
import edu.indiana.d2i.sloan.Constants;
import edu.indiana.d2i.sloan.bean.VmInfoBean;
import edu.indiana.d2i.sloan.utils.SSHProxy;
import edu.indiana.d2i.sloan.utils.SSHProxy.CmdsExecResult;
import edu.indiana.d2i.sloan.utils.SSHProxy.Commands;

class CapsuleHypervisor implements IHypervisor {
	private static Logger logger = Logger.getLogger(CapsuleHypervisor.class);
	private static long timeoutInMillis;

	static {
		timeoutInMillis = Long.parseLong(Configuration.getInstance()
				.getProperty(
						Configuration.PropertyName.HYPERVISOR_TASK_TIMEOUT,
						Constants.DEFAULT_HYPERVISOR_TASK_TIMEOUT));
	}

	// singleton?? It depends on how the ssh lib is implemented!

	class CapsuleTask implements Callable<CmdsExecResult> {
		private SSHProxy sshProxy;
		private Commands cmds;

		public CapsuleTask(SSHProxy sshProxy, Commands cmds) {
			super();
			this.sshProxy = sshProxy;
			this.cmds = cmds;
		}

		@Override
		public CmdsExecResult call() throws Exception {

			return sshProxy.execCmdSync(cmds);
		}

	}

	private <T> T executateTask(Callable<T> task) throws InterruptedException,
			ExecutionException, TimeoutException {
		ExecutorService executor = Executors.newSingleThreadExecutor();
		Future<T> future = executor.submit(task);

		try {
			return future.get(timeoutInMillis, TimeUnit.MILLISECONDS);
		} finally {
			if (executor != null)
				executor.shutdownNow();
		}

	}
	@Override
	public HypervisorResponse createVM(VmInfoBean vminfo) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HypervisorResponse launchVM(VmInfoBean vminfo) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HypervisorResponse queryVM(VmInfoBean vminfo) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HypervisorResponse switchVM(VmInfoBean vminfo) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HypervisorResponse stopVM(VmInfoBean vminfo) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HypervisorResponse delete(VmInfoBean vminfo) {
		// TODO Auto-generated method stub
		return null;
	}
}
