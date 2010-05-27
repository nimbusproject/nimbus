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

package org.globus.workspace.groupauthz;

import org.globus.workspace.service.binding.vm.VirtualMachine;
import org.globus.workspace.service.binding.vm.VirtualMachineDeployment;
import org.globus.workspace.service.binding.vm.VirtualMachinePartition;
import org.globus.workspace.service.binding.authorization.Decision;
import org.nimbustools.api.services.rm.AuthorizationException;
import org.nimbustools.api.services.rm.ResourceRequestDeniedException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.security.NoSuchAlgorithmException;
import java.net.URI;
import java.net.URISyntaxException;

public class DecisionLogic {

    private static final Log logger = LogFactory.getLog(Group.class.getName());

    /**
     * @param dn may not be null
     * @param rights may not be null
     * @param bindings may not be null, can be zerolen array
     * @param elapsedMins may not be null or negative
     * @param reservedMins may not be null or negative
     * @param numWorkspaces number of OTHER, current workspaces
     * @return Decision Integer
     * @throws AuthorizationException processing problem
     * @throws ResourceRequestDeniedException error w/ explanation to client
     */
    public static Integer decide(String dn,
                                 GroupRights rights,
                                 VirtualMachine[] bindings,
                                 Long elapsedMins,
                                 Long reservedMins,
                                 int numWorkspaces)

