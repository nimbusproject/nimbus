/*
 * Copyright 1999-2008 University of Chicago
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

package org.nimbustools.messaging.gt4_0_elastic.v2008_05_05;

import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.*;

import java.rmi.RemoteException;

public interface ServiceRM {

    // -------------------------------------------------------------------------
    // RM RELATED
    // -------------------------------------------------------------------------

    public RunInstancesResponseType runInstances(
                        RunInstancesType runInstancesRequestMsg)
            throws RemoteException;

    public TerminateInstancesResponseType terminateInstances(
                        TerminateInstancesType terminateInstancesRequestMsg)
            throws RemoteException;

    public RebootInstancesResponseType rebootInstances(
                        RebootInstancesType rebootInstancesRequestMsg)
            throws RemoteException;

    public DescribeInstancesResponseType describeInstances(
                        DescribeInstancesType describeInstancesRequestMsg)
            throws RemoteException;

    public MonitorInstancesResponseType monitorInstances(
                        MonitorInstancesType monitorInstancesRequestMsg)
            throws RemoteException;

    public MonitorInstancesResponseType unmonitorInstances(
                        MonitorInstancesType unmonitorInstancesRequestMsg)
            throws RemoteException;
    
    // -------------------------------------------------------------------------
    // SI OPERATIONS
    // -------------------------------------------------------------------------  
    
    public RequestSpotInstancesResponseType requestSpotInstances(
                        RequestSpotInstancesType requestSpotInstancesMsg)
            throws RemoteException;
    
    public CancelSpotInstanceRequestsResponseType cancelSpotInstanceRequests(
                        CancelSpotInstanceRequestsType cancelSpotInstancesMsg)
            throws RemoteException;
    
    public DescribeSpotInstanceRequestsResponseType describeSpotInstanceRequests(
                        DescribeSpotInstanceRequestsType describeSpotInstancesMsg)
            throws RemoteException;
    
    public DescribeSpotPriceHistoryResponseType describeSpotPriceHistory(
                        DescribeSpotPriceHistoryType describeSpotPriceHistoryType)
            throws RemoteException;
}
