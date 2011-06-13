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

package org.globus.workspace.service.impls.async;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.workspace.Lager;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;

/**
 * ((( TODO: move to Spring+cglib prototype factory )))
 * 
 * This factory returns task implementations to the StateTransition engine
 * which decides on when and if they are invoked.  The engine may not care
 * if a command is not implemented (for example, currently if
 * propagateToStart is not implemented, separate propagate and start
 * commands will be run instead).
 *
 * We currently have three sets of command implementations, Xen via our
 * backend driver, Xen via our backend driver over SSH, and Amazon EC2.
 *
 * Every command of each of these can be overriden by configuring a class
 * that implements WorkspaceRequest.
 *
 * As extensions are contributed, they can be configured here (with a
 * recompile).  It seems best to continue to group them in sets.
 *
 * Returning null for a command implementation indicates it is unimplemented
 * or disabled.
 *
 * If demand increases, we can add the ability to configure different
 * combinations via a text configuration file.  But for now, the two sets
 * are hardcoded here, chosen by keyword 'xenlocal' or 'xenssh' in the
 * config (spring)
 *
 * Note that the command configurations are different than the ability
 * to dispatch to different implementations *dynamically*.  That must
 * happen in the WorkspaceRequest implementation for a particular
 * command based on information in the WorkspaceRequestContext.
 */

@SuppressWarnings("unchecked")
public class RequestFactoryImpl implements RequestFactory {
    
    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
                        LogFactory.getLog(RequestFactoryImpl.class.getName());


    // Current command set options -- the set is just a name attached to
    // a group of control commands.  If you would like to implement a
    // multiplexing version you can (as we did for staging in the past).

    public static final String XEN_LOCAL = "xenlocal";
    public static final String XEN_SSH = "xenssh";
    public static final String FAILURE_COMMANDS = "failure_commands"; // for test suites only

    private static final String sshP =
                                  "org.globus.workspace.xen.xenssh";
    private static final String locP =
                                  "org.globus.workspace.xen.xenlocal";

    // All of the commands the StateTransition engine knows about:

    private static final int numCommands = 20;

    private static final Integer cancelUnpropagated = new Integer(0);

    private static final Integer propagate = new Integer(1);
    private static final Integer cancelPropagating = new Integer(2);

    private static final Integer propAndStart = new Integer(3);
    private static final Integer cancelPropToStart = new Integer(4);

    private static final Integer propAndPause = new Integer(5);
    private static final Integer cancelPropToPause = new Integer(6);

    private static final Integer start = new Integer(7);
    private static final Integer startPaused = new Integer(8);
    private static final Integer unpause = new Integer(9);
    private static final Integer reboot = new Integer(10);
    private static final Integer pause = new Integer(11);
    private static final Integer shutdownNormal = new Integer(12);
    private static final Integer shutdownSerialize = new Integer(13);
    private static final Integer unserialize = new Integer(14);
    private static final Integer shutdownTrash = new Integer(15);

    // to cancel any state from propagated to before readyingForTransport
    private static final Integer cancelAllAtVMM = new Integer(16);

    private static final Integer readyForTransport = new Integer(17);
    private static final Integer cancelReadyingForTr = new Integer(18);
    private static final Integer cancelReadyForTr = new Integer(19);

    
    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected final HashMap impls = new HashMap();
    protected final Hashtable cmdStr = new Hashtable();
    protected final Lager lager;

    protected String commandSet;
    
    
    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------

    public RequestFactoryImpl(Lager lagerImpl) {
        if (lagerImpl == null) {
            throw new IllegalArgumentException("lagerImpl may not be null");
        }
        this.lager = lagerImpl;
    }

    
    // -------------------------------------------------------------------------
    // CONFIG SET
    // -------------------------------------------------------------------------

    public void setCommandSet(String set) throws Exception {
        if (!this.init(set)) {
            throw new Exception("Unknown/invalid command set '" + set + "'");
        }
    }
    

    // -------------------------------------------------------------------------
    // SETUP
    // -------------------------------------------------------------------------
    
    // for logging:
    private void initCmdStr() {
        this.cmdStr.put(cancelUnpropagated, "cancelUnpropagated");
        this.cmdStr.put(propagate, "propagate");
        this.cmdStr.put(cancelPropagating, "cancelPropagating");
        this.cmdStr.put(propAndStart, "propagateAndStart");
        this.cmdStr.put(cancelPropToStart, "cancelPropagatingToStart");
        this.cmdStr.put(propAndPause, "propagateAndPause");
        this.cmdStr.put(cancelPropToPause, "cancelPropagatingToPause");
        this.cmdStr.put(startPaused, "startPaused");
        this.cmdStr.put(start, "start");
        this.cmdStr.put(unpause, "unpause");
        this.cmdStr.put(reboot, "reboot");
        this.cmdStr.put(pause, "pause");
        this.cmdStr.put(shutdownNormal, "shutdownNormal");
        this.cmdStr.put(shutdownSerialize, "shutdownSerialize");
        this.cmdStr.put(unserialize, "unserialize");
        this.cmdStr.put(shutdownTrash, "shutdownTrash");
        this.cmdStr.put(cancelAllAtVMM, "cancelAllAtVMM");
        this.cmdStr.put(readyForTransport, "readyForTransport");
        this.cmdStr.put(cancelReadyingForTr, "cancelReadyingForTransport");
        this.cmdStr.put(cancelReadyForTr, "cancelReadyForTransport");
    }

