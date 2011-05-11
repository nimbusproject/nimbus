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

import org.globus.workspace.service.binding.authorization.CreationAuthorizationCallout;
import org.globus.workspace.service.binding.authorization.PostTaskAuthorization;
import org.globus.workspace.service.binding.vm.VirtualMachine;
import org.nimbustools.api.services.rm.AuthorizationException;
import org.nimbustools.api.services.rm.ResourceRequestDeniedException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ResourceLoader;

import javax.security.auth.Subject;
import java.io.File;
import java.net.URI;

public class GroupAuthz implements CreationAuthorizationCallout,
                                   PostTaskAuthorization,
                                   ResourceLoaderAware {

    public boolean isEnabled() {
        return true;
    }

    private static final Log logger =
            LogFactory.getLog(GroupAuthz.class.getName());

    private static final String NO_POLICIES_MESSAGE = 
            "There are no authorization policies in place for you which is " +
                      "unexpected, please contact administrator.";

    private final Group[] groups = new Group[15];

    private ResourceLoader loader;

    // set via config, these are paths to files that list DNs
    private String group01;
    private String group02;
    private String group03;
    private String group04;
    private String group05;
    private String group06;
    private String group07;
    private String group08;
    private String group09;
    private String group10;
    private String group11;
    private String group12;
    private String group13;
    private String group14;
    private String group15;

    // set via config, these are paths to properties files that
    // define groups, see GroupRights
    private String def01;
    private String def02;
    private String def03;
    private String def04;
    private String def05;
    private String def06;
    private String def07;
    private String def08;
    private String def09;
    private String def10;
    private String def11;
    private String def12;
    private String def13;
    private String def14;
    private String def15;

    protected DecisionLogic theDecider = new DecisionLogic();

    public void setGroup01(String group01) {
        this.group01 = group01;
    }

    public void setGroup02(String group02) {
        this.group02 = group02;
    }

    public void setGroup03(String group03) {
        this.group03 = group03;
    }

    public void setGroup04(String group04) {
        this.group04 = group04;
    }

    public void setGroup05(String group05) {
        this.group05 = group05;
    }

    public void setGroup06(String group06) {
        this.group06 = group06;
    }

    public void setGroup07(String group07) {
        this.group07 = group07;
    }

    public void setGroup08(String group08) {
        this.group08 = group08;
    }

    public void setGroup09(String group09) {
        this.group09 = group09;
    }

    public void setGroup10(String group10) {
        this.group10 = group10;
    }

    public void setGroup11(String group11) {
        this.group11 = group11;
    }

    public void setGroup12(String group12) {
        this.group12 = group12;
    }

    public void setGroup13(String group13) {
        this.group13 = group13;
    }

    public void setGroup14(String group14) {
        this.group14 = group14;
    }

    public void setGroup15(String group15) {
        this.group15 = group15;
    }

    public void setGroup16(String nope) {
        throw new IllegalAccessError("Only support for 15 groups right now");
    }

    public void setDef01(String def01) {
        this.def01 = def01;
    }

    public void setDef02(String def02) {
        this.def02 = def02;
    }

    public void setDef03(String def03) {
        this.def03 = def03;
    }

    public void setDef04(String def04) {
        this.def04 = def04;
    }

    public void setDef05(String def05) {
        this.def05 = def05;
    }

    public void setDef06(String def06) {
        this.def06 = def06;
    }

    public void setDef07(String def07) {
        this.def07 = def07;
    }

    public void setDef08(String def08) {
        this.def08 = def08;
    }

    public void setDef09(String def09) {
        this.def09 = def09;
    }

    public void setdef10(String def10) {
        this.def10 = def10;
    }

    public void setdef11(String def11) {
        this.def11 = def11;
    }

    public void setdef12(String def12) {
        this.def12 = def12;
    }

    public void setdef13(String def13) {
        this.def13 = def13;
    }

    public void setdef14(String def14) {
        this.def14 = def14;
    }

    public void setdef15(String def15) {
        this.def15 = def15;
    }

    public void setdef16(String nope) {
        throw new IllegalAccessError("Only support for 15 groups right now");
    }

    // not called initialize on purpose in order to not conflict
    // with the various Initializable interfaces
    public void initializeCallout() throws Exception {
        logger.debug("Initializing group authorization");
        this.initGroup(1, this.group01, this.def01);
        this.initGroup(2, this.group02, this.def02);
        this.initGroup(3, this.group03, this.def03);
        this.initGroup(4, this.group04, this.def04);
        this.initGroup(5, this.group05, this.def05);
        this.initGroup(6, this.group06, this.def06);
        this.initGroup(7, this.group07, this.def07);
        this.initGroup(8, this.group08, this.def08);
        this.initGroup(9, this.group09, this.def09);
        this.initGroup(10, this.group10, this.def10);
        this.initGroup(11, this.group11, this.def11);
        this.initGroup(12, this.group12, this.def12);
        this.initGroup(13, this.group13, this.def13);
        this.initGroup(14, this.group14, this.def14);
        this.initGroup(15, this.group15, this.def15);
    }

    private void initGroup(int num, String path, String defpath)
            throws Exception {

        if (path == null && defpath == null) {
            this.set(num, null);
            return;
        }
        if (path != null && defpath == null) {
            throw new Exception("Group identity file for group #" +
                    num + " found ('" + path + "') but no matching group " +
                    "definition path was configured");
        }
        if (path == null) {
            throw new Exception("Group definition file for group #" +
                    num + " found ('" + defpath + "') but no matching group " +
                    "identity file path was configured");
        }

        final String numString = "Authorization Group #" + num;

        final File policyPath = this.loader.getResource(path).getFile();
        final File memberPath = this.loader.getResource(defpath).getFile();

        if (!policyPath.exists() && !memberPath.exists()) {
            logger.debug(numString + " is not configured.");
            return; // *** EARLY RETURN ***
        }

        if (!policyPath.exists()) {
            throw new Exception(numString +
                    " is missing its policy definition file.");
        }

        if (!memberPath.exists()) {
            throw new Exception(numString +
                    " is missing its member definition file.");
        }

        final Group group =
                new Group(policyPath, memberPath, numString);

        // jump start it
        group.identityRights(" ");

        if (group.getName() != null) {
            logger.debug(numString + " = " + group.getName());
        } else {
            logger.debug(numString + " had no name property configured");
        }

        this.set(num, group);
    }

    private void set(int num, Group group) {
        this.groups[num-1] = group; // value may be null
    }

    public Integer isPermitted(VirtualMachine[] bindings,
                               String callerDN,
                               Subject subject,
                               Long elapsedMins,
                               Long reservedMins,
                               int numWorkspaces,
                               double chargeRatio)

            throws AuthorizationException,
                   ResourceRequestDeniedException {

        if (elapsedMins == null || reservedMins == null) {
            throw new AuthorizationException(
                    "This authorization module requires " +
                            "elapsed and reserved values");
        }

        if (bindings == null) {
            throw new AuthorizationException(
                    "This authorization module requires bindings, ability " +
                            "to examine request specifics.  " +
                            "Please contact administrator.");
        }

        // there may be null values in this.groups, see getRights method
        for (int i = 0; i < this.groups.length; i++) {

            final GroupRights rights = getRights(callerDN, this.groups[i]);

            // only first inclusion of DN is considered
            if (rights != null) {
                return theDecider.decide(callerDN,
                                            rights,
                                            bindings,
                                            elapsedMins,
                                            reservedMins,
                                            numWorkspaces,
                                            chargeRatio);
            }
        }

        logger.error("NOT IN ANY GROUP: '" + callerDN + "'");

        // administrator put DN in grid-mapfile but not in a group...
        throw new ResourceRequestDeniedException(NO_POLICIES_MESSAGE);
    }



    private static GroupRights getRights(String dn, Group group) {
        if (dn == null || group == null) {
            return null;
        }
        return group.identityRights(dn);
    }

    public Integer isRootPartitionUnpropTargetPermitted(URI target,
                                                        String caller)
            throws AuthorizationException {

        // there may be null values in this.groups, see getRights method
        for (int i = 0; i < this.groups.length; i++) {

            final GroupRights rights = getRights(caller, this.groups[i]);
            // only first inclusion of DN is considered
            if (rights != null) {
                return  theDecider.checkNewAltTargetURI(rights,
                                                          target,
                                                          caller);                
            }
        }

        logger.error("NOT IN ANY GROUP: '" + caller + "'");

        // administrator put DN in grid-mapfile but not in a group...
        throw new AuthorizationException(NO_POLICIES_MESSAGE);
    }

    public String getGroupName(String caller) {


        for (int i = 0; i < this.groups.length; i++) {

            final GroupRights rights = getRights(caller, this.groups[i]);
            // only first inclusion of DN is considered
            if (rights != null) {
                return this.groups[i].getName();
            }
        }

        return null;
    }


    // -------------------------------------------------------------------------
    // FOR CLOUD AUTOCONFIG
    // -------------------------------------------------------------------------

    public Group[] getGroups() {
        return this.groups;
    }


    // -------------------------------------------------------------------------
    // implements ResourceLoaderAware
    // -------------------------------------------------------------------------
    
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.loader = resourceLoader;
    }
}
