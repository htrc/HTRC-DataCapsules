package edu.indiana.d2i.sloan.exception;

public class NonRetriableException extends Exception {
	private static final long serialVersionUID = 7226563971730690229L;

	public NonRetriableException(String message, Throwable throwable) {
		super(message, throwable);
	}

	public NonRetriableException(String message) {
		super(message);
	}

}
