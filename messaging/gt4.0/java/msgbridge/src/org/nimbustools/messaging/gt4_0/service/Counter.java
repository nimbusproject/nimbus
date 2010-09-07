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

package org.nimbustools.messaging.gt4_0.service;

public class Counter {

    private int count;

    /**
     * Callback can be null
     * @param initialValue initial int value
     */
    public Counter(int initialValue) {
        this.count = initialValue;
    }

    /**
     * @param val int to add to count, use zero for checking current value
     * @return current count (that's why arg=0 gets you current value)
     */
    public synchronized int addToCount(int val) {
        if (val == 0) {
            return this.count;
        }
        this.count += val;
        if (this.count < 0) {
            this.count = 0;
        }
        return this.count;
    }
}
