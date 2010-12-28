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

import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.*;

import java.rmi.RemoteException;

public interface ServiceBlockStore {

    // -------------------------------------------------------------------------
    // ELASTIC BLOCK STORE RELATED
    // -------------------------------------------------------------------------

    public CreateVolumeResponseType createVolume(
                CreateVolumeType createVolumeRequestMsgReq)
            throws RemoteException;

    public DeleteVolumeResponseType deleteVolume(
                DeleteVolumeType deleteVolumeRequestMsgReq)
            throws RemoteException;

    public DescribeVolumesResponseType describeVolumes(
                DescribeVolumesType describeVolumesRequestMsgReq)
            throws RemoteException;

    public AttachVolumeResponseType attachVolume(
                AttachVolumeType attachVolumeRequestMsgReq)
            throws RemoteException;

    public DetachVolumeResponseType detachVolume(
                DetachVolumeType detachVolumeRequestMsgReq)
            throws RemoteException;

    public CreateSnapshotResponseType createSnapshot(
                CreateSnapshotType createSnapshotRequestMsgReq)
            throws RemoteException;

    public DeleteSnapshotResponseType deleteSnapshot(
                DeleteSnapshotType deleteSnapshotRequestMsgReq)
            throws RemoteException;

    public DescribeSnapshotsResponseType describeSnapshots(
                DescribeSnapshotsType describeSnapshotsRequestMsgReq)
            throws RemoteException;

    public ModifySnapshotAttributeResponseType modifySnapshotAttribute(
            ModifySnapshotAttributeType modifySnapshotAttributeRequestMsgReq)
            throws RemoteException;

    public ResetSnapshotAttributeResponseType resetSnapshotAttribute(
            ResetSnapshotAttributeType resetSnapshotAttributeRequestMsgReq)
            throws RemoteException;

    public DescribeSnapshotAttributeResponseType describeSnapshotAttribute(
            DescribeSnapshotAttributeType describeSnapshotAttributeRequestMsgReq)
            throws RemoteException;
}
