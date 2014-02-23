package edu.indiana.d2i.sloan.exception;

@SuppressWarnings("serial")
public class ResultExpireException extends SloanWSException {

	public ResultExpireException(String message, Throwable throwable) {
        super(message, throwable);
    }
	
	public ResultExpireException(String message) {
		super(message);
	}	
}
