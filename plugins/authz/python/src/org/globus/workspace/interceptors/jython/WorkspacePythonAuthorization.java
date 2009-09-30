/*
 * Copyright 1999-2006 University of Chicago
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

package org.globus.workspace.interceptors.jython;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.jython.Jython;
import org.globus.jython.log;
import org.nimbustools.api.repr.vm.NIC;
import org.nimbustools.api.repr.CannotTranslateException;
import org.nimbustools.api.services.rm.AuthorizationException;
import org.globus.workspace.service.binding.authorization.Decision;
import org.globus.workspace.service.binding.authorization.Restrictions;
import org.globus.workspace.service.binding.vm.VirtualMachine;
import org.globus.workspace.service.binding.vm.VirtualMachineDeployment;
import org.globus.workspace.service.binding.vm.VirtualMachinePartition;
import org.globus.workspace.persistence.DataConvert;
import org.python.core.Py;
import org.python.core.PyCode;
import org.python.core.PyException;
import org.python.core.PyInteger;
import org.python.core.PyObject;
import org.python.core.__builtin__;
import org.python.util.PythonInterpreter;

import javax.security.auth.Subject;
import java.io.StringWriter;
import java.util.Set;

public class WorkspacePythonAuthorization implements Decision {

    private static Log logger =
        LogFactory.getLog(PythonAuthorization.class.getName());

    private static PyCode code;

    // static, can only be set once, use this opportunity to initialize
    // our special return variables
    protected static void setScript(String script) {

        PythonInterpreter interp = Jython.getInterpreter();
        interp.exec(INIT_CODE);
        interp.exec("decision = INDETERMINATE");
        interp.exec("DN = None");
        interp.exec("voms = None");
        interp.exec("shib = None");
        interp.set("restrictions", Py.java2py(new Restrictions()));

        // todo: learn how to extend java2py for complex objects

        if (logger.isDebugEnabled()) {
            logger.debug("environment:");
            interp.exec("import sys");
            interp.exec("print 'sys.prefix=', sys.prefix");
            interp.exec("print 'sys.argv=', sys.argv");
            interp.exec("print 'sys.path=', sys.path");
            interp.exec("print 'sys.cachedir=', sys.cachedir");
            interp.exec("counter = 0");
            interp.exec("print 'counter=', counter");
            interp.exec("print 'decision=', decision");
            interp.exec("print 'restrictions=', restrictions");
            interp.exec("print");
        }

        // precompile the configured authz code
        code = __builtin__.compile(script, "<>", "exec");
    }

    /**
     * Entry
     *
     * @param vms the request
     * @param subject caller
     * @param callerID caller simple string ID
     * @param restr restrictions object
     * @param elapsedMins
     * @param reservedMins
     * @param numWorkspaces
     * @param dataConvert
     * @return Decision integer
     * @throws AuthorizationException error, not decision
     */
    public static Integer isPermitted(VirtualMachine[] vms,
                                      Subject subject,
                                      String callerID,
                                      Restrictions restr,
                                      Long elapsedMins,
                                      Long reservedMins,
                                      int numWorkspaces,
                                      DataConvert dataConvert)
                        throws AuthorizationException {

        if (code == null) {
            throw new AuthorizationException("no code for authz");
        }

        if (vms == null) {
            throw new AuthorizationException("null vms");
        }
        
        if (dataConvert == null) {
            throw new AuthorizationException("dataConvert may not be null");
        }


        // haven't made script interface group-aware yet...

        // prioritize permit over indeterminate, unless there is a deny which
        // short circuits
        Integer ret = Decision.INDETERMINATE;
        for (int i = 0; i < vms.length; i++) {
            Integer decision = handle(vms[i], subject, callerID, restr, dataConvert);
            if (decision.equals(Decision.DENY)) {
                ret = Decision.DENY;
                break;
            }
            if (decision.equals(Decision.PERMIT)) {
                ret = Decision.PERMIT;
            }
        }
        return ret;
    }


    // it is important this is synchronized, we define the interface
    // to the python authorization callout such that it always receives
    // a fresh set of the global variables and can run to completion
    // before being invoked again
    private static synchronized Integer handle(VirtualMachine vm,
                                               Subject subject,
                                               String callerID,
                                               Restrictions restr,
                                               DataConvert dataConvert)
            throws AuthorizationException {

        PythonInterpreter interp = Jython.getInterpreter();
        StringWriter stderrWr = new StringWriter();
        StringWriter stdoutWr = new StringWriter();
        interp.setErr(stderrWr);
        interp.setOut(stdoutWr);

        // set subject credentials including attributes
        setSubject(subject, callerID, interp);

        setVM(vm, interp, dataConvert);

        Integer decision = run(interp, restr);

        stdoutWr.flush();
        stderrWr.flush();
        String stdout = stdoutWr.toString();
        String stderr = stderrWr.toString();

        try { stdoutWr.close(); } catch (Exception e) {logger.error("",e);}
        try { stderrWr.close(); } catch (Exception e) {logger.error("",e);}

        log.stdout(stdout);
        log.stderr(stderr);

        return decision;
    }

    private static Integer run(PythonInterpreter interp,
                               Restrictions restr) {
        logger.debug("run()");

        interp.exec("decision = INDETERMINATE");

        try {
            interp.exec(code);
        } catch (PyException e) {
            logger.error("authorization callout threw Python exception", e);
            return DENY;
        }

        Integer result;

        try {
            PyInteger ret = (PyInteger) interp.get("decision");
            result = new Integer(ret.getValue());
        } catch (ClassCastException e) {
            logger.error("'decision' in authorization callout was set" +
                    " incorrectly, defaulting to DENY");
            result = DENY;
        } catch (Exception e) {
            logger.error("problem retrievng 'decision' from " +
                    "authorization callout, defaulting to DENY");
            result = DENY;
        }

        if (result == null) {
            return DENY;  // Still a script interface error
        } else if (result.equals(DENY)) {
            return DENY;  // no need to process restrictions
        }

        try {
            PyObject ret = interp.get("restrictions");
            Restrictions restr2 =
                    (Restrictions)ret.__tojava__(Restrictions.class);
            restr.setMaxDuration(restr2.getMaxDuration());
            restr.setMaxMem(restr2.getMaxMem());
        } catch (ClassCastException e) {
            // alternatively we could have tested for
            // Object == Py.NoConversion
            logger.error("'Restrictions' in authorization callout was set" +
                    " incorrectly, defaulting to DENY");
            result = DENY;
        } catch (Exception e) {
            logger.error("problem retrievng 'Restrictions' from " +
                    "authorization callout, defaulting to DENY");
            result = DENY;
        }

        return result;

    }

    private static void setSubject(Subject subject,
                                   String callerID,
                                   PythonInterpreter interp) {

        logger.debug("setSubject()");

        setString("DN",callerID,interp);

        // see if there is any extra information attached to the Subject
        if (subject != null) {
            Set credSet = subject.getPublicCredentials();
            try {
                VomsUtil.voms(credSet, interp);
            } catch (ClassNotFoundException e) {
                logger.debug("VOMS PIP is not installed");
            }
            try {
                ShibUtil.shib(credSet, interp);
            } catch (ClassNotFoundException e) {
                logger.debug("GridShib PIP is not installed");
            }
        } else {
            logger.warn("No peer credentials found");
        }
    }

    private static void setVM(VirtualMachine vm,
                              PythonInterpreter interp,
                              DataConvert dataConvert)
            throws AuthorizationException {
        logger.debug("setVM()");

        interp.exec(NEW_REQUEST);

        VirtualMachineDeployment vmdep = vm.getDeployment();
        if (vmdep != null) {

            interp.exec("req.memory = " +
                                 vmdep.getIndividualPhysicalMemory());

            interp.exec("req.duration_secs = " +
                                 vmdep.getMinDuration());
        }

        if (vm.getPartitions() != null) {

            final VirtualMachinePartition[] partitions = vm.getPartitions();

            // convention is that rootdisk will be first in images []
            int found = 0;
            for (int i = 0; i < partitions.length; i++) {
                if (partitions[i].isRootdisk()) {
                    found += 1;
                    interp.exec("req.images.append('" +
                                partitions[i].getImage() + "')");
                }
            }

            if (found == 0) {
                throw new AuthorizationException(
                                         "no root disk found");
            } else if (found > 1) {
                throw new AuthorizationException(
                                        "more than one root disk found");
            }

            for (int i = 0; i < partitions.length; i++) {
                if (!partitions[i].isRootdisk()) {

                    interp.exec("req.images.append('" +
                                partitions[i].getImage() + "')");
                }
            }
        }

        setString("req.kernel", vm.getKernel(), interp);
        setString("req.kernelParams", vm.getKernelParameters(), interp);

        final String netString = vm.getNetwork();
        if (netString != null) {
            // todo: move to sane network situation
            final NIC[] nics;
            try {
                nics = dataConvert.getNICs(vm);
            } catch (CannotTranslateException e) {
                throw new AuthorizationException(e.getMessage(), e);
            }
            if (nics != null) {
                handleNetworking(nics, interp);
            }
        }

    }

    private static void handleNetworking(NIC[] nics,
                                         PythonInterpreter interp) {
        logger.debug("handleNetworking()");

        if (nics == null) {
            return;
        }

        for (int i = 0; i < nics.length; i++) {
            interp.exec(NEW_NIC);

            setString("anic.ip", nics[i].getIpAddress(), interp);

            setString("anic.name", nics[i].getName(), interp);

            setString("anic.association", nics[i].getNetworkName(), interp);

            setString("anic.hostname",
                      nics[i].getHostname(),
                      interp);

            setString("anic.gateway",
                      nics[i].getGateway(),
                      interp);

            setString("anic.method",
                      nics[i].getAcquisitionMethod(),
                      interp);

            interp.exec("req.nics.append(anic)");
        }

    }

    // PyString does not handle nulls for you, nor does it work
    // with setting object.attributes apparently.

    // This assumes the variable is initialized as None already
    // and does not need to set that if given String is null
    protected static void setString(String pyname,
                                  String str,
                                  PythonInterpreter interp) {
        if (str != null) {
            interp.exec(pyname + " = '" +  str + "'");
        }
    }

    /**
     * CODE
     */

    // advantage in creating Java objects and using java2py?
    // Although, these are input only data structures and simple
    // enough to deal with this way.

    protected static final String NEW_REQUEST = "req = REQ()";
    protected static final String NEW_VOMS = "voms = VOMS()";
    protected static final String NEW_SHIB = "shib = SHIB()";
    protected static final String NEW_SAMLATTR = "attr = SAMLATTR()";
    protected static final String NEW_NIC = "anic = NIC()";

    // non-stateful code to init environment
    private static final String INIT_CODE =
            "INDETERMINATE = 0                        \n" +
            "DENY = 1                                 \n" +
            "PERMIT = 2                               \n" +
            "                                         \n" +
            "class VOMS:                              \n" +
            "    def __init__(self):                  \n" +
            "        self.VO = None                   \n" +
            "        self.hostport = None             \n" +
            "        self.attributes = []             \n" +
            "                                         \n" +
            "class SAMLATTR:                          \n" +
            "    def __init__(self):                  \n" +
            "        self.name = None                 \n" +
            "        self.namespace = None            \n" +
            "        self.values = []                 \n" +
            "                                         \n" +
            "class SHIB:                              \n" +
            "    def __init__(self):                  \n" +
            "        self.attributes = []             \n" +
            "                                         \n" +
            "class NIC:                               \n" +
            "    def __init__(self):                  \n" +
            "        self.ip = None                   \n" +
            "        self.name = None                 \n" +
            "        self.association = None          \n" +
            "        self.hostname = None             \n" +
            "        self.gateway = None              \n" +
            "        self.method = None               \n" +
            "                                         \n" +
            "class REQ:                               \n" +
            "    def __init__(self):                  \n" +
            "        self.memory = -1                 \n" +
            "        self.nics = []                   \n" +
            "        self.duration_secs = -1          \n" +
            "        # root image is index 0:         \n" +
            "        self.images = []                 \n" +
            "        self.kernel = None               \n" +
            "        self.kernelParams = None         \n" +
            "                                         \n";
}
