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

import org.nimbustools.api._repr._Caller;
import org.nimbustools.api.brain.ModuleLocator;
import org.nimbustools.api.repr.Caller;
import org.nimbustools.api.repr.CannotTranslateException;
import org.nimbustools.api.repr.ReprFactory;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.rm.ContainerInterface;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.rmi.RemoteException;

public class QueryContainerInterface implements ContainerInterface {

    private final ReprFactory repr;
    private final QueryUserDetailsService userDetailsService;

    public QueryContainerInterface(ModuleLocator locator, QueryUserDetailsService userDetailsService) {
        if (locator == null) {
            throw new IllegalArgumentException("locator may not be null");
        }
        this.repr = locator.getReprFactory();
        if (this.repr == null) {
            throw new IllegalArgumentException("locator has null reprFactory");
        }

        if (userDetailsService == null) {
            throw new IllegalArgumentException("userDetailsService may not be null");
        }
        this.userDetailsService = userDetailsService;
    }

    public Caller getCaller() throws RemoteException {
        final SecurityContext context = SecurityContextHolder.getContext();


        // these errors should not occur if authentication filters are
        // properly in place

        if (context == null) {
            throw new RemoteException("Could not obtain SecurityContext");
        }
        final Authentication auth = context.getAuthentication();
        if (auth == null || !(auth instanceof QueryAuthenticationToken)) {
            throw new RemoteException("Could not obtain a valid authentication token");
        }

        final QueryAuthenticationToken token = (QueryAuthenticationToken) auth;
        final QueryUser principal = token.getPrincipal();

        if (principal == null) {
            throw new RemoteException("Could not obtain Principal from authentication token");
        }

        final String dn = principal.getDn();
        if (dn == null || dn.length() == 0) {
            throw new RemoteException("Could not obtain a valid DN");
        }

        final _Caller caller = this.repr._newCaller();
        caller.setIdentity(dn);
        return caller;
    }

    public String getOwnerID(Caller caller) throws CannotTranslateException {

        if (caller == null || caller.getIdentity() == null) {
            throw new IllegalArgumentException(
                    "caller must be non-null and have a valid identity");
        }
        final String dn = caller.getIdentity();

        try {
            final QueryUser user =
                    userDetailsService.loadUserByDn(dn);

            return user.getAccessID();

        } catch (DataAccessException e) {
            throw new CannotTranslateException(
                    "Unable to resolve DN to an accessID",e);
        } catch (UsernameNotFoundException e) {
            throw new CannotTranslateException(
                    "Unable to resolve DN to an accessID",e);
        }
    }
}
