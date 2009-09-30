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

package org.globus.workspace.service.binding;

public abstract class WorkspaceInstantiation {
    
    protected String name;
    protected Integer id = new Integer(-1);

    protected boolean propagateStartOK = true;
    protected boolean propagateRequired;
    protected boolean unPropagateRequired;

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getID() {
        return this.id;
    }

    public void setID(int id) {
        this.id = new Integer(id);
    }

    public void setID(Integer id) {
        this.id = id;
    }

    public abstract int getRequestedShutdownMechanism();

    public boolean isPropagateRequired() {
        return this.propagateRequired;
    }

    public void setPropagateRequired(boolean required) {
        this.propagateRequired = required;
    }

    public boolean isUnPropagateRequired() {
        return this.unPropagateRequired;
    }

    public void setUnPropagateRequired(boolean required) {
        this.unPropagateRequired = required;
    }

}