    private WorkspaceRequest get(Integer i) {
        try {
            Class clazz = (Class)this.impls.get(i);
            if (clazz == null) {
                return null;
            }
            return (WorkspaceRequest)clazz.newInstance();
        } catch (Exception e) {
            logger.fatal("",e);
            return null;
        }
    }

    // called during init to make sure configured classes are present
    // and implement WorkspaceRequest interface
    private boolean test() {

        String maxStr = null;
        if (lager.traceLog) {
            int max = 0;
            Enumeration en = cmdStr.elements();
            while (en.hasMoreElements()) {
                int len = ((String)en.nextElement()).length();
                if (len > max) {
                    max = len;
                }
            }

            StringBuffer maxbuf = new StringBuffer();
            for (int i = 0; i < max; i++) {
                maxbuf.append(" ");
            }
            maxStr = maxbuf.toString();
        }

        // does not just fail at first problem, to let programmer know
        // as much as possible
        boolean valid = true;
        StringBuffer buf =
                new StringBuffer("\n\nCommand implementations:\n\n");

        for (int i = 0; i < numCommands; i++) {
            Integer key = new Integer(i);
            String keyname = (String) cmdStr.get(key);
            String keynameJus = null;
            if (maxStr != null) {
                keynameJus = justify(keyname, maxStr);
            }
            if (!impls.containsKey(key)) {

                logger.fatal("command set does not include '" +
                                keyname + "', set explicit null");
                valid = false;
                if (lager.traceLog) {
                    buf.append(keynameJus).
                        append(": no class configuration, ").
                        append("set with explicit null instead").
                        append("\n");
                }

            } else {
                try {
                    Class clazz = (Class)impls.get(key);
                    if (clazz != null) {

                        WorkspaceRequest req =
                                (WorkspaceRequest)clazz.newInstance();

                        if (lager.traceLog) {
                            buf.append(keynameJus).
                                append(": ").
                                append(req.getClass().getName()).
                                append("\n");
                        }

                    } else {
                        // no implementation is OK at this stage

                        if (lager.traceLog) {
                            buf.append(keynameJus).
                                append(": not implemented").
                                append("\n");
                        }
                    }
                } catch (Throwable e) {
                    logger.fatal("Problem with " + keyname +
                                        "() implementation class",e);
                    if (lager.traceLog) {
                        buf.append(keynameJus).
                            append(": exception: ").
                            append(e.getMessage()).
                            append("\n");
                    }
                    valid = false;
                }
            }
        }

        if (lager.traceLog) {
            logger.trace(buf.toString());
        }

        return valid;
    }

    private static String justify(String str, String max) {

        if (str == null || max == null) {
            return max;
        }
        final int len = str.length();
        if (len < max.length()) {
            return max.substring(len) + str + "() ";
        } else {
            return str + "() ";
        }
    }

    /* These disable methods can be called to disable certain command subsets
       which will affect StateTransition decisions.  */

    public void disablePropagate() {
        this.impls.put(propagate, null);
        this.impls.put(propAndStart, null);
        this.impls.put(propAndPause, null);
        this.impls.put(cancelPropagating, null);
        this.impls.put(cancelPropToStart, null);
        this.impls.put(cancelPropToPause, null);
    }

    public void disablePropagateAndCreate() {
        this.impls.put(propAndStart, null);
        this.impls.put(propAndPause, null);
        this.impls.put(cancelPropToStart, null);
        this.impls.put(cancelPropToPause, null);
    }

    public void disableReadyTransport() {
        this.impls.put(readyForTransport, null);
        this.impls.put(cancelReadyingForTr, null);
        this.impls.put(cancelReadyForTr, null);
    }

    /* Factory returns implementations.
       All return null if functionality is not implemented. */

    public WorkspaceRequest cancelUnpropagated() {
        return get(cancelUnpropagated);
    }

    public WorkspaceRequest propagate() {
        return get(propagate);
    }

    public WorkspaceRequest cancelPropagating() {
        return get(cancelPropagating);
    }

    public WorkspaceRequest propagateAndStart() {
        return get(propAndStart);
    }

    public WorkspaceRequest cancelPropagatingToStart() {
        return get(cancelPropToStart);
    }

    public WorkspaceRequest propagateAndPause() {
        return get(propAndPause);
    }

    public WorkspaceRequest cancelPropagatingToPause() {
        return get(cancelPropToPause);
    }

    public WorkspaceRequest start() {
        return get(start);
    }

    public WorkspaceRequest startPaused() {
        return get(startPaused);
    }

