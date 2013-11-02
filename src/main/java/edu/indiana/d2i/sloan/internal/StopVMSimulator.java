package edu.indiana.d2i.sloan.internal;

import java.io.File;
import java.io.FileInputStream;
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

import edu.indiana.d2i.sloan.vm.VMState;

public class StopVMSimulator extends HypervisorCmdSimulator {
	private static Logger logger = Logger.getLogger(StopVMSimulator.class);

	@Override
	protected void initOptions() {
		options = new Options();

		Option wdir = OptionBuilder.withArgName("workingdir").hasArg()
				.withDescription("working directory")
				.create(CMD_FLAG_VALUE.get(CMD_FLAG_KEY.WORKING_DIR));

		options.addOption(wdir);
	}

	public static void main(String[] args) {
		StopVMSimulator simulator = new StopVMSimulator();

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

			// cannot stop VM when it is not running
			VMState currentState = VMState.valueOf(prop
					.getProperty(CMD_FLAG_VALUE.get(CMD_FLAG_KEY.VM_STATE)));

			if (!currentState.equals(VMState.RUNNING)) {
				logger.error("Cannot perform stop when VM is not running");
				System.exit(ERROR_CODE.get(ERROR_STATE.VM_NOT_RUNNING));
			}

			// stop VM
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				logger.error(e.getMessage());
			}

			// update VM status file, i.e. set state to shutdown
			prop.put(CMD_FLAG_VALUE.get(CMD_FLAG_KEY.VM_STATE),
					VMState.SHUTDOWN);

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
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			System.exit(ERROR_CODE.get(ERROR_STATE.IO_ERR));
		}

	}
}
