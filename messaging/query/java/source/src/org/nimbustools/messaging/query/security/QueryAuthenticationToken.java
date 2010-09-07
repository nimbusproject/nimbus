/*
 * Copyright 1999-2009 University of Chicago
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
package org.nimbustools.messaging.query.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.Authentication;

import java.util.Collection;

public class QueryAuthenticationToken implements Authentication {

    //TODO this class is probably not right..

    private QueryUser user;
    private boolean authenticated;

    public QueryAuthenticationToken(QueryUser user, boolean isAuthenticated) {
        if (user == null) {
            throw new IllegalArgumentException("user may not be null");
        }
        this.user = user;
        this.authenticated = isAuthenticated;
    }

    public Collection<GrantedAuthority> getAuthorities() {
        return user.getAuthorities();
    }

    public Object getCredentials() {
        return user.getSecret();
    }

    public Object getDetails() {
        return null;
    }

    public QueryUser getPrincipal() {
        return user;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        throw new IllegalArgumentException("Must be set via constructor");
    }

    public String getName() {
        return user.getDn();
    }
}
