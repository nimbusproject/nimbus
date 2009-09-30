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

package org.nimbustools.gateway.accounting.manager.defaults;

import org.nimbustools.gateway.accounting.manager.Accountant;
import org.nimbustools.gateway.accounting.manager.Account;
import org.nimbustools.gateway.accounting.manager.InsufficientCreditException;
import org.nimbustools.api.repr.Caller;
import org.nimbustools.api.repr.vm.ResourceAllocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.classic.Session;
import org.hibernate.SessionFactory;

import java.util.HashMap;

public class DefaultAccountant implements Accountant {

    private static final Log logger =
            LogFactory.getLog(DefaultAccountant.class.getName());

    private final HashMap<String, Account> accountMap;
    private final int maxCreditsPerUser;

    private final SessionFactory sessionFactory;

    public DefaultAccountant(int maxCreditsPerUser,
                             SessionFactory sessionFactory) {

        if (maxCreditsPerUser < 0) {
            throw new IllegalArgumentException("maxCreditsPerUser must be non-negative");
        }
        this.maxCreditsPerUser = maxCreditsPerUser;

        if (sessionFactory == null) {
            throw new IllegalArgumentException("sessionFactory may not be null");
        }
        this.sessionFactory = sessionFactory;

        this.accountMap = new HashMap<String, Account>();
    }

    public boolean isValidUser(Caller user) {
        if (user == null) {
            throw new IllegalArgumentException("user may not be null");
        }

        return true;
    }

    public void chargeUser(Caller user, int count) throws InsufficientCreditException {
        if (user == null) {
            throw new IllegalArgumentException("user may not be null");
        }
        if (count < 0) {
            throw new IllegalArgumentException("count must be non-negative");
        }

        logger.debug("charging user \""+user.getIdentity()+"\" "+count+" credits");

        Account acct = getAccount(user);
        acct.charge(count);

    }

    public void chargeUserWithOverdraft(Caller user, int count) {
        if (user == null) {
            throw new IllegalArgumentException("user may not be null");
        }
        if (count < 0) {
            throw new IllegalArgumentException("count must be non-negative");
        }

        logger.debug("charging user \""+user.getIdentity()+"\" "+count+" " +
                "credits with possible overdraft");

        getAccount(user).chargeWithOverdraft(count);


    }

    public void creditUser(Caller user, int count) throws InsufficientCreditException {
        if (user == null) {
            throw new IllegalArgumentException("user may not be null");
        }
        if (count < 0) {
            throw new IllegalArgumentException("rate must be non-negative");
        }

        logger.debug("crediting user \""+user.getIdentity()+"\" "+count+" credits");

        Account acct = getAccount(user);
        acct.credit(count);
    }

    public void persistUser(Caller user, Session session) {
        if (user == null) {
            throw new IllegalArgumentException("user may not be null");
        }
        if (session == null) {
            throw new IllegalArgumentException("session may not be null");
        }

        final Account account = getAccount(user);
        session.saveOrUpdate(account);

    }

    public int getHourlyRate(ResourceAllocation ra) {
        if (ra == null) {
            throw new IllegalArgumentException("ra may not be null");
        }

        //TODO right now I'm just working with default instances
        return 10;
    }

    private Account getAccount(Caller user) {
        if (user == null) {
            throw new IllegalArgumentException("id may not be null");
        }

        String id = user.getIdentity();
        if (id == null) {
            throw new IllegalArgumentException("user id may not be null");
        }

        synchronized (this.accountMap) {
            Account acct = this.accountMap.get(id);
            if (acct == null) {

                final Session session = sessionFactory.openSession();
                session.beginTransaction();

                logger.info("Attempting to find account for DN '"+
                        id+"' in persistence layer");
                acct = (Account) session.get(DefaultAccount.class, id);
                if (acct == null) {

                    //TODO is this really the right behavior?

                    logger.info("Account for dn '"+id+"' does not exist. Creating with "+
                        "default max hours of "+this.maxCreditsPerUser);

                    acct = new DefaultAccount(id,
                    this.maxCreditsPerUser, 0);
                }
                this.accountMap.put(id, acct);
            }
            return acct;
        }
    }
}
