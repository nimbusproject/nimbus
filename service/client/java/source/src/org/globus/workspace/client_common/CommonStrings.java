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

package org.globus.workspace.client_common;

import org.nimbustools.messaging.gt4_0.common.CommonUtil;
import org.nimbustools.messaging.gt4_0.generated.types.WorkspaceUnknownFault;
import org.oasis.wsrf.faults.BaseFaultType;

public class CommonStrings {

    public static String faultStringOrCommonCause(BaseFaultType e) {
        final String common = anyCommonCause(e);
        if (common == null) {
            return CommonUtil.faultString(e);
        } else {
            return common;
        }
    }

    public static String faultStringOrCommonCause(BaseFaultType e,
                                                  String type) {
        final String common = anyCommonCause(e, type);
        if (common == null) {
            return CommonUtil.faultString(e);
        } else {
            return common;
        }
    }

    public static String anyCommonCause(Exception e, String type) {

        String useType = type;
        if (type == null) {
            useType = "workspace";
        }

        if (e instanceof WorkspaceUnknownFault) {
            return resourceUnknownError(useType);
        } else if (e instanceof org.oasis.wsrf.properties.ResourceUnknownFaultType) {
            return resourceUnknownError(useType);
        } else if (e instanceof org.oasis.wsrf.lifetime.ResourceUnknownFaultType) {
            return resourceUnknownError(useType);
        } else if (e instanceof org.oasis.wsn.ResourceUnknownFaultType) {
            return resourceUnknownError(useType);
        }
        return null;
    }

    public static String anyCommonCause(Exception e) {
        return anyCommonCause(e, null);
    }

    private static String resourceUnknownError(String type) {
        return "This " + type + " is unknown to the service " +
                        "(likely because it was terminated).";
    }
    
}
