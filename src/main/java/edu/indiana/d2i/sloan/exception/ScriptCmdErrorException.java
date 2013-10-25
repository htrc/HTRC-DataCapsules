package edu.indiana.d2i.sloan.exception;

/**
 * 
 * Thrown when the script returns a non-zero exit code
 * 
 */
public class ScriptCmdErrorException extends Exception {

	private static final long serialVersionUID = -6337023739467662121L;

	public ScriptCmdErrorException(String message, Throwable throwable) {
		super(message, throwable);
	}

	public ScriptCmdErrorException(String message) {
		super(message);
	}
}
