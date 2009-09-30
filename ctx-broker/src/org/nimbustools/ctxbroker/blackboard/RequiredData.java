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

package org.nimbustools.ctxbroker.blackboard;

import org.nimbustools.ctxbroker.generated.gt4_0.description.Requires_TypeData;

import java.util.List;
import java.util.ArrayList;

public class RequiredData {

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    private final String name;
    
    // list of filled Requires_TypeData objects
    private final List values = new ArrayList();

    
    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------

    RequiredData(String dataname) {
        if (dataname == null) {
            throw new IllegalArgumentException("dataname may not be null");
        }
        this.name = dataname;
    }


    // -------------------------------------------------------------------------
    // MUTATE
    // -------------------------------------------------------------------------

    void addNewValue(String value) {
        final Requires_TypeData newdata = new Requires_TypeData();
        newdata.setName(this.name);
        if (value == null || value.trim().length() == 0) {
            newdata.set_value("");
        } else {
            newdata.set_value(value);
        }
        this.values.add(newdata);
    }


    // -------------------------------------------------------------------------
    // ACCESS
    // -------------------------------------------------------------------------

    int numValues() {
        return this.values.size();
    }

    // never returns null
    List getDataList() {
        return this.values;
    }

    // never returns null
    Requires_TypeData[] getAllData() {
        return (Requires_TypeData[])this.values.toArray(
                        new Requires_TypeData[this.values.size()]);
    }
    
}
