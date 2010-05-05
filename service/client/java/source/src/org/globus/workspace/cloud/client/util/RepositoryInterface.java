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

import java.io.PrintStream;
import java.io.IOException;
import java.util.Vector;
import java.util.ArrayList;
import org.globus.workspace.cloud.client.AllArgs;
import org.globus.workspace.client_core.ExecutionProblem;
import org.globus.workspace.common.print.Print;
import org.globus.workspace.client_core.ParameterProblem;
import edu.emory.mathcs.backport.java.util.concurrent.ExecutorService;


public interface RepositoryInterface
{
    // -------------------------------------------------------------------------
    // check the parameters
    // -------------------------------------------------------------------------
    public void paramterCheck(
        AllArgs                         args,
        String                          action)
            throws ParameterProblem;

    // -------------------------------------------------------------------------
    // UPLOAD FILE
    // -------------------------------------------------------------------------
    public void uploadVM(
        String                          localfile,
        String                          vmName,
        PrintStream                     info,
        PrintStream                     debug,
        ExecutorService                 executorService)
            throws ExecutionProblem;

    public String getRemoteUrl(String fname);

    public String getLocalUrl(String fname);

    // -------------------------------------------------------------------------
    // DOWNLOAD FILE
    // -------------------------------------------------------------------------
    public void downloadVM(
        String                          localfile,
        String                          vmName,
        PrintStream                     info,
        PrintStream                     debug,
        ExecutorService                 executorService)
            throws ExecutionProblem;

    // -------------------------------------------------------------------------
    // DELETE FILE
    // -------------------------------------------------------------------------
    public void deleteVM(
        String                          vmName,               
        PrintStream                     info,
        PrintStream                     debug)
            throws ExecutionProblem;

    // -------------------------------------------------------------------------
    // LIST FILES
    // -------------------------------------------------------------------------
    public FileListing[] listFiles(
        PrintStream                     info,
        PrintStream                     err,
        PrintStream                     debug) 
              throws ExecutionProblem;

    // -------------------------------------------------------------------------
    // Grant Access
    // -------------------------------------------------------------------------
    public void chmod(
        String                          ownerId,
        String                          permissions,
        String                          vmName,               
        PrintStream                     info,
        PrintStream                     debug)
              throws ExecutionProblem;


    public String getDerivedImageURL(
        String                          imageName)
            throws ExecutionProblem;
}
