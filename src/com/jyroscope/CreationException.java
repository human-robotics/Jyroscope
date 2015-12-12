package com.jyroscope;

public class CreationException extends Exception {

    public CreationException(String message) {
        super(message);
    }
    
    public CreationException(Throwable cause) {
        super(cause);
    }
    
    public CreationException(String message, Throwable cause) {
        super(message, cause);
    }
    
}
