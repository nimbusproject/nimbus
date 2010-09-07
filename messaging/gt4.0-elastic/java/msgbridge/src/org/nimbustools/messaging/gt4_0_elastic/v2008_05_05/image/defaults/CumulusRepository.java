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
import org.nimbus.authz.AuthzDBAdapter;
import org.nimbus.authz.AuthzDBException;
import org.nimbus.authz.ObjectWrapper;
import org.nimbus.authz.UserAlias;
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

import javax.sql.DataSource;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Connection;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class CumulusRepository implements Repository {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(CumulusRepository.class.getName());
    

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected final ContainerInterface container;
    protected final ReprFactory repr;
    protected AuthzDBAdapter authDB;
    protected String cumulusHost = null;
    protected String repoBucket = null;
    protected String prefix = null;
    protected int repo_id = -1;
    protected String rootFileMountAs = null;


    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------

    public CumulusRepository(ContainerInterface containerImpl,
                             ModuleLocator locator,
                            DataSource ds)
        throws Exception
    {
        if (containerImpl == null)
        {
            throw new IllegalArgumentException("containerImpl may not be null");
        }
        this.authDB = new AuthzDBAdapter(ds);


        if (containerImpl == null)
        {
            throw new IllegalArgumentException("containerImpl may not be null");
        }
        this.container = containerImpl;

        if (locator == null)
        {
            throw new IllegalArgumentException("locator may not be null");
        }

        this.repr = locator.getReprFactory();
    }

    

    // -------------------------------------------------------------------------
    // IoC INIT METHOD
    // -------------------------------------------------------------------------

    void validate()
        throws Exception
    {
        if (this.cumulusHost == null)
        {
            throw new Exception("Invalid: Missing 'cumulus host' string");
        }
        if (this.repoBucket == null)
        {
            throw new Exception("Missing the 'repoBucket' setting");
        }
        if (this.prefix == null)
        {
            throw new Exception("Missing the 'prefix' setting");
        }
        if(this.rootFileMountAs == null)
        {
            throw new Exception("Missing the 'rootFileMountAs' setting");
        }

        this.repo_id = this.authDB.getFileID(this.repoBucket, -1, AuthzDBAdapter.ALIAS_TYPE_S3);
        if(this.repo_id < 0)
        {
            throw new Exception("Could not find the repo bucket " + this.repoBucket);
        }
    }

    public void setRepoBucket(String rb)
            throws Exception
    {
        this.repoBucket= rb;
    }

    public String getRepoBucket()
    {
        return this.repoBucket;
    }

    public void setCumulusHost(String rb)
            throws Exception
    {
        this.cumulusHost = rb;
    }

    public String getPrefix()
    {
        return this.cumulusHost;
    }

    public void setPrefix(String rb)
            throws Exception
    {
        this.prefix = rb;
    }

    public String getCumulusHost()
    {
        return this.cumulusHost;
    }

    public boolean isListingEnabled()
    {
        return true;
    }

    public String getRootFileMountAs()
    {
        return this.rootFileMountAs;
    }

    public void setRootFileMountAs(String rootFileMountAs)
    {
        this.rootFileMountAs = rootFileMountAs;
    }
    
    // -------------------------------------------------------------------------
    // implements Repository
    // -------------------------------------------------------------------------

    // XXXX  not sure what to do here
    public String getImageLocation(Caller caller)
            throws CannotTranslateException
    {
        final String dn = caller.getIdentity();
        String ownerID;
        try
        {
            ownerID = this.authDB.getCanonicalUserIdFromDn(dn);
        }
        catch(AuthzDBException ex)
        {
            throw new CannotTranslateException(ex.toString(), ex);
        }
        if (ownerID == null)
        {
            throw new CannotTranslateException("No caller/ownerID?");
        }

        return "cumulus://" + this.cumulusHost + "/" + this.repoBucket + "/" + this.prefix + "/" + ownerID;
    }


    public VMFile[] constructFileRequest(String imageID,
                                         ResourceAllocation ra,
                                         Caller caller)
            throws CannotTranslateException    
    {
        final String dn = caller.getIdentity();
        if (dn == null)
        {
            throw new CannotTranslateException("Cannot construct file " +
                    "request without owner hash/ID, the file(s) location is " +
                    "based on it");
        }
        // todo: look at RA and construct blankspace request
        //return new VMFile[]{this.getRootFile(imageID, ownerID)};
        VMFile [] vma = new VMFile[1];
        
       // vma[0].
        final _VMFile file = this.repr._newVMFile();
        file.setRootFile(true);
        file.setDiskPerms(VMFile.DISKPERMS_ReadWrite);

        // must convert to scp url
        // look up image id
        try
        {
            String urlStr;
            if(imageID.indexOf("cumulus://") == 0)
            {
                urlStr = imageID;
            }
            else
            {
                urlStr = getImageLocation(caller) + "/" + imageID;
            }
            file.setMountAs(this.getRootFileMountAs());
            URI imageURI = new URI(urlStr);
            file.setURI(imageURI);
            vma[0] = file;
            return vma;
        }
        catch(Exception ex)
        {
            throw new CannotTranslateException(ex);
        }                
    }

    // return a list of all the images owned by this user
    public FileListing[] listFiles(Caller caller,
                                   String[] nameScoped,
                                   String[] ownerScoped)
            throws CannotTranslateException, ListingException
    {
        int                             repo_id;
        String                          keyName;

        final String dn = caller.getIdentity();
        if (dn == null)
        {
            throw new CannotTranslateException("Cannot construct file " +
                    "request without owner hash/ID, the file(s) location is " +
                    "based on it");
        }

        try
        {
            String ownerID = this.authDB.getCanonicalUserIdFromDn(dn);

            final ArrayList files = new ArrayList();
            //String canUser = this.authDB.getCanonicalUserIdFromS3(ownerID);
            keyName = this.prefix + "/" + ownerID + '%';

            final List<ObjectWrapper> objList = this.authDB.searchParentFilesByKey(this.repo_id, keyName);
            for (ObjectWrapper ow : objList)
            {
                FileListing fl = new FileListing();
                String name = ow.getName();
                String [] parts = name.split("/", 3);
                if(parts.length != 3)
                {
                    // if a bad name jsut skip this file... they may have uploaded baddness
                    logger.error("The filename " + name + " is not in the proper format");
                    continue;
                }
                name = parts[2];
                fl.setName(name);
                fl.setSize(ow.getSize());

                long tm = ow.getTime();
                Date dt = new Date(tm);
                Calendar cl = Calendar.getInstance();
                cl.setTime(dt);
                String tStr = new Integer(cl.get(Calendar.HOUR_OF_DAY)).toString() + ":" + new Integer(cl.get(Calendar.MINUTE)).toString();
                fl.setTime(tStr);
                String dStr = getMonthStr(cl.get(Calendar.MONTH)) + " " + new Integer(cl.get(Calendar.DAY_OF_MONTH)).toString();
                fl.setDate(dStr);                
                fl.setReadWrite(true);
                files.add(fl);
            }
            return (FileListing[]) files.toArray(new FileListing[files.size()]);
        }
        catch(AuthzDBException ex)
        {
            throw new ListingException("file listing failed when accessing authz db", ex);                    
        }
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
}
