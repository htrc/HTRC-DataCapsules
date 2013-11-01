package edu.indiana.d2i.sloan.internal;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import edu.indiana.d2i.sloan.internal.HypervisorCmdSimulator.ERROR_STATE;
import edu.indiana.d2i.sloan.vm.VMMode;
import edu.indiana.d2i.sloan.vm.VMState;

public class QueryVMSimulator extends HypervisorCmdSimulator {
	private static Logger logger = Logger.getLogger(QueryVMSimulator.class);
	private static final String STATE_FILE_NAME = "vmstatefile.txt";
	private static String ip = null;

	static {
		try {
			ip = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			logger.error(String.format(
					"Cannot get ip address, error message: %s", e.getMessage()));
			ip = "";
		}
	}
	public static class VMStatus {
		private VMMode mode;
		private VMState state;
		private String ip;
		private int vncport;
		private int sshport;
		private int vcpu;
		private int mem;

		public VMStatus(VMMode mode, VMState state, String ip, int vncport,
				int sshport, int vcpu, int mem) {
			super();
			this.mode = mode;
			this.state = state;
			this.ip = ip;
			this.vncport = vncport;
			this.sshport = sshport;
			this.vcpu = vcpu;
			this.mem = mem;
		}

		public VMMode getMode() {
			return mode;
		}
		public VMState getState() {
			return state;
		}
		public String getIp() {
			return ip;
		}
		public int getVncport() {
			return vncport;
		}
		public int getSshport() {
			return sshport;
		}
		public int getVcpu() {
			return vcpu;
		}
		public int getMem() {
			return mem;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			String delimiter = ":";

			sb.append("mode").append(delimiter).append(mode.toString())
					.append("\n").append("state").append(delimiter)
					.append(state.toString()).append("\n").append("ip")
					.append(delimiter).append(ip).append("\n")
					.append("vncport").append(delimiter).append(vncport)
					.append("\n").append("sshport").append(delimiter)
					.append(sshport).append("\n").append("vcpu")
					.append(delimiter).append(vcpu).append("\n").append("mem")
					.append(delimiter).append(mem).append("\n");

			return sb.toString();
		}
	}

	public static VMStatus queryVMStatus(String wdir) {
		// TODO: need to maintain the VM states somewhere so we can query on the
		// fly

		// currently hard code and return static state
		VMStatus status = new VMStatus(VMMode.MAINTENANCE, VMState.RUNNING, ip,
				28, 22, 2, 1024);

		return status;
	}

	@Override
	protected void initOptions() {
		options = new Options();

		Option wdir = OptionBuilder.withArgName("workingdir").hasArg()
				.withDescription("working directory").create("wdir");

		options.addOption(wdir);
	}

	public static void main(String[] args) {
		QueryVMSimulator simulator = new QueryVMSimulator();

		CommandLineParser parser = new PosixParser();

		try {
			CommandLine line = simulator.parseCommandLine(parser, args);
			String wdir = line.getOptionValue("wdir");

			if (!HypervisorCmdSimulator.checkFileExist(wdir)) {
				logger.error(String.format("Cannot find VM working dir: %s",
						wdir));
				System.exit(ERROR_CODE.get(ERROR_STATE.VM_NOT_EXIST));
			}

			VMStatus status = QueryVMSimulator.queryVMStatus(wdir);

			// write state file so shell script can read the vm state info
			FileUtils.writeStringToFile(new File(STATE_FILE_NAME),
					status.toString(), Charset.forName("UTF-8"));

		} catch (ParseException e) {
			logger.error(String.format(
					"Cannot parse input arguments: %s%n, expected:%n%s",
					StringUtils.join(args, " "),
					simulator.getUsage(100, "", 5, 5, "")));

			System.exit(ERROR_CODE.get(ERROR_STATE.INVALID_INPUT_ARGS));
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			System.exit(ERROR_CODE.get(ERROR_STATE.IO_ERR));
		}

	}
}
