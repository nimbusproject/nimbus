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

package org.nimbustools.auto_config.confmgr;

public interface ConfConstants {
    
    // -------------------------------------------------------------------------
    // CONF FILE NAMES
    // -------------------------------------------------------------------------

    public static final String CONF_ACCOUNTING = "accounting.conf";
    public static final String CONF_GLOBAL_POLICIES = "global-policies.conf";
    public static final String CONF_LOGGING = "logging.conf";
    public static final String CONF_NETWORK = "network.conf";
    public static final String CONF_PILOT = "pilot.conf";
    public static final String CONF_REPOSITORY = "repository.conf";
    public static final String CONF_SSH = "ssh.conf";
    public static final String CONF_VMM = "vmm.conf";
    public static final String CONF_COMMON = "other/common.conf";

    
    // -------------------------------------------------------------------------
    // SSH PROPERTIES
    // -------------------------------------------------------------------------

    public static final String KEY_SSH_CONTROLUSER = "control.ssh.user";
    public static final String KEY_SSH_CONTROLUSERKEY = "use.identity";
    public static final String KEY_SSH_SSHDCONTACT = "service.sshd.contact.string";

    // -------------------------------------------------------------------------
    // VMM PROPERTIES
    // -------------------------------------------------------------------------

    public static final String KEY_VMM_CONTROL_PATH = "control.path";
    public static final String KEY_VMM_CONTROL_TMPDIR = "control.tmp.dir";


    // -------------------------------------------------------------------------
    // COMMON PROPERTIES
    // -------------------------------------------------------------------------

    public static final String KEY_COMMON_FAKE_MODE = "fake.mode";

    

}
