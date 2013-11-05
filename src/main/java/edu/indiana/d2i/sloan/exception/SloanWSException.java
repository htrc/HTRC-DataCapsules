package edu.indiana.d2i.sloan.exception;

@SuppressWarnings("serial")
public class SloanWSException extends Exception {
	public SloanWSException(String message, Throwable throwable) {
        super(message, throwable);
    }
    
    public SloanWSException(String message) {
        super(message);
    }
}
