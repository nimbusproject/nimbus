package org.nimbustools.api.services.rm;

public class NotEnoughMemoryException extends ResourceRequestDeniedException {

    public NotEnoughMemoryException() {
        super();
    }

    public NotEnoughMemoryException(String message) {
        super(message);
    }

    public NotEnoughMemoryException(String message, Throwable e) {
        super(message, e);
    }

    public NotEnoughMemoryException(Exception e) {
        super(e);
    }
    
}
