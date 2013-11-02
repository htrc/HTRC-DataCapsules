package edu.indiana.d2i.sloan.internal;

import java.io.File;
import java.io.FileInputStream;
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

import edu.indiana.d2i.sloan.vm.VMState;

public class DeleteVMSimulator extends HypervisorCmdSimulator {
	private static Logger logger = Logger.getLogger(DeleteVMSimulator.class);

	public DeleteVMSimulator() {
		initOptions();
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
		DeleteVMSimulator simulator = new DeleteVMSimulator();

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

			// cannot delete VM when it is not shutdown
			VMState currentState = VMState.valueOf(prop
					.getProperty(CMD_FLAG_VALUE.get(CMD_FLAG_KEY.VM_STATE)));

			if (!currentState.equals(VMState.SHUTDOWN)) {
				logger.error("Cannot perform delete when VM is not shutdown");
				System.exit(ERROR_CODE.get(ERROR_STATE.VM_NOT_SHUTDOWN));
			}

			// delete working directory
			FileUtils.deleteDirectory(new File(wdir));

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
