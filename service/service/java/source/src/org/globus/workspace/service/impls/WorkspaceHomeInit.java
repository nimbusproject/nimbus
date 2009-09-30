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

package org.globus.workspace.service.impls;

import org.globus.workspace.cmdutils.SSHUtil;
import org.globus.workspace.service.impls.async.RequestDispatch;

/**
 * Things that should not be static but that still are.
 *
 * TODO: remove static things
 *
 */
class WorkspaceHomeInit {

    static void initializeRequestDispatch(String initialSize,
                                          String maxSize) throws Exception {
        
        if (initialSize == null) {
            throw new Exception("threadPoolInitialSize is not configured");
        }

        if (maxSize == null) {
            throw new Exception("threadPoolMaxSize is not configured");
        }

        final int initial = Integer.parseInt(initialSize);
        final int max = Integer.parseInt(maxSize);

        if (initial < 1) {
            throw new Exception(
                            "threadPoolInitialSize may not be less than one");
        }

        if (max < 1) {
            throw new Exception(
                            "threadPoolMaxSize may not be less than one");
        }

        if (initial > max) {
            throw new Exception("threadPoolInitialSize may not be " +
                                       "greater than threadPoolMaxSize");
        }

        int highwater = initial * 2;
        if (highwater > max) {
            // half of difference
            final int diff = max - initial;

            if (diff < 2) {
                highwater = initial;
            } else {
                highwater = diff / 2;
            }
        }

        RequestDispatch.setOptions(initial, max, highwater);
    }
    
    static void initializeSSH(String sshPath,
                              String scpPath,
                              String sshAccount,
                              String sshIdFile) throws Exception {

        if (sshPath != null) {
            SSHUtil.setSshexe(sshPath);
            if (sshAccount != null) {
                SSHUtil.setSshaccount(sshAccount);
            }
            if (sshIdFile != null && sshIdFile.trim().length() != 0) {
                SSHUtil.setSshIdentityFile(sshIdFile);
            }
        }
        if (scpPath != null) {
            SSHUtil.setScpexe(scpPath);
            if (sshAccount != null) {
                SSHUtil.setSshaccount(sshAccount);
            }
            if (sshIdFile != null && sshIdFile.trim().length() != 0) {
                SSHUtil.setSshIdentityFile(sshIdFile);
            }
        }
    }
}
