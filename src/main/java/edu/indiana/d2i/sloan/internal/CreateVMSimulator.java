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
import java.io.FileOutputStream;
import java.io.IOException;
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
import org.apache.log4j.Logger;

import edu.indiana.d2i.sloan.vm.VMMode;
import edu.indiana.d2i.sloan.vm.VMState;

public class CreateVMSimulator extends HypervisorCmdSimulator {
	private static Logger logger = Logger.getLogger(CreateVMSimulator.class);

	public CreateVMSimulator() {
		initOptions();
	}

	@Override
	protected void initOptions() {
		options = new Options();

		Option image = OptionBuilder.withArgName("image").isRequired().hasArg()
				.withDescription("path to vm image file")
				.create(CMD_FLAG_VALUE.get(CMD_FLAG_KEY.IMAGE_PATH));

		Option vcpu = OptionBuilder.withArgName("numcpus").isRequired()
				.hasArg().withDescription("# of cpus")
				.create(CMD_FLAG_VALUE.get(CMD_FLAG_KEY.VCPU));

		Option mem = OptionBuilder.withArgName("memorysize").isRequired()
				.hasArg().withDescription("memeory size in MB")
				.create(CMD_FLAG_VALUE.get(CMD_FLAG_KEY.MEM));

		Option wdir = OptionBuilder.withArgName("workingdir").isRequired()
				.hasArg().withDescription("working directory")
				.create(CMD_FLAG_VALUE.get(CMD_FLAG_KEY.WORKING_DIR));

		Option vnc = OptionBuilder.withArgName("vncport").isRequired().hasArg()
				.withDescription("vnc port #")
				.create(CMD_FLAG_VALUE.get(CMD_FLAG_KEY.VNC_PORT));

		Option ssh = OptionBuilder.withArgName("sshport").isRequired().hasArg()
				.withDescription("ssh port #")
				.create(CMD_FLAG_VALUE.get(CMD_FLAG_KEY.SSH_PORT));

		Option loginid = OptionBuilder.withArgName("loginusername")
				.isRequired().hasArg().withDescription("vm login username")
				.create(CMD_FLAG_VALUE.get(CMD_FLAG_KEY.LOGIN_USERNAME));

		Option loginpwd = OptionBuilder.withArgName("loginpassword")
				.isRequired().hasArg().withDescription("vm login password")
				.create(CMD_FLAG_VALUE.get(CMD_FLAG_KEY.LOGIN_PASSWD));

		options.addOption(image);
		options.addOption(vcpu);
		options.addOption(mem);
		options.addOption(wdir);
		options.addOption(vnc);
		options.addOption(ssh);
		options.addOption(loginid);
		options.addOption(loginpwd);
	}

	public static synchronized boolean hasEnoughCPUs(int requestedNumCPUs) {
		// TODO: add more complicated logic, e.g. check whether there are
		// enough remaining CPUs, this requires to maintain info about physical
		// resources. Moreover, we need to consider race condition when
		// accessing
		// and updating the available resource info

		// current always return true, i.e. assume unlimited resources
		return true;
	}

	public static synchronized boolean hasEnoughMem(int requestedMem) {
		// TODO: same as hasEnoughCPUs method
		return true;
	}

	public static void main(String[] args) {
		CreateVMSimulator simulator = new CreateVMSimulator();

		CommandLineParser parser = new PosixParser();

		try {
			CommandLine line = simulator.parseCommandLine(parser, args);

			String imagePath = line.getOptionValue(CMD_FLAG_VALUE
					.get(CMD_FLAG_KEY.IMAGE_PATH));
			int vcpu = Integer.parseInt(line.getOptionValue(CMD_FLAG_VALUE
					.get(CMD_FLAG_KEY.VCPU)));
			int mem = Integer.parseInt(line.getOptionValue(CMD_FLAG_VALUE
					.get(CMD_FLAG_KEY.MEM)));

			if (!HypervisorCmdSimulator.resourceExist(imagePath)) {
				logger.error(String.format("Cannot find requested image: %s",
						imagePath));
				System.exit(ERROR_CODE.get(ERROR_STATE.IMAGE_NOT_EXIST));
			}

			if (!hasEnoughCPUs(vcpu)) {
				logger.error(String.format(
						"Don't have enough cpus, requested %d", vcpu));
				System.exit(ERROR_CODE.get(ERROR_STATE.NOT_ENOUGH_CPU));
			}

			if (!hasEnoughMem(mem)) {
				logger.error(String.format(
						"Don't have enough memory, requested %d", mem));
				System.exit(ERROR_CODE.get(ERROR_STATE.NOT_ENOUGH_MEM));
			}

			String wdir = line.getOptionValue(CMD_FLAG_VALUE
					.get(CMD_FLAG_KEY.WORKING_DIR));

			if (HypervisorCmdSimulator.resourceExist(wdir)) {
				logger.error(String.format(
						"Working directory %s already exists ", wdir));
				System.exit(ERROR_CODE.get(ERROR_STATE.VM_ALREADY_EXIST));
			}

			// copy VM image to working directory
			File imageFile = new File(imagePath);
			FileUtils.copyFile(
					imageFile,
					new File(HypervisorCmdSimulator.cleanPath(wdir)
							+ imageFile.getName()));

			// write state as property file so that we can query later
			Properties prop = new Properties();

			prop.put(CMD_FLAG_VALUE.get(CMD_FLAG_KEY.IMAGE_PATH), imagePath);
			prop.put(CMD_FLAG_VALUE.get(CMD_FLAG_KEY.VCPU),
					String.valueOf(vcpu));
			prop.put(CMD_FLAG_VALUE.get(CMD_FLAG_KEY.MEM), String.valueOf(mem));
			prop.put(CMD_FLAG_VALUE.get(CMD_FLAG_KEY.WORKING_DIR), wdir);
			prop.put(CMD_FLAG_VALUE.get(CMD_FLAG_KEY.VNC_PORT), line
					.getOptionValue(CMD_FLAG_VALUE.get(CMD_FLAG_KEY.VNC_PORT)));
			prop.put(CMD_FLAG_VALUE.get(CMD_FLAG_KEY.SSH_PORT), line
					.getOptionValue(CMD_FLAG_VALUE.get(CMD_FLAG_KEY.SSH_PORT)));
			prop.put(CMD_FLAG_VALUE.get(CMD_FLAG_KEY.LOGIN_USERNAME), line
					.getOptionValue(CMD_FLAG_VALUE
							.get(CMD_FLAG_KEY.LOGIN_USERNAME)));
			prop.put(CMD_FLAG_VALUE.get(CMD_FLAG_KEY.LOGIN_PASSWD), line
					.getOptionValue(CMD_FLAG_VALUE
							.get(CMD_FLAG_KEY.LOGIN_PASSWD)));
			
			// write VM state as shutdown
			prop.put(CMD_FLAG_VALUE.get(CMD_FLAG_KEY.VM_STATE),
					VMState.SHUTDOWN.toString());
			// write VM mode as undefined
			prop.put(CMD_FLAG_VALUE.get(CMD_FLAG_KEY.VM_MODE),
					VMMode.NOT_DEFINED.toString());

			prop.store(
					new FileOutputStream(new File(HypervisorCmdSimulator
							.cleanPath(wdir)
							+ HypervisorCmdSimulator.VM_INFO_FILE_NAME)), "");

			// do other related settings
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				logger.error(e.getMessage());
			}

			// success
			System.exit(0);

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
