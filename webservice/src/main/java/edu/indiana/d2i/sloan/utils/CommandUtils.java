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
package edu.indiana.d2i.sloan.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import edu.indiana.d2i.sloan.Configuration;

public class CommandUtils {
	public enum HYPERVISOR_CMD {
		CREATE_VM, LAUNCH_VM, QUERY_VM, SWITCH_VM, STOP_VM, DELETE_VM, UPDATE_KEY
	}

	private static final Map<HYPERVISOR_CMD, String> commands;

	static {
		commands = new HashMap<HYPERVISOR_CMD, String>() {
			private static final long serialVersionUID = 1445020643148509308L;

			{
				put(HYPERVISOR_CMD.CREATE_VM, Configuration.getInstance()
						.getString(Configuration.PropertyName.CMD_CREATE_VM));

				put(HYPERVISOR_CMD.LAUNCH_VM, Configuration.getInstance()
						.getString(Configuration.PropertyName.CMD_LAUNCH_VM));

				put(HYPERVISOR_CMD.QUERY_VM, Configuration.getInstance()
						.getString(Configuration.PropertyName.CMD_QUERY_VM));

				put(HYPERVISOR_CMD.SWITCH_VM, Configuration.getInstance()
						.getString(Configuration.PropertyName.CMD_SWITCH_VM));

				put(HYPERVISOR_CMD.STOP_VM, Configuration.getInstance()
						.getString(Configuration.PropertyName.CMD_STOP_VM));

				put(HYPERVISOR_CMD.DELETE_VM, Configuration.getInstance()
						.getString(Configuration.PropertyName.CMD_DELETE_VM));

				put(HYPERVISOR_CMD.UPDATE_KEY, Configuration.getInstance()
						.getString(Configuration.PropertyName.CMD_UPDATE_KEY));
			}
		};
	}

	public static class Argument {
		/* set argName to null if not present */
		private String argName;
		private String argValue;

		public Argument(String argName, String argValue) {
//			super();
			this.argName = argName;
			this.argValue = argValue;
		}

		public String getArgName() {
			return argName;
		}
		public String getArgValue() {
			return argValue;
		}

		@Override
		public String toString() {
			return (argName == null) ? argValue : argName + " " + argValue;
		}
	}

	public static class ArgsBuilder {
		private List<Argument> args = new ArrayList<Argument>();

		public ArgsBuilder addArgument(Argument argument) {
			args.add(argument);
			return this;
		}

		public ArgsBuilder addArgument(String argName, String argValue) {
			args.add(new Argument(argName, argValue));
			return this;
		}

		public String build() {
			return (args.size() == 0) ? "": StringUtils.join(args.iterator(), " ");
		}
	}

	/**
	 * 
	 * @param cmdKey
	 * @return the hypervisor script name
	 */
	public static String getCommand(HYPERVISOR_CMD cmdKey) {
		return commands.get(cmdKey);
	}

	public static String composeFullCommand(HYPERVISOR_CMD cmdKey,
			String argList) {
		return commands.get(cmdKey) + " " + argList;
	}
	
}
