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
package org.nimbustools.gateway.accounting.manager.defaults;

import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.Criteria;
import org.hibernate.classic.Session;
import org.nimbustools.gateway.accounting.manager.Account;
import org.nimbustools.gateway.accounting.manager.InvalidAccountException;
import org.nimbustools.gateway.accounting.manager.Admin;

import java.util.List;
import java.util.Collections;

public class DefaultAdmin implements Admin {

    private final SessionFactory sessionFactory;

    public DefaultAdmin(SessionFactory sessionFactory) {
        if (sessionFactory == null) {
            throw new IllegalArgumentException("sessionFactory may not be null");
        }

        this.sessionFactory = sessionFactory;
    }


    public Account addLimitedAccount(String dn, int maxCredits) {
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
        return addAccountImpl(dn, maxCredits);
    }


    public Account addUnlimitedAccount(String dn) {
        if (dn == null) {
            throw new IllegalArgumentException("dn may not be null");
        }
        dn = dn.trim();
        if (dn.length() == 0) {
            throw new IllegalArgumentException("dn may not be empty");
        }

        return addAccountImpl(dn, null);
    }

    protected Account addAccountImpl(String dn, Integer maxCredits) {

        final DefaultAccount account = new DefaultAccount(dn, maxCredits, 0);

        final Session session = sessionFactory.openSession();
        final Transaction transaction = session.beginTransaction();
        session.save(account);
        transaction.commit();
        return account;
    }

    public List<Account> describeAccounts() {
        //TODO this probably isn't right. need to look at hibernate best practices.
        final Session session = sessionFactory.openSession();
        final Criteria criteria = session.createCriteria(DefaultAccount.class);
        @SuppressWarnings("unchecked")
        final List<Account> list = (List<Account>)
                Collections.checkedList(criteria.list(), Account.class);
        return list;
    }

    public Account getAccount(String dn) throws InvalidAccountException {
        if (dn == null) {
            throw new IllegalArgumentException("dn may not be null");
        }
        dn = dn.trim();
        if (dn.length() == 0) {
            throw new IllegalArgumentException("dn may not be empty");
        }
        final Session session = sessionFactory.openSession();
        session.beginTransaction();

        final Account account = (Account) session.get(DefaultAccount.class, dn);
        if (account == null) {
            throw new InvalidAccountException("Account '"+dn+"' does not exist");
        }
        return account;
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


        final Session session = sessionFactory.openSession();
        final Transaction transaction = session.beginTransaction();

        final DefaultAccount account = (DefaultAccount) session.get(DefaultAccount.class, dn);
        if (account == null) {
            throw new InvalidAccountException("Account '"+dn+"' does not exist");
        }

        account.setMaxCredits(maxCredits);

        transaction.commit();
        return account;
    }

}
