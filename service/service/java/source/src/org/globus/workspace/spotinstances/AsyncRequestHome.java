/*
 * Copyright 1999-2010 University of Chicago
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.globus.workspace.spotinstances;

import java.util.Calendar;
import java.util.List;

import org.globus.workspace.persistence.WorkspaceDatabaseException;
import org.nimbustools.api.repr.Caller;
import org.nimbustools.api.repr.SpotPriceEntry;
import org.nimbustools.api.services.rm.DoesNotExistException;


/**
 * Frontend interface that provides 
 * RETRIEVE and CANCEL operations to 
 * Spot Instance Requests
 */
public interface AsyncRequestHome {

    /**
     * Cancels a Spot Instance request
     * @param reqID the id of the request to be canceled
     * @return the canceled request
     * @throws DoesNotExistException in case the id argument does not map
     *                               to any spot instance request
     */
    public AsyncRequest cancelRequest(String reqID) throws DoesNotExistException;
    
    /**
     * Retrieves a Spot Instance request and its related information
     * @param id the id of the request to be retrieved
     * @return the wanted request
     * @throws DoesNotExistException in case the id argument does not map
     *                               to any spot instance request
     */
    public AsyncRequest getRequest(String id) throws DoesNotExistException;
    
    /**
     * Retrieves all Spot Instance requests from a caller
     * @param caller the owner of the Spot Instances' requests
     * @return an array of spot instance requests from this caller
     */
    public AsyncRequest[] getRequests(Caller caller, boolean spot);
    
    /**
     * Retrieves current spot price
     * @return current spot price
     */
    public Double getSpotPrice();

    public List<SpotPriceEntry> getSpotPriceHistory(Calendar startDate, Calendar endDate) 
            throws WorkspaceDatabaseException;

}
