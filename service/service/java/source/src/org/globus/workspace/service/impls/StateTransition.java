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

package org.globus.workspace.service.impls;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.workspace.Lager;
import org.globus.workspace.ProgrammingError;
import org.globus.workspace.WorkspaceConstants;
import org.globus.workspace.persistence.DataConvert;
import org.globus.workspace.service.impls.async.RequestDispatch;
import org.globus.workspace.service.impls.async.RequestFactory;
import org.globus.workspace.service.impls.async.TaskNotImplementedException;
import org.globus.workspace.service.impls.async.WorkspaceRequest;
import org.globus.workspace.service.impls.async.WorkspaceRequestContext;
import org.globus.workspace.service.binding.GlobalPolicies;
import org.globus.workspace.TempLocator;
import org.nimbustools.api.services.rm.ManageException;

/**
 * Evaluate current and target states and adds tasks for the
 * WorkspaceRequest execution thread pool to execute.  Caller
 * is responsible for locking appropriately.
 */
public class StateTransition implements WorkspaceConstants {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
                        LogFactory.getLog(StateTransition.class.getName());


    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected final RequestFactory reqFactory;
    protected final DataConvert dataConvert;
    protected final GlobalPolicies globals;
    protected final TempLocator locator;
    protected final Lager lager;

    protected final boolean trace;
    protected final boolean event;

    
    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------

    public StateTransition(RequestFactory requestFactory,
                           DataConvert dataConvertImpl,
                           GlobalPolicies globalPolicies,
                           TempLocator locatorImpl,
                           Lager lagerImpl) {

        if (requestFactory == null) {
            throw new IllegalArgumentException("requestFactory may not be null");
        }
        this.reqFactory = requestFactory;

        if (dataConvertImpl == null) {
            throw new IllegalArgumentException("dataConvertImpl may not be null");
        }
        this.dataConvert = dataConvertImpl;

        if (globalPolicies == null) {
            throw new IllegalArgumentException("globalPolicies may not be null");
        }
        this.globals = globalPolicies;

        if (locatorImpl == null) {
            throw new IllegalArgumentException("locatorImpl may not be null");
        }
        this.locator = locatorImpl;

        if (lagerImpl == null) {
            throw new IllegalArgumentException("lagerImpl may not be null");
        }
        this.trace = lagerImpl.traceLog;
        this.event = lagerImpl.eventLog;
        this.lager = lagerImpl;
    }


    // -------------------------------------------------------------------------
    // ENTRY
    // -------------------------------------------------------------------------

    /**
     * Evaluate current and target states and adds tasks for the
     * WorkspaceRequest execution thread pool to execute.  Caller
     * is responsible for locking access (in StatefulResourceImpl,
     * setState and setTargetState lock based on resource id for
     * example).
     *
     * Only public method in class.
     *
     * @param resource resource to inspect
     * @throws ManageException problem
     */
    public void run(final StatefulResourceImpl resource)
            throws ManageException {

        if (resource == null) {
            throw new IllegalArgumentException("resource is null");
        }
        
        final int id = resource.getID();
        final String idStr = Lager.id(id);

        if (this.trace) {
            logger.trace(idStr + ": request state transition");
        }

        final int state = resource.getState();
        final int targetState = resource.getTargetState();

        _run(resource,
             state,
             targetState,
             id,
             idStr,
             this.dataConvert.stateName(state),
             this.dataConvert.stateName(targetState));

        if (this.trace) {
            logger.trace(idStr + ": state transition over");
        }
    }

    /*
     * Runs a hardcoded "chain of command" pattern (with jumps).
     * Originally used Apache Commons chain library, but this works well.
     */
    private boolean _run(final StatefulResourceImpl res,
                         final int cur,
                         final int tar,
                         final int id,
                         final String idStr,
                         final String curStr,
                         final String tarStr)

            throws ManageException {

        if (cur == tar) {
            return false;
        }

        return corrupted(    cur,tar,   idStr,curStr,tarStr) ||
                  remove(res,cur,tar,id,idStr,curStr,tarStr) ||
                 stageIn(res,cur,tar,id,idStr,curStr,tarStr) ||
               propagate(res,cur,tar,id,idStr,curStr,tarStr) ||
                   start(res,cur,tar,id,idStr,curStr,tarStr) ||
                shutdown(res,cur,tar,id,idStr,curStr,tarStr) ||
       readyForTransport(res,cur,tar,id,idStr,curStr,tarStr) ||
                stageOut(res,cur,tar,id,idStr,curStr,tarStr);
    }

