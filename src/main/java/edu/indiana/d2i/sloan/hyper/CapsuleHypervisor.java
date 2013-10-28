package edu.indiana.d2i.sloan.hyper;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;

import com.jcraft.jsch.JSchException;

import edu.indiana.d2i.sloan.Configuration;
import edu.indiana.d2i.sloan.Constants;
import edu.indiana.d2i.sloan.bean.VmInfoBean;
import edu.indiana.d2i.sloan.utils.CommandUtils;
import edu.indiana.d2i.sloan.utils.CommandUtils.HYPERVISOR_CMD;
import edu.indiana.d2i.sloan.utils.RetriableTask;
import edu.indiana.d2i.sloan.utils.SSHProxy;
import edu.indiana.d2i.sloan.utils.SSHProxy.CmdsExecResult;
import edu.indiana.d2i.sloan.utils.SSHProxy.Commands;

class CapsuleHypervisor implements IHypervisor {
	private static Logger logger = Logger.getLogger(CapsuleHypervisor.class);
	private static long timeoutInMillis;
	private static String sshUsername;
	private static String sshPasswd;
	private static String privateKeyPath;
	private static boolean retriable;
	private static long retryWaitInMs = 500;
	private static int maxRetry = 5;
	private static Set<String> retriableExpNames = null;

	private CapsuleHypervisor() {

	}

	static {
		timeoutInMillis = Long.parseLong(Configuration.getInstance()
				.getProperty(
						Configuration.PropertyName.HYPERVISOR_TASK_TIMEOUT,
						Constants.DEFAULT_HYPERVISOR_TASK_TIMEOUT));

		sshUsername = Configuration.getInstance().getProperty(
				Configuration.PropertyName.SSH_USERNAME);

		sshPasswd = Configuration.getInstance().getProperty(
				Configuration.PropertyName.SSH_PASSWD);

		privateKeyPath = Configuration.getInstance().getProperty(
				Configuration.PropertyName.SSH_PRIVATE_KEY_PATH);

		retriable = Boolean.parseBoolean(Configuration.getInstance()
				.getProperty(Configuration.PropertyName.USE_RETRY_TASK));

		if (retriable) {
			retryWaitInMs = Long
					.parseLong(Configuration
							.getInstance()
							.getProperty(
									Configuration.PropertyName.RETRY_TASK_WAIT_IN_MILLIS));

			maxRetry = Integer.parseInt(Configuration.getInstance()
					.getProperty(
							Configuration.PropertyName.RETRY_TASK_MAX_ATTEMPT));

			String[] classNames = Configuration
					.getInstance()
					.getProperty(
							Configuration.PropertyName.RETRY_TASK_RETRIABLE_EXPS)
					.split(";");

			retriableExpNames = new HashSet<String>(
					Arrays.<String> asList(classNames));
		}
	}

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

	private static <T> T executeRetriableTask(Callable<T> task)
			throws InterruptedException, ExecutionException, TimeoutException {

		if (retriable) {
			task = new RetriableTask<T>(task, retryWaitInMs, maxRetry,
					retriableExpNames);
		}

		return executeTask(task);
	}

	private static <T> T executeTask(Callable<T> task)
			throws InterruptedException, ExecutionException, TimeoutException {

		ExecutorService executor = null;

		try {
			executor = Executors.newSingleThreadExecutor();
			Future<T> future = executor.submit(task);
			return future.get(timeoutInMillis, TimeUnit.MILLISECONDS);
		} finally {
			if (executor != null)
				executor.shutdownNow();
		}
	}

	private static SSHProxy establishSShCon(String hostname, int port)
			throws JSchException {
		SSHProxy sshProxy = (sshPasswd != null) ? 
			new SSHProxy.SSHProxyBuilder(hostname, port, sshUsername).
				usePassword(sshPasswd).build():
			new SSHProxy.SSHProxyBuilder(hostname, port, sshUsername).
				usePrivateKey(privateKeyPath).build();
		return sshProxy;
	}

	@Override
	public HypervisorResponse createVM(VmInfoBean vminfo) throws JSchException,
			InterruptedException, ExecutionException, TimeoutException {

		SSHProxy sshProxy = null;

		try {
			/* establish ssh connection */
			sshProxy = establishSShCon(vminfo.getPublicip(),
					SSHProxy.SSH_DEFAULT_PORT);

			/* compose script command */
			String argList = new CommandUtils.ArgsBuilder()
					.addArgument("-image", vminfo.getImagepath())
					.addArgument("-vcpu", String.valueOf(vminfo.getNumCPUs()))
					.addArgument("-mem", String.valueOf(vminfo.getMemorySize()))
					.addArgument("-wdir", vminfo.getWorkDir())
					.addArgument("-vnc", String.valueOf(vminfo.getVncport()))
					.addArgument("-ssh", String.valueOf(vminfo.getSshport()))
					.build();

			Commands createVMCmd = new Commands(
					Collections.<String> singletonList(CommandUtils
							.composeFullCommand(HYPERVISOR_CMD.CREATE_VM,
									argList)), false);

			/* execute task */
			CmdsExecResult res = executeRetriableTask(new CapsuleTask(sshProxy,
					createVMCmd));

			return HypervisorResponse.commandRes2HyResp(res);
		} finally {
			/* close ssh connection */
			if (sshProxy != null)
				sshProxy.close();
		}

	}

