package edu.indiana.d2i.sloan.exception;

public class RetriableException extends Exception {
	private static final long serialVersionUID = 2034465618482032236L;

	public RetriableException(String message, Throwable throwable) {
		super(message, throwable);
	}

	public RetriableException(String message) {
		super(message);
	}

}
