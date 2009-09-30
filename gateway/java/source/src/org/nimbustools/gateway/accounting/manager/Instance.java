package org.nimbustools.gateway.accounting.manager;

import org.nimbustools.api.repr.vm.VM;
import org.nimbustools.api.repr.Caller;

import java.util.Date;/*
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

public interface Instance {

    String getID();
    VM getVM();
    Caller getCaller();

    boolean isRunning();
    boolean isTerminated();

    int getRate();
    int getCharge();
    void setCharge(int charge);

    /**
     * Updates start and stop times from VM member object
     */
    boolean updateFromVM();

    /**
     * Calculates the additional charge needed to keep instance running through
     * the provided time
     * @param chargeTime
     */
    int calculateCharge(Date chargeTime);

    void addCharge(int charge);

}
