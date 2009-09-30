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
package org.nimbustools.gateway.accounting.manager;

import org.nimbustools.api.services.rm.*;
import org.nimbustools.api.repr.vm.VM;
import org.nimbustools.api.repr.vm.ResourceAllocation;
import org.nimbustools.api.repr.*;
import org.nimbustools.gateway.accounting.manager.defaults.DefaultInstance;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.classic.Session;

import java.util.*;
import java.util.concurrent.*;


/**
 * RM API implementation that wraps another Manager and provides account management
 */
public class AccountingManager implements Manager {
      // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(AccountingManager.class.getName());

    protected static final VM[] EMPTY_VM_ARRAY = new VM[0];


    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    private static final int DEFAULT_CHARGE_FREQUENCY = 30;
    private static final int DEFAULT_CHARGE_LOOKAHEAD = 45;

    private int chargeFrequencySeconds;
    private int chargeLookaheadSeconds;
    private boolean useScheduledCharging;

    protected final Manager manager;
    protected final Accountant accountant;

    protected final ConcurrentHashMap<String, Instance> instanceMap;

    private final ScheduledExecutorService schedExecutor;
    private final Runnable charger;
    private ScheduledFuture<?> scheduledFuture;

    private final SessionFactory sessionFactory;

    public AccountingManager(Manager backendManager,
                             Accountant accountant,
                             SessionFactory sessionFactory) {
        if (backendManager == null) {
            throw new IllegalArgumentException("backendManager cannot be null");
        }
        this.manager = backendManager;

        if (accountant == null) {
            throw new IllegalArgumentException("accountant may not be null");
        }
        this.accountant = accountant;

        if (sessionFactory == null) {
            throw new IllegalArgumentException("sessionFactory may not be null");
        }
        this.sessionFactory = sessionFactory;

        this.instanceMap = new ConcurrentHashMap<String, Instance>();
        schedExecutor = Executors.newScheduledThreadPool(1);
        this.charger = new Charger();

        // default values in case Spring isn't used
        this.chargeFrequencySeconds = DEFAULT_CHARGE_FREQUENCY;
        this.chargeLookaheadSeconds = DEFAULT_CHARGE_LOOKAHEAD;
        this.useScheduledCharging = true;

    }
    
    public int getChargeFrequencySeconds() {
        return chargeFrequencySeconds;
    }

    public void setChargeFrequencySeconds(int chargeFrequencySeconds) {
        this.chargeFrequencySeconds = chargeFrequencySeconds;
    }

    public int getChargeLookaheadSeconds() {
        return chargeLookaheadSeconds;
    }

    public void setChargeLookaheadSeconds(int chargeLookaheadSeconds) {
        this.chargeLookaheadSeconds = chargeLookaheadSeconds;
    }

    public boolean isUseScheduledCharging() {
        return useScheduledCharging;
    }

    public void setUseScheduledCharging(boolean useScheduledCharging) {
        this.useScheduledCharging = useScheduledCharging;
    }

    void validate() throws Exception {
        if (chargeFrequencySeconds < 1) {
            throw new Exception("Invalid: charge frequency must be at " +
                    "least 1 second");
        }
        if (chargeLookaheadSeconds < 1) {
            throw new Exception("Invalid: charge lookahead must be at " +
                    "least 1 second");
        }
        if (chargeLookaheadSeconds < chargeFrequencySeconds) {
            throw new Exception("Invalid: charge lookahead must be >= " +
                    "charge frequency");
        }
    }


    // -------------------------------------------------------------------------
    // implements NimbusModule
    // -------------------------------------------------------------------------

