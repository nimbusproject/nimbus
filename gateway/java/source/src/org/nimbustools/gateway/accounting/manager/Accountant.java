package org.nimbustools.gateway.accounting.manager;

import org.nimbustools.api.repr.vm.ResourceAllocation;
import org.nimbustools.api.repr.Caller;
import org.hibernate.classic.Session;

import java.util.List;


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

public interface Accountant {

    boolean isValidAccount(Caller user);

    void chargeAccount(Caller user, int count)
            throws InsufficientCreditException, InvalidAccountException;

    void chargeAccountWithOverdraft(Caller user, int count)
            throws InvalidAccountException;

    void creditAccount(Caller user, int count)
            throws InsufficientCreditException, InvalidAccountException;

    /**
     * Determines the hourly rate for a single instance of the provided
     * ResourceAllocation.
     * @param ra
     * @return
     */
    int getHourlyRate(ResourceAllocation ra);

    void addLimitedAccount(String dn, int maxCredits);

    void addUnlimitedAccount(String dn);

    List<Account> describeAccounts();

    Account setAccountMaxCredits(String dn, Integer maxCredits)
            throws InvalidAccountException;
}
