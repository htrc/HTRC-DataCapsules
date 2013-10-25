package edu.indiana.d2i.sloan.exception;

public class StateTransitionException extends Exception {

	private static final long serialVersionUID = 2701822278442073803L;

	public StateTransitionException(String message, Throwable throwable) {
		super(message, throwable);
	}

	public StateTransitionException(String message) {
		super(message);
	}
}