    public String report() {
        final StringBuffer buf = new StringBuffer("Class: ");
        buf.append(this.getClass().getName())
           .append("\n")
           .append("Workspace Accounting Manager");
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

        this.validate();

        // inner manager needs to recover first
        this.manager.recover_initialize();

        final Session session = sessionFactory.openSession();

        final Transaction transaction = session.beginTransaction();
        final List instances = session.createQuery(
                "from DefaultInstance where stopTime is null").list();

        Calendar now = Calendar.getInstance();

        ArrayList<Instance> runningInstances = new ArrayList<Instance>();

        for (Object instObj : instances) {
            final DefaultInstance instance = (DefaultInstance) instObj;

            logger.info("Querying inner manager for state of instance "+instance.getID());

            try {
                VM vm = this.manager.getInstance(instance.getID());

                instance.setVM(vm);

                runningInstances.add(instance);

            } catch (DoesNotExistException e) {

                // instance no longer exists in underlying cloud service. this is bad.
                // we have no clue when the instance actually terminated.

                // the best we can do here is mark the instance as terminated as of now.
                // this could be wildly inaccurate, but it is probably better to over-"charge"
                // someone than undercharge them. we can always grant them more fake money

                instance.setStopTime(now.getTime());
                int charge = instance.calculateCharge(now.getTime());

                logger.info("Instance "+instance.getID()+ " was running at shutdown but "+
                        "no longer exists in underlying service. Charging user '"+
                        instance.getCallerIdentity() +"' "+charge+" credits");

                if (charge > 0) {
                    accountant.chargeUserWithOverdraft(instance.getCaller(), charge);
                    accountant.persistUser(instance.getCaller(), session);
                    instance.addCharge(charge);
                }

                session.update(instance);
            }
        }

        transaction.commit();

        if (!runningInstances.isEmpty()) {
            this.addInstances(runningInstances);
        }
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
        SchedulingException,
        AuthorizationException {

        if (req == null) {
            throw new IllegalArgumentException("req may not be null");
        }
        if (caller == null) {
            throw new IllegalArgumentException("caller may not be null");
        }

        if (!accountant.isValidUser(caller)) {
            throw new ResourceRequestDeniedException("user \""+
                caller.getIdentity()+"\" is unknown");
        }

        if (req.isCoScheduleMember() || req.getCoScheduleID() != null) {
            throw new CoSchedulingException(AccountingManager.class.getName()+
                " does not support coscheduled requests");
        }


        final ResourceAllocation ra = req.getRequestedRA();

        int instanceRate = accountant.getHourlyRate(ra);
        int charge = instanceRate * ra.getNodeNumber();

        try {
            accountant.chargeUser(caller, charge);
        } catch (InsufficientCreditException e) {
            logger.error("User \""+caller.getIdentity()+"\" was short on funds", e);

            throw new ResourceRequestDeniedException("User has insufficent " +
                "credit available to fulfill the request", e);
        }

        // okay now user has been charged for the first hour of usage.
        // we need to try hard to back out the charge if the instances don't
        // actually start.


        // DGL: java exception hierarchy can rot in hell

        final CreateResult createResult;
        try {
            createResult = manager.create(req, caller);
        } catch (SchedulingException e) {
            refundUser(caller, charge);
            throw e;
        } catch (ResourceRequestDeniedException e) {
            refundUser(caller, charge);
            throw e;
        } catch (CreationException e) {
            refundUser(caller, charge);
            throw e;
        } catch (AuthorizationException e) {
            refundUser(caller, charge);
            throw e;
        } catch (MetadataException e) {
            refundUser(caller, charge);
            throw e;
        } catch (CoSchedulingException e) {
            refundUser(caller, charge);
            throw e;
        }

        final VM[] vms = createResult.getVMs();
        if (vms == null) {
            throw new CreationException("VMs in creation result is null (??)");
        }

        for (VM vm : vms) {

            String id = vm.getID();
            if (id == null) {
                logger.warn("VM has no ID! cannot track!");
                continue;
                // we can't kill the VM because it has no id

            }

            Instance inst = new DefaultInstance(vm, caller, instanceRate);
            // account was already charged for the first hour
            inst.setCharge(instanceRate);


            final Session session = sessionFactory.openSession();
            final Transaction transaction = session.beginTransaction();
            session.persist(inst);
            transaction.commit();

            addInstance(inst);
        }


        return createResult;
    }


    private void refundUser(Caller caller, int charge) {
        logger.info("Issuing refund of "+charge+" credits because instance failed to start");
        try {
            accountant.creditUser(caller, charge);
        } catch (InsufficientCreditException e1) {
            logger.error("Tried to refund user \""+caller.getIdentity()+
                "\" for their failed instances but the refund failed (?)", e1);
        }
    }

    public void setDestructionTime(String id, int type, Calendar time)
            throws DoesNotExistException, ManageException {

        manager.setDestructionTime(id, type, time);
    }

    public void trash(String id, int type, Caller caller)
            throws DoesNotExistException, ManageException {

        manager.trash(id,type,caller);

    }

    public void start(String id, int type, Caller caller)
            throws DoesNotExistException, ManageException,
                   OperationDisabledException {

        manager.start(id, type, caller);

    }

    public void shutdown(String id, int type,
                         ShutdownTasks tasks, Caller caller)
            throws DoesNotExistException,
                   ManageException,
                   OperationDisabledException {
        
        manager.shutdown(id, type, tasks, caller);

    }

    public void shutdownSave(String id, int type,
                             ShutdownTasks tasks, Caller caller)
            throws DoesNotExistException,
                   ManageException,
                   OperationDisabledException {

        manager.shutdownSave(id, type, tasks, caller);

    }

    public void pause(String id, int type,
                      ShutdownTasks tasks, Caller caller)
            throws DoesNotExistException,
                   ManageException,
                   OperationDisabledException {

        manager.pause(id, type, tasks, caller);

    }

    public void serialize(String id, int type,
                          ShutdownTasks tasks, Caller caller)
            throws DoesNotExistException,
                   ManageException,
                   OperationDisabledException {

        manager.serialize(id,type,tasks,caller);

    }

    public void reboot(String id, int type,
                       ShutdownTasks tasks, Caller caller)
            throws DoesNotExistException,
                   ManageException,
                   OperationDisabledException {

        manager.reboot(id, type, tasks, caller);
    }

    public void coscheduleDone(String id, Caller caller)

            throws DoesNotExistException,
                   ManageException,
                   CoSchedulingException {

        manager.coscheduleDone(id, caller);

    }


    // -------------------------------------------------------------------------
    // INFORMATION QUERIES
    // -------------------------------------------------------------------------

    public boolean exists(String id, int type) {
        return manager.exists(id, type);
    }

    public Calendar getDestructionTime(String id, int type)
            throws DoesNotExistException, ManageException {

        return manager.getDestructionTime(id, type);
    }

    public Caller[] getAuthorizedManagers(String id, int type)
            throws DoesNotExistException, ManageException {

        return manager.getAuthorizedManagers(id, type);
    }

    public VM getInstance(String id)
            throws DoesNotExistException, ManageException {

        return manager.getInstance(id);
    }

    public VM[] getAll(String id, int type)
            throws DoesNotExistException, ManageException {

        return manager.getAll(id, type);
    }

    public VM[] getAllByCaller(Caller caller)
            throws ManageException {

        return manager.getAllByCaller(caller);
    }

    public VM[] getAllByIPAddress(String ip) throws ManageException {

        return manager.getAllByIPAddress(ip);
    }

    public VM[] getGlobalAll() throws ManageException {

        return manager.getGlobalAll();
    }

    public Usage getCallerUsage(Caller caller) throws ManageException {

        return manager.getCallerUsage(caller);
    }

    public Advertised getAdvertised() {
        return manager.getAdvertised();
    }

    public void registerStateChangeListener(String id,
                                            int type,
                                            StateChangeCallback listener)
            throws ManageException, DoesNotExistException {

        manager.registerStateChangeListener(id, type, listener);
    }

    public void registerDestructionListener(String id,
                                            int type,
                                            DestructionCallback listener)
            throws ManageException, DoesNotExistException {

        manager.registerDestructionListener(id, type, listener);
    }


    /**
     * Examines running instances and charges or terminates as need be
     * @param now the current time
     * @param windowSeconds a window (in milliseconds) of how far in advance to charge for instances. Probably
     * the time until you expect to run the charge cycle again, (plus some fudge factor)
     */
    void doChargeCycle(Date now, int windowSeconds) {

        // note that this process assumes that something else is taking care of updating the states of
        // instances. In the initial case (EC2), we are cheating and relying on the fact that
        // the EC2 manager is secretly updating the same VM instances that we have reference to.
        // It would be better if instead we relied on the callback facility to detect state
        // changes.

        // Additionally, this whole practice of making charges at regular intervals is pretty
        // silly. We can fairly easily calculate the next charge point and then just sleep until
        // then.


        final Date chargeTime = new Date(now.getTime() + windowSeconds*1000);
        ArrayList<Instance> deadbeats = null; //late initialized
        ArrayList<Instance> terminated = null;

        logger.debug("Starting charge cycle - charge time : "+
            chargeTime.toString());

        final Session session = sessionFactory.openSession();
        final Transaction transaction = session.beginTransaction();

        // okay this is too stupidly simple and inefficient
        for (Instance inst : instanceMap.values()) {
            final String instId = inst.getID();

            boolean dirty = inst.updateFromVM();
            int charge = inst.calculateCharge(chargeTime);
            if (charge > 0) {
                try {

                    accountant.chargeUser(inst.getCaller(), charge);
                    inst.addCharge(charge);
                    dirty = true;

                } catch (InsufficientCreditException e) {
                    logger.info("Charge for instance \""+instId+"\"" +
                        " to user \""+inst.getCaller().getIdentity()+"\" failed " +
                        "due to insufficient credit available");

                    // try the charge calculation and
                    // attempt again, using the current time. but use 'overdraft' mode
                    // where an account can go negative. This is because we want to make
                    // sure we completely drain the accounts of deadbeats and also to
                    // correctly account for all used hours, even if that puts the balance
                    // in the red.

                    // note that an overdraft will rarely actually occur, only when
                    // the gateway was out of contact with the underlying service for a
                    // while (i.e. network outage) or perhaps if it took a long time for
                    // a VM to actually terminate after we killed it.

                    int newCharge = inst.calculateCharge(now);
                    if (newCharge > 0) {
                        dirty = true;
                        accountant.chargeUserWithOverdraft(inst.getCaller(), newCharge);
                        inst.addCharge(charge);
                    }

                    // add instance to list of instances which are to be killed
                    if (deadbeats == null) {
                        deadbeats = new ArrayList<Instance>();
                    }
                    deadbeats.add(inst);
                }
            }

            if (dirty) {
                accountant.persistUser(inst.getCaller(), session);
                session.update(inst);
            }

            if (inst.isTerminated()) {
                logger.debug("Removing terminated instance \""+ instId +"\"from manager");

                if (terminated == null) {
                    terminated = new ArrayList<Instance>();
                }
                terminated.add(inst);
            }

        }

        transaction.commit();

        if (deadbeats != null) {
            for (Instance inst : deadbeats) {
                final String id = inst.getID();

                if (inst.isTerminated()) {
                    continue;
                }

                logger.info("Attempting to mark deadbeat instance \""+
                    id + "\" as terminated");

                try {           

                    manager.trash(
                        id,
                        Manager.INSTANCE,
                        inst.getCaller());

                } catch (DoesNotExistException e) {
                    logger.info("Deadbeat instance \""+id+"\" is already " +
                        "terminated in inner Manager", e);

                } catch (ManageException e) {
                    logger.info("Deadbeat instance \""+id+"\" could not be " +
                        "terminated!", e);
                }
            }
        }


        if (terminated != null) {
            for (Instance inst : terminated) {

                logger.debug("Removing terminated instance "+inst.getID()+ " from charge cycle");

                removeInstance(inst);
            }
        }

        logger.debug("Ending charge cycle");
    }

    private void addInstances(Collection<Instance> instances) {
        synchronized (instanceMap) {
            for (Instance inst : instances) {
                if (instanceMap.put(inst.getID(), inst) != null) {
                    logger.warn("VM id \""+inst.getID()+"\" was already being tracked (??)");
                }
            }

            if (!useScheduledCharging) {
                return; // for unit tests
            }

            startCharger();
        }
    }

    private void addInstance(Instance inst) {

        synchronized (instanceMap) {
            if (instanceMap.put(inst.getID(), inst) != null) {
                logger.warn("VM id \""+inst.getID()+"\" was already being tracked (??)");
            }

            if (!useScheduledCharging) {
                return; // for unit tests
            }

            startCharger();
        }
    }


    private void removeInstance(Instance inst) {
        instanceMap.remove(inst.getID());

        synchronized (instanceMap) {
            if (instanceMap.isEmpty()) {
                stopCharger();
            }
        }
    }

    private void startCharger() {
        if (scheduledFuture == null || scheduledFuture.isCancelled()) {

            logger.debug("Starting instance charger");
            scheduledFuture = schedExecutor.scheduleAtFixedRate(this.charger,
                    chargeFrequencySeconds,
                    chargeFrequencySeconds,
                    TimeUnit.SECONDS);
        }
    }
    private void stopCharger() {
        if (scheduledFuture != null) {
            logger.debug("Stopping instance charger");

            scheduledFuture.cancel(false);
        }
    }

    private class Charger implements Runnable {

        public void run() {
            try {
                Date date = new Date(System.currentTimeMillis());
                doChargeCycle(date, chargeLookaheadSeconds);
            } catch (Throwable t) {
                logger.warn("got error during charging cycle: "
                        +t.getMessage(), t);
            }

        }
    }
}