    // allows a curstate jump, see stageIn handler
    private boolean _runProp(final StatefulResourceImpl res,
                             final int cur,
                             final int tar,
                             final int id,
                             final String idStr,
                             final String curStr,
                             final String tarStr)

            throws ManageException {

        return     propagate(res,cur,tar,id,idStr,curStr,tarStr) ||
                       start(res,cur,tar,id,idStr,curStr,tarStr) ||
                    shutdown(res,cur,tar,id,idStr,curStr,tarStr) ||
           readyForTransport(res,cur,tar,id,idStr,curStr,tarStr) ||
                    stageOut(res,cur,tar,id,idStr,curStr,tarStr);
    }

    // allows a curstate jump, see propagate handler
    private boolean _runStart(final StatefulResourceImpl res,
                              final int cur,
                              final int tar,
                              final int id,
                              final String idStr,
                              final String curStr,
                              final String tarStr)

            throws ManageException {

        return         start(res,cur,tar,id,idStr,curStr,tarStr) ||
                    shutdown(res,cur,tar,id,idStr,curStr,tarStr) ||
           readyForTransport(res,cur,tar,id,idStr,curStr,tarStr) ||
                    stageOut(res,cur,tar,id,idStr,curStr,tarStr);
    }


    /* Callers of StateTransition.run make these guarantees */

    /* The only possible current state values, ordered, no gaps: */

    /* STATE_FIRST_LEGAL --> STATE_LAST_LEGAL

    /* STATE_UNSTAGED
       STATE_STAGING_IN
       STATE_UNPROPAGATED
       STATE_PROPAGATING
       STATE_PROPAGATING_TO_START
       STATE_PROPAGATING_TO_PAUSE

       STATE_PROPAGATED
       STATE_STARTING
       STATE_STARTED
       STATE_SERIALIZING
       STATE_SERIALIZED
       STATE_PAUSING
       STATE_PAUSED
       STATE_REBOOT
       STATE_SHUTTING_DOWN

       STATE_READYING_FOR_TRANSPORT
       STATE_READY_FOR_TRANSPORT
       STATE_STAGING_OUT
       STATE_STAGED_OUT

       // see top of remove handler for note about the cancelling states:
       STATE_CANCELLING_STAGING_IN
       STATE_CANCELLING_UNPROPAGATED
       STATE_CANCELLING_PROPAGATING
       STATE_CANCELLING_PROPAGATING_TO_START
       STATE_CANCELLING_PROPAGATING_TO_PAUSE
       STATE_CANCELLING_AT_VMM
       STATE_CANCELLING_READYING_FOR_TRANSPORT
       STATE_CANCELLING_READY_FOR_TRANSPORT
       STATE_CANCELLING_STAGING_OUT

       STATE_DESTROYING
       STATE_CORRUPTED_GENERIC */

    /* (and then from STATE_CORRUPTED to STATE_LAST_LEGAL) */



    /* The only possible target state values, ordered: */

    /* STATE_UNSTAGED
       STATE_UNPROPAGATED
       STATE_PROPAGATED
       STATE_STARTED
       STATE_SERIALIZED
       STATE_PAUSED
       STATE_REBOOT
       STATE_READY_FOR_TRANSPORT
       STATE_STAGED_OUT
       STATE_DESTROYING
       STATE_CORRUPTED_GENERIC */

    /* (and then from STATE_CORRUPTED to STATE_LAST_LEGAL) */



    /* ********* */
    /* corrupted */
    /* ********* */

    private boolean corrupted(final int current,
                              final int target,
                              final String idStr,
                              final String curStr,
                              final String tarStr) {

        if (current < STATE_CORRUPTED_GENERIC) {
            return false;
        }

        if (target == STATE_DESTROYING) {
            return false;
        }

        if (this.trace) {
            logger.trace("\n\n   ***** ST--corrupted: processing " +
                    idStr + ", current = " + curStr + ", target = " +
                    tarStr + "\n");
        }

        // Currently can not move from corrupted-* to anything except
        // removal.

        if (current == STATE_CORRUPTED_GENERIC) {
            logger.warn("Workspace is corrupted: can not change state " +
                "anymore unless workspace is going to be destroyed");
        } else {
            logger.warn("Workspace was corrupted (when moving to state " +
                        this.dataConvert.stateName(current - STATE_CORRUPTED) +
                        "): can not change state anymore unless workspace " +
                        "is going to be destroyed");
        }
        return true;
    }



    /* ****** */
    /* remove */
    /* ****** */

