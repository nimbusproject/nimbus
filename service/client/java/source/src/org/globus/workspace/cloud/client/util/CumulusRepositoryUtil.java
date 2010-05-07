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


public class CumulusRepositoryUtil
    implements RepositoryInterface {

    private AllArgs                     args;
    private final Print                 print;
    private CumulusTask                 cumulusTask;

    public CumulusRepositoryUtil(
        AllArgs                         args,
        Print                           pr)
    {
        this.args = args;
        this.print = pr;

        cumulusTask = new CumulusTask(args, pr);
    }

    public void paramterCheck(
        AllArgs                         args,
        String                          action)
            throws ParameterProblem
    {
        // verify that we have a secret key and ID
        String awsAccessKey = this.args.getXferS3ID();
        String awsSecretKey = this.args.getXferS3Key();
        String baseBucket = this.args.getS3Bucket();
        String baseKey = this.args.getXferS3BaseKey();

        if(awsAccessKey == null)
        {
            throw new ParameterProblem(
                "Cumulus user ID must be added to the configuration file");
        }
        if(awsSecretKey == null)
        {
            throw new ParameterProblem(
                "Cumulus user secret key must be added to the configuration file");
        }
        if(baseBucket == null)
        {
            throw new ParameterProblem(
                "The configuration file must include a cumulus base bucket.  This is a value that must be obtained from the nimbus site administrator");
        }
        if(baseKey == null)
        {
            throw new ParameterProblem(
                "The configuration file must include a cumulus base key.  This is a value that must be obtained from the nimbus site administrator");
        }
    }

    public void uploadVM(
        String                          localfile,
        String                          vmName,
        PrintStream                     info,
        PrintStream                     debug,
        ExecutorService                 executorService)
            throws ExecutionProblem
    {
        this.cumulusTask.setLocalfile(localfile);
        this.cumulusTask.setVmname(vmName);
        this.cumulusTask.setInfo(info);
        this.cumulusTask.setDebug(debug);

        this.cumulusTask.setTask(CumulusTask.UPLOAD_TASK);
        this.performTask(executorService);
    }

    public void downloadVM(
        String                          localfile,
        String                          vmName,
        PrintStream                     info,
        PrintStream                     debug,
        ExecutorService                 executorService)
            throws ExecutionProblem
    {
        this.cumulusTask.setLocalfile(localfile);
        this.cumulusTask.setVmname(vmName);
        this.cumulusTask.setInfo(info);
        this.cumulusTask.setDebug(debug);

        this.cumulusTask.setTask(CumulusTask.DELETE_TASK);
        this.performTask(executorService);
    }

    private void performTask(
        ExecutorService                 executorService)
        throws ExecutionProblem
    {
        if (executorService == null) 
        {
            throw new IllegalArgumentException("executorService may not be null");
        }

        FutureTask task = new FutureTask(this.cumulusTask);
        long timeoutMinutes = this.args.getTimeoutMinutes();

        executorService.submit(task);

        try 
        {
            if (timeoutMinutes < 1)
            {
                task.get();
            } 
            else 
            {
                task.get(timeoutMinutes, TimeUnit.MINUTES);
            }
        } 
        catch (InterruptedException e) 
        {
            throw new ExecutionProblem(e);
        } 
        catch (ExecutionException e) 
        {
            throw new ExecutionProblem("Problem transferring: " +
                                                        e.getMessage());
        } 
        catch (TimeoutException e) 
        {
            throw new ExecutionProblem("Timeout limit exceeded.");
        }
    }

    public void deleteVM(
        String                          vmName,
        PrintStream                     info,
        PrintStream                     debug)
            throws ExecutionProblem
    {
        this.cumulusTask.deleteVM(vmName, info, debug);
    }

    public FileListing[] listFiles(
        PrintStream                     info,
        PrintStream                     err,
        PrintStream                     debug) 
              throws ExecutionProblem 
    {
        return this.cumulusTask.listFiles(info, err, debug);
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
        try
        {
            return this.getDerivedImageURL(fname);
        }
        catch(Exception ex)
        {
            return null;
        }
    }

    public String getDerivedImageURL(
        String                          imageName)
            throws ExecutionProblem
    {
        String baseKey = this.args.getXferS3BaseKey();
        String ID = this.args.getXferS3ID();

        return "cumulus://" + this.args.getXferHostPort() + "/" + this.args.getS3Bucket() + "/" + ID + "/" + imageName;
    }

}
