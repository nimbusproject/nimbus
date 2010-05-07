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

package org.globus.workspace.cloud.client.util;

import org.globus.ftp.GridFTPClient;
import org.globus.ftp.Session;
import org.globus.ftp.MlsxEntry;
import org.globus.ftp.exception.ServerException;
import org.globus.util.GlobusURL;
import org.globus.gsi.gssapi.auth.HostAuthorization;
import org.globus.gsi.gssapi.auth.IdentityAuthorization;
import org.globus.gsi.gssapi.auth.Authorization;
import org.globus.workspace.client_core.ExecutionProblem;
import org.globus.workspace.cloud.client.tasks.CopyTask;
import org.globus.workspace.cloud.client.tasks.CopyWatchTask;
import org.globus.io.urlcopy.UrlCopy;

import java.io.PrintStream;
import java.io.IOException;
import java.util.Vector;
import java.util.Date;
import java.util.ArrayList;
import java.io.File;
import java.util.Calendar;

import edu.emory.mathcs.backport.java.util.concurrent.FutureTask;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import edu.emory.mathcs.backport.java.util.concurrent.ExecutionException;
import edu.emory.mathcs.backport.java.util.concurrent.TimeoutException;
import edu.emory.mathcs.backport.java.util.concurrent.ExecutorService;
import org.globus.workspace.client_core.ParameterProblem;

import org.globus.workspace.common.SecurityUtil;
import org.globus.workspace.cloud.client.AllArgs;
import org.globus.workspace.cloud.client.Opts;
import org.globus.workspace.client_core.ExecutionProblem;
import org.globus.workspace.common.print.Print;
import edu.emory.mathcs.backport.java.util.concurrent.ExecutorService;

import org.jets3t.service.*;
import org.jets3t.service.security.*;
import org.jets3t.service.model.*;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.S3ServiceException;

import edu.emory.mathcs.backport.java.util.concurrent.Callable;


