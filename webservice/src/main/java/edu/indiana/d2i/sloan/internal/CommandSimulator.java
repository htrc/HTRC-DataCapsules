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

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public abstract class CommandSimulator {
	protected Options options;

	/* this method should be called within constructor as initialization code */
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
