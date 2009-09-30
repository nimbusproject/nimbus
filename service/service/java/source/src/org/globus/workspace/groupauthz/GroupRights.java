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

package org.globus.workspace.groupauthz;

import java.util.Properties;

public class GroupRights {

    // name, for logging only
    private String groupName;
    public static final String PROPKEY_GROUP_NAME =
            "vws.group.authz.groupname";

    // this controls 'at any one time', 0 is unlimited
    private long maxReservedMinutes;
    public static final String PROPKEY_MAX_RESERVED_MINUTES =
            "vws.group.authz.maxReservedMinutes";

    // this controls 'all time usage', 0 is unlimited
    private long maxElapsedReservedMinutes;
    public static final String PROPKEY_MAX_ELAPSED_RESERVED_MINUTES =
            "vws.group.authz.maxElapsedReservedMinutes";

    // this controls 'number of VMs in group', 0 is unlimited
    // this is different than 'number of VMs running at once'
    private long maxWorkspaceNumber;
    public static final String PROPKEY_MAX_WORKSPACE_NUMBER =
            "vws.group.authz.maxWorkspaceNumber";

    // this controls 'number of VMs in group', 0 is unlimited
    // this is different than 'number of VMs running at once'
    private long maxWorkspacesInGroup;
    public static final String PROPKEY_MAX_WORKSPACES_IN_GROUP =
            "vws.group.authz.maxWorkspacesInGroup";

    // hostname of image node
    private String imageNodeHostname;
    public static final String PROPKEY_IMAGE_NODE_HOSTNAME =
            "vws.group.authz.imageNodeHostname";

    private String imageBaseDirectory;
    public static final String PROPKEY_IMAGE_BASE_DIRECTORY =
            "vws.group.authz.imageBaseDirectory";

    // only allow to/from directory which is under baseURL + DN hash convention
    private boolean dirHashMode;
    public static final String PROPKEY_DIR_HASH_MODE =
            "vws.group.authz.dirHashMode";

    public static final String[] ALL_REQUIRED_KEYS = {
                                         PROPKEY_MAX_RESERVED_MINUTES,
                                         PROPKEY_MAX_ELAPSED_RESERVED_MINUTES,
                                         PROPKEY_MAX_WORKSPACE_NUMBER,
                                         PROPKEY_MAX_WORKSPACES_IN_GROUP };

    public GroupRights(Properties props, String source) throws Exception {
        if (props == null) {
            return;
        }

        for (int i = 0; i < ALL_REQUIRED_KEYS.length; i++) {
            checkKeyValue(props, ALL_REQUIRED_KEYS[i], source);
        }

        this.maxReservedMinutes =
                    getZeroOrPositiveLong(props,
                                          PROPKEY_MAX_RESERVED_MINUTES,
                                          source);

        this.maxElapsedReservedMinutes =
                    getZeroOrPositiveLong(props,
                                          PROPKEY_MAX_ELAPSED_RESERVED_MINUTES,
                                          source);

        this.maxWorkspaceNumber =
                    getZeroOrPositiveLong(props,
                                          PROPKEY_MAX_WORKSPACE_NUMBER,
                                          source);

        this.maxWorkspacesInGroup =
                    getZeroOrPositiveLong(props,
                                          PROPKEY_MAX_WORKSPACES_IN_GROUP,
                                          source);

        // can be null
        this.groupName =  props.getProperty(PROPKEY_GROUP_NAME);

        // can be null
        this.imageNodeHostname =
                props.getProperty(PROPKEY_IMAGE_NODE_HOSTNAME);

        // can be null
        this.imageBaseDirectory =
                props.getProperty(PROPKEY_IMAGE_BASE_DIRECTORY);

        // can not exit (makes it false)
        final String val = props.getProperty(PROPKEY_DIR_HASH_MODE);
        this.dirHashMode = Boolean.valueOf(val).booleanValue();

        if (this.dirHashMode && this.imageBaseDirectory == null) {
            throw new Exception("Subdirectory DN-hash check is enabled " +
                    "but there is no image base directory configuration");
        }

    }

    private static void checkKeyValue(Properties props,
                                      String key,
                                      String source) throws Exception {
        if (!props.containsKey(key)) {
            throw new Exception(
                    "Group rights properties are invalid, missing key '" +
                    key + "'.  Source: '" + source + "'");
        }

        final String val = props.getProperty(key);
        if (val == null || val.trim().length() == 0) {
            throw new Exception(
                    "Group rights properties are invalid, missing value " +
                    "for key '" + key + "'.  Source: '" + source + "'");
        }
    }

    private static long getZeroOrPositiveLong(Properties props,
                                              String key,
                                              String source) throws Exception {

        final String prop = props.getProperty(key);
        final Long val;
        try {
            val = Long.valueOf(prop);
        } catch (NumberFormatException e) {
            throw new Exception("Group rights properties are invalid, " +
                    "value '" + prop + "' of property '" + key + "' can not " +
                    "be converted to number.  Source: '" + source + "'");
        }
        if (val.longValue() < 0) {
            throw new Exception("Group rights properties are invalid, " +
                    "value '" + prop + "' of property '" + key + "' is " +
                    "less than zero.  Source: '" + source + "'");
        }
        return val.longValue();
    }

    public long getMaxReservedMinutes() {
        return this.maxReservedMinutes;
    }

    public long getMaxElapsedReservedMinutes() {
        return this.maxElapsedReservedMinutes;
    }

    public long getMaxWorkspaceNumber() {
        return this.maxWorkspaceNumber;
    }

    public long getMaxWorkspacesInGroup() {
        return this.maxWorkspacesInGroup;
    }

    public String getImageNodeHostname() {
        return this.imageNodeHostname;
    }

    public String getImageBaseDirectory() {
        return this.imageBaseDirectory;
    }

    public boolean isDirHashMode() {
        return this.dirHashMode;
    }

    public String getName() {
        return this.groupName;
    }

    public String toString() {

        final String name;
        if (this.groupName == null) {
            name = "";
        } else {
            name = " for group '" + this.groupName + "': ";
        }

        return "GroupRights" + name + " {" +
                "maxReservedMinutes=" + this.maxReservedMinutes +
                ", maxElapsedReservedMinutes=" + this.maxElapsedReservedMinutes +
                ", maxWorkspaceNumber=" + this.maxWorkspaceNumber +
                ", maxWorkspacesInGroup=" + this.maxWorkspacesInGroup +
                ", imageNodeHostname='" + this.imageNodeHostname + '\'' +
                ", imageBaseDirectory='" + this.imageBaseDirectory + '\'' +
                ", dirHashMode=" + this.dirHashMode +
                '}';
    }
}