    // Can block waiting for work to complete (the only potentially blocking
    // command in chain)
    private boolean remove(final StatefulResourceImpl resource,
                           final int current,
                           final int target,
                           final int id,
                           final String idStr,
                           final String curStr,
                           final String tarStr)

            throws ManageException {

        if (current >= STATE_CANCELLING_STAGING_IN
                        && current <= STATE_CANCELLING_STAGING_OUT) {


            // since setState(STATE_DESTROYING) is called before any
            // invocation to a CANCELLING command, this should never be
            // the case (because setState doesn't call StateTransition.run()
            // if target has already been set to DESTROYING  (target =
            // STATE_DESTROYING is only the case here in this handler when
            // setTargetState first changes it to DESTROYING

            logger.fatal("programming error, stopping state transition");
            // stop processing
            return true;


            // TODO: In the future an action should be allowed to be
            //       cancelled without its target being set to DESTROYING
            //       first.  When that happens, this assumption above will
            //       need to change (add another handler before this remove
            //       handler to handle resources with a cancelling state or
            //       cancel target state.  A smart scheduler could for
            //       example just want cancelPropagate to execute because
            //       it got a priority request and needs the network, i.e.,
            //       postpone functionality.
        }


        if (target != STATE_DESTROYING) {
            return false;
        }

        if (this.trace) {
            logger.trace("\n\n   ***** ST--remove: processing " +
                    idStr + ", current = " + curStr + ", target = " +
                    tarStr + "\n");
        }

        final WorkspaceRequestContext requestContext =
                new WorkspaceRequestContext(id, resource.getName(),
                                            this.locator, this.lager);
        
        requestContext.setGroupID(resource.getGroupId());
        requestContext.setGroupSize(resource.getGroupSize());
        if (resource.isLastInGroup()) {
            requestContext.setLastInGroup(true);
            resource.setLastInGroup(false);
        }
        requestContext.setPartOfGroupRequest(resource.isPartOfGroupRequest());

        WorkspaceRequest req = null;
        int nextstate = STATE_INVALID;

        switch (current) {
            case STATE_STAGING_IN: // now unused
            case STATE_UNPROPAGATED:
                req = reqFactory.cancelUnpropagated();
                nextstate = STATE_CANCELLING_UNPROPAGATED;
                requestContext.setVm(resource.getVM());
                break;
            case STATE_PROPAGATING:
                req = reqFactory.cancelPropagating();
                nextstate = STATE_CANCELLING_PROPAGATING;
                requestContext.setVm(resource.getVM());
                break;
            case STATE_PROPAGATING_TO_START:
                req = reqFactory.cancelPropagatingToStart();
                nextstate = STATE_CANCELLING_PROPAGATING_TO_START;
                requestContext.setVm(resource.getVM());
                break;
            case STATE_PROPAGATING_TO_PAUSE:    
                req = reqFactory.cancelPropagatingToPause();
                nextstate = STATE_CANCELLING_PROPAGATING_TO_PAUSE;
                requestContext.setVm(resource.getVM());
                break;
            case STATE_PROPAGATED:
            case STATE_STARTING:
            case STATE_STARTED:
            case STATE_SERIALIZING:
            case STATE_SERIALIZED:
            case STATE_PAUSING:
            case STATE_PAUSED:
            case STATE_SHUTTING_DOWN:
                req = reqFactory.cancelAllAtVMM();
                nextstate = STATE_CANCELLING_AT_VMM;
                requestContext.setVm(resource.getVM());
                break;
            case STATE_READYING_FOR_TRANSPORT:
                req = reqFactory.cancelReadyingForTransport();
                nextstate = STATE_CANCELLING_READYING_FOR_TRANSPORT;
                requestContext.setVm(resource.getVM());
                break;
            case STATE_READY_FOR_TRANSPORT:
                req = reqFactory.cancelReadyForTransport();
                nextstate = STATE_CANCELLING_READY_FOR_TRANSPORT;
                requestContext.setVm(resource.getVM());
                break;
            case STATE_STAGING_OUT: // now unused
            default:
        }

        if (current >= STATE_CORRUPTED) {

            // currently we will try to do something about a workspace
            // corrupted at times that may have left image files or state
            // at the backend node, we do not handle other corrupt-*
            // situations now.

            final int oldstate = current - STATE_CORRUPTED;

            if (oldstate >= STATE_PROPAGATING
                    && oldstate < STATE_READYING_FOR_TRANSPORT) {

                req = reqFactory.cancelAllAtVMM();
                nextstate = STATE_CANCELLING_AT_VMM;
                requestContext.setVm(resource.getVM());

            } else {

                // candidate for admin log/trigger of severe issues

                final String err = "Destroying a corrupted " +
                            "resource in state '" + curStr +
                            "'. That state does not indicate files or " +
                            "cruft may be on VMM node, not doing anything" +
                            " (but there may be stray staged files off-VMM).";

                if (this.event) {
                    logger.info(Lager.ev(id) + err);
                } else if (this.trace) {
                    logger.trace(idStr + err);
                }
            }
        }

        if (req != null) {

            resource.setStateUnderLock(nextstate, null);

            // ctx does not have notify field set which triggers a conforming
            // WorkspaceRequest implementation to NOT call back to us when
            // its done with work.
            req.setRequestContext(requestContext);

            if (this.trace) {
                logger.trace("\n\n   ***** ST--remove: " + idStr
                                    + ", excecuting " + req.toString() + "\n");
            }

            // TODO: add a timeout
            try {
                req.execute(); // could block
            } catch (Throwable t) {
                // candidate for admin log/trigger of severe issues
                logger.error("",t);
            }

            if (this.trace) {
                logger.trace("\n\n   ***** ST--remove: " + idStr
                              + ", done excecuting " + req.toString() + "\n");
            }

        } else {
            if (this.trace) {
                logger.trace("\n\n   ***** ST--remove: " + idStr
                        + ", nothing to do\n");
            }
        }

        resource.setStateUnderLock(STATE_DESTROYING, null);

        return true;
    }


