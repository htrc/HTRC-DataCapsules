package edu.indiana.d2i.sloan.internal;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public abstract class CommandSimulator {
	protected Options options;

	abstract protected void initOptions();

	protected CommandLine parseCommandLine(CommandLineParser parser,
			String[] args) throws ParseException {

		// parse the command line arguments
		return parser.parse(options, args);

	}

	protected void printUsage() {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(getClass().getCanonicalName(), options);
	}

	protected String getUsage(int width, String header, int leftPad,
			int descPad, String footer) {
		HelpFormatter formatter = new HelpFormatter();
		StringWriter sw = new StringWriter();

		formatter.printHelp(new PrintWriter(sw), width, getClass()
				.getCanonicalName(), header, options, leftPad, descPad, footer);

		return sw.toString();
	}
}
