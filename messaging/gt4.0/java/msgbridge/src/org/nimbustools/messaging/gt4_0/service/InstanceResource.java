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

package org.nimbustools.messaging.gt4_0.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.wsrf.InvalidResourceKeyException;
import org.globus.wsrf.NoSuchResourceException;
import org.globus.wsrf.ResourceException;
import org.globus.wsrf.ResourceKey;
import org.globus.wsrf.ResourceProperties;
import org.globus.wsrf.ResourceProperty;
import org.globus.wsrf.ResourcePropertySet;
import org.globus.wsrf.TopicList;
import org.globus.wsrf.TopicListAccessor;
import org.globus.wsrf.WSRFConstants;
import org.globus.wsrf.config.ConfigException;
import org.globus.wsrf.impl.ResourcePropertyTopic;
import org.globus.wsrf.impl.SimpleResourcePropertySet;
import org.globus.wsrf.impl.SimpleTopicList;
import org.globus.wsrf.impl.SimpleTopic;
import org.nimbustools.api.repr.Caller;
import org.nimbustools.api.repr.ShutdownTasks;
import org.nimbustools.api.repr.vm.VM;
import org.nimbustools.api.repr.vm.State;
import org.nimbustools.api.services.rm.DoesNotExistException;
import org.nimbustools.api.services.rm.ManageException;
import org.nimbustools.api.services.rm.Manager;
import org.nimbustools.api.services.rm.OperationDisabledException;
import org.nimbustools.api.services.rm.DestructionCallback;
import org.nimbustools.api.services.rm.StateChangeCallback;
import org.nimbustools.messaging.gt4_0.common.Constants_GT4_0;
import org.nimbustools.messaging.gt4_0.GeneralPurposeResource;
import org.nimbustools.messaging.gt4_0.RP_CurrentTime;
import org.nimbustools.messaging.gt4_0.RP_TerminationTime;
import org.nimbustools.messaging.gt4_0.generated.metadata.logistics.Logistics;
import org.nimbustools.messaging.gt4_0.generated.negotiable.PostShutdown_Type;
import org.nimbustools.messaging.gt4_0.generated.negotiable.ResourceAllocation_Type;
import org.nimbustools.messaging.gt4_0.generated.types.CurrentState;
import org.nimbustools.messaging.gt4_0.generated.types.Schedule_Type;
import org.nimbustools.messaging.gt4_0.generated.types.ShutdownEnumeration;

import java.net.URISyntaxException;

import commonj.timers.TimerManager;

