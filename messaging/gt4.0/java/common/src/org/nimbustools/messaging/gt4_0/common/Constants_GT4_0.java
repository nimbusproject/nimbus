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

package org.nimbustools.messaging.gt4_0.common;

import javax.xml.namespace.QName;

/**
 * Globus stuff (4.0.x).
 */
public interface Constants_GT4_0 {

    public static final String SERVICE_SECURITY_CONFIG =
                                   "etc/nimbus/security-config.xml";

    public static final String NS = "http://www.globus.org/2008/06/workspace";
    public static final String NS_TYPES = NS + "/types";
    public static final String NS_GROUP = NS + "/group";
    public static final String NS_ENSEMBLE = NS + "/ensemble";
    public static final String NS_STATUS = NS + "/status";
    public static final String NS_METADATA = NS + "/metadata";
    public static final String NS_LOGISTICS = NS + "/metadata/logistics";
    public static final String NS_DEFINITION = NS + "/metadata/definition";
    public static final String NS_NEGOTIABLE = NS + "/negotiable";
    public static final String NS_JSDL = "http://schemas.ggf.org/jsdl/2005/11/jsdl";

    public static final String OLD_NS_CONTEXTUALIZATION = NS + "/contextualization";
    public static final String NS_NIMBUS_CONTEXTUALIZATION =
            "http://www.globus.org/2008/12/nimbus/contextualization";

    // service names

    public static final String SERVICE_PATH = "WorkspaceService";
    public static final String FACTORY_PATH = "WorkspaceFactoryService";
    public static final String GROUP_SERVICE_PATH = "WorkspaceGroupService";
    public static final String ENSEMBLE_SERVICE_PATH = "WorkspaceEnsembleService";
    public static final String STATUS_PATH = "WorkspaceStatusService";
    public static final String OLD_CTX_BROKER_PATH = "WorkspaceContextBroker";

    // key names

    public static final QName RESOURCE_KEY_QNAME =
            new QName(NS, "WorkspaceKey");

    public static final QName GROUP_RESOURCE_KEY_QNAME =
            new QName(NS_GROUP, "WorkspaceGroupKey");

    public static final QName ENSEMBLE_RESOURCE_KEY_QNAME =
            new QName(NS_ENSEMBLE, "WorkspaceEnsembleKey");

    public static final QName CONTEXTUALIZATION_RESOURCE_KEY_QNAME =
            new QName(OLD_NS_CONTEXTUALIZATION, "WorkspaceContextKey");

    public static final QName OLD_CONTEXTUALIZATION_RESOURCE_KEY_QNAME =
            CONTEXTUALIZATION_RESOURCE_KEY_QNAME;

    public static final QName NIMBUS_CONTEXTUALIZATION_RESOURCE_KEY_QNAME =
            new QName(NS_NIMBUS_CONTEXTUALIZATION, "NimbusContextBrokerKey");

    public static final String FACTORY_DEFAULT_RSRC_KEY_NAME = "default";

    // factory RPs

    public static final QName FACTORY_RP_SET =
            new QName(NS, "FactoryRPSet");

    public static final QName RP_FACTORY_DefTTL =
            new QName(NS_TYPES, "DefaultRunningTime");

    public static final QName RP_FACTORY_MaxTTL =
            new QName(NS_TYPES, "MaximumRunningTime");

    public static final QName RP_FACTORY_VMM =
            new QName(NS_DEFINITION, "VMM");

    public static final QName RP_FACTORY_ASSOCIATIONS =
            new QName(NS_TYPES, "Associations");

    public static final QName RP_FACTORY_CPUArch =
            new QName(NS_JSDL, "CPUArchitectureName");

    // service RPs

    public static final QName RP_SET = new QName(NS, "ServiceRPSet");

    public static final QName RP_CURRENT_STATE =
                           new QName(NS_TYPES, "currentState");
    public static final QName RP_SCHEDULE =
                           new QName(NS_TYPES, "schedule");
    public static final QName RP_RESOURCE_ALLOCATION =
                           new QName(NS_NEGOTIABLE, "ResourceAllocation");
    public static final QName RP_LOGISTICS =
                           new QName(NS_LOGISTICS, "logistics");

    // status service RPs

    public static final QName STATUS_RP_SET =
                           new QName(NS_STATUS, "StatusRPSet");
    public static final QName RP_STATUS_CHARGEGRAN =
                           new QName(NS_STATUS, "chargeGranularity");

    // context broker RPs

    // namespace: http://www.globus.org/2008/06/workspace/contextualization

    public static final QName CONTEXTUALIZATION_RP_SET =
                                    new QName(OLD_NS_CONTEXTUALIZATION,
                                              "ContextBrokerRPSet");

    public static final QName OLD_CONTEXTUALIZATION_RP_SET =
                                    new QName(OLD_NS_CONTEXTUALIZATION,
                                              "ContextBrokerRPSet");

    public static final QName OLD_RP_CONTEXTUALIZATION_CONTEXT =
                           new QName(NS_TYPES, "contextualizationContext");

    public static final QName RP_CONTEXTUALIZATION_CONTEXT =
                           new QName(NS_TYPES, "contextualizationContext");


    // namespace: http://www.globus.org/2008/12/nimbus/ctxtypes

    public static final QName CTXBROKER_RP =
            new QName("http://www.globus.org/2008/12/nimbus/ctxtypes",
                      "contextualizationContext");
}
