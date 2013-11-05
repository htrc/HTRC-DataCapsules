package edu.indiana.d2i.sloan.utils;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import edu.indiana.d2i.sloan.utils.SSHProxy.CmdsExecResult;
import edu.indiana.d2i.sloan.utils.SSHProxy.Commands;

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