    /* ******* */
    /* stageIn */
    /* ******* */

    private boolean stageIn(final StatefulResourceImpl resource,
                            final int current,
                            final int target,
                            final int id,
                            final String idStr,
                            final String curStr,
                            final String tarStr)

            throws ManageException {

        if (current == STATE_STAGING_IN) {
            // can happen if user calls operations while staging is still
            // happening, just cut out and do nothing
            if (this.trace) {
                logger.debug("current is " + curStr + ", nothing to do");
            }
            return true;
        }

        // target not applicable
        if (target < STATE_UNPROPAGATED) {
            return false;
        }

        // nothing to do
        if (current >= STATE_STAGING_IN) {
            return false;
        }

        if (this.trace) {
            logger.trace("\n\n   ***** ST--stageIn (now a no-op): processing " +
                    idStr + ", current = " + curStr + ", target = " +
                    tarStr + "\n");
        }

        resource.setStateUnderLock(STATE_UNPROPAGATED, null);
        _runProp(resource, STATE_UNPROPAGATED, target, id,
                 idStr, this.dataConvert.stateName(STATE_UNPROPAGATED), tarStr);
        return true;
    }


    /* ********* */
    /* propagate */
    /* ********* */

    private boolean propagate(final StatefulResourceImpl resource,
                              final int current,
                              int target,
                              final int id,
                              final String idStr,
                              final String curStr,
                              final String tarStr)

