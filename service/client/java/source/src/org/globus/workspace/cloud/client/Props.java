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

package org.globus.workspace.cloud.client;

public class Props {

    public static final String
            KEY_FACTORY_IDENTITY = "vws.factory.identity";

    public static final String
            KEY_GRIDFTP_IDENTITY = "vws.repository.identity";
    
    public static final String
            KEY_SSHFILE = "ssh.pubkey";

    public static final String
            KEY_SSH_KNOWN_HOSTS = "ssh.hostsfile";

    public static final String
            KEY_FACTORY_HOSTPORT = "vws.factory";

    public static final String
            KEY_GRIDFTP_HOSTPORT = "vws.repository";

    public static final String
            KEY_TARGET_BASEDIR = "vws.repository.basedir";

    public static final String
            KEY_POLL_INTERVAL = "vws.poll.interval";

    public static final String
            KEY_USE_NOTIFICATIONS = "vws.usenotifications";

    public static final String
            KEY_XFER_TIMEOUT = "vws.gridftp.timeout";

    public static final String
            KEY_CAHASH = "vws.cahash";

    public static final String
            KEY_MEMORY_REQ = "vws.memory.request";

    public static final String
            KEY_PROPAGATION_SCHEME = "vws.propagation.scheme";

    public static final String
            KEY_PROPAGATION_KEEPPORT = "vws.propagation.keepport";

    // ----

    public static final String
            KEY_METADATA_ASSOCIATION = "vws.metadata.association";

    public static final String
            KEY_METADATA_MOUNTAS = "vws.metadata.mountAs";
    
    public static final String
            KEY_METADATA_NICNAME = "vws.metadata.nicName";

    public static final String
            KEY_METADATA_CPUTYPE = "vws.metadata.cpuType";

    public static final String
            KEY_METADATA_VMMTYPE = "vws.metadata.vmmType";
    
    public static final String
            KEY_METADATA_VMMVERSION = "vws.metadata.vmmVersion";

    public static final String
            KEY_METADATA_FILENAME = "vws.metadata.fileName";

    public static final String
            KEY_DEPREQ_FILENAME = "vws.depreq.fileName";

    // ----

    public static final String
            KEY_BROKER_PUB = "broker.publicnic.prefix";

    public static final String
            KEY_BROKER_LOCAL = "broker.localnic.prefix";
    
    public static final String
            KEY_BROKER_URL = "broker.url";

    public static final String
            KEY_BROKER_IDENTITY = "broker.identity";
}
