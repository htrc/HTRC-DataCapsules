package edu.indiana.d2i.sloan.exception;

@SuppressWarnings("serial")
public class NoItemIsFoundInDBException	extends SloanWSException {
	public NoItemIsFoundInDBException(String message, Throwable throwable) {
        super(message, throwable);
    }
    
    public NoItemIsFoundInDBException(String message) {
        super(message);
    }
}