            throws ManageException {

        if (current == STATE_PROPAGATING ||
            current == STATE_PROPAGATING_TO_PAUSE ||
            current == STATE_PROPAGATING_TO_START) {
            // can happen if user calls operations while propagation is still
            // happening, just cut out and do nothing
            if (this.trace) {
                logger.debug("current state is " + curStr + ", nothing to do");
            }
            return true;
        }

        if (target < STATE_PROPAGATED) {
            return false;
        }

        // nothing to do
        if (current >= STATE_PROPAGATING) {
            return false;
        }

        if (this.trace) {
            logger.trace("\n\n   ***** ST--propagate: processing " +
                    idStr + ", current = " + curStr + ", target = " +
                    tarStr + "\n");
        }

        if (!resource.isPropagateRequired()) {
            resource.setStateUnderLock(STATE_PROPAGATED, null);
            _runStart(resource, STATE_PROPAGATED, target, id,
                      idStr, this.dataConvert.stateName(STATE_PROPAGATED), tarStr);
            return true;
        }

        //propagate is needed

        if (!this.globals.isPropagateEnabled()) {

            logger.error("should be unreachable, Binding should have " +
                    "rejected this creation request");

            int next = STATE_INVALID;
            if (target == STATE_PROPAGATED) {
                next = STATE_PROPAGATING;
            } else if (target == STATE_STARTED) {
                next = STATE_PROPAGATING_TO_START;
            } else if (target == STATE_PAUSED) {
                next = STATE_PROPAGATING_TO_PAUSE;
            }

            final String errMsg = "propagate functionality " +
                            "needed but it has been disabled";
            final ManageException wexc = new ManageException(errMsg);

            final int stateToSet;
            if (next == STATE_INVALID) {
                stateToSet = STATE_CORRUPTED_GENERIC;
            } else {
                stateToSet = STATE_CORRUPTED + next;
            }
            resource.setStateUnderLock(stateToSet, wexc);
            throw wexc;
        }

        WorkspaceRequest req;
        boolean fallback = false;
        final boolean propstartOK = resource.isPropagateStartOK();

        if (target == STATE_STARTED) {

            if (propstartOK) {
                req = this.reqFactory.propagateAndStart();

                if (req == null) {
                    if (this.trace) {
                        logger.trace("\n\n   ***** ST--propagate " + idStr +
                                ": could use propagateToStart, " +
                                            "but not implemented\n");
                    }
                    fallback = true;
                }

            } else {

                req = reqFactory.propagate();

                if (req == null) {

                    final String errMsg = "propagate functionality " +
                            "needed but it is not implemented";
                    final ManageException wexc =
                            new ManageException(errMsg);

                    resource.setStateUnderLock(
                                STATE_CORRUPTED + STATE_PROPAGATING, wexc);
                    
                    throw wexc;
                }

                // just change local var, not real resource target
                target = STATE_PROPAGATED;

                if (this.trace) {
                    logger.trace("\n\n   ***** ST--propagate " + idStr +
                    ": propagateToStart not OK for this resource, doing" +
                        " propagate-only\n");
                }

            }

        } else if (target == STATE_PAUSED) {

            if (propstartOK) {

                req = this.reqFactory.propagateAndPause();

                if (req == null) {
                    if (this.trace) {
                        logger.trace("\n\n   ***** ST--propagate " + idStr +
                       ": could use propagateToPause, but not implemented\n");
                    }
                    fallback = true;
                }

            } else {

                req = reqFactory.propagate();

                if (req == null) {

                    final String errMsg = "propagate functionality " +
                            "needed but it is not implemented";
                    final ManageException wexc =
                            new ManageException(errMsg);

                    resource.setStateUnderLock(
                                STATE_CORRUPTED + STATE_PROPAGATING, wexc);
                    
                    throw wexc;
                }

                // just change local var, not real resource target
                // todo: comment why
                target = STATE_PROPAGATED;

                if (this.trace) {

                    logger.trace("\n\n   ***** ST--propagate " + idStr +
                    ": propagateToPause not OK for this resource, doing" +
                        " propagate-only\n");
                }
            }

        } else if (target == STATE_PROPAGATED) {

            req = this.reqFactory.propagate();

            if (req == null) {

                final String errMsg = "propagate functionality " +
                            "needed but it is not implemented";
                final ManageException wexc =
                        new ManageException(errMsg);

                resource.setStateUnderLock(
                                STATE_CORRUPTED + STATE_PROPAGATING, wexc);

                throw wexc;
            }

        } else {

            // handlers before propagate handler should not
            // let this happen

            throw new ManageException("Current state is " +
                    this.dataConvert.stateName(current) + ", " +
                    "propagate is needed but not" +
                    " achieved, but target state is not " +
                    this.dataConvert.stateName(STATE_STARTED) + ", " +
                    this.dataConvert.stateName(STATE_PAUSED) + ", or" +
                    this.dataConvert.stateName(STATE_PROPAGATED) + ", it is " +
                    this.dataConvert.stateName(target));
        }

        if (fallback) {

            if (this.trace) {
                logger.trace("\n\n   ***** ST--propagate " + idStr +
                                        ": falling back to propagate-only\n");
            }

            req = reqFactory.propagate();

            if (req == null) {

                final String errMsg = "propagate functionality " +
                            "needed but it is not implemented";
                final ManageException wexc =
                        new ManageException(errMsg);

                resource.setStateUnderLock(
                                STATE_CORRUPTED + STATE_PROPAGATING, wexc);
                
                throw wexc;
            }

            // just change local var, not real resource target
            target = STATE_PROPAGATED;
        }


        final WorkspaceRequestContext requestContext =
                new WorkspaceRequestContext(id, resource.getName(),
                                            this.locator, this.lager);
        
        requestContext.setVm(resource.getVM());
        requestContext.setGroupID(resource.getGroupId());
        requestContext.setGroupSize(resource.getGroupSize());
        if (resource.isLastInGroup()) {
            requestContext.setLastInGroup(true);
            resource.setLastInGroup(false);
        }
        requestContext.setPartOfGroupRequest(resource.isPartOfGroupRequest());

        // req cannot be null here

        if (target == STATE_PROPAGATED) {

            requestContext.setNotify(STATE_PROPAGATED);
            req.setRequestContext(requestContext);

            resource.setStateUnderLock(STATE_PROPAGATING, null);

            if (this.trace) {
                logger.trace("\n\n   ***** ST--propagate " + idStr +
                        ": adding propagate request: " + req + "\n");
            }

            RequestDispatch.addRequest(req, id);
            return true;

        } else if (target == STATE_STARTED) {

            requestContext.setNotify(STATE_STARTED);
            req.setRequestContext(requestContext);

            resource.setStateUnderLock(STATE_PROPAGATING_TO_START, null);

            if (this.trace) {
                logger.trace("\n\n   ***** ST--propagate " + idStr +
                      ": adding propagate-to-start request: " + req + "\n");
            }

            RequestDispatch.addRequest(req, id);
            return true;

        } else {

            requestContext.setNotify(STATE_PAUSED);
            req.setRequestContext(requestContext);

            resource.setStateUnderLock(STATE_PROPAGATING_TO_PAUSE, null);

            if (this.trace) {
                logger.trace("\n\n   ***** ST--propagate " + idStr +
                        ": adding propagate-to-pause request: " + req + "\n");
            }

            RequestDispatch.addRequest(req, id);
            return true;
        }
    }


