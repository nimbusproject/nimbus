package org.globus.workspace.spotinstances;

import org.nimbustools.api.repr.Caller;
import org.nimbustools.api.services.rm.DoesNotExistException;


public interface SpotInstancesHome {

    public void addRequest(SIRequest request);

    public SIRequest cancelRequest(String reqID) throws DoesNotExistException;
    
    public SIRequest getRequest(String id) throws DoesNotExistException;
    
    public SIRequest[] getRequests(Caller caller);
    
    public Double getSpotPrice();

}
