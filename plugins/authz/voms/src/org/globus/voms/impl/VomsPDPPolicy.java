/*
 * Copyright 1999-2007 University of Chicago
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

package org.globus.voms.impl;

import java.util.ArrayList;
import java.util.Iterator;

import java.io.IOException;
import java.io.Serializable;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class VomsPDPPolicy implements Serializable {

    private ArrayList DNs = null; //todo: Set?
    private ArrayList attrs = null;

    public VomsPDPPolicy(String[] _DNs, String[] _attrs) {
        this.DNs = new ArrayList();
        if (_DNs != null) {
            for (int i = 0; i < _DNs.length; i++) {
                this.DNs.add(_DNs[i]);
            }
        }
        this.attrs = new ArrayList();
        if (_attrs != null) {
            for (int i = 0; i < _attrs.length; i++) {
                this.attrs.add(_attrs[i]);
            }
        }
    }
    // encapsulate details of storage method
    public Iterator getDNs() {
        return this.DNs.iterator();
    }

    public Iterator getAttrs() {
        return this.attrs.iterator();
    }

    /**
     * @param DN The DN to add to policy.
     * @return true if policy was altered.
     */
    public boolean addDN(String DN) {
        if (!this.DNs.contains(DN)) {
            this.DNs.add(DN);
            return true;
        }
        return false;
    }

    /**
     * @param DN The DN to remove from policy.
     * @return true if policy was altered.
     */
    public boolean removeDN(String DN) {
        return this.DNs.remove(DN);
    }

    /**
     * @param attr The attribute to add to policy.
     * @return true if policy was altered.
     */
    public boolean addAttr(String attr) {
        if (!this.attrs.contains(attr)) {
            this.attrs.add(attr);
            return true;
        }
        return false;
    }

    /**
     * @param attr The attribute to remove from policy.
     * @return true if policy was altered.
     */
    public boolean removeAttr(String attr) {
        return this.attrs.remove(attr);
    }
        
    /**
     * Delete all entries.
     */
    public void clearPolicy() {
        this.DNs.clear();
        this.attrs.clear();
    }

    public void setDNs(ArrayList DNs) {
        this.DNs = DNs;
    }
    
    public void setAttrs(ArrayList attrs) {
        this.attrs = attrs;
    }
    protected void readObject(ObjectInputStream ois) 
        throws IOException, ClassNotFoundException {

        this.DNs = (ArrayList)ois.readObject();
        this.attrs = (ArrayList)ois.readObject();
    }

    protected void writeObject(ObjectOutputStream oos) throws IOException {

        oos.writeObject(this.DNs);
        oos.writeObject(this.attrs);
    }
}
