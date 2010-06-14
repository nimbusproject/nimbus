package org.globus.workspace.spotinstances;

import org.globus.workspace.scheduler.defaults.PreemptableSpaceManager;

public interface SpotInstancesManager extends PreemptableSpaceManager {

    public void addRequest(SIRequest request);
    
}
