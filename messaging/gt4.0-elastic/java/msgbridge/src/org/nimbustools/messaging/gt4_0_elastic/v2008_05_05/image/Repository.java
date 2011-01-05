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

import org.nimbustools.api.repr.vm.VMFile;
import org.nimbustools.api.repr.vm.ResourceAllocation;
import org.nimbustools.api.repr.Caller;
import org.nimbustools.api.repr.CannotTranslateException;

public interface Repository {

    public VMFile[] constructFileRequest(String imageID,
                                         ResourceAllocation ra,
                                         Caller caller)
            throws CannotTranslateException;

    public String getImageLocation(Caller caller, String vmname)
            throws CannotTranslateException;

    public boolean isListingEnabled();

    /**
     * List files in remote client's repository directory.
     * 
     * @param caller remote client
     * @param nameScoped filter the query by image name
     * @param ownerScoped filter the query by owner
     * @return all caller's file or zero-length array if none
     * @throws CannotTranslateException problem with internal bridge layer
     * @throws ListingException problem contacting repository or disabled
     */
    public FileListing[] listFiles(Caller caller,
                                   String[] nameScoped,
                                   String[] ownerScoped)
            throws CannotTranslateException, ListingException;
}
