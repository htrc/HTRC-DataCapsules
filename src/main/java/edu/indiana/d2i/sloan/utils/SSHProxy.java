package edu.indiana.d2i.sloan.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 * 
 * If private key path is given, SSHProxy will use public-private key
 * authentication, otherwise it will use password.
 * 
 */
public class SSHProxy {
	private static final Log logger = LogFactory.getLog(SSHProxy.class);

	public static int SSH_DEFAULT_PORT = 22;

	private static int BUFFER_SIZE = 1024;
	/* sleep duration in milliseconds */
	private static long THREAD_SLEEP_DURATION = 1000;

	private String hostname;
	private int port;
	private String username;
	private String passwd;
	private String privateKeyPath;

	private JSch jsch = null;
	private Session session = null;

	private final String SUDO_PREFIX = "sudo ";

	public static class Commands {
		private List<String> commands;
		private boolean isSudoCmds;

		public Commands(List<String> commands, boolean isSudoCmds) {
			super();
			this.commands = commands;
			this.isSudoCmds = isSudoCmds;
		}

		public List<String> getCommands() {
			return commands;
		}

		public boolean isSudoCmds() {
			return isSudoCmds;
		}

		public String getConcatenatedForm() {
			return (commands == null) || (commands.size() == 0) ? "":
				StringUtils.join(commands.iterator(), ";");
		}
	}

	public static class CmdsExecResult {
		private Commands cmds;
		private String hostname;
		private int exitCode;
		private String screenOutput;

		public CmdsExecResult(Commands cmds, String hostname, int exitCode,
				String screenOutput) {
			super();
			this.cmds = cmds;
			this.hostname = hostname;
			this.exitCode = exitCode;
			this.screenOutput = screenOutput;
		}

		public Commands getCmds() {
			return cmds;
		}

		public int getExitCode() {
			return exitCode;
		}

		public String getScreenOutput() {
			return screenOutput;
		}

		public String getHostname() {
			return hostname;
		}

	}
	
	public static class SSHProxyBuilder {
		private String hostname;
		private int port;
		private String username;
		private String passwd = null;
		private String privateKeyPath = null;
		
		public SSHProxyBuilder(String hostname, int port, String username) {
			this.hostname = hostname;
			this.port = port;
			this.username = username;
		}
		
		public SSHProxyBuilder usePassword(String passwd) {
			this.passwd = passwd;
			return this;
		}
		
		public SSHProxyBuilder usePrivateKey(String privateKeyPath) {
			this.privateKeyPath = privateKeyPath;
			return this;
		}
		
		public SSHProxy build() throws JSchException {
			if (passwd != null && privateKeyPath != null)
				throw new IllegalArgumentException("Cannot take password and private key at the same time!");
			if (passwd == null && privateKeyPath == null)
				throw new IllegalArgumentException("Must set password or private key!");
			return new SSHProxy(this);
		}
	}
	
	private SSHProxy(SSHProxyBuilder builder) throws JSchException {
		this.hostname = builder.hostname;
		this.port = builder.port;
		this.username = builder.username;
		this.passwd = builder.passwd;
		this.privateKeyPath = builder.privateKeyPath;
		
		jsch = new JSch();

		/* prefer public-private key authentication */
		if (privateKeyPath != null) {
			jsch.addIdentity(privateKeyPath);
		}
		
		Properties sshConfig = new Properties();
		sshConfig.put("StrictHostKeyChecking", "no");
		session = jsch.getSession(username, hostname, port);

		if (privateKeyPath == null) {
			session.setPassword(passwd);
		}

		session.setConfig(sshConfig);
		session.connect();
	}

//	public SSHProxy(String hostname, int port, String username, String passwd,
//			String privateKeyPath) throws JSchException {
//		this.hostname = hostname;
//		this.port = port;
//		this.username = username;
//		this.passwd = passwd;
//		this.privateKeyPath = privateKeyPath;
//
//		jsch = new JSch();
//
//		/* prefer public-private key authentication */
//		if (privateKeyPath != null) {
//			jsch.addIdentity(privateKeyPath);
//		}
//	}
//	
//	public void connect() throws JSchException {
//		Properties sshConfig = new Properties();
//		sshConfig.put("StrictHostKeyChecking", "no");
//		session = jsch.getSession(username, hostname, port);
//
//		if (privateKeyPath == null) {
//			session.setPassword(passwd);
//		}
//
//		session.setConfig(sshConfig);
//		session.connect();
//	}
	
	public void close() {
		if (session != null)
			session.disconnect();
	}

	/**
	 * non-blocking execution of a list of commands
	 * 
	 * @param cmds
	 * @param requireSudo
	 * @throws Exception
	 */
	public void execCmdAsync(Commands cmds) throws Exception {
		String command = cmds.getConcatenatedForm();

		if (cmds.isSudoCmds)
			command = SUDO_PREFIX + command;

		Channel channel = session.openChannel("exec");
		((ChannelExec) channel).setCommand(command);

		channel.connect();
		channel.disconnect();
	}

	/**
	 * execute a list of commands in blocking way
	 * 
	 * @param cmds
	 * @param requireSudo
	 * @return
	 * @throws JSchException
	 * @throws IOException
	 * @throws Exception
	 */
	public CmdsExecResult execCmdSync(Commands cmds) throws JSchException,
			IOException {
		String command = cmds.getConcatenatedForm();
		int exitCode = Integer.MIN_VALUE;

		if (cmds.isSudoCmds)
			command = SUDO_PREFIX + command;

		StringBuilder screenOutput = new StringBuilder();

		Channel channel = session.openChannel("exec");

		((ChannelExec) channel).setCommand(command);
		((ChannelExec) channel).setErrStream(System.err);

		channel.connect();

		InputStream is = channel.getInputStream();

		byte[] buf = new byte[BUFFER_SIZE];

		while (true) {

			while (is.available() > 0) {
				int bytesRead = is.read(buf, 0, 1024);

				if (bytesRead < 0)
					break;

				screenOutput.append(new String(buf, 0, bytesRead));
			}

			if (channel.isClosed()) {
				exitCode = channel.getExitStatus();
				break;
			}

			/**
			 * sleep a while waiting for more outputs
			 */
			try {
				Thread.sleep(THREAD_SLEEP_DURATION);
			} catch (InterruptedException e) {
				logger.error(e.getMessage());
			}
		}

		// disconnect
		channel.disconnect();

		return new CmdsExecResult(cmds, hostname, exitCode,
				screenOutput.toString());
	}

}
