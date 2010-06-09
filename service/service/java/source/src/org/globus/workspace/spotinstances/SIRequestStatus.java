package org.globus.workspace.spotinstances;

public enum SIRequestStatus {

    OPEN, ACTIVE, CLOSED, CANCELLED, FAILED;
    
    public boolean isAlive(){
        return this.equals(OPEN) || this.equals(ACTIVE);
    }
    
    public boolean isActive(){
        return this.equals(ACTIVE);
    }    
    
}
