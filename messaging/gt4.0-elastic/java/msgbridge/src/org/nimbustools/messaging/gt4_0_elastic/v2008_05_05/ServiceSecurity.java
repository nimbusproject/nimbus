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

package org.nimbustools.messaging.gt4_0_elastic.v2008_05_05;

import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.AuthorizeSecurityGroupIngressResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.AuthorizeSecurityGroupIngressType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.CreateKeyPairResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.CreateKeyPairType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.CreateSecurityGroupResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.CreateSecurityGroupType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.DeleteKeyPairResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.DeleteKeyPairType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.DeleteSecurityGroupResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.DeleteSecurityGroupType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.DescribeKeyPairsResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.DescribeKeyPairsType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.DescribeSecurityGroupsResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.DescribeSecurityGroupsType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.ImportKeyPairResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.ImportKeyPairType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.RevokeSecurityGroupIngressResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.RevokeSecurityGroupIngressType;

import java.rmi.RemoteException;

public interface ServiceSecurity {
        
    // -------------------------------------------------------------------------
    // SECURITY RELATED
    // -------------------------------------------------------------------------

    public CreateKeyPairResponseType createKeyPair(
                        CreateKeyPairType createKeyPairRequestMsg)
            throws RemoteException;


    public ImportKeyPairResponseType importKeyPair(
            ImportKeyPairType importKeyPairRequestMsg)
            throws RemoteException;

    public DescribeKeyPairsResponseType describeKeyPairs(
                        DescribeKeyPairsType describeKeyPairsRequestMsg)
            throws RemoteException;

    public DeleteKeyPairResponseType deleteKeyPair(
                        DeleteKeyPairType deleteKeyPairRequestMsg)
            throws RemoteException;

    public CreateSecurityGroupResponseType createSecurityGroup(
                        CreateSecurityGroupType createSecurityGroupRequestMsg)
            throws RemoteException;

    public DeleteSecurityGroupResponseType deleteSecurityGroup(
                        DeleteSecurityGroupType deleteSecurityGroupRequestMsg)
            throws RemoteException;

    public DescribeSecurityGroupsResponseType describeSecurityGroups(
                        DescribeSecurityGroupsType describeSecurityGroupsRequestMsg)
            throws RemoteException;

    public AuthorizeSecurityGroupIngressResponseType authorizeSecurityGroupIngress(
                        AuthorizeSecurityGroupIngressType authorizeSecurityGroupIngressRequestMsg)
            throws RemoteException;

    public RevokeSecurityGroupIngressResponseType revokeSecurityGroupIngress(
                        RevokeSecurityGroupIngressType revokeSecurityGroupIngressRequestMsg)
            throws RemoteException;
}
