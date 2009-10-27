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

package org.nimbustools.gateway.ec2.monitoring.defaults;

import org.nimbustools.gateway.ec2.monitoring.InstanceTracker;
import org.nimbustools.gateway.ec2.monitoring.EC2Instance;
import org.nimbustools.gateway.ec2.creds.EC2AccessManager;
import org.nimbustools.gateway.ec2.creds.EC2AccessID;
import org.nimbustools.gateway.ec2.creds.EC2AccessException;
import org.nimbustools.gateway.ec2.manager.Translator;
import org.nimbustools.api.repr.Caller;
import org.nimbustools.api.repr.CannotTranslateException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.classic.Session;
import org.hibernate.Transaction;
import org.hibernate.SessionFactory;

import java.util.concurrent.*;
import java.util.*;

import com.xerox.amazonws.ec2.Jec2;
import com.xerox.amazonws.ec2.EC2Exception;
import com.xerox.amazonws.ec2.ReservationDescription;

public class DefaultInstanceTracker implements InstanceTracker {

    private static final Log logger =
               LogFactory.getLog(DefaultInstanceTracker.class.getName());

    private final EC2AccessManager accessManager;
    private final Translator translator;
    private final SessionFactory sessionFactory;

    private int ec2PollFrequencySeconds;
    private int instanceExpireSeconds;

    private final ConcurrentHashMap<String, EC2Instance> instanceMap;
    private final ScheduledExecutorService schedExecutor;
    private final Watcher watcher;
    private ScheduledFuture<?> scheduledFuture;

    public DefaultInstanceTracker(EC2AccessManager accessManager,
                                  Translator translator,
                                  SessionFactory sessionFactory) {

        if (accessManager == null) {
            throw new IllegalArgumentException("accessManager may not be null");
        }
        this.accessManager = accessManager;

        if (translator == null) {
            throw new IllegalArgumentException("translator may not be null");
        }
        this.translator = translator;
        
        if (sessionFactory == null) {
            throw new IllegalArgumentException("sessionFactory may not be null");
        }
        this.sessionFactory = sessionFactory;

        instanceMap = new ConcurrentHashMap<String, EC2Instance>();

        schedExecutor = Executors.newScheduledThreadPool(1);
        watcher = new Watcher();

    }

    void validate() throws Exception {
        if (ec2PollFrequencySeconds <= 0) {
            throw new Exception("Invalid: EC2 poll frequency is zero or negative");
        }
        if (instanceExpireSeconds <= 0) {
            throw new Exception("Invalid: Instance sweep time is zero or negative");
        }
    }

    public void addInstance(EC2Instance instance) {

        if (instance == null) {
            throw new IllegalArgumentException("instance may not be null");
        }

        synchronized (instanceMap) {
            instanceMap.put(instance.getId(), instance);
            startWatcher();
        }

    }

    public EC2Instance getInstanceByID(String id) {
        return instanceMap.get(id);
    }

    public List<EC2Instance> getInstancesByCaller(Caller caller) {
        if (caller == null) {
            throw new IllegalArgumentException("caller may not be null");
        }
        String callerId = caller.getIdentity();

        if (callerId == null) {
            throw new IllegalArgumentException("caller identity may not be null");
        }

        ArrayList<EC2Instance> list = null;

        for (EC2Instance inst : this.instanceMap.values()) {
            final Caller instCaller = inst.getCaller();
            if (callerId.equals(instCaller.getIdentity())) {

                if (list == null)  {
                    list = new ArrayList<EC2Instance>();
                }
                list.add(inst);
            }
        }

        if (list == null) {
            return Collections.emptyList();
        } else {
            return list;
        }
    }

    private void startWatcher() {
        if (scheduledFuture == null || scheduledFuture.isCancelled()) {
            logger.debug("Starting EC2 InstanceTracker watcher");
            scheduledFuture = schedExecutor.scheduleAtFixedRate(watcher,
                    ec2PollFrequencySeconds,
                    ec2PollFrequencySeconds,
                    TimeUnit.SECONDS);
        }

    }

    private void stopWatcher() {
        if (scheduledFuture != null) {
            logger.debug("Stopping EC2 InstanceTracker watcher");
            scheduledFuture.cancel(false);
        }

    }

