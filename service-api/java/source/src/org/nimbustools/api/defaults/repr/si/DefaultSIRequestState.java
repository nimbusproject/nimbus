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

package org.nimbustools.api.defaults.repr.si;

import org.nimbustools.api._repr.si._SIRequestState;

public class DefaultSIRequestState implements _SIRequestState {

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    private String state;
    private Throwable problem;

    
    // -------------------------------------------------------------------------
    // implements org.nimbustools.api.repr.si.SIRequestState
    // -------------------------------------------------------------------------

    public String getStateStr() {
        return this.state;
    }

    public Throwable getProblem() {
        return this.problem;
    }
    
    
    // -------------------------------------------------------------------------
    // implements org.nimbustools.api._repr.si._SIRequestState
    // -------------------------------------------------------------------------

    public void setState(String state) {
        this.state = state;
    }

    public void setProblem(Throwable e) {
        this.problem = e;
    }


    // -------------------------------------------------------------------------
    // DEBUG STRING
    // -------------------------------------------------------------------------


    public String toString() {
        return "DefaultState{" +
                "state='" + this.state + '\'' +
                ", problem=" + excString(this.problem) +
                "}";
    }

    private static String excString(Throwable e) {
        if (e == null) {
            return "null";
        }

        final String msg = recurseForSomeString(e);
        if (msg == null) {
            return "[no exception message found, class: '" +
                            e.getClass().toString() + "']";
        } else {
            return "'" + msg + "'";
        }
    }

    private static String recurseForSomeString(Throwable exc) {
        Throwable t = exc;
        while (true) {
            if (t == null) {
                return null;
            }
            final String msg = t.getMessage();
            if (msg != null) {
                return msg;
            }
            t = t.getCause();
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((problem == null) ? 0 : problem.hashCode());
        result = prime * result + ((state == null) ? 0 : state.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DefaultSIRequestState other = (DefaultSIRequestState) obj;
        if (problem == null) {
            if (other.problem != null)
                return false;
        } else if (!problem.equals(other.problem))
            return false;
        if (state == null) {
            if (other.state != null)
                return false;
        } else if (!state.equals(other.state))
            return false;
        return true;
    }
    
    
}
