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

package org.nimbustools.ctxbroker.blackboard;

import org.nimbustools.ctxbroker.Identity;
import org.nimbustools.ctxbroker.ContextBrokerException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

public class Node {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(Blackboard.class.getName());

    private static final String[] EMPTY = new String[0];

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    private boolean allIdentitiesRequired = false;

    private final Integer id;

    private final CtxResult ctxResult = new CtxResult();

    // String interface name --> Identity
    private final Hashtable<String, Identity> identities =
            new Hashtable<String, Identity>();

    // RequiredRole objects
    private final Set<RequiredRole> requiredRoles =
            new HashSet<RequiredRole>();

    // data names
    private final String[] requiredDatas;

    
    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------

    Node(Integer idnum, String[] requiredDatas) {
        if (idnum == null) {
            throw new IllegalArgumentException("idnum may not be null");
        }
        this.id = idnum;
        if (requiredDatas == null || requiredDatas.length == 0) {
            this.requiredDatas = EMPTY;
        } else {
            this.requiredDatas = requiredDatas;
        }
     }


    // -------------------------------------------------------------------------
    // INFORMATION
    // -------------------------------------------------------------------------

    Integer getId() {
        return id;
    }

    Enumeration<Identity> getIdentities() {
        return this.identities.elements();
    }

    Identity getParticularIdentity(String iface) {
        return this.identities.get(iface);
    }

    boolean isAllIdentitiesRequired() {
        return this.allIdentitiesRequired;
    }

    void setAllIdentitiesRequired(boolean allIdentitiesRequired) {
        this.allIdentitiesRequired = allIdentitiesRequired;
    }

    Iterator<RequiredRole> getRequiredRoles() {
        return this.requiredRoles.iterator();
    }

    String[] getRequiredDataNames() {
        return this.requiredDatas;
    }

    CtxResult getCtxResult() {
        return this.ctxResult;
    }
    
    // -------------------------------------------------------------------------
    // ADDITIONS
    // -------------------------------------------------------------------------

    void addIdentity(Identity id) throws ContextBrokerException {
        this.addIdentity(id.getIface(), id);
    }

    synchronized void addIdentity(String iface, Identity id)
            throws ContextBrokerException {

        Identity perhaps = this.identities.get(iface);
        if (perhaps != null) {
            // Binding does not allow different NIC names in the real id's,
            // it must be the provides section's fault
            throw new ContextBrokerException("Duplicate interface found." +
                    "  Identity names in provides section can not be" +
                    " duplicated. [[interface already added: interface = " +
                    iface + ", given Identity = " + id + ", Identity " +
                    "already given for this interface: " + perhaps + "]]");
        }
        this.identities.put(iface, id);
    }


    // Reference added to node specific list as well as Blackboard's list.
    // Only ever called under Blackboard's DB lock during node creation,
    // so no need to be synchronized (HashSet is not synchronized itself).
    // Returns true if this was a new role.
    boolean addRequiredRole(RequiredRole role) {

        if (role == null) {
            throw new IllegalArgumentException("role cannot be null");
        }

        boolean newRole = this.requiredRoles.add(role);

        if (logger.isTraceEnabled()) {
            String msg = "it was ";
            if (!newRole) {
                msg += "not ";
            }
            msg += "a new role.";
            logger.trace("Added RequiredRole " + role + " to Node #" +
                         this.id + "'s required role set: " + msg);
        }

        return newRole;
    }
}
