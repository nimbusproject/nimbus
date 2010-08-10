package org.globus.workspace.async;

public enum AsyncRequestStatus {

    OPEN, ACTIVE, CLOSED, CANCELLED, FAILED;
    
    public boolean isActive(){
        return this.equals(ACTIVE);
    }

    public boolean isClosed() {
        return this.equals(CLOSED);
    }

    public boolean isOpen() {
        return this.equals(OPEN);
    }

    public boolean isCancelled() {
        return this.equals(CANCELLED);
    }

    public boolean isFailed() {
        return this.equals(FAILED);
    }    
    
}
