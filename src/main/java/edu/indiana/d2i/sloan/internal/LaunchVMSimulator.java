package edu.indiana.d2i.sloan.internal;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import edu.indiana.d2i.sloan.internal.HypervisorCmdSimulator.ERROR_STATE;

public class LaunchVMSimulator extends HypervisorCmdSimulator {
	private static Logger logger = Logger.getLogger(LaunchVMSimulator.class);

	@Override
	protected void initOptions() {
		options = new Options();

		Option wdir = OptionBuilder.withArgName("workingdir").hasArg()
				.withDescription("working directory").create("wdir");

		Option mode = OptionBuilder.withArgName("maintain|secure").hasArg()
				.withDescription("vm mode").create("mode");

		Option policy = OptionBuilder.withArgName("firewallpolicy").hasArg()
				.withDescription("path to firewall policy file")
				.create("policy");

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

			String wdir = line.getOptionValue("wdir");
			String mode = line.getOptionValue("mode");
			String policyFilePath = line.getOptionValue("policy");

			if (!HypervisorCmdSimulator.checkFileExist(wdir)) {
				logger.error(String.format("Cannot find VM working dir: %s",
						wdir));
				System.exit(ERROR_CODE.get(ERROR_STATE.VM_NOT_EXIST));
			}

			if (!HypervisorCmdSimulator.checkFileExist(policyFilePath)) {
				logger.error(String.format("Cannot find plicy file: %s",
						policyFilePath));
				System.exit(ERROR_CODE
						.get(ERROR_STATE.FIREWALL_POLICY_NOT_EXIST));
			}

			if (!(HypervisorCmdSimulator.MAINTAIN_MODE_STR
					.equalsIgnoreCase(mode) || HypervisorCmdSimulator.SECURE_MODE_STR
					.equalsIgnoreCase(mode))) {
				logger.error(String.format("Invalid requested mode: %s", mode));
				System.exit(ERROR_CODE.get(ERROR_STATE.INVALID_VM_MODE));
			}

			// launch VM
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
		}
	}
}
