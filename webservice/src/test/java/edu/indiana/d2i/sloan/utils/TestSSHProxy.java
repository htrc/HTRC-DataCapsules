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

import java.util.Collections;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import edu.indiana.d2i.sloan.utils.SSHProxy.CmdsExecResult;
import edu.indiana.d2i.sloan.utils.SSHProxy.Commands;

@Ignore
public class TestSSHProxy {
	private SSHProxy proxy = null;
	
	@Before
	public void before() throws Exception {
		proxy = new SSHProxy.SSHProxyBuilder("localhost", 22, 
			"root").usePrivateKey("~/.ssh/id_rsa").build();
	}
	
	@Test
	public void testExec() throws Exception {
		String argList = new CommandUtils.ArgsBuilder()
		.addArgument("-image", "/tmp/script-simulator/vm-image/ubuntu-image")
		.addArgument("-vcpu", "1")
		.addArgument("-mem", "1024")
		.addArgument("-wdir", "/tmp/script-simulator/vm-workingdir/instance10")
		.addArgument("-vnc", "2000")
		.addArgument("-ssh", "2001")
		.addArgument("-loginid", "ubuntu")
		.addArgument("-loginpwd", "ubuntu")
		.build();
		
		CmdsExecResult result = proxy.execCmdSync(
			new Commands(Collections.<String>singletonList(
				"/tmp/script-simulator/create_vm_script.sh " + argList), 
//			new Commands(Collections.<String>singletonList(
//				"java -version"),
			false));
		System.out.println(result);
	}
}