    private void doStatusCycle() {
        int count = instanceMap.size();

        if (count == 0) {
            logger.warn("EC2 Watcher called when 0 instances are running.");
            return;
        }

        logger.debug("Checking for status updates for "+ count +" EC2 instances");

        // get instances that we do not know to be terminated yet
        ArrayList<EC2Instance> instances = getActiveInstances();

        if (instances.isEmpty()) {
            logger.info("No instances are running, not polling EC2");
            return;
        }

        // break it down by access id. would be better
        // to do this once, as instances are added/removed
        Map<String, List<String>> accessMap =
                new HashMap<String, List<String>>();

        for (EC2Instance instance : instances) {
            final String key = instance.getAccessKey();
            List<String> list =
                    accessMap.get(key);
            if (list == null) {
                list = new ArrayList<String>();
                accessMap.put(key,list);
            }
            list.add(instance.getId());
        }

        final List<ReservationDescription> descriptionList =
                new ArrayList<ReservationDescription>();

        for (Map.Entry<String,List<String>> entry : accessMap.entrySet()) {
            final Jec2 client;
            try {
                final String accessKey = entry.getKey();
                final EC2AccessID accessID = accessManager.getAccessIDByKey(accessKey);
                client = new Jec2(accessID.getKey(), accessID.getSecret());
            } catch (EC2AccessException e) {
                logger.error("Failed to get EC2 Access ID: "+e.getMessage(),e);
                continue;
            }

            final List<String> idList = entry.getValue();
            final String[] instanceIds = idList.toArray(new String[idList.size()]);

            try {
                descriptionList.addAll(client.describeInstances(instanceIds));
            } catch (EC2Exception e) {
                logger.error("Got EC2 error during instance describe: "+e.getMessage(), e);
            }
        }

        // get the current time to be used as time-of-death for any terminated instances
        final Calendar now = Calendar.getInstance();

        // whatever was in our request but is not in the response has been terminated
        // this would probably only happen if there was a long gap between polls

        final Map<String, ReservationDescription.Instance> descriptionMap =
                getDescriptionMap(descriptionList);

        for (EC2Instance inst : instances) {

            final ReservationDescription.Instance instDesc =
                    descriptionMap.get(inst.getId());

            if (instDesc == null) {
                logger.info("Instance "+inst.getId()+" was not in EC2 " +
                        "describe response, marking as terminated");
                inst.markTerminated(now);

                continue; // next please
            }

            try {
                this.translator.updateVM(inst.getVM(), instDesc);
            } catch (CannotTranslateException e) {
                logger.error("Failed to translate instance description into VM", e);
            }

            Calendar launchTime = instDesc.getLaunchTime();
            // if this instance has started since our last check
            if (!inst.isLaunched() && launchTime != null) {

                Date date = new Date(launchTime.getTimeInMillis());

                logger.info("Instance "+inst.getId()+" has launched on " +
                        "EC2 at "+ date.toString());


                inst.markLaunched(launchTime);
            }

            if (instDesc.isTerminated()) {
                logger.info("Instance "+inst.getId()+" is marked as " +
                        "terminated in EC2, marking as terminated locally");
                inst.markTerminated(now);
            }
        }
    }


    private Map<String, ReservationDescription.Instance> getDescriptionMap(
            List<ReservationDescription> descriptionList) {

        HashMap<String, ReservationDescription.Instance> map =
                new HashMap<String, ReservationDescription.Instance>();

        for (ReservationDescription desc : descriptionList) {
            for (ReservationDescription.Instance instDesc : desc.getInstances()) {
                map.put(instDesc.getInstanceId(), instDesc);

            }
        }
        return map;

    }

    private ArrayList<EC2Instance> getActiveInstances() {
        ArrayList<EC2Instance> list = new ArrayList<EC2Instance>(instanceMap.size());
        ArrayList<EC2Instance> sweep = null;
        for (EC2Instance inst : instanceMap.values()) {

            if (inst.isTerminated()) {
                // sweep old terminated instances out of tracker
                Date termTime = inst.getTerminationTime();

                long curTime = System.currentTimeMillis();

                if (termTime == null || curTime - termTime.getTime() >=
                        instanceExpireSeconds*1000) {
                    if (sweep == null) {
                        sweep = new ArrayList<EC2Instance>();
                    }
                    sweep.add(inst);
                }

            } else {
                list.add(inst);
            }
        }

        if (sweep != null) {
            synchronized (instanceMap) {
                for (EC2Instance inst : sweep) {
                    logger.debug("Sweeping expired instance "+inst.getId()+" from tracker");
                    instanceMap.remove(inst.getId());
                }
                if (instanceMap.isEmpty()) {
                    stopWatcher();
                }
            }


            //remove from persistence layer as well
            final Session session = sessionFactory.openSession();
            final Transaction transaction = session.beginTransaction();
            for (EC2Instance inst : sweep) {
               session.delete(inst);
            }
            transaction.commit();

        }

        logger.debug("Found "+list.size()+" active EC2 instances");

        return list;
    }


    public int getEc2PollFrequencySeconds() {
        return ec2PollFrequencySeconds;
    }

    public void setEc2PollFrequencySeconds(int ec2PollFrequencySeconds) {
        this.ec2PollFrequencySeconds = ec2PollFrequencySeconds;
    }

    public int getInstanceExpireSeconds() {
        return instanceExpireSeconds;
    }

    public void setInstanceExpireSeconds(int instanceExpireSeconds) {
        this.instanceExpireSeconds = instanceExpireSeconds;
    }

    private class Watcher implements Runnable {
        public void run() {
            try {
                doStatusCycle();
            } catch (Throwable t) {
                logger.warn("EC2 Watcher got error during status check cycle: "
                        +t.getMessage(), t);
            }
        }
    }
}
