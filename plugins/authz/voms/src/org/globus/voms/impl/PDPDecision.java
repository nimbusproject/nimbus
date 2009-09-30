/*
 * Copyright 1999-2007 University of Chicago
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

package org.globus.voms.impl;

/**
 * A superset of the decisions possible in the old and new authorization
 * frameworks.  The old-API wrapper should return 'true' for PERMIT and
 * 'false' for the rest.
 */
public class PDPDecision {
    public static final int PERMIT = 2;
    public static final int INDETERMINATE = 1;
    public static final int NOT_APPLICABLE = 0;
    public static final int DENY = -1;

    public int decision = NOT_APPLICABLE;
}
