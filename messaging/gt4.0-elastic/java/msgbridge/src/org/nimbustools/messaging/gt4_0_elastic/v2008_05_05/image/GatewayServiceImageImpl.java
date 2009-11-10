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

package org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.image;

import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.rm.ContainerInterface;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.DescribeImagesResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.DescribeImagesResponseInfoType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.DescribeImagesResponseItemType;

import java.util.List;
import java.util.LinkedList;

public class GatewayServiceImageImpl extends ServiceImageImpl {

    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    public GatewayServiceImageImpl(ContainerInterface containerImpl,
                                   Repository repositoryImpl) {
        super(containerImpl, repositoryImpl);
    }

    // -------------------------------------------------------------------------
    // extends ServiceImageImpl
    // -------------------------------------------------------------------------

    protected DescribeImagesResponseType convertFileListings(
                                                FileListing[] listings,
                                                String ownerID,
                                                String givenLocationBase) {

        // "givenLocationBase" is ignored.

        final DescribeImagesResponseType dirt = new DescribeImagesResponseType();
        final DescribeImagesResponseInfoType dirits =
                                new DescribeImagesResponseInfoType();
        dirt.setImagesSet(dirits);

        if (listings == null || listings.length  == 0) {
            dirits.setItem(EMPTY_RESP_ITEM_TYPE);
            return dirt; // *** EARLY RETURN ***
        }

        final List retList = new LinkedList();
        for (int i = 0; i < listings.length; i++) {
            final FileListing listing = listings[i];
            if (listing == null) {
                continue; // *** SKIP ***
            }

            final DescribeImagesResponseItemType dirit =
                                    new DescribeImagesResponseItemType();
            dirit.setArchitecture("i386"); // todo

            final String name = listing.getName();
            dirit.setImageId(name);
            dirit.setImageLocation("");
            dirit.setImageOwnerId(ownerID);
            dirit.setImageState("available"); // todo
            dirit.setImageType("machine"); // todo
            dirit.setIsPublic(!listing.isReadWrite()); // cloud convention
            dirit.setKernelId("default-kernel"); // todo
            dirit.setRamdiskId("default-ramdisk"); // todo
            retList.add(dirit);
        }

        dirits.setItem(
              (DescribeImagesResponseItemType[])
                  retList.toArray(
                          new DescribeImagesResponseItemType[retList.size()])
                        );

        return dirt;
    }
    
}
