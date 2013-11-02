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

public class LaunchVMSimulator extends HypervisorCmdSimulator {
	private static Logger logger = Logger.getLogger(LaunchVMSimulator.class);

	public LaunchVMSimulator() {
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
		LaunchVMSimulator simulator = new LaunchVMSimulator();

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

			VMMode vmmode = HypervisorCmdSimulator.getVMMode(mode);

			if (vmmode == null) {
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

			// load VM status info
			Properties prop = new Properties();
			String filename = HypervisorCmdSimulator.cleanPath(wdir)
					+ HypervisorCmdSimulator.VM_INFO_FILE_NAME;

			prop.load(new FileInputStream(new File(filename)));

			// can only launch VM when it is in shutdown state
			VMState currentState = VMState.valueOf(prop
					.getProperty(CMD_FLAG_VALUE.get(CMD_FLAG_KEY.VM_STATE)));

			if (!currentState.equals(VMState.SHUTDOWN)) {
				logger.error(String
						.format("Can only launch VM when it is in %s state, current VM state is %s",
								VMState.SHUTDOWN.toString(),
								currentState.toString()));
				System.exit(ERROR_CODE.get(ERROR_STATE.VM_NOT_SHUTDOWN));
			}
			// launch VM
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				logger.error(e.getMessage());
			}

			// update VM state file

			// set following properties
			prop.put(CMD_FLAG_VALUE.get(CMD_FLAG_KEY.POLICY_PATH),
					policyFilePath);
			prop.put(CMD_FLAG_VALUE.get(CMD_FLAG_KEY.VM_MODE),
					vmmode.toString());

			// set VM state to running
			prop.put(CMD_FLAG_VALUE.get(CMD_FLAG_KEY.VM_STATE),
					VMState.RUNNING.toString());

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
