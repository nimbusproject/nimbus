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

package org.globus.workspace.service.binding.authorization;

public class Restrictions {

    public static final int NOTSET = -1;

    private int maxMem = NOTSET;
    private int maxDuration = NOTSET;

    public int getMaxMem() {
        return this.maxMem;
    }

    public void setMaxMem(int maxMem) {
        this.maxMem = maxMem;
    }

    public int getMaxDuration() {
        return this.maxDuration;
    }

    public void setMaxDuration(int maxDuration) {
        this.maxDuration = maxDuration;
    }


    public String toString() {
        return "Restrictions{" +
                "maxMem=" + this.maxMem +
                ", maxDuration=" + this.maxDuration +
                '}';
    }
}
