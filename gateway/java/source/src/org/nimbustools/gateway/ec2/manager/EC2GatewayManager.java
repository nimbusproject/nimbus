/*
 * Copyright 1999-2009 University of Chicago
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

package org.nimbustools.gateway.ec2.manager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nimbustools.api.repr.*;
import org.nimbustools.api.repr.vm.VM;
import org.nimbustools.api.services.rm.CoSchedulingException;
import org.nimbustools.api.services.rm.CreationException;
import org.nimbustools.api.services.rm.DestructionCallback;
import org.nimbustools.api.services.rm.DoesNotExistException;
import org.nimbustools.api.services.rm.ManageException;
import org.nimbustools.api.services.rm.Manager;
import org.nimbustools.api.services.rm.MetadataException;
import org.nimbustools.api.services.rm.OperationDisabledException;
import org.nimbustools.api.services.rm.ResourceRequestDeniedException;
import org.nimbustools.api.services.rm.SchedulingException;
import org.nimbustools.api.services.rm.StateChangeCallback;
import org.nimbustools.api.defaults.repr.DefaultCaller;
import org.nimbustools.api.defaults.repr.DefaultCreateResult;
import org.nimbustools.api._repr.vm._VM;
import org.nimbustools.api._repr._CreateResult;
import org.nimbustools.gateway.ec2.creds.EC2AccessID;
import org.nimbustools.gateway.ec2.creds.EC2AccessException;
import org.nimbustools.gateway.ec2.creds.EC2AccessManager;
import org.nimbustools.gateway.ec2.monitoring.*;
import org.nimbustools.gateway.ec2.monitoring.defaults.DefaultEC2Instance;
import org.hibernate.Transaction;
import org.hibernate.SessionFactory;
import org.hibernate.classic.Session;

import java.util.*;

import com.xerox.amazonws.ec2.Jec2;
import com.xerox.amazonws.ec2.EC2Exception;
import com.xerox.amazonws.ec2.ReservationDescription;
import com.xerox.amazonws.ec2.LaunchConfiguration;

public class EC2GatewayManager implements Manager {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(EC2GatewayManager.class.getName());

    protected static final VM[] EMPTY_VM_ARRAY = new VM[0];


    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    private final HashMap<String, Jec2> clientMap;

    private final EC2AccessManager ec2AccessManager;
    private final Translator translator;
    private final InstanceTracker instanceTracker;

    private final SessionFactory sessionFactory;

    public EC2GatewayManager(EC2AccessManager accessManager,
                           InstanceTracker tracker,
                           Translator translator,
                           SessionFactory sessionFactory) {
        if (accessManager == null) {
            throw new IllegalArgumentException("accessManager may not be null");
        }

        if (tracker == null) {
            throw new IllegalArgumentException("tracker may not be null");
        }

        if (translator == null) {
            throw new IllegalArgumentException("translator may not be null");
        }

        if (sessionFactory == null) {
            throw new IllegalArgumentException("sessionFactory may not be null");
        }

        this.ec2AccessManager = accessManager;
        this.instanceTracker = tracker;
        this.translator = translator;
        this.sessionFactory = sessionFactory;

        clientMap = new HashMap<String, Jec2>();
    }


    // -------------------------------------------------------------------------
    // implements NimbusModule
    // -------------------------------------------------------------------------

    public String report() {
        final StringBuffer buf = new StringBuffer("Class: ");
        buf.append(this.getClass().getName())
           .append("\n")
           .append("Workspace Service Manager");
        return buf.toString();
    }


    // -------------------------------------------------------------------------
    // implements Manager (several following sections)
    // -------------------------------------------------------------------------

    // -------------------------------------------------------------------------
    // LIFECYCLE
    // -------------------------------------------------------------------------

    /**
     * <p>This informs the implementation that its container (or whatever is
     * instantiating it) has just recovered from a failure or restart.</p>
     *
     * <p>If it is left up to normal initialization mechanisms, then there is
     * no chance for change/destruction listeners to be registered <i>before</i>
     * something happens.</p>
     *
     * @throws Exception problem, cannot continue
     */
    public void recover_initialize() throws Exception {

        logger.info("Checking persistence for EC2 instances that need to be recovered");

        final Session session = sessionFactory.openSession();
        final Transaction transaction = session.beginTransaction();
        List instances = session.createQuery("from DefaultEC2Instance").list();

        for (Object instObj : instances) {
            DefaultEC2Instance inst = (DefaultEC2Instance) instObj;

            logger.info("Trying to recover the current status of EC2 instance "+
                    inst.getId());

            if (tryRecoverInstance(inst)) {
                this.instanceTracker.addInstance(inst);
            } else {

                // instance could not be recovered from EC2.
                // Presumably it was terminated and AWS no longer knows anything
                // about it. So we delete.

                session.delete(inst);
            }
        }
        transaction.commit();
    }

    private boolean tryRecoverInstance(DefaultEC2Instance instance) {
        final String accessKey = instance.getAccessKey();
        if (accessKey == null) {
            logInstanceRecoveryProblem(instance,
                    "instance has no EC2 access key");
            return false;
        }

        final String callerIdentity = instance.getCallerIdentity();
        if (callerIdentity == null) {
            logInstanceRecoveryProblem(instance,
                    "instance has no caller identity");
            return false;
        }

        final EC2AccessID accessId;
        try {
            accessId = ec2AccessManager.getAccessIDByKey(accessKey);
        } catch (EC2AccessException e) {
            logInstanceRecoveryProblem(instance, "could not find EC2 access " +
                    "credentials with key value '"+accessKey+"'");
            return false;

        }

        final Jec2 client = getClient(accessId);

        final List<ReservationDescription> resDescList;
        try {
            resDescList = client.describeInstances(new String[]{instance.getId()});
        } catch (EC2Exception e) {
            //TODO this one maybe we just want to percolate up?
            logInstanceRecoveryProblem(instance,
                    "failed to query EC2 for instance");
            return false;
        }

        if (resDescList == null || resDescList.isEmpty()) {
            logInstanceRecoveryProblem(instance, "EC2 didn't know about instance");
            return false;
        }

        ReservationDescription resDesc = resDescList.get(0);
        final List<ReservationDescription.Instance> instanceList =
                resDesc.getInstances();

        if (instanceList == null || instanceList.isEmpty()) {
            logInstanceRecoveryProblem(instance, "EC2 res. had no instance (??)");
            return false;
        }

        final ReservationDescription.Instance instDesc = instanceList.get(0);

        final Caller caller = instance.getCaller();

        final _VM vm;
        try {
            vm = translator.translateVM(instDesc, caller, 0);
        } catch (CannotTranslateException e) {
            logInstanceRecoveryProblem(instance, "failed to translate EC2 " +
                    "instance description");
            //TODO kill here?
            return false;
        }

        instance.setVM(vm);

        return true;
    }

    private void logInstanceRecoveryProblem(EC2Instance instance, String msg) {
        logger.info("Problem while recovering EC2 instance '"+ instance.getId()
                +"' from persistence: "+msg);
    }


    public void shutdownImmediately() {
        logger.info("shutdownImmediately");
    }


    // -------------------------------------------------------------------------
    // EVENTS CAUSED BY USER OPERATIONS - MUTATIVE
    // -------------------------------------------------------------------------

    public CreateResult create(CreateRequest req, Caller caller)
           throws CoSchedulingException,
                  CreationException,
                  MetadataException,
                  ResourceRequestDeniedException,
                  SchedulingException {

        final EC2AccessID accessId;
        try {
            accessId = ec2AccessManager.getAccessID(caller);
        } catch (EC2AccessException e) {
            throw new ResourceRequestDeniedException("Caller \""+caller.getIdentity()+
                    "\" is not authorized to an EC2 credential");
        }

        final Jec2 client = this.getClient(accessId);

        logger.debug("Create request: "+req.toString());

        final LaunchConfiguration lc;
        try {
            lc = this.translator.translateCreateRequest(req, caller);
        } catch (CannotTranslateException e) {
            logger.error("Failed to translate creation request: "+
                e.getMessage(), e);

            throw new CreationException("Error translating request: "+
                e.getMessage(), e);
        }

        if (lc.getMinCount() != lc.getMaxCount()) {
            throw new CreationException("The gateway only supports a fixed #"+
                " of instances (minCount must equal maxCount)");
        }
        final int count = lc.getMaxCount();

        logger.debug("Attempting to start EC2 instances");

        final ReservationDescription reservation;
        try {
            reservation = client.runInstances(lc);
        } catch (EC2Exception e) {

            logger.error("got EC2 error during runInstances", e);

            throw new CreationException("Failed to create: "+e.getMessage(), e);
        }

        StringBuffer buf = new StringBuffer();
        buf.append("started new EC2 instances for user '");
        buf.append(caller.getIdentity());
        buf.append("': ");

        VM[] vms = new VM[count];

        final Session session = sessionFactory.openSession();

        for (int i=0; i<count; i++) {
            ReservationDescription.Instance instDesc =
                    reservation.getInstances().get(i);

            final _VM vm;

            //TODO i don't think I'm quite handling this correctly
            try {
                vm = translator.translateVM(instDesc, caller, i);
            } catch (CannotTranslateException e) {
                logger.error("Failed to build up the VM from the EC2 "+
                        "response. Big problem: there are new instances running that "+
                        "may not be properly tracked!");
                throw new CreationException("Failed to build response: "+
                        e.getMessage(), e);
            }
            vms[i] = vm;


            EC2Instance ec2Inst = new DefaultEC2Instance(
                    instDesc.getInstanceId(),
                    vm,
                    accessId.getKey());


            // add instance tracking to persistence
            final Transaction transaction = session.beginTransaction();
            session.persist(ec2Inst);
            transaction.commit();

            instanceTracker.addInstance(ec2Inst);

            buf.append(instDesc.getInstanceId());
            buf.append("  ");
        }

        logger.info(buf.toString());

        _CreateResult result = new DefaultCreateResult();
        result.setCoscheduledID(null);
        result.setGroupID(null);
        result.setVMs(vms);

        return result;
    }

    public void setDestructionTime(String id, int type, Calendar time)
            throws DoesNotExistException, ManageException {
        logger.info("setDestructionTime called -- but it doesn't do anything!");
    }

    public void trash(String id, int type, Caller caller)
            throws DoesNotExistException, ManageException {

        if (id == null) {
            throw new IllegalArgumentException("id may not be null");
        }
        if (caller == null) {
            throw new IllegalArgumentException("caller may not be null");
        }

        ensureTypeIsInstance(type);

        EC2Instance instance = getInstanceFromTracker(id);

        logger.info("trashing instance "+id);


        String callerId = caller.getIdentity();
        String instCallerId = instance.getCaller().getIdentity();

        if (callerId == null || instCallerId == null ||
                !callerId.equals(instCallerId)) {
            throw new ManageException("You do not own this instance");
        }

        Jec2 client = getInstanceClient(instance);
        if (client == null) {
            throw new ManageException("instance has no client (??)");
        }

        try {
            client.terminateInstances(new String[] {instance.getId()});
        } catch (EC2Exception e) {
            logger.error("Got EC2 exception while attempting to markTerminated " +
                    "EC2 instance "+instance.getId()+": "+e.getMessage(), e);
            throw new ManageException("Failed to markTerminated: "+e.getMessage(), e);
        }

    }


    public void start(String id, int type, Caller caller)
            throws DoesNotExistException, ManageException,
                   OperationDisabledException {

        throw new OperationDisabledException("The start operation is disabled on "+
            "the EC2 gateway manager");
    }

    public void shutdown(String id, int type,
                         ShutdownTasks tasks, Caller caller)
            throws DoesNotExistException,
                   ManageException,
                   OperationDisabledException {

        throw new OperationDisabledException("The shutdown operation is disabled on "+
            "the EC2 gateway manager");
    }

    public void shutdownSave(String id, int type,
                             ShutdownTasks tasks, Caller caller)
            throws DoesNotExistException,
                   ManageException,
                   OperationDisabledException {

        throw new OperationDisabledException("The shutdownSave operation is disabled on "+
            "the EC2 gateway manager");
    }

    public void pause(String id, int type,
                      ShutdownTasks tasks, Caller caller)
            throws DoesNotExistException,
                   ManageException,
                   OperationDisabledException {

        throw new OperationDisabledException("The pause operation is disabled on "+
            "the EC2 gateway manager");
    }

    public void serialize(String id, int type,
                          ShutdownTasks tasks, Caller caller)
            throws DoesNotExistException,
                   ManageException,
                   OperationDisabledException {

        throw new OperationDisabledException("The serialize operation is disabled on "+
            "the EC2 gateway manager");
    }

    public void reboot(String id, int type,
                       ShutdownTasks tasks, Caller caller)
            throws DoesNotExistException,
                   ManageException,
                   OperationDisabledException {

        if (id == null) {
            throw new IllegalArgumentException("id may not be null");
        }
        if (caller == null) {
            throw new IllegalArgumentException("caller may not be null");
        }

        ensureTypeIsInstance(type);

        EC2Instance instance = getInstanceFromTracker(id);

        logger.info("Rebooting instance "+id);

        String callerId = caller.getIdentity();
        String instCallerId = instance.getCaller().getIdentity();

        if (callerId == null || instCallerId == null ||
                !callerId.equals(instCallerId)) {
            throw new ManageException("You do not own this instance");
        }

        Jec2 client = getInstanceClient(instance);
        if (client == null) {
            throw new ManageException("instance has no client (??)");
        }

        try {
            client.rebootInstances(new String[] {instance.getId()});
        } catch (EC2Exception e) {
            logger.error("Got EC2 exception while attempting to reboot " +
                    "EC2 instance "+instance.getId()+": "+e.getMessage(), e);
            throw new ManageException("Failed to reboot: "+e.getMessage(), e);
        }

    }

    public void coscheduleDone(String id, Caller caller)

            throws DoesNotExistException,
                   ManageException,
                   CoSchedulingException {

        throw new CoSchedulingException("Coscheduling is not supported in the "+
            "EC2 gateway manager");
    }


    // -------------------------------------------------------------------------
    // INFORMATION QUERIES
    // -------------------------------------------------------------------------

    public boolean exists(String id, int type) {

        return type == Manager.INSTANCE &&
                id != null &&
                (instanceTracker.getInstanceByID(id) != null);
    }

    public Calendar getDestructionTime(String id, int type)
            throws DoesNotExistException, ManageException {
        logger.warn("getDestructionTime called but is not implemented!");
        return null;
    }

    public Caller[] getAuthorizedManagers(String id, int type)
            throws DoesNotExistException, ManageException {

        if (id == null) {
            throw new IllegalArgumentException("id may not be null");
        }

        ensureTypeIsInstance(type);

        EC2Instance instance = getInstanceFromTracker(id);

        Caller caller = instance.getCaller();
        if (caller == null) {
            // shouldn't be?
            return new Caller[] {};
        }

        // defensive copy
        DefaultCaller copy = new DefaultCaller();
        copy.setIdentity(caller.getIdentity());
        copy.setSubject(null); //uhh. don't need this??

        return new Caller[] {copy};
    }


    public VM getInstance(String id)
            throws DoesNotExistException, ManageException {

        if (id == null) {
            throw new IllegalArgumentException("id may not be null");
        }

        EC2Instance instance = getInstanceFromTracker(id);

        //maybe it would be a good idea to force a status update from EC2 here?
        return instance.getVM();

    }

    public VM[] getAll(String id, int type)
            throws DoesNotExistException, ManageException {

        ensureTypeIsInstance(type);

        return new VM[] {
                this.getInstance(id)
        };
    }

    public VM[] getAllByCaller(Caller caller)
            throws ManageException {

        final List<EC2Instance> instances =
                instanceTracker.getInstancesByCaller(caller);

        if (instances.isEmpty()) {
            return EMPTY_VM_ARRAY;
        }

        final VM[] vms = new VM[instances.size()];
        for (int i = 0; i < vms.length; i++) {
            vms[i] = instances.get(i).getVM();
        }
        return vms;
    }

    public VM[] getAllByIPAddress(String ip) throws ManageException {
        logger.info("getAllByIPAddress called but is not implemented!");
        return null;
    }

    public VM[] getGlobalAll() throws ManageException {
        logger.info("getGlobalAll called but is not implemented!");
        return null;
    }

    public Usage getCallerUsage(Caller caller) throws ManageException {
        logger.info("getCallerUsage called but is not implemented!");
        return null;
    }

    public Advertised getAdvertised() {
        logger.info("getAdvertised called but is not implemented!");
        return null;
    }

    public void registerStateChangeListener(String id,
                                            int type,
                                            StateChangeCallback listener)

            throws ManageException, DoesNotExistException {

        //TODO fill this in if/when it is needed
    }

    public void registerDestructionListener(String id,
                                            int type,
                                            DestructionCallback listener)

            throws ManageException, DoesNotExistException {

        if (id == null) {
            throw new IllegalArgumentException("id may not be null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("listener may not be null");
        }

        ensureTypeIsInstance(type);

        EC2Instance instance = getInstanceFromTracker(id);
        instance.registerDestructionListener(listener);
    }


    private void ensureTypeIsInstance(int type) throws ManageException {
        if (type != Manager.INSTANCE) {
            throw new ManageException("EC2 gateway manager only supports "+
                "instances");
        }
    }


    private EC2Instance getInstanceFromTracker(String id) throws DoesNotExistException {
        EC2Instance instance = instanceTracker.getInstanceByID(id);

        if (instance == null) {
            throw new DoesNotExistException("Gateway is not aware of" +
                    " this instance");
        }
        return instance;
    }

    synchronized Jec2 getClient(EC2AccessID id) {
        if (id == null) {
            throw new IllegalArgumentException("id may not be null");
        }

        Jec2 client = clientMap.get(id.getKey());
        if (client == null) {
            client = new Jec2(id.getKey(), id.getSecret());
            clientMap.put(id.getKey(), client);
        }
        return client;
    }

    private Jec2 getInstanceClient(EC2Instance instance) {
        if (instance == null) {
            throw new IllegalArgumentException("instance may not be null");
        }

        return clientMap.get(instance.getAccessKey());
    }

}
