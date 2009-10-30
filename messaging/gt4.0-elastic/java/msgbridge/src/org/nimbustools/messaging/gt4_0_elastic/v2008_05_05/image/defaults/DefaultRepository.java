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
import org.globus.ftp.GridFTPClient;
import org.globus.ftp.MlsxEntry;
import org.globus.ftp.Session;
import org.globus.gsi.gssapi.auth.HostAuthorization;
import org.globus.gsi.gssapi.auth.IdentityAuthorization;
import org.globus.util.GlobusURL;
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

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Vector;

public class DefaultRepository implements Repository {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(DefaultRepository.class.getName());
    

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected final ContainerInterface container;
    protected final ReprFactory repr;
    protected String baseDirectory;
    protected String scheme;
    protected String rootFileMountAs;

    protected boolean enableListing;
    protected String idAuthz;


    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------

    public DefaultRepository(ContainerInterface containerImpl,
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

    public void setBaseDirectory(String baseDirectoryStr) throws Exception {
        this.baseDirectory = baseDirectoryStr;
    }

    public String getBaseDirectory() {
        return this.baseDirectory;
    }

    public void setScheme(String scheme) throws Exception {
        this.scheme = scheme;
    }

    public String getScheme() {
        return this.scheme;
    }

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

    public String getIdAuthz() {
        return this.idAuthz;
    }

    public void setIdAuthz(String idAuthz) {
        this.idAuthz = idAuthz;
    }
    

    // -------------------------------------------------------------------------
    // IoC INIT METHOD
    // -------------------------------------------------------------------------

    void validate() throws Exception {

        if (this.rootFileMountAs == null
                || this.rootFileMountAs.trim().length() == 0) {

            throw new Exception("Invalid: Missing 'rootFileMountAs' string");
        }

        if (this.baseDirectory == null ||
                this.baseDirectory.trim().length() == 0) {
            
            throw new Exception("Missing the 'baseDirectory' setting");
        }

        try {
            new URL(this.baseDirectory);
        } catch (MalformedURLException e) {

            // gsiftp is an unknown scheme for Java
            // replace just gsiftp and check URL
            String newTestURL = this.baseDirectory.trim();
            if (newTestURL.startsWith("gsiftp")) {
                newTestURL = newTestURL.replaceFirst("gsiftp", "http");
                try {
                    new URL(newTestURL);
                } catch (MalformedURLException e2) {
                    throw new Exception("URL is invalid: " + this.baseDirectory);
                }
            }
        }

        if (this.enableListing) {
            if (this.idAuthz != null && this.idAuthz.trim().length() == 0) {
                this.idAuthz = null;
            }
        }
    }
    

    // -------------------------------------------------------------------------
    // implements Repository
    // -------------------------------------------------------------------------

    public String getImageLocation(Caller caller)
            throws CannotTranslateException {

        final String ownerID = this.container.getOwnerID(caller);

        if (ownerID == null) {
            throw new CannotTranslateException("No caller/ownerID?");
        }

        return this.getBaseDirectory() + "/" + ownerID + "/";
    }

    public VMFile[] constructFileRequest(String imageID,
                                         ResourceAllocation ra,
                                         Caller caller)
            throws CannotTranslateException {

        final String ownerID = this.container.getOwnerID(caller);

        if (ownerID == null) {
            throw new CannotTranslateException("Cannot construct file " +
                    "request without owner hash/ID, the file(s) location is " +
                    "based on it");
        }

        // todo: look at RA and construct blankspace request
        return new VMFile[]{this.getRootFile(imageID, ownerID)};
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
            return this.listFilesImpl(ownerID, nameScoped, ownerScoped);
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
    // ROOT FILE
    // -------------------------------------------------------------------------
    
    protected VMFile getRootFile(String imageID, String ownerID)
            throws CannotTranslateException {

        if (imageID == null) {
            throw new CannotTranslateException("Request is missing image ID");
        }

        if (ownerID == null) {
            throw new CannotTranslateException("Request is missing image ID");
        }

        String baseDir = this.getBaseDirectory();
        if (baseDir == null) {
            throw new CannotTranslateException("base directory missing");
        }

        final String imageURIstr = baseDir + "/" + ownerID + "/" + imageID;
        URI imageURI;
        try {
            imageURI = new URI(imageURIstr);
        } catch (URISyntaxException e) {
            throw new CannotTranslateException(e.getMessage(), e);
        }

        if (this.scheme != null && this.scheme.equals("scp")) {
            baseDir = baseDir.replaceFirst("gsiftp", "http");
            final URL url;
            try {
                url = new URL(baseDir);
            } catch (MalformedURLException e) {
                throw new CannotTranslateException("unexpected, invalid URL: " +
                        imageURI.toASCIIString(), e);
            }
            if (url.getPort() != 22) {
                String newurl = "scp://";
                newurl += url.getHost();
                newurl += ":22";
                newurl += url.getPath() + "/" + ownerID + "/" + imageID;
                try {
                    imageURI = new URI(newurl);
                } catch (URISyntaxException e) {
                    throw new CannotTranslateException(e.getMessage(), e);
                }
            }
        }


        final _VMFile file = this.repr._newVMFile();
        file.setRootFile(true);
        file.setDiskPerms(VMFile.DISKPERMS_ReadWrite);
        file.setMountAs(this.getRootFileMountAs());
        file.setURI(imageURI);
        return file;
    }


    // -------------------------------------------------------------------------
    // LIST FILES
    // -------------------------------------------------------------------------

    // generally a code dup from cloud client
    protected FileListing[] listFilesImpl(String ownerID,
                                          String[] nameScoped,
                                          String[] ownerScoped)
                throws Exception {

        if (ownerID == null) {
            throw new CannotTranslateException("Cannot contact repository " +
                    "without owner ID");
        }

        final String directoryURLstr =
                this.getBaseDirectory() + "/" + ownerID + "/";
        
        final GlobusURL listdir = new GlobusURL(directoryURLstr);

        final GridFTPClient client =
                new GridFTPClient(listdir.getHost(), listdir.getPort());

        final String idauthz = this.getIdAuthz();
        if (idauthz == null) {
            client.setAuthorization(HostAuthorization.getInstance());
        } else {
            final IdentityAuthorization idA =
                    new IdentityAuthorization(idauthz);
            client.setAuthorization(idA);
        }

        client.authenticate(null);
        client.setType(Session.TYPE_ASCII);
        client.setPassive();
        client.setLocalActive();
        client.changeDir(listdir.getPath());
        final Vector v = client.mlsd(null);
        int len = v.size();
        final ArrayList files = new ArrayList(len);
        while (! v.isEmpty()) {
            final MlsxEntry f = (MlsxEntry)v.remove(0);
            if (f == null) {
                continue; // *** SKIP ***
            }

            final String fileName = f.getFileName();
            if (fileName == null) {
                continue; // *** SKIP ***
            }

            if (fileName.equals(".")) {
                len -= 1;
                continue; // *** SKIP ***
            }
            if (fileName.equals("..")) {
                len -= 1;
                continue; // *** SKIP ***
            }

            if (nameScoped == null || nameScoped.length == 0) {
                final FileListing listing = this.getOneListing(f, ownerScoped);
                if (listing != null) {
                    files.add(listing);
                }
            } else {

                for (int i = 0; i < nameScoped.length; i++) {
                    if (fileName.equals(nameScoped[i])) {
                        final FileListing listing =
                                this.getOneListing(f, ownerScoped);
                        if (listing != null) {
                            files.add(listing);
                        }
                        break; // assuming unique file names..
                    }
                }
            }
        }

        client.close();

        return (FileListing[]) files.toArray(new FileListing[files.size()]);
    }

    protected FileListing getOneListing(MlsxEntry f,
                                        String[] ownerScoped) throws Exception {

        if (f == null) {
            throw new IllegalArgumentException("f may not be null");
        }

        final FileListing fl = new FileListing();

        fl.setName(f.getFileName());

        final String sizeStr = f.get("size");
        if (sizeStr == null) {
            fl.setSize(-1);
        } else {
            long x = -1;
            try {
                x = Long.parseLong(sizeStr);
            } catch (NumberFormatException e) {
                // pass.
            }
            fl.setSize(x);
        }

        final String modified = f.get("modify");
        // 20080522161726
        if (modified == null || modified.length() != 14) {
            throw new Exception("cannot parse modified time");
        }
        fl.setDate(parseDate(modified));
        fl.setTime(parseTime(modified));

        final String type = f.get("type");
        if (type != null && type.equals("dir")) {
            fl.setDirectory(true);
        }


        // If user is root and perms are group no-write and all no-write,
        // we can, because of several conventions, safely say that the user
        // only has read-only access.
        final String owner = f.get("unix.owner");
        final String mode = f.get("unix.mode");
        if (mode == null) {
            fl.setReadWrite(false);
        } else if (mode.substring(3,4).equals("6")) {
            fl.setReadWrite(true);
        } else if (mode.substring(1,2).equals("6")) {
            if (owner != null && owner.equals("root")) {
                fl.setReadWrite(false);
            } else {
                fl.setReadWrite(true);
            }
        } else if (mode.substring(2,3).equals("6")) {
            fl.setReadWrite(true); // unknown to be actually true.
        }

        if (ownerScoped != null) {
            for (int i = 0; i < ownerScoped.length; i++) {
                final String s = ownerScoped[i];
                logger.info("OWNER SCOPE: " + s);
            }
        }

        return fl;
    }

    private static String parseDate(String modified) {
        if (modified == null || modified.length() != 14) {
            throw new IllegalArgumentException("invalid modified arg");
        }
        final String monthNum = modified.substring(4,6);
        final String month = getMonthStr(Integer.parseInt(monthNum));
        final String day = modified.substring(6,8);
        return month + " " + day;
    }

    private static String getMonthStr(int month) {
        switch (month) {
            case 1: return "Jan";
            case 2: return "Feb";
            case 3: return "Mar";
            case 4: return "Apr";
            case 5: return "May";
            case 6: return "Jun";
            case 7: return "Jul";
            case 8: return "Aug";
            case 9: return "Sep";
            case 10: return "Oct";
            case 11: return "Nov";
            case 12: return "Dec";
            default: return "???";
        }
    }

    private static String parseTime(String modified) {
        if (modified == null || modified.length() != 14) {
            throw new IllegalArgumentException("invalid modified arg");
        }
        final String hours = modified.substring(8,10);
        final String minutes = modified.substring(10,12);
        return hours + ":" + minutes;
    }
    
}