    /* ***** */
    /* start */
    /* ***** */

    private boolean start(final StatefulResourceImpl resource,
                          final int current,
                          final int target,
                          final int id,
                          final String idStr,
                          final String curStr,
                          final String tarStr)

            throws ManageException {

        if (target != STATE_STARTED
             && target != STATE_REBOOT
             && target != STATE_PAUSED) {
            return false;
        }

        if (target == STATE_PAUSED && current == STATE_STARTED) {
            return false; //shutdown will get this
        }

        if (current >= STATE_READYING_FOR_TRANSPORT) {
            // restriction could go away in the future
            throw new ManageException("illegal to move to " +
                    this.dataConvert.stateName(STATE_STARTED) + " from " +
                    this.dataConvert.stateName(current));
        }

        if (this.trace) {
            logger.trace("\n\n   ***** ST--start: processing " + idStr +
                           ", current = " + curStr + ", target = " +
                           tarStr + "\n");
        }

        // no implementation of these does not cause resource to move to
        // corrupted which is why TaskNotImplementedException is thrown

        boolean notifyPaused = false;

        final WorkspaceRequest req;

        if (target == STATE_REBOOT) {

            req = this.reqFactory.reboot();

            if (req == null) {
                throw new TaskNotImplementedException("reboot not implemented");
            }

            // would be cool to implement a 'reboot -> start-paused' option,
            // (there may be a conceivable application use), but for now we
            // expect reboot results in STATE_STARTED
            resource.setTargetStateUnderLock(STATE_STARTED);

            if (this.trace) {
                logger.trace("\n\n   ***** ST--start " + idStr +
                        ": adding reboot request: " + req + "\n");
            }

        } else if (current == STATE_PAUSED) {

            if (target == STATE_PAUSED) {
                throw new ProgrammingError("current and target are " +
                        "both paused");
            }

            req = reqFactory.unpause();

            if (req == null) {
                throw new TaskNotImplementedException(
                                        "unpause not implemented");
            }

            if (this.trace) {
                logger.trace("\n\n   ***** ST--start " + idStr +
                        ": adding unpause request: " + req + "\n");
            }

        } else if (current == STATE_PROPAGATED) {

            if (target == STATE_PAUSED) {
                req = reqFactory.startPaused();
                notifyPaused = true;
            } else {
                req = reqFactory.start();
            }

            if (req == null) {
                throw new TaskNotImplementedException(
                                        "start (create) not implemented");
            }

            if (this.trace) {
                logger.trace("\n\n   ***** ST--start " + idStr +
                        ": adding start (create) request: " + req + "\n");
            }

        } else if (current == STATE_SERIALIZED) {

            req = reqFactory.unserialize();

            if (req == null) {
                throw new TaskNotImplementedException(
                                        "unserialize not implemented");
            }

            if (this.trace) {
                logger.trace("\n\n   ***** ST--start " + idStr +
                        ": adding unserialize request: " + req + "\n");
            }

        } else {
            // If current is another state between STATE_PROPAGATED and
            // STATE_SHUTTING_DOWN, there is nothing to do but wait until
            // the notification comes in that STATE_PROPAGATED,
            // STATE_PAUSED, or STATE_SERIALIZED has been achieved.

            // If target is still STATE_STARTED at that point, StateTransition
            // will run again via WorkspaceResourceImpl.setState() and will
            // cause one of the three commands to be launched

            return true;
        }

        final WorkspaceRequestContext requestContext =
                new WorkspaceRequestContext(id, resource.getName(),
                                            this.locator, this.lager);

        requestContext.setVm(resource.getVM());
        if (notifyPaused) {
            requestContext.setNotify(STATE_PAUSED);
        } else {
            requestContext.setNotify(STATE_STARTED);
        }
        requestContext.setGroupID(resource.getGroupId());
        requestContext.setGroupSize(resource.getGroupSize());
        if (resource.isLastInGroup()) {
            requestContext.setLastInGroup(true);
            resource.setLastInGroup(false);
        }
        requestContext.setPartOfGroupRequest(resource.isPartOfGroupRequest());
        req.setRequestContext(requestContext);

        resource.setStateUnderLock(STATE_STARTING, null);
        RequestDispatch.addRequest(req, id);

        return true;
    }