	@Override
	public HypervisorResponse launchVM(VmInfoBean vminfo) throws JSchException,
			InterruptedException, ExecutionException, TimeoutException {
		SSHProxy sshProxy = null;

		try {
			/* establish ssh connection */
			sshProxy = establishSShCon(vminfo.getPublicip(),
					SSHProxy.SSH_DEFAULT_PORT);

			/* compose script command */

			// TODO: should remove the -mode flag if the VM is launched in
			// maintenance
			String argList = new CommandUtils.ArgsBuilder()
					.addArgument("-wdir", vminfo.getWorkDir())
					.addArgument("-mode",
							vminfo.getRequestedVMMode().toString())
					.addArgument("-policy", vminfo.getPolicypath()).build();

			Commands launchVMCmd = new Commands(
					Collections.<String> singletonList(CommandUtils
							.composeFullCommand(HYPERVISOR_CMD.LAUNCH_VM,
									argList)), false);

			/* execute task */
			CmdsExecResult res = executeRetriableTask(new CapsuleTask(sshProxy,
					launchVMCmd));

			return HypervisorResponse.commandRes2HyResp(res);
		} finally {
			/* close ssh connection */
			if (sshProxy != null)
				sshProxy.close();
		}
	}

	@Override
	public HypervisorResponse queryVM(VmInfoBean vminfo) throws JSchException,
			InterruptedException, ExecutionException, TimeoutException {
		SSHProxy sshProxy = null;

		try {
			/* establish ssh connection */
			sshProxy = establishSShCon(vminfo.getPublicip(),
					SSHProxy.SSH_DEFAULT_PORT);

			/* compose script command */
			String argList = new CommandUtils.ArgsBuilder().addArgument(
					"-wdir", vminfo.getWorkDir()).build();

			Commands queryVMCmd = new Commands(
					Collections.<String> singletonList(CommandUtils
							.composeFullCommand(HYPERVISOR_CMD.QUERY_VM,
									argList)), false);

			/* execute task */
			CmdsExecResult res = executeRetriableTask(new CapsuleTask(sshProxy,
					queryVMCmd));

			return HypervisorResponse.commandRes2HyResp(res);
		} finally {
			/* close ssh connection */
			if (sshProxy != null)
				sshProxy.close();
		}
	}

	@Override
	public HypervisorResponse switchVM(VmInfoBean vminfo) throws JSchException,
			InterruptedException, ExecutionException, TimeoutException {
		SSHProxy sshProxy = null;

		try {
			/* establish ssh connection */
			sshProxy = establishSShCon(vminfo.getPublicip(),
					SSHProxy.SSH_DEFAULT_PORT);

			/* compose script command */
			String argList = new CommandUtils.ArgsBuilder()
					.addArgument("-wdir", vminfo.getWorkDir())
					.addArgument("-mode",
							vminfo.getRequestedVMMode().toString())
					.addArgument("-policy", vminfo.getPolicypath()).build();

			Commands switchVMCmd = new Commands(
					Collections.<String> singletonList(CommandUtils
							.composeFullCommand(HYPERVISOR_CMD.SWITCH_VM,
									argList)), false);

			/* execute task */
			CmdsExecResult res = executeRetriableTask(new CapsuleTask(sshProxy,
					switchVMCmd));

			return HypervisorResponse.commandRes2HyResp(res);
		} finally {
			/* close ssh connection */
			if (sshProxy != null)
				sshProxy.close();
		}
	}

	@Override
	public HypervisorResponse stopVM(VmInfoBean vminfo) throws JSchException,
			InterruptedException, ExecutionException, TimeoutException {
		SSHProxy sshProxy = null;

		try {
			/* establish ssh connection */
			sshProxy = establishSShCon(vminfo.getPublicip(),
					SSHProxy.SSH_DEFAULT_PORT);

			/* compose script command */
			String argList = new CommandUtils.ArgsBuilder().addArgument(
					"-wdir", vminfo.getWorkDir()).build();

			Commands stopVMCmd = new Commands(
					Collections
							.<String> singletonList(CommandUtils
									.composeFullCommand(HYPERVISOR_CMD.STOP_VM,
											argList)),
					false);

			/* execute task */
			CmdsExecResult res = executeRetriableTask(new CapsuleTask(sshProxy,
					stopVMCmd));

			return HypervisorResponse.commandRes2HyResp(res);
		} finally {
			/* close ssh connection */
			if (sshProxy != null)
				sshProxy.close();
		}
	}

	@Override
	public HypervisorResponse delete(VmInfoBean vminfo) throws JSchException,
			InterruptedException, ExecutionException, TimeoutException {
		SSHProxy sshProxy = null;

		try {
			/* establish ssh connection */
			sshProxy = establishSShCon(vminfo.getPublicip(),
					SSHProxy.SSH_DEFAULT_PORT);

			/* compose script command */
			String argList = new CommandUtils.ArgsBuilder().addArgument(
					"-wdir", vminfo.getWorkDir()).build();

			Commands deleteVMCmd = new Commands(
					Collections.<String> singletonList(CommandUtils
							.composeFullCommand(HYPERVISOR_CMD.DELETE_VM,
									argList)), false);

			/* execute task */
			CmdsExecResult res = executeRetriableTask(new CapsuleTask(sshProxy,
					deleteVMCmd));

			return HypervisorResponse.commandRes2HyResp(res);
		} finally {
			/* close ssh connection */
			if (sshProxy != null)
				sshProxy.close();
		}
	}
}
