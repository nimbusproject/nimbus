/*
 * Copyright 1999-2010 University of Chicago
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

import org.nimbustools.api.services.rm.ResourceRequestDeniedException;

import commonj.timers.TimerListener;
import commonj.timers.Timer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class BackfillTimer implements TimerListener {
    
    private static final Log logger =
            LogFactory.getLog(BackfillTimer.class.getName());

    private final Backfill backfill;

    public BackfillTimer(Backfill backfill) {
        this.backfill = backfill;
    }

    public void timerExpired(Timer timer) {

        final int maxInstances = this.backfill.getMaxInstances();
        int curNumInstances = this.backfill.getCurNumInstances();

        if (maxInstances == 0) {
            curNumInstances = -1;
        }
        boolean wantMoreBackfill = true;
        while((wantMoreBackfill == true) &&
              (curNumInstances < maxInstances)) {
            try {
                this.backfill.createBackfillNode();
                this.backfill.addCurInstances(1);
            } catch (ResourceRequestDeniedException rDE) {
                logger.info("Backfill launch failed. " +
                            "Looks like we're full.");
                wantMoreBackfill = false;
            } catch (Exception e) {
                logger.error("BackfillTimer caught " +
                             "an exception: " + e.getMessage());
                wantMoreBackfill = false;
            }
            if (maxInstances > 0) {
                curNumInstances = this.backfill.getCurNumInstances();
            }
        }
    }
}