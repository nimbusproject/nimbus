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

public interface VomsConstants {
    public static final String TRUST_STORE_DIR_PROP = "vomsTrustStore";
    public static final String VALIDATE = "vomsValidate";
    public static final String REFRESH_TIME_PROP = "vomsRefreshTime";

    public static final String ATTR_SECURITY_CONFIG_FILE = "vomsAttrAuthzFile";
    public static final String ATTR_MAP_CONFIG_FILE = "vomsAttrMapFile";

    public static final String VOMS_PDP_POLICY = "vomsPDPPolicy";

    public static final String VOMS_PDP_AND_LOGIC = "vomsPDPAndLogic";

    public static final String DEFAULT_GRIDMAP = "vomsDefaultGridmap";
    public static final String CONSULT_GRIDMAP_KEY = "vomsConsultGridmap";

    // for config translation to Hashtable in PDP proxies
    public static final String[] ALL_CONFIG_KEYS =
            { TRUST_STORE_DIR_PROP, VALIDATE, REFRESH_TIME_PROP,
              ATTR_SECURITY_CONFIG_FILE, ATTR_MAP_CONFIG_FILE,
              VOMS_PDP_POLICY, VOMS_PDP_AND_LOGIC, DEFAULT_GRIDMAP,
              CONSULT_GRIDMAP_KEY};
}
