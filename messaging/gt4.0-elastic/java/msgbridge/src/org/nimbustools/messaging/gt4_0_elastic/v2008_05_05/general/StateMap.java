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

package org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.general;

import org.nimbustools.api.repr.vm.State;

import java.util.Hashtable;

public class StateMap {

    // it is important these integers do not change
    public static final Integer STATE_UNSET = new Integer(-1);
    public static final Integer STATE_PENDING = new Integer(0);
    public static final Integer STATE_RUNNING = new Integer(16);
    public static final Integer STATE_SHUTTING_DOWN = new Integer(32);
    public static final Integer STATE_TERMINATED = new Integer(48);

    public static final String STATE_UNSET_STR = "unset";
    public static final String STATE_PENDING_STR = "pending";
    public static final String STATE_RUNNING_STR = "running";
    public static final String STATE_SHUTTING_DOWN_STR = "shutting-down";
    public static final String STATE_TERMINATED_STR = "terminated";

    private static final Hashtable elasticStringToInteger = new Hashtable(8);
    private static final Hashtable elasticIntegerToString = new Hashtable(8);
    
    private static final Hashtable managerStringToInteger = new Hashtable(8);
    private static final Hashtable managerStringToString = new Hashtable(8);

    static {
        elasticStringToInteger.put(STATE_UNSET_STR, STATE_UNSET);
        elasticStringToInteger.put(STATE_PENDING_STR, STATE_PENDING);
        elasticStringToInteger.put(STATE_RUNNING_STR, STATE_RUNNING);
        elasticStringToInteger.put(STATE_SHUTTING_DOWN_STR, STATE_SHUTTING_DOWN);
        elasticStringToInteger.put(STATE_TERMINATED_STR, STATE_TERMINATED);

        elasticIntegerToString.put(STATE_UNSET, STATE_UNSET_STR);
        elasticIntegerToString.put(STATE_PENDING, STATE_PENDING_STR);
        elasticIntegerToString.put(STATE_RUNNING, STATE_RUNNING_STR);
        elasticIntegerToString.put(STATE_SHUTTING_DOWN, STATE_SHUTTING_DOWN_STR);
        elasticIntegerToString.put(STATE_TERMINATED, STATE_TERMINATED_STR);

        managerStringToInteger.put(State.STATE_Unpropagated, STATE_PENDING);
        managerStringToInteger.put(State.STATE_Propagated, STATE_PENDING);
        managerStringToInteger.put(State.STATE_Paused, STATE_PENDING);
        managerStringToInteger.put(State.STATE_Running, STATE_RUNNING);
        managerStringToInteger.put(State.STATE_TowardsTransportReady,
                                   STATE_SHUTTING_DOWN);
        managerStringToInteger.put(State.STATE_TransportReady,
                                   STATE_TERMINATED);
        managerStringToInteger.put(State.STATE_Cancelled, STATE_TERMINATED);
        managerStringToInteger.put(State.STATE_Corrupted, STATE_TERMINATED);

        managerStringToString.put(State.STATE_Unpropagated, STATE_PENDING_STR);
        managerStringToString.put(State.STATE_Propagated, STATE_PENDING_STR);
        managerStringToString.put(State.STATE_Paused, STATE_PENDING_STR);
        managerStringToString.put(State.STATE_Running, STATE_RUNNING_STR);
        managerStringToString.put(State.STATE_TowardsTransportReady,
                                   STATE_SHUTTING_DOWN_STR);
        managerStringToString.put(State.STATE_TransportReady,
                                   STATE_TERMINATED_STR);
        managerStringToString.put(State.STATE_Cancelled, STATE_TERMINATED_STR);
        managerStringToString.put(State.STATE_Corrupted, STATE_TERMINATED_STR);
    }

    public static String elasticIntToElasticString(int i) {
        final String str = (String) elasticIntegerToString.get(new Integer(i));
        return str == null ? STATE_UNSET_STR : str;
    }
    
    public static String managerStringToElasticString(String mgrState) {
        final String str = (String) managerStringToString.get(mgrState);
        return str == null ? STATE_UNSET_STR : str;
    }

    public static int elasticStringToElasticInt(String state) {
        final Integer integer = (Integer) elasticStringToInteger.get(state);
        return integer == null ? STATE_UNSET.intValue() : integer.intValue();
    }

    public static int managerStringToElasticInt(String mgrState) {
        final Integer integer = (Integer) managerStringToInteger.get(mgrState);
        return integer == null ? STATE_UNSET.intValue() : integer.intValue();
    }
}
