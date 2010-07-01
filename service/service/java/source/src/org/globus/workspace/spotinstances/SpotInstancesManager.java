package org.globus.workspace.spotinstances;

import org.globus.workspace.StateChangeInterested;
import org.globus.workspace.scheduler.defaults.PreemptableSpaceManager;

public interface SpotInstancesManager extends SpotInstancesHome, PreemptableSpaceManager, StateChangeInterested {

    public void addRequest(SIRequest request);
    
}
