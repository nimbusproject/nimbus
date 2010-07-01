package org.globus.workspace.scheduler.defaults;


public interface PreemptableSpaceManager {

    public void freeSpace(Integer memoryToFree);

    public void start();
    
}
