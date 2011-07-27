package org.globus.workspace.xen;
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.workspace.scheduler.defaults.ResourcepoolEntry;
import org.globus.workspace.service.binding.vm.VirtualMachine;
import org.globus.workspace.service.impls.async.*;

import org.globus.workspace.Lager;
import org.globus.workspace.ReturnException;
import org.globus.workspace.WorkspaceConstants;
import org.globus.workspace.WorkspaceException;
import org.globus.workspace.WorkspaceUtil;

public abstract class XenRequest implements VMMRequest {

    protected static final Log logger =
                            LogFactory.getLog(XenTask.class.getName());

    protected VMMRequestContext ctx;

    protected String name;
    protected boolean doFakeLag;
    protected String[] cmd;

    // if success just means another asynchronous task was started
    // then this task goes away without calling back to the service
    protected boolean async;

    public void setRequestContext(VMMRequestContext context) {
        this.ctx = context;
    }

    protected abstract void init() throws WorkspaceException;

    // Give children a chance to interpret the context of this invocation
    // and possibly track it etc.
    protected Exception preExecute(boolean fake) {
        return null;
    }

    public String execute() throws WorkspaceException {

        if (this.ctx == null) {
            logger.fatal("request had null request ctx: " + this.name +
                    " [[ " + this.getClass().getName() + " ]]");
            throw new WorkspaceException("request had null request ctx: " + this.name +
                    " [[ " + this.getClass().getName() + " ]]");
        }

        this.init();

        final boolean fake = false;
//        this.ctx.getLocator().getGlobalPolicies().isFake();

        Exception e = this.preExecute(fake);
        String ret = "";
        ret = this._execute(fake);

        final boolean trace = this.ctx.lager().traceLog;
        final boolean event = this.ctx.lager().eventLog;

        if (this.async && e == null) {
            if (trace) {
                logger.trace(Lager.id(ctx.getId()) + ": " +
                                        this.name + " async task started");
            }
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

        return ret;
    }

    private String _execute(boolean fake) throws WorkspaceException {

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
                final long lag = 1000; //FIXME
//                        this.ctx.getLocator().getGlobalPolicies().getFakelag();
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
        }

        try {
            if (this.ctx != null) {
                final ResourcepoolEntry vmm = this.ctx.getVmm();
                final String err;
                if (vmm != null) {
                    err = "Do not run have rights to run " +
                           this.name + " for VMM " + vmm.getHostname();
                } else {
                    err = "Do not run have rights to run " +
                            this.name + " with ctx ID = " + id;
                }
                throw new WorkspaceException(err);
            }

            String ret = null;
            if (this.cmd != null) {
                ret = WorkspaceUtil.runCommand(this.cmd, eventLog, traceLog, id);
            }
            return ret;
        } catch (ReturnException e) {
            throw XenUtil.translateReturnException(e);
        } catch (WorkspaceException e) {
            throw e;
        }
    }
}
