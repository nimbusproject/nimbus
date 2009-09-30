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

package org.globus.workspace;

public class Counter {

    private int count;
    private final CounterCallback call;

    /**
     * Callback can be null
     * @param initialValue initial int value
     * @param call callback for change notification, can be null
     */
    public Counter(int initialValue, CounterCallback call) {
        this.count = initialValue;
        this.call = call;
    }

    /**
     * @param val int to add to count, use zero for checking current value 
     * @return current count
     */
    public synchronized int addToCount(int val) {
        if (val == 0) {
            return this.count;
        }
        this.count += val;
        if (this.count < 0) {
            this.count = 0;
        }
        if (this.call != null) {
            call.newCounterValue(this.count);
        }
        return this.count;
    }
}
