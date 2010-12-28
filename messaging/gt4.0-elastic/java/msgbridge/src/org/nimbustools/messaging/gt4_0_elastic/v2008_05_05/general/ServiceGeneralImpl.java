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

package org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.general;

import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.ServiceGeneral;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.service.UnimplementedOperations;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.DescribeAvailabilityZonesResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.DescribeAvailabilityZonesType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.DescribeAvailabilityZonesSetType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.DescribeAvailabilityZonesSetItemType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.AvailabilityZoneSetType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.AvailabilityZoneItemType;

import java.rmi.RemoteException;
import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;

/**
 * extends UnimplementedOperations to make sure the unimplemented parts of the
 * General interface are covered by some implementation
 */
public class ServiceGeneralImpl extends UnimplementedOperations
                                implements ServiceGeneral {

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected final AvailabilityZones zones;


    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    public ServiceGeneralImpl(AvailabilityZones zones) {
        if (zones == null) {
            throw new IllegalArgumentException("zones may not be null");
        }
        this.zones = zones;
    }

    
    // -------------------------------------------------------------------------
    // *PARTIALLY* implements ServiceGeneral
    // -------------------------------------------------------------------------

    public DescribeAvailabilityZonesResponseType describeAvailabilityZones(
            DescribeAvailabilityZonesType describeAvailabilityZonesRequestMsg)
            throws RemoteException {

        final DescribeAvailabilityZonesResponseType response =
                new DescribeAvailabilityZonesResponseType();

        final AvailabilityZoneSetType availabilityZoneSetType =
                    new AvailabilityZoneSetType();
        response.setAvailabilityZoneInfo(availabilityZoneSetType);

        final String[] zoneNames = this.zoneNames();

        if (zoneNames == null || zoneNames.length == 0) {
            availabilityZoneSetType.setItem(new AvailabilityZoneItemType[0]);
            return response;
        }

        // this is pretty awesome
        final List scopedQuery = new LinkedList();
        if (describeAvailabilityZonesRequestMsg != null) {
            final DescribeAvailabilityZonesSetType availabilityZoneSet =
                describeAvailabilityZonesRequestMsg.getAvailabilityZoneSet();
            if (availabilityZoneSet != null) {
                final DescribeAvailabilityZonesSetItemType[] item =
                        availabilityZoneSet.getItem();
                if (item != null) {
                    for (int i = 0; i < item.length; i++) {
                        final DescribeAvailabilityZonesSetItemType
                                describeAvailabilityZonesSetItemType = item[i];
                        if (describeAvailabilityZonesSetItemType != null) {
                            final String zone =
                                    describeAvailabilityZonesSetItemType
                                            .getZoneName();
                            if (zone != null) {
                                for (int j = 0; j < zoneNames.length; j++) {
                                    final String zoneName = zoneNames[j];
                                    if (zoneName.equals(zone)) {
                                        scopedQuery.add(zone);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        final AvailabilityZoneItemType[] azits;
        if (scopedQuery.isEmpty()) {
            azits = new AvailabilityZoneItemType[zoneNames.length];
            for (int i = 0; i < zoneNames.length; i++) {
                final String zoneName = zoneNames[i];
                azits[i] = this.getOneZone(zoneName);
            }
        } else {
            azits = new AvailabilityZoneItemType[scopedQuery.size()];
            int idx = 0;
            final Iterator iter = scopedQuery.iterator();
            while (iter.hasNext()) {
                azits[idx] = this.getOneZone((String)iter.next());
                idx += 1;
                if (idx == zoneNames.length) {
                    break;
                }
            }
        }

        availabilityZoneSetType.setItem(azits);
        return response;
        
    }

    protected String[] zoneNames() {
        return this.zones.getAvailabilityZones();
    }

    protected AvailabilityZoneItemType getOneZone(String name) {
        final AvailabilityZoneItemType ret = new AvailabilityZoneItemType();
        ret.setZoneName(name);
        ret.setZoneState("available"); // this is all a stub for the future
        return ret;
    }
}
