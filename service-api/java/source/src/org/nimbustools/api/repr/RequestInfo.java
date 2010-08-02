package org.nimbustools.api.repr;

import java.util.Calendar;

import org.nimbustools.api.repr.si.RequestState;
import org.nimbustools.api.repr.vm.ResourceAllocation;
import org.nimbustools.api.repr.vm.VMFile;

public interface RequestInfo {

    public String getRequestID();
    public Integer getInstanceCount();
    public RequestState getState();
    public Calendar getCreationTime();
    public String[] getVMIds();
    
    public VMFile[] getVMFiles();
    public String getSshKeyName();
    public Caller getCreator();
    public String getMdUserData();    
    public ResourceAllocation getResourceAllocation();
    public String getGroupID();    
    
}
