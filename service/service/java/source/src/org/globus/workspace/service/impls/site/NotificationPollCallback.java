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

package org.globus.workspace.service.impls.site;

public interface NotificationPollCallback {

    public int numPendingNotifications() throws Exception;

    // sends positive number
    public void decreaseNumPending(int n) throws Exception;

    /**
     * Current cursor position if the impl cares to store this long term.
     * For low poll intervals it's recommended that the implementation
     * does not store every single time (unless it can absolutely not handle
     * duplicate notifications upon container recovery).
     *
     * @param pos position
     */
    public void cursorPosition(long pos);
}
