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

package org.globus.workspace.client_core.subscribe_tools.internal;

import edu.emory.mathcs.backport.java.util.concurrent.ExecutorService;
import org.globus.workspace.common.print.Print;
import org.globus.workspace.client_core.print.PrCodes;
import org.globus.workspace.client_core.subscribe_tools.NotificationImplementationException;
import org.globus.workspace.client_core.subscribe_tools.ListeningSubscriptionMaster;
import org.globus.workspace.client_core.utils.WSUtils;
import org.globus.workspace.client_core.utils.EPRUtils;
import org.globus.workspace.common.ExceptionDuringBackoutHandlerException;
import org.globus.wsrf.NotificationConsumerManager;
import org.globus.wsrf.NotifyCallback;
import org.globus.wsrf.ResourceException;
import org.globus.wsrf.container.ContainerException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.axis.message.addressing.EndpointReferenceType;
import org.apache.axis.message.addressing.AttributedURI;
import org.apache.axis.types.URI;

import java.util.List;

public class ListeningSubscriptionMasterImpl extends SubscriptionMasterImpl
                                             implements ListeningSubscriptionMaster {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(ListeningSubscriptionMasterImpl.class.getName());


    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------
    
    private final NotificationConsumerManager consumer;

    private EndpointReferenceType consumerEPR;
    

    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    public ListeningSubscriptionMasterImpl(ExecutorService executorService,
                                           Print print) {
        super(executorService, print);
        this.consumer = NotificationConsumerManager.getInstance();
    }

    public ListeningSubscriptionMasterImpl(Print print) {
        super(null, print);
        this.consumer = NotificationConsumerManager.getInstance();
    }

    // -------------------------------------------------------------------------
    // implements ListeningSubscriptionMaster
    // -------------------------------------------------------------------------

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
    public EndpointReferenceType getConsumerEPR() {

        synchronized (this.accessLock) {

            if (this.consumerEPR != null) {
                return this.consumerEPR;
            }

            try {
                return this.listen();
            } catch (Throwable t) {
                if (this.pr.enabled()) {
                    final String err = "Issue getting consumer to " +
                            "listen: " + t.getMessage();
                    if (this.pr.useThis()) {
                        this.pr.errln(PrCodes.WSLISTEN__ERRORS, err);
                        t.printStackTrace(this.pr.getDebugProxy());
                    } else if (this.pr.useLogging()) {
                        if (logger.isDebugEnabled()) {
                            logger.error(err, t);
                        } else {
                            logger.error(err);
                        }
                    }
                }
                return null;
            }
        }
    }

    /**
     * This will NOT attempt to up consumer if there is no cached consumer
     * EPR to return.  It will just return null.
     *
     * @return EPR to send with subscribe requests, might be null
     * @see #getConsumerEPR() for thread safe "get EPR + set up if not already"
     */
    public EndpointReferenceType getConsumerEPRnoSetup() {

        synchronized (this.accessLock) {

            if (this.consumerEPR == null) {
                return null;
            } else {
                return this.consumerEPR;
            }
        }
    }

    public boolean isListening() {
        synchronized (this.accessLock) {
            return this.consumer.isListening();
        }
    }

    public void stopListening() throws NotificationImplementationException {
        
        synchronized (this.accessLock) {

            if (!this.consumer.isListening()) {
                return; // *** EARLY RETURN ***
            }

            try {
                this.consumer.stopListening();
            } catch (ContainerException e) {
                final String err = "Problem getting consumer to stop " +
                    "listening: " + e.getMessage();
                throw new NotificationImplementationException(err, e);
            }
        }
    }

    /**
     * In most cases you should just call getConsumerEPR.
     *
     * Causes underlying notification implementation to listen for
     * notifications and returns EPR to supply when subscribing.
     *
     * Callers take the EPR and launch subscribe requests with it.
     *
     * @return EPR of consumer to send with subscription requests
     * @throws IllegalStateException if consumer is already listening
     * @throws NotificationImplementationException problem setting up consumer
     * @throws org.globus.workspace.common.ExceptionDuringBackoutHandlerException severe, uncorrectable
     *         problem that leaves consumer is unknown, possibly corrupt state
     */
    public EndpointReferenceType listen()
            throws NotificationImplementationException,
                   IllegalStateException,
                   ExceptionDuringBackoutHandlerException {

        return this.listen(null, null);
    }

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
     * @return EPR of consumer to send with subscription requests
     * @throws NotificationImplementationException problem setting up consumer
     * @throws IllegalStateException if consumer is already listening
     * @throws IllegalArgumentException problem with overrides
     * @throws org.globus.workspace.common.ExceptionDuringBackoutHandlerException severe, uncorrectable
     *         problem that leaves consumer is unknown, possibly corrupt state
     */
    public EndpointReferenceType listen(String overrideIpAddress,
                                        Integer overridePort)

            throws NotificationImplementationException,
                   IllegalArgumentException,
                   IllegalStateException,
                   ExceptionDuringBackoutHandlerException {

        synchronized (this.accessLock) {
            try {
                return this.listenImpl(overrideIpAddress, overridePort);
            } catch (NotificationImplementationException e) {
                if (this.consumer.isListening()) {
                    try {
                        this.consumer.stopListening();
                    } catch (ContainerException e2) {
                        final String err = "Problem getting consumer-listen " +
                            "set up. Exception encountered after consumer " +
                            "started listening AND another exception happened " +
                            "during backout while trying to get consumer to " +
                            "stop listening.";
                        ExceptionDuringBackoutHandlerException e3 =
                                new ExceptionDuringBackoutHandlerException(err);
                        e3.setOriginalProblem(e);
                        e3.setBackoutProblem(e2);
                        throw e3;
                    }
                }
                throw e;
            }
        }
    }


    // -------------------------------------------------------------------------
    // private methods supporting previous section
    // -------------------------------------------------------------------------

    private EndpointReferenceType listenImpl(String overrideIpAddress,
                                             Integer overridePort)
            throws NotificationImplementationException {

        if (this.consumer.isListening()) {
            throw new IllegalStateException(
                        "Notification consumer is already listening");
        }

        final List[] topicPaths = new List[2];
        topicPaths[0] = WSUtils.CURRENT_STATE_TOPIC_PATH;
        topicPaths[1] = WSUtils.TERMINATION_TOPIC_PATH;

        final NotifyCallback[] listeners = new NotifyCallback[2];
        listeners[0] = new StateWSListener(this, this.pr);
        listeners[1] = new TerminationWSListener(this, this.pr);

        try {
            consumer.startListening();
        } catch (ContainerException e) {
            final String err = "Problem getting consumer to start " +
                    "listening: " + e.getMessage();
            throw new NotificationImplementationException(err, e);
        }

        final EndpointReferenceType defaultConsumerEPR;
        try {
            defaultConsumerEPR =
                    this.consumer.createNotificationConsumer(topicPaths,
                                                             listeners);
        } catch (ResourceException e) {
            final String err = "Problem getting consumer to start " +
                    "listening: " + e.getMessage();
            throw new NotificationImplementationException(err, e);
        }

        if (this.pr.enabled()) {
            this.logDefaultEPR(defaultConsumerEPR);
        }

        try {
            handleOverrides(defaultConsumerEPR,
                            overrideIpAddress,
                            overridePort);
        } catch (URI.MalformedURIException e) {
            throw new IllegalArgumentException(
                "Problem with IP/host and/or port overrides on the " +
                    "default notification consumer EPR: " + e.getMessage());
        }

        this.consumerEPR = defaultConsumerEPR;
        return this.consumerEPR;
    }

    private static void handleOverrides(EndpointReferenceType defaultEPR,
                                        String overrideIpAddress,
                                        Integer overridePort)
            throws URI.MalformedURIException {

        if (overrideIpAddress == null && overridePort == null) {

            return;  // *** EARLY RETURN ***
        }

        final AttributedURI defaultAddress = defaultEPR.getAddress();

        if (overrideIpAddress != null) {
            defaultAddress.setHost(overrideIpAddress);
        }

        if (overridePort != null) {
            defaultAddress.setPort(overridePort.intValue());
        }
    }

    private void logDefaultEPR(EndpointReferenceType defaultConsumerEPR) {

        try {
            if (this.pr.useThis()) {
                final String msg = "\nNotification consumer:\n    " +
                            defaultConsumerEPR.getAddress();
                this.pr.infoln(PrCodes.WSLISTEN__INFO, msg);
            } else if (this.pr.useLogging()) {
                final String msg = "Notification consumer: " +
                            defaultConsumerEPR.getAddress();
                logger.info(msg);
            }

            final String dbg = "\n\nDefault notification consumer EPR:" +
                    "\n----------------------------------\n" +
                    EPRUtils.eprToString(defaultConsumerEPR) +
                    "----------------------------------\n";
            if (this.pr.useThis()) {
                this.pr.dbg(dbg);
            } else if (this.pr.useLogging()) {
                logger.debug(dbg);
            }

        } catch (Exception e) {

            String err = "Could not examine notification default " +
                                                "notification consumer EPR ";

            err += "[[" + e.getClass().getName() + "]]: " + e.getMessage();

            if (this.pr.useThis()) {
                this.pr.errln(PrCodes.WSLISTEN__ERRORS, err);
                e.printStackTrace(this.pr.getDebugProxy());
            } else {
                if (logger.isDebugEnabled()) {
                    logger.error(err, e);
                } else {
                    logger.error(err);
                }
            }
        }
    }

}
