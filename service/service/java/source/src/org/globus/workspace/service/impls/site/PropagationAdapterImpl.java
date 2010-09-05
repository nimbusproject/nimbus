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

package org.globus.workspace.service.impls.site;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.workspace.*;
import org.globus.workspace.persistence.WorkspaceDatabaseException;
import org.globus.workspace.persistence.PersistenceAdapter;
import org.globus.workspace.service.binding.vm.VirtualMachine;
import org.globus.workspace.service.binding.GlobalPolicies;
import org.globus.workspace.service.WorkspaceHome;
import org.globus.workspace.service.impls.async.ResourceMessage;
import org.globus.workspace.xen.XenUtil;

import java.util.ArrayList;

import commonj.timers.TimerManager;
import org.springframework.core.io.Resource;

public class PropagationAdapterImpl implements PropagationAdapter,
                                               CounterCallback,
                                               NotificationPollCallback {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------
    
    private static final Log logger =
        LogFactory.getLog(PropagationAdapterImpl.class.getName());


    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected final PersistenceAdapter persistence;
    protected final WorkspaceHome whome;
    protected final ResourceMessage resourceMessage;
    protected final TimerManager timerManager;
    protected final GlobalPolicies globals;
    protected final Lager lager;

    private String notify;

    // (conditionally) created in init:
    private Counter counter;
    private CursorPersistence cp = null;
    NotificationPoll watcher;

    // set via config mechanism (spring):
    private String notificationInfo;
    private Resource pollScript;
    private boolean enabled;
    private long watcherDelay = 2000;

    private String extraArgs = null;

    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------

    public PropagationAdapterImpl(PersistenceAdapter persistenceAdapter,
                                  WorkspaceHome workspaceHome,
                                  ResourceMessage resourceMessageImpl,
                                  TimerManager timerManagerImpl,
                                  GlobalPolicies globalPolicies,
                                  Lager lagerImpl) {

        if (persistenceAdapter == null) {
            throw new IllegalArgumentException("persistenceAdapter may not be null");
        }
        this.persistence = persistenceAdapter;

        if (workspaceHome == null) {
            throw new IllegalArgumentException("workspaceHome may not be null");
        }
        this.whome = workspaceHome;

        if (resourceMessageImpl == null) {
            throw new IllegalArgumentException("resourceMessageImpl may not be null");
        }
        this.resourceMessage = resourceMessageImpl;

        if (timerManagerImpl == null) {
            throw new IllegalArgumentException("timerManagerImpl may not be null");
        }
        this.timerManager = timerManagerImpl;
        
        if (globalPolicies == null) {
            throw new IllegalArgumentException("globalPolicies may not be null");
        }
        this.globals = globalPolicies;
        
        if (lagerImpl == null) {
            throw new IllegalArgumentException("lagerImpl may not be null");
        }
        this.lager = lagerImpl;
    }

    // -------------------------------------------------------------------------
    // SET CONFIGS
    // -------------------------------------------------------------------------


    public void setExtraArgs(String extraArgs) {
        this.extraArgs = extraArgs;
    }

    public String getExtraArgs() {
        return this.extraArgs;
    }

    public void setNotificationInfo(String notifInfo) {
            this.notificationInfo = notifInfo;
        }

    public void setPollScript(Resource pollScript) {
        this.pollScript = pollScript;
    }

    public void setEnabled(String isEnabled) {
        if (isEnabled.trim().equalsIgnoreCase("true")) {
            this.enabled = true;
        }
    }

    // optional in config, default is 2 seconds
    public void setWatcherDelay(long delay) {
        this.watcherDelay = delay;
    }

    
    // -------------------------------------------------------------------------
    // IoC INIT METHOD
    // -------------------------------------------------------------------------

    public synchronized void validate() throws Exception {

        logger.debug("validating/initializing");

        if (!this.enabled) {
            // this will result in propagation related command classes
            // being set to null, so it ends up that this class is cutting
            // itself off from ever being reached again
            this.globals.setPropagateEnabled(false);
            this.globals.setUnpropagateEnabled(false);

            if (this.lager.eventLog) {
                logger.info(Lager.ev(-1) + "Propagation functionality " +
                        "disabled");
            } else if (this.lager.traceLog) {
                logger.trace("Propagation functionality disabled");
            }

            return;
        }

        if (this.watcherDelay < 1) {
            throw new Exception("sweeper delay is less than 1 ms, invalid");
        }

        if (this.watcherDelay < 50) {
            logger.warn("you should probably not set sweeper delay to less " +
                    "than 50ms");
        }

        final boolean fake = this.globals.isFake();

        final String pollScriptPath;
        if (this.pollScript == null) {
            // If fake mode is on, allow pollScript to be absent. We rely
            // on knowing fake is set before propagation is initialized
            if (!fake) {
                throw new Exception("pollScript setting is missing from" +
                        " propagation configuration");
            }
            pollScriptPath = "/bin/true";
        } else {
            pollScriptPath = this.pollScript.getFile().getAbsolutePath();
        }

        if (this.notificationInfo == null) {
            throw new Exception("propagation scheme given, but " +
                            "notification information is not set");
        }

        // todo: could do with parsing notificationInfo for validity.
        // for now, we just pass it through to the backend


        final String[] cmd = {pollScriptPath};
        try {
            WorkspaceUtil.runCommand(cmd,
                                     this.lager.eventLog,
                                     this.lager.traceLog);
        } catch (Exception e) {
            final String err = "error testing notification script: " + pollScriptPath;
            // passing e to error gives very long stacktrace to user
            // logger.error(err, e);
            throw new Exception(err + e.getMessage());
        }

        this.notify = this.notificationInfo + pollScriptPath;

        logger.debug("test run of notification script '" +
                                        pollScriptPath + "' succeeded");


        if (fake) {

            logger.info("fake mode ON, cluster event notification " +
                    "polling is disabled");

        } else {

            // will create a new table entry if necessary
            final int val = this.persistence.readPropagationCounter();
            this.counter = new Counter(val, this);

            // todo: persistence-write delay should be configurable
            this.cp = new CursorPersistence(this.timerManager,
                                            this.persistence,
                                            3000);

            final String eventsPath = pollScriptPath + ".txt";
            logger.debug("Setting events file to '" + eventsPath + "'");

            this.watcher = new ControlPoll(this.timerManager,
                                           this.lager,
                                           this.whome,
                                           this.resourceMessage,
                                           this.watcherDelay,
                                           eventsPath,
                                           this.persistence.currentCursorPosition(),
                                           this);

            this.watcher.scheduleNotificationWatcher();
        }

        this.globals.setPropagateEnabled(true);
        this.globals.setUnpropagateEnabled(true);

        logger.debug("validated/initialized");
    }    
    
    // -------------------------------------------------------------------------
    // implements PropagationAdapter
    // -------------------------------------------------------------------------

    public ArrayList constructPropagateCommand(VirtualMachine vm) {
        try {
            ArrayList al = XenUtil.constructPropagateCommand(vm, this.notify);
            if(this.extraArgs != null && !this.extraArgs.trim().equals(""))
            {
                al.add("--prop-extra-args");
                al.add(this.extraArgs);
            }
            return al;
        } catch (WorkspaceException e) {
            return null;
        }
    }

    public ArrayList constructPropagateToStartCommand(VirtualMachine vm) {
        try {
            return XenUtil.constructCreateCommand(vm, false, this.notify);
        } catch (WorkspaceException e) {
            return null;
        }
    }

    public ArrayList constructPropagateToPauseCommand(VirtualMachine vm) {
        try {
            return XenUtil.constructCreateCommand(vm, true, this.notify);
        } catch (WorkspaceException e) {
            return null;
        }
    }

    public ArrayList constructUnpropagateCommand(VirtualMachine vm) {
        try {
            return XenUtil.constructUnpropagateCommand(vm, this.notify);
        } catch (WorkspaceException e) {
            return null;
        }
    }

    public void prePropagate() throws Exception {
        if (this.counter != null) {
            this.counter.addToCount(1);
            if (this.watcher != null) {
                this.watcher.scheduleNotificationWatcher();
            } else {
                throw new Exception("watcher is null");
            }
        } else {
            throw new Exception("counter is null");
        }
    }

    // -------------------------------------------------------------------------
    // implements CounterCallback
    // -------------------------------------------------------------------------

    public void newCounterValue(int n) {
        if (this.persistence != null) {
            try {
                this.persistence.updatePropagationCounter(n);
            } catch (WorkspaceDatabaseException e) {
                logger.fatal("",e);
            }
        } else {
            logger.fatal("persistenceAdapter is null");
        }
    }


    // -------------------------------------------------------------------------
    // implements NotificationPollCallback
    // -------------------------------------------------------------------------

    public int numPendingNotifications() throws Exception {
        return 1;
        /*
        if (this.counter != null) {
            return this.counter.addToCount(0);
        } else {
            throw new Exception("counter is null");
        }
        */
    }

    public void decreaseNumPending(int n) throws Exception {
        if (this.counter != null) {
            this.counter.addToCount(-n);
        }
    }

    public void cursorPosition(long pos) {
        if (this.cp != null) {
            this.cp.cursorPosition(pos);
        }
    }
}
