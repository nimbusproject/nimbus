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

package org.globus.workspace.client_core.subscribe_tools;

import org.apache.axis.message.addressing.EndpointReferenceType;
import org.globus.workspace.common.ExceptionDuringBackoutHandlerException;

/**
 * Asynchronous notifications based NotificationMaster
 *
 * @see SubscriptionMaster
 * @see SubscriptionMasterFactory
 */
public interface ListeningSubscriptionMaster extends SubscriptionMaster {

    /**
     * Returns true if underlying implementation is in listening mode.
     *
     * If multiple threads control this master, don't use this to decide
     * to listen or not (check then act issue).  Other methods are atomic,
     * use those or if you must use this method for that purpose, create
     * your own locking around all relevant object references.
     *
     * @return true if underlying implementation is in listening mode
     */
    public boolean isListening();


    /**
     * Stops listening if underlying implementation is in listening mode.
     * @throws NotificationImplementationException problem stopping
     */
    public void stopListening() throws NotificationImplementationException;
    

    /**
     * If consumer is not set up yet, this will trigger set-up.
     *
     * If set-up fails, this will return null.  To see the exposed failure
     * interface for setup, see listen().  Call that if you want exceptions,
     * this is just a convenience method to squelch.  If set-up fails, errors
     * will be logged via print system before returning null.
     *
     * @return EPR to send with subscribe requests, might be null
     * @see #listen(String, Integer)
     */
    public EndpointReferenceType getConsumerEPR();

    
    /**
     * This will NOT attempt to up consumer if there is no cached consumer
     * EPR to return.  It will just return null.
     *
     * @return EPR to send with subscribe requests, might be null
     * @see #getConsumerEPR() for thread safe "get EPR + set up if not already"
     */
    public EndpointReferenceType getConsumerEPRnoSetup();


    /**
     * In most cases you should just call getConsumerEPR.
     *
     * Causes underlying notification implementation to listen for
     * notifications and returns EPR to supply when subscribing.
     *
     * Callers take the EPR and launch subscribe requests with it.
     *
     * @return EPR of consumer to send with subscription requests, never null
     * @throws IllegalStateException if consumer is already listening
     * @throws NotificationImplementationException problem setting up consumer
     * @throws ExceptionDuringBackoutHandlerException severe, uncorrectable
     *         problem that leaves consumer is unknown, possibly corrupt state
     */
    public EndpointReferenceType listen()
                throws NotificationImplementationException,
                       IllegalStateException,
                       ExceptionDuringBackoutHandlerException;


    /**
     * All listen helper methods call this.  You can use the default consumer
     * EPR or override the ip/host or even the port which is a less common need
     * (be careful with either).
     *
     * Callers take the EPR and launch subscribe requests with it.  They could
     * override the address manually before sending, these mechanisms are here
     * as a convenience so other threads, methods, etc. in the same application
     * can call getConsumerEPR() without worrying about needing to manually
     * override the address.
     *
     * @param overrideIpAddress null or IP/hostname
     * @param overridePort null or port number
     * @return EPR of consumer to send with subscription requests, never null
     * @throws NotificationImplementationException problem setting up consumer
     * @throws IllegalStateException if consumer is already listening
     * @throws IllegalArgumentException problem with overrides
     * @throws ExceptionDuringBackoutHandlerException severe, uncorrectable
     *         problem that leaves consumer is unknown, possibly corrupt state
     */
    public EndpointReferenceType listen(String overrideIpAddress,
                                        Integer overridePort)

            throws NotificationImplementationException,
                   IllegalArgumentException,
                   IllegalStateException,
                   ExceptionDuringBackoutHandlerException;
    
}