            throws AuthorizationException,
                   ResourceRequestDeniedException {

        if (!HashUtil.isInitialized()) {
            throw new AuthorizationException("Cannot give an authorization " +
                    "decision without a properly initialized hashing system");
        }

        if (dn == null) {
            throw new IllegalArgumentException("dn is null");
        }

        if (rights == null) {
            throw new IllegalArgumentException("rights is null");
        }

        if (bindings == null) {
            throw new IllegalArgumentException("bindings is null");
        }

        if (elapsedMins == null) {
            throw new IllegalArgumentException("elapsedMins is null");
        } else if (elapsedMins.longValue() < 0) {
            throw new IllegalArgumentException("elapsedMins is negative");
        }

        if (reservedMins == null) {
            throw new IllegalArgumentException("reservedMins is null");
        } else if (reservedMins.longValue() < 0) {
            throw new IllegalArgumentException("reservedMins is negative");
        }

        final StringBuffer buf = new StringBuffer("\n\nConsidering caller: '");
        buf.append(dn)
           .append("'.\nCurrent elapsed minutes: ")
           .append(elapsedMins)
           .append(".\nCurrent reserved minutes: ")
           .append(reservedMins)
           .append(".\nNumber of VMs in request: ")
           .append(bindings.length)
           .append(".\nNumber of VMs caller is already currently running: ")
           .append(numWorkspaces)
           .append(".\nRights:\n")
           .append(rights)
           .append("\n\n");

        final int maxCurrentPolicy = (int) rights.getMaxWorkspaceNumber();
        if (maxCurrentPolicy > 0) {

            if (numWorkspaces + bindings.length > maxCurrentPolicy) {

                final StringBuffer newbuf = 
                        new StringBuffer("\nDenied: Request for ");
                newbuf.append(bindings.length)
                      .append(" workspaces");

                if (numWorkspaces != 0) {
                    newbuf.append(", together with number of currently " +
                                  "running workspaces (")
                          .append(numWorkspaces)
                          .append("),");
                }

                newbuf.append(" exceeds the maximum, which is ")
                      .append(maxCurrentPolicy)
                      .append(" concurrently running workspaces.");

                final String msg = newbuf.toString();
                buf.append(msg);
                logger.warn(buf.toString());
                throw new ResourceRequestDeniedException(msg);
            }
        }

        long requestDur = 0;
        for (int i = 0; i < bindings.length; i++) {

            final VirtualMachineDeployment dep = bindings[i].getDeployment();
            if (dep == null) {
                final String msg = "ERROR: No deployment information in " +
                        "binding, can't make decision.";
                buf.append(msg);
                logger.error(buf.toString());
                throw new AuthorizationException(msg);
            }

            final long seconds = dep.getMinDuration();
            requestDur += seconds / 60;
        }

        if (bindings.length > 1) {
            buf.append("Duration total of all requests in group: ");
        } else {
            buf.append("Duration request: ");
        }

        buf.append(requestDur)
           .append("\n");

        // zero or below means no check should be made
        if (rights.getMaxCPUs() > 0) {
            final long maxCPUs = rights.getMaxCPUs();
            for (int i = 0; i < bindings.length; i++) {

                final VirtualMachineDeployment dep = bindings[i].getDeployment();
                if (dep == null) {
                    final String msg = "ERROR: No deployment information in " +
                            "binding, can't make decision.";
                    buf.append(msg);
                    logger.error(buf.toString());
                    throw new AuthorizationException(msg);
                }
                final long currentCPUs = dep.getIndividualCPUCount();
                if (currentCPUs > maxCPUs) {

                    buf.append("\nDenied: Requested CPU count (")
                       .append(currentCPUs)
                       .append(") + is greater or equal to maximum CPU count (")
                       .append(maxCPUs)
                       .append(").\n");

                    logger.warn(buf.toString());

                    throw new ResourceRequestDeniedException(
                                "You requested too many CPUs (" +
                                        currentCPUs + "), the " +
                                        "maximum is " +
                                        maxCPUs + " CPUs.");
                    }
            }
        }

        // zero or below means no check should be made
        if (rights.getMaxReservedMinutes() > 0) {
            final long max = rights.getMaxReservedMinutes();
            final long current = reservedMins.longValue();
            if (requestDur + current > max) {

                buf.append("\nDenied: Request duration (")
                   .append(requestDur)
                   .append(") + current reserved tally (")
                   .append(current)
                   .append(") + is greater or equal to maximum reserved (")
                   .append(max)
                   .append(").\n");

                logger.warn(buf.toString());

                throw new ResourceRequestDeniedException(
                            "Your request is for too much time (" +
                                    requestDur + "), the " +
                                    "maximum reserved at once is " +
                                    max + " minutes.  You currently have " +
                                    current + " other reserved minutes.");
            }
        }

        // zero or below means no check should be made
        if (rights.getMaxElapsedReservedMinutes() > 0) {
            final long max = rights.getMaxElapsedReservedMinutes();
            final long currentElapsed = elapsedMins.longValue();
            final long currentReserved = reservedMins.longValue();
            final long tally = currentElapsed + currentReserved;
            if (requestDur + tally > max) {

                buf.append("\nDenied: Request duration (")
                   .append(requestDur)
                   .append(") + current reserved+elapsed tally (")
                   .append(tally)
                   .append(") + is greater or equal to maximum reserved+elapsed (")
                   .append(max)
                   .append(").\n");

                logger.warn(buf.toString());

                throw new ResourceRequestDeniedException(
                            "Your request is for too much time (" +
                                requestDur + "), this would exceed the " +
                                "maximum you can have both used in the " +
                                "past and have reserved currently. " +
                                "This maximum is " +
                                max + " minutes.  You currently have " +
                                currentElapsed + " elapsed minutes " +
                                "and " + currentReserved +
                                " reserved minutes and the request for " +
                                requestDur + " minutes would exceed this.");
            }
        }

        final String dnhash;
        if (rights.isDirHashMode()) {
            try {
                dnhash = HashUtil.hashDN(dn);
            } catch (NoSuchAlgorithmException e) {
                final String msg = "ERROR: DN hash required but it " +
                        "is not available: " + e.getMessage();
                buf.append(msg);
                logger.error(buf.toString());
                throw new AuthorizationException(msg);
            }
        } else {
            dnhash = null;
        }

        for (int i = 0; i < bindings.length; i++) {

            final VirtualMachinePartition[] parts =
                                bindings[i].getPartitions();

            if (parts == null) {
               final String msg = "ERROR: No partition information in " +
                        "binding, can't make decision.";
                buf.append(msg);
                logger.error(buf.toString());
                throw new AuthorizationException(msg);
            }

            checkImages(parts, rights, buf, dnhash);
        }

        buf.append("\n");
        logger.info(buf.toString());
        return Decision.PERMIT;
    }

    private static void checkImages(VirtualMachinePartition[] parts,
                                    GroupRights rights,
                                    StringBuffer buf,
                                    String dnhash)

