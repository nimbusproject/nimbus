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

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.List;
import java.util.Collections;
import java.util.ArrayList;

public class QueryUser implements UserDetails {

    private String accessID;
    private String secret;
    private String dn;
    private List<GrantedAuthority> authorities;

    public QueryUser(String accessID, String secret, String dn) {

        if (accessID == null || accessID.length() == 0) {
            throw new IllegalArgumentException("accessID may not be null or empty");
        }

        if (secret == null || secret.length() == 0) {
            throw new IllegalArgumentException("secret may not be null or empty");
        }

        if (dn == null || dn.length() == 0) {
            throw new IllegalArgumentException("dn may not be null or empty");
        }

        this.accessID = accessID;
        this.secret = secret;
        this.dn = dn;

        //TODO need to use this business
        final ArrayList<GrantedAuthority> list = new ArrayList<GrantedAuthority>();
        list.add(new GrantedAuthority() {
            public String getAuthority() {
                return "ROLE_USER";
            }
        });
        this.authorities = Collections.unmodifiableList(list);
    }

    public Collection<GrantedAuthority> getAuthorities() {
        return authorities;
    }

    public String getAccessID() {
        return accessID;
    }

    public String getSecret() {
        return secret;
    }

    public String getDn() {
        return dn;
    }

    public String getPassword() {
        return secret;
    }

    public String getUsername() {
        return accessID;
    }

    public boolean isAccountNonExpired() {
        return true;
    }

    public boolean isAccountNonLocked() {
        return true;
    }

    public boolean isCredentialsNonExpired() {
        return true;
    }

    public boolean isEnabled() {
        return true;
    }

    @Override
    public String toString() {
        return "QueryUser{" +
                "accessID='" + accessID + '\'' +
                ", dn='" + dn + '\'' +
                '}';
    }
}
