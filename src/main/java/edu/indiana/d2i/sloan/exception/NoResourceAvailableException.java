package edu.indiana.d2i.sloan.exception;

@SuppressWarnings("serial")
public class NoResourceAvailableException extends SloanWSException {
	public NoResourceAvailableException(String message, Throwable throwable) {
        super(message, throwable);
    }
    
    public NoResourceAvailableException(String message) {
        super(message);
    }
}