            throws AuthorizationException,
                   ResourceRequestDeniedException {

        final String basedir = rights.getImageBaseDirectory();
        final String comparisonDir = normalize(basedir, buf);
        if (basedir != null && !basedir.equals(comparisonDir)) {
            logger.debug("Configured base directory policy normalized from '" +
                    basedir + "' into '" + comparisonDir + "'");
        }

        final String hostname = rights.getImageNodeHostname();

        String subdir = null;
        if (dnhash != null) {
            subdir = comparisonDir + "/" + dnhash;
        }

        for (int i = 0; i < parts.length; i++) {

            if (!parts[i].isPropRequired() && !parts[i].isUnPropRequired()) {
                logger.debug("groupauthz not examining '" +
                        parts[i].getImage() + "': no prop/unprop needed");
                continue;
            }

            final String imgString = parts[i].getImage();
            final String altTargetString = parts[i].getAlternateUnpropTarget();
            try {

                final URI imgURI = new URI(imgString);
                URI altTargetURI = null;
                if (altTargetString != null) {
                    altTargetURI = new URI(altTargetString);
                }

                if (hostname != null) {
                    checkNodeHostname(hostname, buf, imgURI, altTargetURI);
                }

                if (basedir != null) {
                    checkNodeBasedir(comparisonDir,
                                     buf,
                                     imgURI,
                                     altTargetURI);
                }

                if (subdir != null) {
                    checkNodeBasedir(subdir,
                                     buf,
                                     imgURI,
                                     altTargetURI);
                }



            } catch (URISyntaxException e) {
                final String msg = "ERROR: Partition in " +
                    "binding is not a valid URI? Can't make decision. " +
                        " Error message: " + e.getMessage();
                buf.append(msg);
                logger.error(buf.toString());
                throw new AuthorizationException(msg);
            }
        }
    }

    private static void checkNodeBasedir(String path,
                                         StringBuffer buf,
                                         URI imgURI,
                                         URI altTargetURI)
            throws ResourceRequestDeniedException {

        if (!imgURI.getPath().startsWith(path)) {

            buf.append("Request denied because image path in request ('")
               .append(imgURI.getPath())
               .append("') does not start with directory in rights ('")
               .append(path)
               .append("').\n");
            logger.warn(buf.toString());
            throw new ResourceRequestDeniedException(
                    "You may only use images under directory '" + path + "'");
        }

        if (altTargetURI != null &&
                !altTargetURI.getPath().startsWith(path)) {

            buf.append("Request denied because alternate target" +
                    " image path in request ('")
               .append(altTargetURI.getHost())
               .append("') does not start with directory in rights ('")
               .append(path)
               .append("').\n");
            logger.warn(buf.toString());
            throw new ResourceRequestDeniedException(
                    "You may only save images to alternate " +
                            "locations starting with base directory '" +
                            path + "'");
        }
    }

    private static void checkNodeHostname(String hostname,
                                          StringBuffer buf,
                                          URI imgURI,
                                          URI altTargetURI)
            throws ResourceRequestDeniedException {
        
        if (!hostname.equalsIgnoreCase(imgURI.getHost())) {

            buf.append("Request denied because image node in request ('")
               .append(imgURI.getHost())
               .append("') does not match image node in rights ('")
               .append(hostname)
               .append("').\n");
            logger.warn(buf.toString());
            throw new ResourceRequestDeniedException(
                    "You may only use images from host '" + hostname + "'");
        }

        if (altTargetURI != null &&
                !imgURI.getHost().equalsIgnoreCase(
                                    altTargetURI.getHost())) {

            buf.append("Request denied because alternate target" +
                    " image node in request ('")
               .append(altTargetURI.getHost())
               .append("') does not match approved image source " +
                       "node in request ('")
               .append(imgURI.getHost())
               .append("').\n");
            logger.warn(buf.toString());
            throw new ResourceRequestDeniedException(
                    "You may only save images to alternate " +
                            "locations on host '" + hostname + "'");
        }
    }

