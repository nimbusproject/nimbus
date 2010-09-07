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

package org.nimbustools.ctxbroker;

import javax.xml.namespace.QName;

public class BrokerConstants {

    public static final String NS = "http://www.globus.org/2008/12/nimbus";
    public static final String NS_CTX = NS + "/contextualization";
    public static final String NS_CTXTYPES = NS + "/ctxtypes";
    public static final String NS_CTXDESC = NS + "/ctxdescription";

    public static final QName CONTEXTUALIZATION_RP_SET =
                            new QName(NS_CTX, "ContextBrokerRPSet");

    public static final QName RP_CONTEXTUALIZATION_CONTEXT =
                            new QName(NS_CTXTYPES, "contextualizationContext");

    public static final QName CONTEXTUALIZATION_RESOURCE_KEY_QNAME =
                            new QName(NS_CTX, "NimbusContextBrokerKey");

    public static final String SERVICE_SECURITY_CONFIG =
                                   "etc/nimbus-context-broker/security-config.xml";

    public static final String CTX_BROKER_PATH = "NimbusContextBroker";
}
