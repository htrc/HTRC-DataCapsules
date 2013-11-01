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

public class CreateVMSimulator extends HypervisorCmdSimulator {
	private static Logger logger = Logger.getLogger(CreateVMSimulator.class);

	@Override
	protected void initOptions() {
		options = new Options();

		Option image = OptionBuilder.withArgName("image").hasArg()
				.withDescription("path to vm image file").create("image");

		Option vcpu = OptionBuilder.withArgName("numcpus").hasArg()
				.withDescription("# of cpus").create("vcpu");

		Option mem = OptionBuilder.withArgName("memorysize").hasArg()
				.withDescription("memeory size in MB").create("mem");

		Option wdir = OptionBuilder.withArgName("workingdir").hasArg()
				.withDescription("working directory").create("wdir");

		Option vnc = OptionBuilder.withArgName("vncport").hasArg()
				.withDescription("vnc port #").create("vnc");

		Option ssh = OptionBuilder.withArgName("sshport").hasArg()
				.withDescription("ssh port #").create("ssh");

		Option loginid = OptionBuilder.withArgName("loginusername").hasArg()
				.withDescription("vm login username").create("loginid");

		Option loginpwd = OptionBuilder.withArgName("loginpassword").hasArg()
				.withDescription("vm login password").create("loginpwd");

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
		// TODO: add more complicated logic, e.g. check whether if there are
		// enough remaining CPUs, this requires to maintain info about physical
		// resources. We need to consider race condition when access the info

		// current always return true, i.e. assume unlimited resources
		return true;
	}

	public static synchronized boolean hasEnoughMem(int requestedMem) {
		// TODO:
		return true;
	}

	public static void main(String[] args) {
		CreateVMSimulator simulator = new CreateVMSimulator();

		CommandLineParser parser = new PosixParser();

		try {
			CommandLine line = simulator.parseCommandLine(parser, args);

			String imagePath = line.getOptionValue("image");
			int vcpu = Integer.parseInt(line.getOptionValue("vcpu"));
			int mem = Integer.parseInt(line.getOptionValue("mem"));

			if (!HypervisorCmdSimulator.checkVMImageExist(imagePath)) {
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

			// copy VM image
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {

			}

			// start VM
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {

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
