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

import edu.emory.mathcs.backport.java.util.concurrent.locks.Lock;
import edu.emory.mathcs.backport.java.util.concurrent.locks.ReentrantLock;

import java.util.Map;
import java.util.HashMap;

public class DefaultLockManager implements LockManager {

    private final Map locks;

    public DefaultLockManager() {
        this.locks = new HashMap(512);
    }

    public synchronized Lock getLock(int key) {
        return this.getLock(String.valueOf(key));
    }

    public synchronized Lock getLock(String key) {
        Lock lock = (Lock)this.locks.get(key);
        if (lock == null) {
            lock = new ReentrantLock(true);
            this.locks.put(key, lock);
        }
        return lock;
    }

    public synchronized void removeLock(int key) {
        this.removeLock(String.valueOf(key));
    }

    public synchronized void removeLock(String key) {
        this.locks.remove(key);
    }

}
