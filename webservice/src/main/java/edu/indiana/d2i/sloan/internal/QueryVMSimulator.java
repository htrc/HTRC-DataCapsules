/*******************************************************************************
 * Copyright 2014 The Trustees of Indiana University
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package edu.indiana.d2i.sloan.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import edu.indiana.d2i.sloan.vm.VMMode;
import edu.indiana.d2i.sloan.vm.VMState;

public class QueryVMSimulator extends HypervisorCmdSimulator {
	private static Logger logger = LoggerFactory.getLogger(QueryVMSimulator.class);
	private static final String QUERY_RES_FILE_NAME = "vmstate-query-result.txt";

	public QueryVMSimulator() {
		initOptions();
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

	@Override
	protected void initOptions() {
		options = new Options();

		Option wdir = OptionBuilder.withArgName("workingdir").isRequired()
				.hasArg().withDescription("working directory")
				.create(CMD_FLAG_VALUE.get(CMD_FLAG_KEY.WORKING_DIR));

		options.addOption(wdir);
	}

	public static void main(String[] args) {
		QueryVMSimulator simulator = new QueryVMSimulator();

		CommandLineParser parser = new PosixParser();

		try {
			CommandLine line = simulator.parseCommandLine(parser, args);
			String wdir = line.getOptionValue(CMD_FLAG_VALUE
					.get(CMD_FLAG_KEY.WORKING_DIR));

			if (!HypervisorCmdSimulator.resourceExist(wdir)) {
				logger.error(String.format("Cannot find VM working dir: %s",
						wdir));
				System.exit(ERROR_CODE.get(ERROR_STATE.VM_NOT_EXIST));
			}

			Properties prop = new Properties();
			String filename = HypervisorCmdSimulator.cleanPath(wdir)
					+ HypervisorCmdSimulator.VM_INFO_FILE_NAME;

			prop.load(new FileInputStream(new File(filename)));

			VMStatus status = new VMStatus(VMMode.valueOf(prop
					.getProperty(CMD_FLAG_VALUE.get(CMD_FLAG_KEY.VM_MODE))),
					VMState.valueOf(prop.getProperty(CMD_FLAG_VALUE
							.get(CMD_FLAG_KEY.VM_STATE))), ip,
					Integer.parseInt(prop.getProperty(CMD_FLAG_VALUE
							.get(CMD_FLAG_KEY.VNC_PORT))),
					Integer.parseInt(prop.getProperty(CMD_FLAG_VALUE
							.get(CMD_FLAG_KEY.SSH_PORT))),
					Integer.parseInt(prop.getProperty(CMD_FLAG_VALUE
							.get(CMD_FLAG_KEY.VCPU))), Integer.parseInt(prop
							.getProperty(CMD_FLAG_VALUE.get(CMD_FLAG_KEY.MEM))));

			// write state query file so shell script can read the vm state info
			filename = HypervisorCmdSimulator.cleanPath(wdir)
					+ QueryVMSimulator.QUERY_RES_FILE_NAME;
			FileUtils.writeStringToFile(new File(filename), status.toString(),
					Charset.forName("UTF-8"));

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
