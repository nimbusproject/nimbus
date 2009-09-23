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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * The generic ProvidedRole class cannot be used to express host and key
 * requirements because these requirements may be different in each
 * of the workspace/group's provided contextualization document's
 * requires section (unlikely but possible).
 */
public class RequiredRole {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------
    
    private static final Log logger =
            LogFactory.getLog(Blackboard.class.getName());


    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    private final ArrayList<Identity> providers = new ArrayList<Identity>(); // list of Identity

    private final String name;
    private final boolean hostnameRequired;
    private final boolean pubkeyRequired;

    // number filled at last getResponsePieces call, even if it was incomplete
    private short numFilled = 0;


    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------

    public RequiredRole(String name,
                 boolean hostnameRequired,
                 boolean pubkeyRequired) {
        this.name = name;
        this.hostnameRequired = hostnameRequired;
        this.pubkeyRequired = pubkeyRequired;
    }

    
    // -------------------------------------------------------------------------
    // MUTATE
    // -------------------------------------------------------------------------

    void clearProviders() {
        synchronized(this.providers) {
            this.providers.clear();
        }
    }

    void addProvider(Identity identity) {
        if (identity == null) {
            return;
        }
        synchronized(this.providers) {
            this.providers.add(identity);
        }
    }


    // -------------------------------------------------------------------------
    // COMPARISON etc
    // -------------------------------------------------------------------------

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RequiredRole that = (RequiredRole) o;

        if (hostnameRequired != that.hostnameRequired) return false;
        if (pubkeyRequired != that.pubkeyRequired) return false;

        // name can not be null
        return name.equals(that.name);
    }

    public int hashCode() {
        // name can not be null
        int result = 16 * name.hashCode();
        result = 16 + result + (hostnameRequired ? 1 : 0);
        result = 16 + result + (pubkeyRequired ? 1 : 0);
        return result;
    }

    public String toString() {
        return "RequiredRole{" +
                "name='" + name + '\'' +
                ", hostnameRequired=" + hostnameRequired +
                ", pubkeyRequired=" + pubkeyRequired +
                '}';
    }

    
    // -------------------------------------------------------------------------
    // ACCESS
    // -------------------------------------------------------------------------

    String getName() {
        return name;
    }

    short getFilledNum() {
        synchronized (this.providers) {
            return this.numFilled;
        }
    }

    short getProviderNum() {
        synchronized(this.providers) {
            return (short) this.providers.size();
        }
    }
    
    boolean isHostnameRequired() {
        return this.hostnameRequired;
    }

    boolean isPubkeyRequired() {
        return this.pubkeyRequired;
    }

    List<Identity> getProviders() {
        synchronized (this.providers) {
            return new ArrayList<Identity>(this.providers);
        }
    }

}
