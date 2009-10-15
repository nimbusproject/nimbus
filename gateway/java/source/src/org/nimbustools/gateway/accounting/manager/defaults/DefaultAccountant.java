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
import org.nimbustools.gateway.accounting.manager.InvalidAccountException;
import org.nimbustools.api.repr.Caller;
import org.nimbustools.api.repr.vm.ResourceAllocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.classic.Session;
import org.hibernate.SessionFactory;

public class DefaultAccountant implements Accountant {

    private static final Log logger =
            LogFactory.getLog(DefaultAccountant.class.getName());

    private final SessionFactory sessionFactory;

    public DefaultAccountant(SessionFactory sessionFactory) {

        if (sessionFactory == null) {
            throw new IllegalArgumentException("sessionFactory may not be null");
        }
        this.sessionFactory = sessionFactory;
    }

    public boolean isValidUser(Caller user) {
        if (user == null) {
            throw new IllegalArgumentException("user may not be null");
        }

        return true;
    }

    public void chargeUser(Caller user, int count, Session session)
            throws InsufficientCreditException, InvalidAccountException {
        if (user == null) {
            throw new IllegalArgumentException("user may not be null");
        }
        if (count < 0) {
            throw new IllegalArgumentException("count must be non-negative");
        }

        logger.debug("charging user \""+user.getIdentity()+"\" "+count+" credits");

        Account acct = getAccount(user);
        acct.charge(count);
        session.saveOrUpdate(acct);
       
    }

    public void chargeUserWithOverdraft(Caller user, int count, Session session)
            throws InvalidAccountException {
        if (user == null) {
            throw new IllegalArgumentException("user may not be null");
        }
        if (count < 0) {
            throw new IllegalArgumentException("count must be non-negative");
        }

        logger.debug("charging user \""+user.getIdentity()+"\" "+count+" " +
                "credits with possible overdraft");

        final Account acct = getAccount(user);
        acct.chargeWithOverdraft(count);
        session.saveOrUpdate(acct);
    }

    public void creditUser(Caller user, int count, Session session)
            throws InsufficientCreditException, InvalidAccountException {
        if (user == null) {
            throw new IllegalArgumentException("user may not be null");
        }
        if (count < 0) {
            throw new IllegalArgumentException("rate must be non-negative");
        }

        logger.debug("crediting user \""+user.getIdentity()+"\" "+count+" credits");

        Account acct = getAccount(user);
        acct.credit(count);
        session.saveOrUpdate(acct);
    }

    public void persistUser(Caller user, Session session)
            throws InvalidAccountException {
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

    private Account getAccount(Caller user) throws InvalidAccountException {
        if (user == null) {
            throw new IllegalArgumentException("id may not be null");
        }

        String id = user.getIdentity();
        if (id == null) {
            throw new IllegalArgumentException("user id may not be null");
        }

        final Session session = sessionFactory.openSession();
        session.beginTransaction();

        logger.info("Attempting to find account for DN '"+
                id+"' in persistence layer");
        Account acct = (Account) session.get(DefaultAccount.class, id);
        if (acct == null) {
            throw new InvalidAccountException("Account '"+id+"' does not exist");
        }
        return acct;
    }
}
