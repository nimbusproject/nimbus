/*
 * Copyright 1999-2010 University of Chicago
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

package org.globus.workspace.scheduler.defaults;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Date;

public class BackfillNode implements Serializable {
    private int vmid;
    private Date startDate;
    private String hostname;

    public BackfillNode(int vmid,
                        Date startDate,
                        String hostname) {

        this.vmid = vmid;

        if (startDate == null) {
            throw new IllegalArgumentException("startDate may not be null");
        }
        this.startDate = startDate;

        if (hostname == null) {
            throw new IllegalArgumentException("hostname may not be null");
        }
        this.hostname = hostname;
    }

    public int getVMID() {
        return this.vmid;
    }

    public Date getStartDate() {
        return this.startDate;
    }

    public String getHostname() {
        return this.hostname;
    }

    public void setVMID(int vmid) {
        this.vmid = vmid;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String toString() {
        return "BackfillNode{vmid=" + Integer.toString(this.vmid) +
               ", startDate=" + this.startDate.toString() +
               ", hostname=" + this.hostname + "}";
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    private void readObject(ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        in.defaultReadObject();
    }
}