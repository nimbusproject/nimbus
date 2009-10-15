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
import org.hibernate.Criteria;

import java.util.List;
import java.util.Collections;

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

    public boolean isValidAccount(Caller user) {
        if (user == null) {
            throw new IllegalArgumentException("user may not be null");
        }

        final Session session = sessionFactory.getCurrentSession();

        try {
            getAccount(user, session);
        } catch (InvalidAccountException e) {
            return false;
        }
        return true;
    }

    public void chargeAccount(Caller user, int count)
            throws InsufficientCreditException, InvalidAccountException {
        if (user == null) {
            throw new IllegalArgumentException("user may not be null");
        }
        if (count < 0) {
            throw new IllegalArgumentException("count must be non-negative");
        }

        logger.debug("charging user \""+user.getIdentity()+"\" "+count+" credits");

        final Session session = sessionFactory.getCurrentSession();

        Account acct = getAccount(user, session);
        acct.charge(count);
        session.update(acct);
       
    }

    public void chargeAccountWithOverdraft(Caller user, int count)
            throws InvalidAccountException {
        if (user == null) {
            throw new IllegalArgumentException("user may not be null");
        }
        if (count < 0) {
            throw new IllegalArgumentException("count must be non-negative");
        }

        logger.debug("charging user \""+user.getIdentity()+"\" "+count+" " +
                "credits with possible overdraft");
        final Session session = sessionFactory.getCurrentSession();

        final Account acct = getAccount(user, session);
        acct.chargeWithOverdraft(count);
        session.update(acct);
    }

    public void creditAccount(Caller user, int count)
            throws InsufficientCreditException, InvalidAccountException {
        if (user == null) {
            throw new IllegalArgumentException("user may not be null");
        }
        if (count < 0) {
            throw new IllegalArgumentException("rate must be non-negative");
        }

        logger.debug("crediting user \""+user.getIdentity()+"\" "+count+" credits");

        final Session session = sessionFactory.getCurrentSession();

        Account acct = getAccount(user, session);
        acct.credit(count);
        session.saveOrUpdate(acct);
    }

    public int getHourlyRate(ResourceAllocation ra) {
        if (ra == null) {
            throw new IllegalArgumentException("ra may not be null");
        }

        //TODO right now I'm just working with default instances
        return 10;
    }

    public void addLimitedAccount(String dn, int maxCredits) {
        if (dn == null) {
            throw new IllegalArgumentException("dn may not be null");
        }
        dn = dn.trim();
        if (dn.length() == 0) {
            throw new IllegalArgumentException("dn may not be empty");
        }
        if (maxCredits < 0) {
            throw new IllegalArgumentException("maxCredits may not be negative");
        }
        addAccountImpl(dn, maxCredits);
    }


    public void addUnlimitedAccount(String dn) {
        if (dn == null) {
            throw new IllegalArgumentException("dn may not be null");
        }
        dn = dn.trim();
        if (dn.length() == 0) {
            throw new IllegalArgumentException("dn may not be empty");
        }

        addAccountImpl(dn, null);
    }

    protected void addAccountImpl(String dn, Integer maxCredits) {

        final DefaultAccount account = new DefaultAccount(dn, maxCredits, 0);

        final Session session = sessionFactory.getCurrentSession();
        session.save(account);
    }

    public List<Account> describeAccounts() {
        final Session session = sessionFactory.getCurrentSession();
        final Criteria criteria = session.createCriteria(DefaultAccount.class);
        @SuppressWarnings("unchecked")
        final List<Account> list = (List<Account>)
                Collections.checkedList(criteria.list(), Account.class);
        return list;
    }

    public Account setAccountMaxCredits(String dn, Integer maxCredits)
            throws InvalidAccountException {

        if (dn == null) {
            throw new IllegalArgumentException("dn may not be null");
        }
        dn = dn.trim();
        if (dn.length() == 0) {
            throw new IllegalArgumentException("dn may not be empty");
        }
        if (maxCredits < 0) {
            throw new IllegalArgumentException("maxCredits may not be negative");
        }

        final Session session = sessionFactory.getCurrentSession();

        final DefaultAccount account = (DefaultAccount) session.get(DefaultAccount.class, dn);
        if (account == null) {
            throw new InvalidAccountException("Account '"+dn+"' does not exist");
        }
        account.setMaxCredits(maxCredits);
        session.update(account);
        

        return account;
    }

    private Account getAccount(Caller user, Session session) throws InvalidAccountException {
        if (user == null) {
            throw new IllegalArgumentException("id may not be null");
        }

        String id = user.getIdentity();
        if (id == null) {
            throw new IllegalArgumentException("user id may not be null");
        }

        logger.debug("Attempting to find account for DN '"+
                id+"' in persistence layer");
        Account acct = (Account) session.get(DefaultAccount.class, id);
        if (acct == null) {
            throw new InvalidAccountException("Account '"+id+"' does not exist");
        }
        return acct;
    }
}
