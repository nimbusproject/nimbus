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

package org.nimbustools.api.defaults.repr;

import java.util.Calendar;

import org.nimbustools.api._repr._SpotPriceEntry;

public class DefaultSpotPriceEntry implements _SpotPriceEntry {

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    private Calendar timeStamp;
    private Double spotPrice;
    
    // -------------------------------------------------------------------------
    // implements org.nimbustools.api.repr.SpotPriceEntry
    // -------------------------------------------------------------------------
    
    public Calendar getTimeStamp() {
        return timeStamp;
    }    
    
    public Double getSpotPrice() {
        return this.spotPrice;
    }    
    
    // -------------------------------------------------------------------------
    // implements org.nimbustools.api._repr._SpotPriceEntry
    // -------------------------------------------------------------------------

    public void setTimeStamp(Calendar timeStamp) {
        this.timeStamp = timeStamp;
    }    
    
    public void setSpotPrice(Double spotPrice) {
        this.spotPrice = spotPrice;
    }
    
    // -------------------------------------------------------------------------
    // DEBUG STRING
    // -------------------------------------------------------------------------

    public String toString() {
        return "DefaultSpotPriceEntry [spotPrice=" + spotPrice + ", timeStamp="
                + (timeStamp != null? timeStamp.getTime() : null) + "]";
    }    
    
}
