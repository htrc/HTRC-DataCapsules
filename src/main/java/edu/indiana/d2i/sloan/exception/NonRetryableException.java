package edu.indiana.d2i.sloan.exception;

public class NonRetryableException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7226563971730690229L;

	public NonRetryableException(String message, Throwable throwable) {
		super(message, throwable);
	}

	public NonRetryableException(String message) {
		super(message);
	}

}
