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

package org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.image.defaults;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nimbustools.api._repr.vm._VMFile;
import org.nimbustools.api.brain.ModuleLocator;
import org.nimbustools.api.repr.Caller;
import org.nimbustools.api.repr.CannotTranslateException;
import org.nimbustools.api.repr.ReprFactory;
import org.nimbustools.api.repr.vm.ResourceAllocation;
import org.nimbustools.api.repr.vm.VMFile;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.image.FileListing;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.image.ListingException;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.image.Repository;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.rm.ContainerInterface;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;


public class DefaultGatewayRepository implements Repository {
    
    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(DefaultGatewayRepository.class.getName());


    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected final ContainerInterface container;
    protected final ReprFactory repr;
    protected String baseDirectory;
    protected String rootFileMountAs;

    protected boolean enableListing;


    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------

    public DefaultGatewayRepository(ContainerInterface containerImpl,
                                    ModuleLocator locator)
            throws Exception {

        if (containerImpl == null) {
            throw new IllegalArgumentException("containerImpl may not be null");
        }
        this.container = containerImpl;

        if (locator == null) {
            throw new IllegalArgumentException("locator may not be null");
        }
        this.repr = locator.getReprFactory();
    }


    // -------------------------------------------------------------------------
    // GET/SET
    // -------------------------------------------------------------------------

    public String getRootFileMountAs() {
        return this.rootFileMountAs;
    }

    public void setRootFileMountAs(String rootFileMountAs) {
        this.rootFileMountAs = rootFileMountAs;
    }

    public boolean isListingEnabled() {
        return this.enableListing;
    }

    public void setEnableListing(boolean enableListing) {
        this.enableListing = enableListing;
    }


    // -------------------------------------------------------------------------
    // IoC INIT METHOD
    // -------------------------------------------------------------------------

    void validate() throws Exception {

        if (this.rootFileMountAs == null
                || this.rootFileMountAs.trim().length() == 0) {

            throw new Exception("Invalid: Missing 'rootFileMountAs' string");
        }
    }


    // -------------------------------------------------------------------------
    // implements Repository
    // -------------------------------------------------------------------------

    // not applicable
    public String getImageLocation(Caller caller)
            throws CannotTranslateException {
        return null;
    }

    public VMFile[] constructFileRequest(String imageID,
                                         ResourceAllocation ra,
                                         Caller caller)
            throws CannotTranslateException {

        return new VMFile[]{this.getGatewayFile(imageID, null)};
    }

    public FileListing[] listFiles(Caller caller,
                                   String[] nameScoped,
                                   String[] ownerScoped)
            throws CannotTranslateException, ListingException {

        if (!this.enableListing) {
            throw new ListingException(
                    "Repository interaction has been disabled");
        }

        final String ownerID = this.container.getOwnerID(caller);
        try {
            return this.listFilesImpl(ownerID);
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.error(e.getMessage(), e);
            } else {
                logger.error(e.getMessage());
            }
            throw new ListingException("Failed to list repository files.");
        }
    }


    // -------------------------------------------------------------------------
    // GATEWAY FILE
    // -------------------------------------------------------------------------

    protected VMFile getGatewayFile(String imageID, String ownerID)
            throws CannotTranslateException {

        // "ownerID" is ignored.

        if (imageID == null) {
            throw new CannotTranslateException("Request is missing image ID");
        }

        final URI amiuri;
        try {
            amiuri = new URI("ec2ami://" + imageID);
        } catch (URISyntaxException e) {
            throw new CannotTranslateException(e.getMessage(), e);
        }

        final _VMFile file = this.repr._newVMFile();
        file.setRootFile(true);
        file.setDiskPerms(VMFile.DISKPERMS_ReadWrite);
        file.setMountAs(this.getRootFileMountAs());
        file.setURI(amiuri);
        return file;
    }


    // -------------------------------------------------------------------------
    // LIST FILES
    // -------------------------------------------------------------------------

    protected FileListing[] listFilesImpl(String ownerID)
                throws Exception {

        if (ownerID == null) {
            throw new CannotTranslateException("Cannot contact repository " +
                    "without owner ID");
        }

        final ArrayList amiIDs = new ArrayList();

        // TODO: put text file (owner->amis) back in, rigging this:
        // amiIDs.add(this.getOneListing("ami-cc9a7da5"));
        // amiIDs.add(this.getOneListing("ami-dd8b6cb4"));

        return (FileListing[]) amiIDs.toArray(new FileListing[amiIDs.size()]);
    }

    protected FileListing getOneListing(String ami) throws Exception {
        if (ami == null) {
            throw new IllegalArgumentException("ami may not be null");
        }

        final FileListing fl = new FileListing();
        fl.setName(ami);
        fl.setSize(-1);
        fl.setDirectory(false);
        fl.setReadWrite(false);
        fl.setDate("");
        fl.setTime("");
        return fl;
    }
}
