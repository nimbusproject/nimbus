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

package org.globus.workspace.xen;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.workspace.Lager;
import org.globus.workspace.ReturnException;
import org.globus.workspace.WorkspaceConstants;
import org.globus.workspace.WorkspaceException;
import org.globus.workspace.WorkspaceUtil;
import org.globus.workspace.service.binding.vm.VirtualMachine;
import org.globus.workspace.service.impls.async.ResourceMessage;
import org.globus.workspace.service.impls.async.WorkspaceRequest;
import org.globus.workspace.service.impls.async.WorkspaceRequestContext;

public abstract class XenTask implements WorkspaceRequest {

    protected static final Log logger =
                            LogFactory.getLog(XenTask.class.getName());

    protected WorkspaceRequestContext ctx;

    protected String name;
    protected boolean doFakeLag;
    protected String[] cmd;

    // if success just means another asynchronous task was started
    // then this task goes away without calling back to the service
    protected boolean async;

    public void setRequestContext(WorkspaceRequestContext context) {
        this.ctx = context;
    }

    private void done(Exception err) {
        if (this.ctx.getNotify() != WorkspaceConstants.STATE_INVALID) {
            final ResourceMessage notif =
                    this.ctx.getLocator().getResourceMessage();
            notif.message(this.ctx.getId(), this.ctx.getNotify(), err);
        }
    }

    protected abstract void init() throws WorkspaceException;

    // Give children a chance to interpret the context of this invocation
    // and possibly track it etc.
    protected Exception preExecute(boolean fake) {
        return null;
    }

    // give children a chance to interpret the exception
    protected Exception postExecute(Exception e, boolean fake) {
        return e;
    }

    public void execute() {

        if (this.ctx == null) {
            logger.fatal("request had null request ctx: " + this.name +
                    " [[ " + this.getClass().getName() + " ]]");
            return;
        }

        try {
            this.init();
        } catch (WorkspaceException e) {
            this.done(e);
            return;
        }

        final boolean fake = this.ctx.getLocator().getGlobalPolicies().isFake();

        Exception e = this.preExecute(fake);

        if (e == null) {
            this._execute(fake);
            e = this.postExecute(e, fake);
        }

        final boolean trace = this.ctx.lager().traceLog;
        final boolean event = this.ctx.lager().eventLog;

        if (this.async && e == null) {
            if (trace) {
                logger.trace(Lager.id(ctx.getId()) + ": " +
                                        this.name + " async task started");
            }
            return;
        }

        if (e == null) {
            if (event) {
                logger.info(Lager.ev(ctx.getId()) + this.name + " succeeded");
            } else if (trace) {
                logger.trace(Lager.id(ctx.getId()) + ": " +
                                                    this.name + " succeeded");
            }
        } else {
            if (event) {
                logger.info(Lager.ev(ctx.getId()) + this.name + " failed");
            } else if (trace) {
                logger.trace(Lager.id(ctx.getId()) + ": " +
                                                this.name + " failed");
            }
        }

        this.done(e);
    }

    private Exception _execute(boolean fake) {

        final int id = this.ctx.getId();
        final boolean traceLog = this.ctx.lager().traceLog;
        final boolean eventLog = this.ctx.lager().eventLog;

        if (traceLog) {
            logger.trace(Lager.id(id) + " " + this.name + ", invoking " +
                     "command: " + WorkspaceUtil.printCmd(this.cmd));
        }

        if (fake) {

            if (eventLog) {

                logger.info(Lager.ev(id) + "banner:" +
                     "\n\n   ***** " + Lager.id(id) + " Fake " + this.name +
                       " Request Begins\n");

            } else if (traceLog) {

                logger.trace("banner:" +
                     "\n\n   ***** " + Lager.id(id) + " Fake " + this.name +
                       " Request Begins\n");
            }

            if (this.cmd != null) {
                logger.info("Command associated with fake '" + this.name +
                        "' is: " + WorkspaceUtil.printCmd(this.cmd));
            }

            if (this.doFakeLag) {
                final long lag =
                        this.ctx.getLocator().getGlobalPolicies().getFakelag();
                try {

                    if (lag > 0) {
                        logger.debug(Lager.id(id) + ": Fake " + this.name +
                                " request, sleeping for " + lag + " ms");
                        Thread.sleep(lag);
                    }
                } catch (InterruptedException e) {
                    logger.error("",e);
                }
            }

            if (eventLog) {

               logger.info(Lager.ev(id) + "banner:" +
                     "\n\n   ***** " + Lager.id(id) + " Fake " + this.name +
                        " Request Ends\n");

            } else if (traceLog) {

                logger.trace("banner:" +
                     "\n\n   ***** " + Lager.id(id) + " Fake " + this.name +
                        " Request Ends\n");
            } else {

                logger.debug(Lager.id(id) + "Fake " + this.name
                                                    + " request complete");
            }

            // Since this is fake mode, we can cheat all we need to.  Simulate
            // asynchronous notifications:
            if (this.async) {

                int stateNotify = WorkspaceConstants.STATE_INVALID;

                if (this.name.equals("Propagate-To-Start")) {
                    stateNotify = WorkspaceConstants.STATE_STARTED;
                } else if (this.name.equals("Propagate-Only")) {
                    stateNotify = WorkspaceConstants.STATE_PROPAGATED;
                } else if (this.name.equals("Propagate-To-Pause")) {
                    stateNotify = WorkspaceConstants.STATE_PAUSED;
                } else if (this.name.equals("Ready-For-Transport")) {
                    stateNotify = WorkspaceConstants.STATE_READY_FOR_TRANSPORT;
                }

                if (stateNotify != WorkspaceConstants.STATE_INVALID) {
                    final ResourceMessage notif =
                                this.ctx.getLocator().getResourceMessage();
                    notif.message(this.ctx.getId(), stateNotify, null);
                }
            }

            return null;
        }

        try {
            // See WorkspaceResource.isVMMaccessOK() javadoc
            if (this.ctx != null && !this.ctx.isVmmAccessOK()) {
                final VirtualMachine vm = this.ctx.getVm();
                final String err;
                if (vm != null) {
                    err = "Do not run have rights to run " +
                           this.name + " for VM " + vm.getID() +
                           " on node " + vm.getNode();
                } else {
                    err = "Do not run have rights to run " +
                            this.name + " with ctx ID = " + id;
                }
                return new WorkspaceException(err);
            }

            // while developing it is sometimes helpful to set cmd to null
            // before it is implemented (i.e., make just this one thing fake
            // for the timebeing without using the fakeness infrastructure
            // for other commands...).

            //TODO: add json content here
            String ret = null;
            if (this.cmd != null) {
                WorkspaceUtil.runCommand(this.cmd, eventLog, traceLog, id);
            }
            return null;
        } catch (ReturnException e) {
            return XenUtil.translateReturnException(e);
        } catch (WorkspaceException e) {
            return e;
        }
    }
}