public class InstanceResource extends GeneralPurposeResource
                              implements ResourceProperties,
                                         TopicListAccessor,
                                         DestructionCallback,
                                         StateChangeCallback {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    protected static final Log logger =
            LogFactory.getLog(InstanceResource.class.getName());
    
    
    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected final InstanceTranslate translate;
    protected final InstanceHome ihome;
    protected final TimerManager timer;
    protected final Counter pendingStateChangeNotifs;
    
    protected TopicList topics;
    protected ResourcePropertySet props;
    protected final ResourcePropertyTopic statusTopic;

    
    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------

    /**
     * @param idString instance key
     * @param managerImpl Manager impl
     * @param securityDescPath path to resource-security descriptor template
     * @param translateImpl move between GT/Axis1 and VWS
     * @param instanceHome for destroy callback
     * @throws ConfigException problem with secDescPath
     * @throws DoesNotExistException gone (race with a destroyer)
     * @throws ManageException general problem
     */
    public InstanceResource(String idString,
                            Manager managerImpl,
                            String securityDescPath,
                            InstanceTranslate translateImpl,
                            InstanceHome instanceHome,
                            TimerManager timerManagerImpl)
            throws ConfigException, ManageException, DoesNotExistException {
        
        super(idString, Manager.INSTANCE, managerImpl,
              securityDescPath, translateImpl);
        
        if (translateImpl == null) {
            throw new IllegalArgumentException("translate may not be null");
        }
        this.translate = translateImpl;

        if (instanceHome == null) {
            throw new IllegalArgumentException("instanceHome may not be null");
        }
        this.ihome = instanceHome;

        if (timerManagerImpl == null) {
            throw new IllegalArgumentException("timerManagerImpl may not be null");
        }
        this.timer = timerManagerImpl;
        this.pendingStateChangeNotifs = new Counter(0);

        try {
            this.props = new SimpleResourcePropertySet(Constants_GT4_0.RP_SET);

            this.props.add(new RP_Logistics(this));
            this.props.add(new RP_Schedule(this));
            this.props.add(new RP_ResourceAllocation(this));
            this.props.add(new RP_CurrentTime(this));
            this.props.add(new RP_TerminationTime(this));

            final ResourceProperty stateRP = new RP_CurrentState(this);
            this.props.add(stateRP);
            this.statusTopic = new ResourcePropertyTopic(stateRP);
            this.statusTopic.setAutoNotify(false);

            this.topics = new SimpleTopicList(this);
            this.topics.addTopic(this.statusTopic);
            this.topics.addTopic(
                    new SimpleTopic(WSRFConstants.TERMINATION_TOPIC));

        } catch (Exception e) {
            throw new ManageException(e.getMessage(), e);
        }

        final CurrentState state = this.getCurrentState();
        if (state == null) {

            throw new DoesNotExistException("The instance is gone (highly " +
                    "unlikely destruction race)");

            // It does not matter if this happens, the layer below is
            // authoritative.

            // We're still in the constructor of the proxy object -- a destroy
            // race here would mean a cache eviction happened on a previous
            // proxy object and right now there is an unlikely confluence of
            // destruction time + remote access of the instance (since there
            // is an "exists" check right before this proxy object is created).
        }

        this.statusTopic.set(0, state);
    }


    // -------------------------------------------------------------------------
    // implements DestructionCallback
    // -------------------------------------------------------------------------

    public void destroyed() {
        this.ihome.destroyedNotification(this);
    }

    
    // -------------------------------------------------------------------------
    // implements StateChangeCallback
    // -------------------------------------------------------------------------

    public void newState(State state) {
        // ignore argument
        this.statusTopic.set(0, this.getCurrentState());

        // delay this -- mainly just get it out of this thread but it has
        // the added benefit of sending just one notification if the state
        // is changing very quickly, as it can in some create situations

        try {
            this.pendingStateChangeNotifs.addToCount(1);
            final DelayedNotification dn =
                    new DelayedNotification(this.statusTopic,
                                            this.pendingStateChangeNotifs);
            this.timer.schedule(dn, 50);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            // do it now, then:
            try {
                this.statusTopic.notify(null);
            } catch (Exception e1) {
                logger.error("Giving up on notification, compound failure. " +
                        "Second error: " +
                        e1.getMessage(), e);
            }
        }
    }

    
    // -------------------------------------------------------------------------
    // REMOTE CLIENT INVOCATIONS - MUTATIVE
    // -------------------------------------------------------------------------

    /**
     * Only invoked from WS client via WorkspaceService
     * @param callerDN caller name
     * @throws OperationDisabledException operation is disabled
     * @throws ManageException general
     * @throws DoesNotExistException gone (race with a destroyer)
     */
    public void start(String callerDN) throws OperationDisabledException,
                                              ManageException,
                                              DoesNotExistException {

        final Caller caller = this.translate.getCaller(callerDN);
        this.manager.start(this.id, Manager.INSTANCE, caller);
    }

    /**
     * Only invoked from WS client via WorkspaceService
     *
     * @param shutdownEnum  Type of shutdown event
     * @param postTask      possible request adjustment
     * @param appendID      if there is a new unprop target, append ID to URL?
     *                      (used to differentiate group files)
     * @param callerDN      caller name
     * @throws OperationDisabledException operation is disabled
     * @throws ManageException general
     * @throws DoesNotExistException gone (race with a destroyer)
     * @throws URISyntaxException problem with request
     */
    public void shutdown(ShutdownEnumeration shutdownEnum,
                         PostShutdown_Type postTask,
                         boolean appendID,
                         String callerDN)
            throws OperationDisabledException,
                   ManageException,
                   DoesNotExistException,
                   URISyntaxException {

        final Caller caller = this.translate.getCaller(callerDN);

        final ShutdownTasks tasks =
                this.translate.getShutdownTasks(postTask, appendID);

        if (ShutdownEnumeration.Normal.equals(shutdownEnum)) {
            this.manager.shutdown(this.id, Manager.INSTANCE, tasks, caller);
        } else if (ShutdownEnumeration.Pause.equals(shutdownEnum)) {
            this.manager.pause(this.id, Manager.INSTANCE, tasks, caller);
        } else if (ShutdownEnumeration.ReadyForTransport.equals(shutdownEnum)) {
            this.manager.shutdownSave(this.id, Manager.INSTANCE, tasks, caller);
        } else if (ShutdownEnumeration.Reboot.equals(shutdownEnum)) {
            this.manager.reboot(this.id, Manager.INSTANCE, tasks, caller);
        } else if (ShutdownEnumeration.Serialize.equals(shutdownEnum)) {
            this.manager.serialize(this.id, Manager.INSTANCE, tasks, caller);
        }
    }


    // -------------------------------------------------------------------------
    // INFORMATION QUERIES
    // -------------------------------------------------------------------------

    public CurrentState getCurrentState() {
        try {
            final VM vm = this.manager.getInstance(this.id);
            return this.translate.getCurrentState(vm);
        } catch (DoesNotExistException e) {
            logger.warn("no current-state for id '" + this.id + "': race on " +
                    "destruction between proxy object and the real system");
            return null;
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.error(e.getMessage(), e);
            } else {
                logger.error(e.getMessage());
            }
            return null;
        }
    }

    public Logistics getLogistics() {
        try {
            final VM vm = this.manager.getInstance(this.id);
            return this.translate.getLogistics(vm);
        } catch (DoesNotExistException e) {
            logger.warn("no current-state for id '" + this.id + "': race on " +
                    "destruction between proxy object and the real system");
            return null;
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.error(e.getMessage(), e);
            } else {
                logger.error(e.getMessage());
            }
            return null;
        }
    }

    public Schedule_Type getSchedule() {
        try {
            final VM vm = this.manager.getInstance(this.id);
            return this.translate.getSchedule_Type(vm);
        } catch (DoesNotExistException e) {
            logger.warn("no current-state for id '" + this.id + "': race on " +
                    "destruction between proxy object and the real system");
            return null;
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.error(e.getMessage(), e);
            } else {
                logger.error(e.getMessage());
            }
            return null;
        }
    }

    public ResourceAllocation_Type getResourceAllocation() {
        try {
            final VM vm = this.manager.getInstance(this.id);
            return this.translate.getResourceAllocation_Type(vm);
        } catch (DoesNotExistException e) {
            logger.warn("no current-state for id '" + this.id + "': race on " +
                    "destruction between proxy object and the real system");
            return null;
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.error(e.getMessage(), e);
            } else {
                logger.error(e.getMessage());
            }
            return null;
        }
    }
    

    // -------------------------------------------------------------------------
    // implements ResourceProperties
    // -------------------------------------------------------------------------

    public ResourcePropertySet getResourcePropertySet() {
        return this.props;
    }

    
    // -------------------------------------------------------------------------
    // implements TopicListAccessor
    // -------------------------------------------------------------------------

    public TopicList getTopicList() {
        return this.topics;
    }

    
    // -------------------------------------------------------------------------
    // implements PersistenceCallback
    // -------------------------------------------------------------------------

    public void load(ResourceKey key) throws ResourceException,
                                             NoSuchResourceException,
                                             InvalidResourceKeyException {
        super.load(key);
        // TODO: get persistent subscriptions
    }

    public void store() throws ResourceException {

        // TODO: store persistent subscriptions
    }
}
