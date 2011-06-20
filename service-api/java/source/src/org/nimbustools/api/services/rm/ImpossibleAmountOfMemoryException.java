package org.nimbustools.api.services.rm;

public class ImpossibleAmountOfMemoryException extends ResourceRequestDeniedException {

    public ImpossibleAmountOfMemoryException() {
        super();
    }

    public ImpossibleAmountOfMemoryException(String message) {
        super(message);
    }

    public ImpossibleAmountOfMemoryException(String message, Throwable e) {
        super(message, e);
    }

    public ImpossibleAmountOfMemoryException(Exception e) {
        super(e);
    }
}
