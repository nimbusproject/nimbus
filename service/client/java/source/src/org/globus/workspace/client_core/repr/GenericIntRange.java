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

import org.ggf.jsdl.RangeValue_Type;
import org.ggf.jsdl.Exact_Type;

/**
 * Stub for the future.
 * 
 * Range requests are not supported in the released service.
 */
public class GenericIntRange {

    private final int min;
    private final int max;

    public GenericIntRange(int minimum, int maximum) {
        this.min = minimum;
        this.max = maximum;
    }

    /**
     * This is a stub for the future.
     * 
     * @param jsdlRange must be EXACT, not a real range
     */
    public GenericIntRange(RangeValue_Type jsdlRange) {
        if (jsdlRange == null) {
            throw new IllegalArgumentException("jsdlRange may not be null");
        }

        //TODO: support ranges and Exact_Type[]
        final Exact_Type exact = jsdlRange.getExact(0);
        if (exact == null) {
            throw new IllegalArgumentException(
                                "jsdlRange may not be a range yet ;-)");
        }

        // casting double, possible precision loss
        final int val = (int) exact.get_value();
        this.min = val;
        this.max = val;
    }

    public int getMin() {
        return this.min;
    }

    public int getMax() {
        return this.max;
    }
}