    /* ******** */
    /* shutdown */
    /* ******** */

    private boolean shutdown(final StatefulResourceImpl resource,
                             final int current,
                             final int target,
                             final int id,
                             final String idStr,
                             final String curStr,
                             final String tarStr)

            throws ManageException {

        if (current != STATE_STARTING &&
            current != STATE_STARTED  &&
            current != STATE_PAUSING  &&
            current != STATE_PAUSED) {

            return false;
        }

        if (target != STATE_PROPAGATED &&
            target != STATE_SERIALIZED &&
            target != STATE_PAUSED &&
            target != STATE_READY_FOR_TRANSPORT &&
            target != STATE_STAGED_OUT) {

            return false;
        }

        if (this.trace) {
            logger.trace("\n\n   ***** ST--shutdown: processing " +
                    idStr + ", current = " + curStr + ", target = " +
                    tarStr + "\n");
        }

        final WorkspaceRequest req;

        final WorkspaceRequestContext requestContext =
                new WorkspaceRequestContext(id, resource.getName(),
                                            this.locator, this.lager);

        if (target == STATE_PROPAGATED ||
            target == STATE_READY_FOR_TRANSPORT ||
            target == STATE_STAGED_OUT) {

            req = reqFactory.shutdownNormal();

            if (req == null) {
                throw new TaskNotImplementedException(
                                    "shutdown normal unimplemented");
            }

            requestContext.setNotify(STATE_PROPAGATED);
            resource.setStateUnderLock(STATE_SHUTTING_DOWN, null);

            if (this.trace) {
                logger.trace("\n\n   ***** ST--shutdown " + idStr +
                        ": target state is " + tarStr +
                        ", adding shutdown-normal request: " + req + "\n");
            }


        } else if (target == STATE_PAUSED) {

            if (current == STATE_PAUSING) {
                return true;
            }

            // current == STATE_PAUSED guaranteed to be false, so we are
            // just left with STATE_STARTING and STATE_STARTED

            req = reqFactory.pause();

            if (req == null) {
                throw new TaskNotImplementedException("pause not implemented");
            }

            requestContext.setNotify(STATE_PAUSED);
            resource.setStateUnderLock(STATE_PAUSING, null);

            if (this.trace) {
                logger.trace("\n\n   ***** ST--shutdown " + idStr +
                        ": target state is " + tarStr +
                        ", adding pause request: " + req + "\n");
            }

        } else {

            req = reqFactory.shutdownSerialize();

            if (req == null) {
                throw new TaskNotImplementedException(
                                    "shutdown-serialize not implemented");
            }

            requestContext.setNotify(STATE_PROPAGATED);
            resource.setStateUnderLock(STATE_SERIALIZING, null);

            if (this.trace) {
                logger.trace("\n\n   ***** ST--shutdown " + idStr +
                        ": target state is " + tarStr +
                        ", adding serialize request: " + req + "\n");
            }
        }

        requestContext.setVm(resource.getVM());
        requestContext.setGroupID(resource.getGroupId());
        requestContext.setGroupSize(resource.getGroupSize());
        if (resource.isLastInGroup()) {
            requestContext.setLastInGroup(true);
            resource.setLastInGroup(false);
        }
        requestContext.setPartOfGroupRequest(resource.isPartOfGroupRequest());
        req.setRequestContext(requestContext);
        RequestDispatch.addRequest(req, id);

        return true;
    }


