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

import org.nimbustools.gateway.accounting.manager.Account;
import org.nimbustools.gateway.accounting.manager.InsufficientCreditException;

import javax.persistence.*;

@Entity
@Table(name = "account")
public class DefaultAccount implements Account {

    private final Object lock = new Object();

    private String id;
    private int maxCredits;
    private int usedCredits;

    public DefaultAccount(String id, int maxCredits, int usedCredits) {
        if (id == null || id.length() == 0) {
            throw new IllegalArgumentException("id may not be null or empty");
        }
        if (maxCredits < 0) {
            throw new IllegalArgumentException("maxCredits must be non-negative");
        }
        if (usedCredits < 0) {
            throw new IllegalArgumentException("usedCredits must be non-negative");
        }

        this.id = id;
        this.maxCredits = maxCredits;
        this.usedCredits = usedCredits;
    }

    DefaultAccount() {
    }

    @Id
    @Column(name = "DN")
    public String getId() {
        return id;
    }

    void setId(String id) {
        this.id = id;
    }

    @Column(name = "CREDITS_MAX")
    public int getMaxCredits() {
        return maxCredits;
    }

    void setMaxCredits(int maxCredits) {
        synchronized (this.lock) {
            this.maxCredits = maxCredits;
        }
    }

    @Column(name = "CREDITS_USED")
    public int getUsedCredits() {
        return usedCredits;
    }

    void setUsedCredits(int usedCredits) {
        synchronized (this.lock) {
            this.usedCredits = usedCredits;
        }
    }

    @Transient
    public int getAvailableCredits() {
        return maxCredits - usedCredits;
    }

    public void charge(int count) throws InsufficientCreditException {
        if (count < 1) {
            throw new IllegalArgumentException("count must be greater than zero");
        }
        synchronized (this.lock) {
            if (this.getAvailableCredits() >= count) {
                usedCredits += count;
            } else {
                throw new InsufficientCreditException("not enough funds " +
                    "available for charge");
            }
        }
    }

    public void chargeWithOverdraft(int count) {
         if (count < 1) {
            throw new IllegalArgumentException("count must be greater than zero");
        }
        synchronized (this.lock) {
            usedCredits += count;
        }
    }

    public void credit(int count) throws InsufficientCreditException {
        if (count < 1) {
            throw new IllegalArgumentException("count must be greater than zero");
        }
        synchronized (this.lock) {
            if (usedCredits >= count) {
                usedCredits -= count;
            } else {
                throw new InsufficientCreditException("not enough balance " +
                    "available for credit");
            }
        }
    }

}
