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
package org.nimbustools.gateway.admin.commands;

import org.nimbustools.gateway.admin.Command;
import org.nimbustools.gateway.admin.ParameterProblem;
import org.nimbustools.gateway.admin.CommandProblem;
import org.nimbustools.gateway.admin.AdminTool;
import org.nimbustools.gateway.accounting.manager.Accountant;
import org.nimbustools.gateway.accounting.manager.Account;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.classic.Session;

import java.util.List;

public class ListUsersCommand implements Command {

    public static final String NAME = "list";

    private final AdminTool admin;

    public ListUsersCommand(AdminTool admin) {
        if (admin == null) {
            throw new IllegalArgumentException("admin may not be null");
        }
        this.admin = admin;
    }

    public void run(String[] args) throws ParameterProblem, CommandProblem {
        final SessionFactory sessionFactory = admin.getSessionFactory();
        final Session session = sessionFactory.getCurrentSession();
        final Transaction transaction = session.beginTransaction();

        final Accountant accountant = admin.getAccountant();
        final List<Account> list = accountant.describeAccounts();

        transaction.commit();

        for (Account acct : list) {
            System.out.println("DN: "+acct.getId());
            String maxCredits = acct.getMaxCredits() == null ? "Unlimited" :
                    acct.getMaxCredits().toString();
            System.out.println("Max Credits: "+ maxCredits );
            System.out.println("Used Credits: "+ acct.getUsedCredits());
            System.out.println("-----------------------");
        }

    }

    public String getName() {
        return NAME;
    }

    public String getDescription() {
        return "List users authorized for gateway";
    }

    public String getUsage() {
        return NAME;
    }
}
