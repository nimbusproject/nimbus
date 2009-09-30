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

package org.globus.workspace.client_core.repr;

import org.nimbustools.messaging.gt4_0.generated.types.Schedule_Type;
import org.nimbustools.messaging.gt4_0.common.CommonUtil;
import org.nimbustools.messaging.gt4_0.common.InvalidDurationException;
import org.globus.workspace.client_core.utils.ScheduleUtils;
import org.apache.axis.types.Duration;

import java.util.Calendar;

/**
 * Serves as a way to begin encapsulating protocol/implementations but
 * also importantly serves as a copy.  Also provides convenient views
 * (conversions) of the data.
 */
public class Schedule {

    private Calendar actualInstantiationTime;
    private Calendar actualTerminationTime;

    // max ~25k days
    private int durationSeconds = -1;

    public Schedule() {
        // empty
    }

    public Schedule(Schedule_Type xmlSched) throws InvalidDurationException {

        if (xmlSched == null) {
            throw new IllegalArgumentException("xmlSched may not be " +
                    "null, use null " + this.getClass().getName() +
                    " reference instead");
        }

        final Duration duration = xmlSched.getDuration();
        this.durationSeconds = CommonUtil.durationToSeconds(duration);

        final Calendar instantiationTime =
                            xmlSched.getActualInstantiationTime();

        if (instantiationTime != null) {
            this.actualInstantiationTime = (Calendar)instantiationTime.clone();
        }

        final Calendar terminationTime =
                            xmlSched.getActualTerminationTime();
        
        if (terminationTime != null) {
            this.actualTerminationTime = (Calendar)terminationTime.clone();
        }
    }

    public Calendar getActualInstantiationTime() {
        return this.actualInstantiationTime;
    }

    public void setActualInstantiationTime(Calendar instantiationTime) {
        this.actualInstantiationTime = instantiationTime;
    }

    public Calendar getActualTerminationTime() {
        return this.actualTerminationTime;
    }

    public void setActualTerminationTime(Calendar terminationTime) {
        this.actualTerminationTime = terminationTime;
    }

    /**
     * @return runtime seconds; if negative, it means this was not set
     */
    public int getDurationSeconds() {
        return this.durationSeconds;
    }

    /**
     * @param seconds runtime; if negative, it means this was not set
     */
    public void setDurationSeconds(int seconds) {
        this.durationSeconds = seconds;
    }

    /**
     * Returns ceiling of converted "durationSeconds" field.
     *
     * If 123 seconds is stored, 3 minutes is returned
     * If 43 seconds is stored, 1 minutes is returned
     * If 3 seconds is stored, 1 minutes is returned
     *
     * @return runtime minutes; if negative, it means this was not set
     */
    public int getDurationMinutes() {
        final int seconds = this.durationSeconds;
        int minutes = seconds/60;
        if (seconds % 60 > 0) {
            minutes += 1;
        }
        return minutes;
    }

    /**
     * Helper for pretty printing.
     * 
     * @see #getDurationMinutes()
     * @return true if seconds is an exact multiple of 60
     *         0 seconds stored returns true
     *         negative seconds stored (unset) returns false
     */
    public boolean isMinutesExact() {
        return ScheduleUtils.isWholeMinute(this.durationSeconds);
    }

    /**
     * Just sets the "durationSeconds" field to minutes * 60
     * @param minutes runtime; if negative, it means this was not set
     */
    public void setDurationMinutes(int minutes) {
        this.durationSeconds = minutes * 60;
    }
}
