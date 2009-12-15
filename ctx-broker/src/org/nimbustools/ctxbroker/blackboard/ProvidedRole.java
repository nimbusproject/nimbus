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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nimbustools.ctxbroker.Identity;

import java.util.ArrayList;
import java.util.Iterator;

public class ProvidedRole {

    private static final Log logger =
            LogFactory.getLog(Blackboard.class.getName());

    private final String name;

    private final ArrayList<Identity> providers = new ArrayList<Identity>(); // list of Identity
    private final Object listLock = new Object();

    public ProvidedRole(String name) {
        if (name == null) {
            throw new IllegalArgumentException("name can not be null");
        }
        this.name = name;
    }
    
    public String getName() {
        return this.name;
    }

    public void addProvider(Identity identity) {

        if (identity == null) {
            return;
        }
        synchronized(this.listLock) {
            this.providers.add(identity);
            if (logger.isTraceEnabled()) {
                logger.trace("Appended provider " + identity + " to " + this);
            }
        }
    }

    public Iterator<Identity> getProviders() {
        synchronized(this.listLock) {
            return this.providers.iterator();
        }
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProvidedRole role = (ProvidedRole) o;

        // name can not be null
        return name.equals(role.name);
    }

    public int hashCode() {
        return (name != null ? name.hashCode() : 0);
    }


    public String toString() {
        return "ProvidedRole{" +
                "name='" + name + '\'' +
                '}';
    }
}
