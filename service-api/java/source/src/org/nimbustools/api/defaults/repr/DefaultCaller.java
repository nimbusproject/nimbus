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

package org.nimbustools.api.defaults.repr;

import org.nimbustools.api._repr._Caller;
import javax.security.auth.Subject;

public class DefaultCaller implements _Caller {

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    private String identity;
    private boolean superuser;
    private Subject subject;


    // -------------------------------------------------------------------------
    // implements org.nimbustools.api.repr.Caller
    // -------------------------------------------------------------------------

    public String getIdentity() {
        return this.identity;
    }

    public boolean isSuperUser() {
        return this.superuser;
    }

    public Subject getSubject() {
        return this.subject;
    }

    // -------------------------------------------------------------------------
    // implements org.nimbustools.api._repr._Caller
    // -------------------------------------------------------------------------

    public void setIdentity(String identity) {
        this.identity = identity;
        // this object is not strictly a "bean"
        this.superuser = (identity == null);
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    // -------------------------------------------------------------------------
    // DEBUG STRING
    // -------------------------------------------------------------------------

    public String toString() {
        return "DefaultCaller{" +
                "identity='" + identity + '\'' +
                ", superuser=" + superuser +
                ", subject=" + subject +
                '}';
    }
}
