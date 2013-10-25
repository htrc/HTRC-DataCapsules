package edu.indiana.d2i.sloan.exception;

public class RetryableException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2034465618482032236L;

	public RetryableException(String message, Throwable throwable) {
		super(message, throwable);
	}

	public RetryableException(String message) {
		super(message);
	}

}
