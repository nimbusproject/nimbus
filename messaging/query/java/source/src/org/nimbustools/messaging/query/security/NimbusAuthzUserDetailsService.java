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
package org.nimbustools.messaging.query.security;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nimbus.authz.AuthzDBAdapter;
import org.nimbus.authz.AuthzDBException;
import org.nimbus.authz.UserAlias;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;

public class NimbusAuthzUserDetailsService
        implements QueryUserDetailsService, InitializingBean {

    private static final Log logger =
            LogFactory.getLog(NimbusAuthzUserDetailsService.class.getName());

    private AuthzDBAdapter authzDBAdapter;


    public QueryUser loadUserByUsername(String username)
            throws UsernameNotFoundException, DataAccessException {

        if (username == null) {
            throw new IllegalArgumentException("username may not be null");
        }

        try {
            final String userId = authzDBAdapter.getCanonicalUserIdFromS3(username);
            final List<UserAlias> aliasList = authzDBAdapter.getUserAliases(userId);

            String secret = null;
            String dn = null;

            for (UserAlias alias : aliasList) {
                final int type = alias.getAliasType();
                final String name = alias.getAliasName();
                if (type == AuthzDBAdapter.ALIAS_TYPE_S3 &&
                        username.equals(name)) {
                    if (secret == null) {
                        secret = alias.getAliasTypeData();
                    } else {

                        final boolean match = secret.equals(alias.getAliasTypeData());
                        final String secretState = match ? "Secrets match." : "Secrets don't match.";
                        logger.warn(
                                String.format("Found multiple query user aliases for canonical user %s. %s", 
                                        userId, secretState));
                    }
                } else if (type == AuthzDBAdapter.ALIAS_TYPE_DN) {
                    if (dn == null) {
                        dn = name;
                    } else {
                        logger.warn(String.format(
                                "Found multiple DN user aliases for canonical user %s('%s' and '%s')",
                                userId, dn, name));
                    }
                }
            }

            if (secret == null || dn == null) {
                throw new UsernameNotFoundException("User record is missing or incomplete");
            }

            return new QueryUser(username,secret, dn);


        } catch (AuthzDBException e) {
            throw new UsernameNotFoundException("Failed to retrieve credentials for access ID " + username, e);
        }
    }

    public QueryUser loadUserByDn(String dn)
            throws UsernameNotFoundException, DataAccessException {

        if (dn == null) {
            throw new IllegalArgumentException("dn may not be null");
        }

        try {
            final String userId = authzDBAdapter.getCanonicalUserIdFromDn(dn);
            final List<UserAlias> aliasList = authzDBAdapter.getUserAliases(userId);

            String accessId = null;
            String secret = null;

            for (UserAlias alias : aliasList) {
                if (alias.getAliasType() == AuthzDBAdapter.ALIAS_TYPE_S3) {
                    if (accessId == null) {
                        secret = alias.getAliasTypeData();
                        accessId = alias.getAliasName();
                    } else {
                        logger.warn(String.format(
                                        "Found multiple query user aliases for canonical user %s. Using the first one (%s)",
                                        userId, accessId));
                    }
                }
            }

            if (secret == null || accessId == null) {
                throw new UsernameNotFoundException("User DN '" + dn + "' does not map to query credentials");
            }

            return new QueryUser(accessId, secret, dn);


        } catch (AuthzDBException e) {
            throw new UsernameNotFoundException("Failed to retrieve query credentials for DN '" + dn + "'", e);
        }
    }

    public void afterPropertiesSet() throws Exception {
        if (this.authzDBAdapter == null) {
            throw new IllegalArgumentException("authzDBAdapter may not be null");
        }
    }

    public AuthzDBAdapter getAuthzDBAdapter() {
        return authzDBAdapter;
    }

    public void setAuthzDBAdapter(AuthzDBAdapter authzDBAdapter) {
        this.authzDBAdapter = authzDBAdapter;
    }
}
