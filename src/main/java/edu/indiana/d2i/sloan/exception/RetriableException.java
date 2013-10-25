package edu.indiana.d2i.sloan.exception;

@SuppressWarnings("serial")
public class RetriableException extends SloanWSException {
	public RetriableException(String message) {
		super(message);
	}

	public RetriableException(String message, Throwable throwable) {
		super(message, throwable);
	}
}
