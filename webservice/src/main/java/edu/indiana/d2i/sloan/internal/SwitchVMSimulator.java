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
import java.io.FileNotFoundException;
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
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import edu.indiana.d2i.sloan.vm.VMMode;
import edu.indiana.d2i.sloan.vm.VMState;

public class SwitchVMSimulator extends HypervisorCmdSimulator {
	private static Logger logger = Logger.getLogger(SwitchVMSimulator.class);

	public SwitchVMSimulator() {
		initOptions();
	}

	@Override
	protected void initOptions() {
		options = new Options();

		Option wdir = OptionBuilder.withArgName("workingdir").isRequired()
				.hasArg().withDescription("working directory")
				.create(CMD_FLAG_VALUE.get(CMD_FLAG_KEY.WORKING_DIR));

		Option mode = OptionBuilder.withArgName("maintenance|secure")
				.isRequired().hasArg().withDescription("vm mode")
				.create(CMD_FLAG_VALUE.get(CMD_FLAG_KEY.VM_MODE));

		Option policy = OptionBuilder.withArgName("firewallpolicy")
				.isRequired().hasArg()
				.withDescription("path to firewall policy file")
				.create(CMD_FLAG_VALUE.get(CMD_FLAG_KEY.POLICY_PATH));

		options.addOption(wdir);
		options.addOption(mode);
		options.addOption(policy);

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		SwitchVMSimulator simulator = new SwitchVMSimulator();

		CommandLineParser parser = new PosixParser();

		try {
			CommandLine line = simulator.parseCommandLine(parser, args);

			String wdir = line.getOptionValue(CMD_FLAG_VALUE
					.get(CMD_FLAG_KEY.WORKING_DIR));
			String mode = line.getOptionValue(CMD_FLAG_VALUE
					.get(CMD_FLAG_KEY.VM_MODE));
			String policyFilePath = line.getOptionValue(CMD_FLAG_VALUE
					.get(CMD_FLAG_KEY.POLICY_PATH));

			if (!HypervisorCmdSimulator.resourceExist(wdir)) {
				logger.error(String.format("Cannot find VM working dir: %s",
						wdir));
				System.exit(ERROR_CODE.get(ERROR_STATE.VM_NOT_EXIST));
			}

			VMMode requestedMode = HypervisorCmdSimulator.getVMMode(mode);

			if (requestedMode == null) {
				logger.error(String.format(
						"Invalid requested mode: %s, can only be %s or %s",
						mode, VMMode.MAINTENANCE.toString(),
						VMMode.SECURE.toString()));
				System.exit(ERROR_CODE.get(ERROR_STATE.INVALID_VM_MODE));
			}

			if (!HypervisorCmdSimulator.resourceExist(policyFilePath)) {
				logger.error(String.format("Cannot find plicy file: %s",
						policyFilePath));
				System.exit(ERROR_CODE
						.get(ERROR_STATE.FIREWALL_POLICY_NOT_EXIST));
			}

			// load VM state file
			Properties prop = new Properties();
			String filename = HypervisorCmdSimulator.cleanPath(wdir)
					+ HypervisorCmdSimulator.VM_INFO_FILE_NAME;

			prop.load(new FileInputStream(new File(filename)));

			// cannot switch when VM is not running
			VMState currentState = VMState.valueOf(prop
					.getProperty(CMD_FLAG_VALUE.get(CMD_FLAG_KEY.VM_STATE)));

			if (!currentState.equals(VMState.RUNNING)) {
				logger.error("Cannot perform switch when VM is not running");
				System.exit(ERROR_CODE.get(ERROR_STATE.VM_NOT_RUNNING));
			}

			// get current mode
			VMMode currentMode = VMMode.valueOf(prop.getProperty(CMD_FLAG_VALUE
					.get(CMD_FLAG_KEY.VM_MODE)));

			if (currentMode.equals(requestedMode)) {
				logger.error(String.format(
						"VM is already in the requested mode: %s",
						requestedMode.toString()));
				System.exit(ERROR_CODE
						.get(ERROR_STATE.VM_ALREADY_IN_REQUESTED_MODE));
			}

			// switch VM
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				logger.error(e.getMessage());
			}

			// update firewall policy
			prop.put(CMD_FLAG_VALUE.get(CMD_FLAG_KEY.POLICY_PATH),
					policyFilePath);

			// update VM status file, i.e. set mode to the requested mode
			prop.put(CMD_FLAG_VALUE.get(CMD_FLAG_KEY.VM_MODE),
					requestedMode.toString());

			// save VM state file back
			prop.store(new FileOutputStream(new File(filename)), "");

			// success
			System.exit(0);
		} catch (ParseException e) {
			logger.error(String.format(
					"Cannot parse input arguments: %s%n, expected:%n%s",
					StringUtils.join(args, " "),
					simulator.getUsage(100, "", 5, 5, "")));

			System.exit(ERROR_CODE.get(ERROR_STATE.INVALID_INPUT_ARGS));
		} catch (FileNotFoundException e) {
			logger.error(String.format("Cannot find vm state file: %s",
					e.getMessage()));

			System.exit(ERROR_CODE.get(ERROR_STATE.VM_STATE_FILE_NOT_FOUND));
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			System.exit(ERROR_CODE.get(ERROR_STATE.IO_ERR));
		}
	}
}
