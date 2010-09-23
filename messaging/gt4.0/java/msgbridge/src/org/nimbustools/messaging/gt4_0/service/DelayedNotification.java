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

import commonj.timers.TimerListener;
import commonj.timers.Timer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.wsrf.Topic;

class DelayedNotification implements TimerListener {

    private static final Log logger =
            LogFactory.getLog(DelayedNotification.class.getName());

    private final Topic topic;
    private final Counter pending;

    DelayedNotification(Topic topic, Counter pending) {
        this.topic = topic;
        this.pending = pending;
    }

    public void timerExpired(Timer timer) {
        if (this.topic == null) {
            logger.fatal("topic is null");
            return;
        }

        if (this.pending != null) {
            int result = this.pending.addToCount(-1);
            if (result > 0) {
                return;
            }
        }

        try {
            this.topic.notify(null);
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.error("problem notifying subscribers", e);
            } else {
                logger.error("problem notifying subscribers "+ e.getMessage());
            }
        }
    }
}
