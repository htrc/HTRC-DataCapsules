package edu.indiana.d2i.sloan.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class SSHProxy {
	private static final Log logger = LogFactory.getLog(SSHProxy.class);
	private static int BUFFER_SIZE = 1024;
	/* sleep duration in milliseconds */
	private static long THREAD_SLEEP_DURATION = 1000;

	private String hostname;
	private int port;
	private String username;
	private String passwd;

	private JSch jsch = null;
	private Session session;

	private final String SUDO_PREFIX = "sudo ";

	public class Commands {
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

			if ((commands == null) || (commands.size() == 0)) {
				return "";
			}

			StringBuilder cmd = new StringBuilder();

			for (int i = 0; i < commands.size() - 1; i++) {
				cmd.append(commands.get(i)).append(";");
			}

			cmd.append(commands.get(commands.size() - 1));

			return cmd.toString();
		}
	}

	public class CmdsExecResult {
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

	public SSHProxy(String hostname, int port, String username, String passwd) {
		this.hostname = hostname;
		this.port = port;
		this.username = username;
		this.passwd = passwd;

		jsch = new JSch();
	}

	public void connect() throws JSchException {
		Properties sshConfig = new Properties();
		sshConfig.put("StrictHostKeyChecking", "no");
		session = jsch.getSession(username, hostname, port);
		session.setPassword(passwd);
		session.setConfig(sshConfig);
		session.connect();
	}

	public void close() throws Exception {
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

	public String getHostname() {
		return hostname;
	}
}
