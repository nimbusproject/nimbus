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

import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.DescribeImagesResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.DescribeImagesType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.DescribeImagesOwnersType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.DescribeImagesOwnerType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.DescribeImagesInfoType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.DescribeImagesItemType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.DescribeImagesResponseInfoType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.DescribeImagesResponseItemType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.BlockDeviceMappingType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.BlockDeviceMappingItemType;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.rm.ContainerInterface;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.service.UnimplementedOperations;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.ServiceImage;
import org.nimbustools.api.repr.Caller;
import org.nimbustools.api.repr.CannotTranslateException;

import java.rmi.RemoteException;
import java.util.List;
import java.util.LinkedList;
import java.net.URL;
import java.net.MalformedURLException;

/**
 * extends UnimplementedOperations to make sure the unimplemented operations of
 * the ServiceImage interface are covered by some implementation.
 */
public class ServiceImageImpl extends UnimplementedOperations
                              implements ServiceImage {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------
    
    protected static final DescribeImagesResponseItemType[] EMPTY_RESP_ITEM_TYPE =
            new DescribeImagesResponseItemType[0];

    
    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected final Repository repository;
    protected final ContainerInterface container;


    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    public ServiceImageImpl(ContainerInterface containerImpl,
                            Repository repositoryImpl) {
        if (containerImpl == null) {
            throw new IllegalArgumentException("containerImpl may not be null");
        }
        this.container = containerImpl;

        if (repositoryImpl == null) {
            throw new IllegalArgumentException("repositoryImpl may not be null");
        }
        this.repository = repositoryImpl;
    }
    

    // -------------------------------------------------------------------------
    // *PARTIALLY* implements ServiceImage
    // -------------------------------------------------------------------------

    public DescribeImagesResponseType describeImages(DescribeImagesType req)
            throws RemoteException {

        if (!this.repository.isListingEnabled()) {
            throw new RemoteException("The describeImages operation has " +
                    "been disabled by the administrator.");
        }

        // no use proceeding if these calls fail:
        final Caller caller = this.container.getCaller();
        final String ownerID;
        try {
            ownerID = this.container.getOwnerID(caller);
        } catch (CannotTranslateException e) {
            throw new RemoteException(e.getMessage(), e);
        }

        if (req == null) {
            throw new RemoteException("describeImages request is missing");
        }

        // ignoring ExecutableBySet

        final String[] ownerScoped = this.getOwnerScopedPossibly(req);

        final String[] nameScoped = this.getNamedScopedPossibly(req);

        final FileListing[] listings;
        try {
            listings = this.repository.listFiles(caller,
                                                 nameScoped,
                                                 ownerScoped);
        } catch (CannotTranslateException e) {
            throw new RuntimeException("Unexpected: " + e.getMessage(), e);
        } catch (ListingException e) {
            throw new RemoteException(
                    "Problem contacting repository: " + e.getMessage(), e);
        }

        return this.convertFileListings(listings, ownerID, caller);
    }

    
    // -------------------------------------------------------------------------
    // DESCRIBE IMPL
    // -------------------------------------------------------------------------

    /**
     * @param req original client request
     * @return owner scopes or null, never length=0
     */
    protected String[] getOwnerScopedPossibly(DescribeImagesType req) {

        if (req == null) {
            return null; // *** EARLY RETURN ***
        }

        final DescribeImagesOwnersType ownersType = req.getOwnersSet();
        if (ownersType == null) {
            return null; // *** EARLY RETURN ***
        }

        final DescribeImagesOwnerType[] diots = ownersType.getItem();
        if (diots == null || diots.length == 0) {
            return null; // *** EARLY RETURN ***
        }

        final List ownerScopes = new LinkedList();
        for (int i = 0; i < diots.length; i++) {
            final DescribeImagesOwnerType diot = diots[i];
            if (diot != null) {
                String owner = diot.getOwner();
                if (owner != null) {
                    owner = owner.trim();
                    if (owner.length() > 0) {
                        ownerScopes.add(owner);
                    }
                }
            }

        }

        if (ownerScopes.isEmpty()) {
            return null;
        } else {
            return (String[])
                        ownerScopes.toArray(new String[ownerScopes.size()]);
        }
    }

    /**
     * @param req original client request
     * @return image name scopes or null, never length=0
     */
    protected String[] getNamedScopedPossibly(DescribeImagesType req) {

        if (req == null) {
            return null; // *** EARLY RETURN ***
        }

        final DescribeImagesInfoType iiT = req.getImagesSet();
        if (iiT == null) {
            return null; // *** EARLY RETURN ***
        }

        final DescribeImagesItemType[] imagesItemTypes = iiT.getItem();
        if (imagesItemTypes == null || imagesItemTypes.length == 0) {
            return null; // *** EARLY RETURN ***
        }

        final List scopedQuery = new LinkedList();
        for (int i = 0; i < imagesItemTypes.length; i++) {
            final DescribeImagesItemType imagesItemType =
                    imagesItemTypes[i];
            if (imagesItemType != null) {
                final String imageid = imagesItemType.getImageId();
                if (imageid != null && imageid.trim().length() > 0) {
                    scopedQuery.add(imageid.trim());
                }
            }
        }

        if (scopedQuery.isEmpty()) {
            return null;
        } else {
            return (String[])
                        scopedQuery.toArray(new String[scopedQuery.size()]);
        }
    }

    protected DescribeImagesResponseType convertFileListings(
                            FileListing[] listings,
                            String ownerID,
                            Caller caller) {

        final DescribeImagesResponseType dirt = new DescribeImagesResponseType();
        final DescribeImagesResponseInfoType dirits =
                                new DescribeImagesResponseInfoType();
        dirt.setImagesSet(dirits);

        if (listings == null || listings.length  == 0) {
            dirits.setItem(EMPTY_RESP_ITEM_TYPE);
            return dirt; // *** EARLY RETURN ***
        }

        String givenLocationBase;

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
            try {
                // all from same place currently
                givenLocationBase = this.repository.getImageLocation(caller, name);
            } catch (CannotTranslateException e) {
                throw new RuntimeException("Unexpected: " + e.getMessage(), e);
            }
            if (givenLocationBase == null) {
                throw new IllegalArgumentException(
                    "givenLocationBase may not be null");
            }
            //String locationBase = cleanLocationBase(givenLocationBase);

            dirit.setImageId(name);
            dirit.setImageLocation(givenLocationBase + "/");
            dirit.setImageOwnerId(ownerID);
            dirit.setImageState("available"); // todo
            dirit.setImageType("machine"); // todo
            dirit.setIsPublic(!listing.isReadWrite()); // cloud convention
            dirit.setKernelId("default-kernel"); // todo
            dirit.setRamdiskId("default-ramdisk"); // todo
            dirit.setBlockDeviceMapping(new BlockDeviceMappingType(
                    new BlockDeviceMappingItemType[]{}));
            retList.add(dirit);
        }

        dirits.setItem(
              (DescribeImagesResponseItemType[])
                  retList.toArray(
                          new DescribeImagesResponseItemType[retList.size()])
                        );

        return dirt;
    }
    
    private static String cleanLocationBase(String givenLocationBase) {

        if (givenLocationBase == null
                || givenLocationBase.trim().length() == 0) {
            throw new IllegalArgumentException("empty givenLocationBase");
        }

        // XXX it seems historically this could only be gsiftp so the exception will always trip
        URL url = null;
        try {
            url = new URL(givenLocationBase);
        } catch (MalformedURLException e) {

            // CUMULUS is an unknown scheme for Java
            // replace just gsiftp and check URL
            String newTestURL = givenLocationBase.trim();
            if (newTestURL.startsWith("cumulus")) {
                newTestURL = newTestURL.replaceFirst("cumulus", "http");
                try {
                    url = new URL(newTestURL);
                } catch (MalformedURLException e2) {
                    throw new IllegalArgumentException(
                            "invalid givenLocationBase?");
                }
            }
        }

        if (url == null) {
            throw new IllegalArgumentException(
                    "invalid givenLocationBase? (no url)");
        }

        // This will be http now?  i am confused
        final String scheme = url.getProtocol();
        if (scheme == null) {
            throw new IllegalArgumentException(
                    "invalid givenLocationBase? (no scheme)");
        }

        final String host = url.getHost();
        if (host == null) {
            throw new IllegalArgumentException(
                    "invalid givenLocationBase? (no host)");
        }

        final int port = url.getPort();
        return scheme + "://" + host + ":" + port;
    }
}