    /* ***************** */
    /* readyForTransport */
    /* ***************** */

    private boolean readyForTransport(final StatefulResourceImpl resrc,
                                     final int current,
                                     final int target,
                                     final int id,
                                     final String idStr,
                                     final String curStr,
                                     final String tarStr)
            throws ManageException {


        if (target < STATE_READY_FOR_TRANSPORT) {
            return false;
        }

        if (current >= STATE_READYING_FOR_TRANSPORT) {
            return false;
        }

        // TODO: it makes sense to remove memory and IP address allocations
        // once we start readying for transport (since the workspace can not
        // turn back from that in this version).  But, we need to be careful
        // about hitting back-out-allocate in the cancellation and corruption
        // situations, so leaving it in the do_remove() method of
        // WorkspaceResourceImpl for now until there is more time to consider
        // the options.  Also, it would not make sense to back out every
        // allocation when we start reserving intra-site propagation network
        // bandwidth etc. (unless that is kept track of completely in the
        // scheduler).

        if (!resrc.isUnPropagateRequired()) {
            resrc.setStateUnderLock(STATE_READY_FOR_TRANSPORT, null);
            stageOut(resrc, STATE_READY_FOR_TRANSPORT, target, id,
                     idStr,
                     this.dataConvert.stateName(STATE_READY_FOR_TRANSPORT), tarStr);
            return true;
        }

        if (this.trace) {
            logger.trace("\n\n   ***** ST--rForT: processing " + idStr +
                         ", current = " + curStr + ", target = " +
                         tarStr + "\n");
        }


        int corrupTag = STATE_INVALID;
        if (target == STATE_READY_FOR_TRANSPORT) {
            corrupTag = STATE_READYING_FOR_TRANSPORT;
        } else if (target == STATE_STAGED_OUT) {
            corrupTag = STATE_STAGING_OUT;
        }

        // unpropagate is needed

        if (!this.globals.isUnpropagateEnabled()) {

            logger.error("should be unreachable, Binding should have " +
                    "rejected this creation request");

            final String errMsg = "unpropagate functionality " +
                            "needed but it has been disabled";
            final ManageException wexc =
                    new ManageException(errMsg);

            final int stateToSet;
            if (corrupTag == STATE_INVALID) {
                stateToSet = STATE_CORRUPTED_GENERIC;
            } else {
                stateToSet = STATE_CORRUPTED + corrupTag;
            }
            resrc.setStateUnderLock(stateToSet, wexc);
            throw wexc;
        }

        final WorkspaceRequest req = this.reqFactory.readyForTransport();
        if (req == null) {

            final String errMsg = "ready-for-transport " +
                    "functionality needed and enabled but no implementation " +
                        "class is configured";
            final ManageException wexc =
                    new ManageException(errMsg);

            final int stateToSet;
            if (corrupTag == STATE_INVALID) {
                stateToSet = STATE_CORRUPTED_GENERIC;
            } else {
                stateToSet = STATE_CORRUPTED + corrupTag;
            }
            resrc.setStateUnderLock(stateToSet, wexc);
            throw wexc;
        }

        final WorkspaceRequestContext requestContext =
                new WorkspaceRequestContext(id, resrc.getName(),
                                            this.locator, this.lager);

        requestContext.setVm(resrc.getVM());
        requestContext.setNotify(STATE_READY_FOR_TRANSPORT);
        req.setRequestContext(requestContext);

        resrc.setStateUnderLock(STATE_READYING_FOR_TRANSPORT, null);

        if (this.trace) {
            logger.trace("\n\n   ***** ST--rForT " + idStr +
                    ": adding readyForTransport request: " + req + "\n");
        }
        RequestDispatch.addRequest(req, id);

        return true;
    }


    /* ******** */
    /* stageOut */
    /* ******** */

    private boolean stageOut(final StatefulResourceImpl resource,
                             final int current,
                             final int target,
                             final int id,
                             final String idStr,
                             final String curStr,
                             final String tarStr)

            throws ManageException {

        if (current != STATE_READY_FOR_TRANSPORT) {
            return false;
        }

        if (target < STATE_READY_FOR_TRANSPORT) {
            return false;
        }

        if (this.trace) {
            logger.trace("\n\n   ***** ST--stageOut: " + idStr +
                         " is now a no-op, staging is not possible, current " +
                         "state = " + curStr + "\n");
        }

        // return value does not matter, end of chain, but return
        // false in case someone wants to add a handler after this

        return false;
    }
}
