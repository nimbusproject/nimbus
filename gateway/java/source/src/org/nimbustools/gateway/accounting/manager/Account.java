package org.nimbustools.gateway.accounting.manager;

import org.nimbustools.gateway.ec2.EC2AccessID;
import org.nimbustools.gateway.accounting.manager.InsufficientCreditException;/*
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

// this is probably gonna end up being too simple..

public interface Account {

    String getId();

    int getUsedCredits();

    int getAvailableCredits();

    /***
     * Charges the Account if it has enough available credits and throws
     * an exception otherwise
     * @param count amount to charge
     * @throws InsufficientCreditException
     */
    void charge(int count) throws InsufficientCreditException;

    /***
     * Charges the Account even if there is insufficient credit available.
     * Can produce a negative balance.
     * @param count amount to charge
     */
    void chargeWithOverdraft(int count);

    void credit(int count) throws InsufficientCreditException;
}