    public static Integer checkNewAltTargetURI(GroupRights rights,
                                               URI altTargetURI,
                                               String dn)
            throws AuthorizationException {

        if (altTargetURI == null) {
            throw new IllegalArgumentException("altTargetURI may not be null");
        }

        if (rights == null) {
            throw new IllegalArgumentException("rights may not be null");
        }

        if (dn == null) {
            throw new IllegalArgumentException("DN may not be null");
        }

        final String dnhash;
        if (rights.isDirHashMode()) {
            try {
                dnhash = HashUtil.hashDN(dn);
            } catch (NoSuchAlgorithmException e) {
                final String msg = "ERROR: DN hash required but it " +
                        "is not available: " + e.getMessage();
                throw new AuthorizationException(msg);
            }
        } else {
            dnhash = null;
        }

        final String hostname = rights.getImageNodeHostname();
        if (hostname != null) {
            if (!hostname.equalsIgnoreCase(altTargetURI.getHost())) {
                throw new AuthorizationException(
                        "You may only use images from host '" + hostname + "'");
            }
        }

        final String basedir = rights.getImageBaseDirectory();
        if (basedir == null) {
            return Decision.PERMIT; // *** EARLY RETURN ***
        }

        final String comparisonDir;
        
        try {
            comparisonDir = normalize(basedir, null);
        } catch (ResourceRequestDeniedException e) {
            throw new AuthorizationException(e.getMessage(), e);
        }
        
        if (!basedir.equals(comparisonDir)) {
            logger.debug("Configured base directory policy normalized from '" +
                    basedir + "' into '" + comparisonDir + "'");
        }

        String subdir = null;
        if (dnhash != null) {
            subdir = comparisonDir + "/" + dnhash;
        }

        if (!altTargetURI.getPath().startsWith(comparisonDir)) {

            throw new AuthorizationException(
                    "You may only save images to alternate " +
                            "locations starting with base directory '" +
                            comparisonDir + "'");
        }

        if (subdir != null &&
                !altTargetURI.getPath().startsWith(subdir)) {

            throw new AuthorizationException(
                    "You may only save images to alternate " +
                            "locations starting with base directory '" +
                            subdir + "'");
        }

        return Decision.PERMIT;
    }

    static String normalize(String input, StringBuffer buf)
            throws ResourceRequestDeniedException {

        if (input == null) {
            return "";
        }

        if (input.trim().length() == 0) {
            return "";
        }

        final String prefix = "Input path '" + input + "' contains " +
                    "illegal relative path construction";

        final String[] parts = input.split("/");
        for (int i = 0; i < parts.length; i++) {

            String msg = null;
            if (parts[i].equals(".")) {
                msg = prefix + " '/./' (it's benign but unhandled in " +
                        "matching code)";
            } else if (parts[i].equals("..")) {
                msg = prefix + " '/../'";
            }

            if (msg != null) {
                if (buf != null) {
                    buf.append(msg);
                    logger.error(buf.toString());
                }
                throw new ResourceRequestDeniedException(msg);
            }
        }

        final StringBuffer newbuf = new StringBuffer(input.length());

        final char[] chars = input.trim().toCharArray();

        boolean lastWasSeparator = false;
        for (int i = 0; i < chars.length; i++) {
            if (lastWasSeparator && chars[i] == '/') {
                continue;
            } else if (lastWasSeparator) {
                lastWasSeparator = false;
            } else if (chars[i] == '/') {
                lastWasSeparator = true;
            }
            newbuf.append(chars[i]);
        }

        return newbuf.toString();
    }

    static void testNormalize() throws Exception {

        final String[] inputs =
                {"///abc/def/g", "/abc/def//g", "/abc/def///", "  ", null, "/asd\n"};
        final String[] expects =
                {"/abc/def/g",   "/abc/def/g",  "/abc/def/",   "",   "",   "/asd"};

        for (int i = 0; i < inputs.length; i++) {
            final String ret = normalize(inputs[i], null);
            if (!ret.equals(expects[i])) {
                throw new Exception("'" + ret + "' from '" + inputs[i] + "'");
            }
        }

        final String[] allShouldFail =
                {"/../abc/def/g", "/abc/def//g/../asd", "/abc/def///../..//"};

        for (int i = 0; i < allShouldFail.length; i++) {
            try {
                normalize(allShouldFail[i], null);
            } catch (ResourceRequestDeniedException e) {
                continue;
            }
            throw new Exception("'" + allShouldFail[i] + "' did not fail");
        }

    }

    public static void main(String[] args) throws Exception {
        testNormalize();
    }
}
