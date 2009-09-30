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

package org.globus.workspace.accounting.impls;

public class Util {

    /**
     * Input is rounded up to next closest integer that is also a multiple of
     * the 'factor' parameter.  Special behavior: input equal to zero and
     * below result in return of zero.
     *
     * @param actual input
     * @param factor factor by which to ceiling to (must be > 0)
     * @return 'input' rounded up to next closest integer that is also a multiple of 'factor'
     */
    public static long positiveCeiling(final long actual,
                                       final long factor) {

        if (factor <= 0) {
            throw new IllegalArgumentException("factor must be > 0");
        }

        if (actual <= 0) {
            return 0;
        }

        if (actual <= factor) {
            return factor;
        }

        final long multiple = actual / factor;
        final long remainder = actual % factor;

        // todo: watch for overflow...
        if (remainder == 0) {
            return multiple * factor;
        } else {
            return multiple * factor + factor;
        }
    }
}
