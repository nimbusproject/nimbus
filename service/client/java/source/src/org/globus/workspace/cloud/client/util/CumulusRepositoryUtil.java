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

import edu.emory.mathcs.backport.java.util.concurrent.ExecutionException;
import edu.emory.mathcs.backport.java.util.concurrent.ExecutorService;
import edu.emory.mathcs.backport.java.util.concurrent.FutureTask;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import edu.emory.mathcs.backport.java.util.concurrent.TimeoutException;
import org.globus.workspace.client_core.ExecutionProblem;
import org.globus.workspace.client_core.ParameterProblem;
import org.globus.workspace.cloud.client.AllArgs;
import org.globus.workspace.common.print.Print;

import java.io.PrintStream;

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

        String useHttps = this.args.getXferS3Https();
        if (useHttps == null)
        {
            useHttps = "false";
        }
        boolean ss;
        String selfSigned = this.args.getXferS3AllowSelfSigned();
        if(selfSigned == null || selfSigned.equalsIgnoreCase("true"))
        {
            ss = true;
        }
        else
        {
            ss = false;
        }
        
        cumulusTask = new CumulusTask(args, pr, useHttps, ss);
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
        String ownerID = this.args.getXferCanonicalID();

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
        if(ownerID == null)
        {
            throw new ParameterProblem(
                "Cumulus canonical user ID must be added to the configuration file");
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

        this.cumulusTask.setTask(CumulusTask.DOWNLOAD_TASK);
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
        String ID = this.args.getXferCanonicalID();

        return "cumulus://" + this.args.getXferHostPort() + "/" + this.args.getS3Bucket() + "/" + baseKey + "/" + ID + "/" + imageName;
    }

}