public class CumulusTask
    implements Callable
{
    public static final int DELETE_TASK = 1;
    public static final int UPLOAD_TASK = 2;
    public static final int DOWNLOAD_TASK = 3;
    public static final int LIST_TASK = 4;

    private int task = -1;

    private String                      localfile;
    private String                      vmName;
    private PrintStream                 info;
    private PrintStream                 debug;
    private AllArgs                     args;
    private Print                       print;


    public CumulusTask(
        AllArgs                         args,
        Print                           pr)
    {
        this.args = args;
        this.print = pr;
    }

    public void setTask(
        int                             t)
    {
        this.task = t;
    }

    private AWSCredentials getAwsCredentail()
    {
        String awsAccessKey = this.args.getXferS3ID();
        String awsSecretKey = this.args.getXferS3Key();
        
       
        AWSCredentials awsCredentials = 
            new AWSCredentials(awsAccessKey, awsSecretKey);

        return awsCredentials;
    }

    private String makeKey(
        String                          vmName,
        String                          ID)
    {
        String baseKey = this.args.getXferS3BaseKey();
        if(ID == null)
        {
            ID = this.args.getXferS3ID();
        }

        return baseKey + "/" + ID + "/" + vmName;
    }

    private String stripKey(
        String                          key)
            throws ExecutionProblem
    {
        int ndx = key.lastIndexOf("/"); 
        if (ndx < 0)
        {
            this.print.debugln("\nCumulus returned a bad VM key " + 
                key);
            return null;
        }
        return key.substring(ndx+1);
    }

    public void uploadVM(
        String                          localfile,
        String                          vmName,
        PrintStream                     info,
        PrintStream                     debug)
            throws ExecutionProblem
    {
        try
        {
            String awsAccessKey = this.args.getXferS3ID();
            S3Service s3Service = this.getService();

            String baseBucketName = this.args.getS3Bucket();
            String key = this.makeKey(vmName, null);

            File file = new File(localfile);
            S3Object s3Object = new S3Object(file);
            s3Object.setKey(key);

            s3Service.putObject(baseBucketName, s3Object);
        }
        catch(Exception s3ex)
        {
            throw new ExecutionProblem(s3ex.toString());
        }
    }

    public void downloadVM(
        String                          localfile,
        String                          vmName,
        PrintStream                     info,
        PrintStream                     debug)
            throws ExecutionProblem
    {
        try
        {
            String awsAccessKey = this.args.getXferS3ID();
            S3Service s3Service = this.getService();

            String baseBucketName = this.args.getS3Bucket();
            String key = this.makeKey(vmName, null);
            S3Bucket bucket = s3Service.getBucket(baseBucketName);

            S3Object objectComplete = s3Service.getObject(bucket, key);

            File file = new File(localfile);
            S3Object s3Object = new S3Object(bucket, file);
            s3Object.setKey(key);
        }
        catch(Exception s3ex)
        {
            throw new ExecutionProblem(s3ex.toString());
        }

    }

    public void setLocalfile(
        String                          localfile)
    {
        this.localfile = localfile;
    }

    public void setVmname(
        String                          vmName)
    {
        this.vmName = vmName;
    }

    public void setInfo(
        PrintStream                     info)
    {
        this.info = info;
    }

    public void setDebug(
        PrintStream                     debug)
    {
        this.debug = debug;
    }

    public void deleteVM(
        String                          vmName,
        PrintStream                     info,
        PrintStream                     debug)
            throws ExecutionProblem
    {
        try
        {
            S3Service s3Service = this.getService();

            String baseBucketName = this.args.getS3Bucket();
            String keyName = this.makeKey(vmName, null);

            s3Service.deleteObject(baseBucketName, keyName);
        }
        catch(S3ServiceException s3ex)
        {
            throw new ExecutionProblem(s3ex.toString());
        }

    }

    private S3Service getService()
        throws S3ServiceException
    {
        String host = this.args.getXferHostPort();
        int ndx = host.lastIndexOf(":");
        int port = 80;
        String portS = "80";

        if(ndx > 0)
        {
            portS = host.substring(ndx+1);
            port = new Integer(portS).intValue();
            host = host.substring(0, ndx);
        }
        
        Jets3tProperties j3p = new Jets3tProperties();

        j3p.setProperty("s3service.s3-endpoint", host);   
        j3p.setProperty("s3service.s3-endpoint-http-port", portS);
        j3p.setProperty("s3service.disable-dns-buckets", "true");
        j3p.setProperty("s3service.https-only", "false"); 

        AWSCredentials awsCredentials = this.getAwsCredentail();
        S3Service s3Service = new RestS3Service(
            awsCredentials,
            "cloud-client",
            null,
            j3p);

        return s3Service;
    }


    public FileListing[] listFiles(
        PrintStream                     info,
        PrintStream                     err,
        PrintStream                     debug) 
              throws ExecutionProblem 
    {
        try
        {
            S3Service s3Service = this.getService();

            String baseBucketName = this.args.getS3Bucket();
            String keyName = this.makeKey("", null);

            ArrayList files = new ArrayList();
            // first get all of this users objects
            S3Object[] usersVMs = s3Service.listObjects(baseBucketName, keyName, "", 1000);
            s3ObjToFileList(files, usersVMs, true);
            S3Object[] VMs = s3Service.listObjects(baseBucketName, this.makeKey("", "common"), "", 1000);
            s3ObjToFileList(files, VMs, false);

            return (FileListing[]) files.toArray(new FileListing[files.size()]);
        }
        catch(S3ServiceException s3ex)
        {
            throw new ExecutionProblem(s3ex.toString());
        }
    }

    private void s3ObjToFileList(
        ArrayList                       files,
        S3Object []                     s3Objs,
        boolean                         rw)
          throws ExecutionProblem 
    {
        Calendar cal = Calendar.getInstance();

        for(int i = 0; i < s3Objs.length; i++)
        {
            String name = s3Objs[i].getKey();
            name = this.stripKey(name);
            if(name != null)
            {
                Date dt = s3Objs[i].getLastModifiedDate();
                cal.setTime(dt);

                FileListing fl = new FileListing();
            
                fl.setName(name);
                fl.setSize(s3Objs[i].getContentLength());

                fl.setDate(convertDate(cal));
                fl.setTime(convertTime(cal));
                fl.setDirectory(false);
                fl.setReadWrite(rw);
                fl.setOwner(s3Objs[i].getOwner().getDisplayName());

                files.add(fl);
            }
        }
    }

    private String convertDate(
        Calendar                        cal)
    {
        int m = cal.get(Calendar.MONTH);
        int d = cal.get(Calendar.DAY_OF_MONTH);
        int y = cal.get(Calendar.YEAR);

        String rc = getMonthStr(m) + " " + 
            new Integer(d).toString() + " " + new Integer(y).toString();

        return rc;
    }

    private String getMonthStr(int month) {
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

    private String convertTime(
        Calendar                        cal)
    {
        int hr = cal.get(Calendar.HOUR_OF_DAY);
        int m = cal.get(Calendar.MINUTE);

        return new Integer(hr).toString() + ":" + new Integer(m).toString();
    }

    public void chmod(
        String                          ownerId,
        String                          permissions,
        String                          vmName,
        PrintStream                     info,
        PrintStream                     debug)
              throws ExecutionProblem
    {
    }

    public String getRemoteUrl(
        String                          fname)
    {
        return "";
    }

    public Object call() throws Exception
    {
        switch(this.task)
        {
            case UPLOAD_TASK:
                this.uploadVM(
                    this.localfile,
                    this.vmName,
                    this.info,
                    this.debug);
                break;

            case DOWNLOAD_TASK:
                this.downloadVM(
                    this.localfile,
                    this.vmName,
                    this.info,
                    this.debug);
                break;

            case LIST_TASK:
            case DELETE_TASK:
        }
        return null;
    }

}