    public WorkspaceRequest unpause() {
        return get(unpause);
    }

    public WorkspaceRequest reboot() {
        return get(reboot);
    }

    public WorkspaceRequest pause() {
        return get(pause);
    }

    public WorkspaceRequest shutdownNormal() {
        return get(shutdownNormal);
    }

    public WorkspaceRequest shutdownSerialize() {
        return get(shutdownSerialize);
    }

    public WorkspaceRequest unserialize() {
        return get(unserialize);
    }

    public WorkspaceRequest shutdownTrash() {
        return get(shutdownTrash);
    }

    /*
     To cancel any state between propagated and before readyingForTransport
     */
    public WorkspaceRequest cancelAllAtVMM() {
        return get(cancelAllAtVMM);
    }

    public WorkspaceRequest readyForTransport() {
        return get(readyForTransport);
    }

    public WorkspaceRequest cancelReadyingForTransport() {
        return get(cancelReadyingForTr);
    }

    public WorkspaceRequest cancelReadyForTransport() {
        return get(cancelReadyForTr);
    }

    
    // -------------------------------------------------------------------------
    // INIT
    // -------------------------------------------------------------------------

    protected boolean init(String keyword) {
        if (!this.impls.isEmpty()) {
            logger.fatal("already initialized");
            return false;
        }

        this.initCmdStr();

        boolean result = false;
        if (keyword.trim().equalsIgnoreCase(XEN_SSH)) {
            result = this.loadSSH();
            this.commandSet = XEN_SSH;
        } else if (keyword.trim().equalsIgnoreCase(XEN_LOCAL)) {
            result = loadLocal();
            this.commandSet = XEN_LOCAL;
        } else if (keyword.trim().equalsIgnoreCase(FAILURE_COMMANDS)) {
            result = loadFailureCommands();
            this.commandSet = FAILURE_COMMANDS;
        }

        // addressed Bug 6998 until Bug 7063 can be fixed
        this.disablePropagateAndCreate();

        return result && test();
    }

    private boolean loadSSH() {
        try {
            loadXenCommon(sshP);
            return true;
        } catch (Throwable e) {
            logger.fatal("",e);
            return false;
        }
    }

    private boolean loadLocal() {
        try {
            loadXenCommon(locP);
            return true;
        } catch (Throwable e) {
            logger.fatal("",e);
            return false;
        }
    }

    private boolean loadFailureCommands() {
        try {
            final String prefix = sshP;
            loadXenCommon(prefix);
            testingReplaceShutdownTrash(prefix);
            return true;
        } catch (Throwable e) {
            logger.fatal("",e);
            return false;
        }
    }

    private void loadXenCommon(String pre) throws Throwable {

        impls.put(cancelUnpropagated, null);
        
        this.impls.put(propagate, Class.forName(pre + ".Propagate"));
        this.impls.put(cancelPropagating, Class.forName(pre + ".ShutdownTrash"));

        this.impls.put(propAndStart, Class.forName(pre + ".PropagateStart"));
        this.impls.put(cancelPropToStart, Class.forName(pre + ".ShutdownTrash"));

        this.impls.put(propAndPause, Class.forName(pre + ".PropagatePause"));
        this.impls.put(cancelPropToPause, Class.forName(pre + ".ShutdownTrash"));

        this.impls.put(start, Class.forName(pre + ".Start"));
        this.impls.put(startPaused, Class.forName(pre + ".StartPaused"));
        this.impls.put(unpause, Class.forName(pre + ".Unpause"));
        this.impls.put(reboot, Class.forName(pre + ".Reboot"));
        this.impls.put(pause, Class.forName(pre + ".Pause"));
        this.impls.put(shutdownNormal, Class.forName(pre + ".ShutdownNormal"));
        
        this.impls.put(shutdownSerialize, null);
        this.impls.put(unserialize, null);

        this.impls.put(shutdownTrash, Class.forName(pre + ".ShutdownTrash"));

        this.impls.put(cancelAllAtVMM, Class.forName(pre + ".ShutdownTrash"));
        
        this.impls.put(readyForTransport, Class.forName(pre + ".ReadyTransport"));
        this.impls.put(cancelReadyingForTr, Class.forName(pre + ".ShutdownTrash"));

        this.impls.put(cancelReadyForTr, null);
    }

    // See the MockShutdownTrash notes.
    private void testingReplaceShutdownTrash(String pre) throws Throwable {
        this.impls.put(cancelPropagating, Class.forName(pre + ".MockShutdownTrash"));
        this.impls.put(cancelPropToStart, Class.forName(pre + ".MockShutdownTrash"));
        this.impls.put(cancelPropToPause, Class.forName(pre + ".MockShutdownTrash"));
        this.impls.put(shutdownTrash, Class.forName(pre + ".MockShutdownTrash"));
        this.impls.put(cancelAllAtVMM, Class.forName(pre + ".MockShutdownTrash"));
        this.impls.put(cancelReadyingForTr, Class.forName(pre + ".MockShutdownTrash"));
    }
}
