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

package org.globus.workspace.client_core.utils;

import org.globus.workspace.client_core.ExecutionProblem;
import org.nimbustools.messaging.gt4_0.common.CommonUtil;

import java.rmi.RemoteException;

public class RMIUtils {

    /**
     * <p>*Returns* ExecutionProblem with some nice message and input exception
     * as its cause.</p>
     *
     * <p>Returning exceptions like this, typically for use in a catch block,
     * is useful for compiler/ide analysis.</p>
     *
     * <p>Example use:</p>
     *
     * 
     * <pre>
     * [...]
     * } catch (WorkspaceSchedulingFault e) {
     *     throw e;
     * } catch (RemoteException e) {
     *     throw RMIUtils.generalRemoteException(e);
     * }
     * </pre>
     *
     *
     * @param e may be null but more useful if it is not
     * @return ExecutionProblem some nice message, with input exception as cause
     * @see #generalRemoteExceptionHandler(RemoteException) 
     */
    public static ExecutionProblem generalRemoteException(RemoteException e) {

        final String msg = CommonUtil.genericExceptionMessageWrapper(e);
        return new ExecutionProblem("General error: " + msg, e);
    }

    /**
     * Never returns, always *throws* ExecutionProblem with some nice message
     * and input exception as its cause.
     * 
     * @param e may be null but more useful if it is not
     * @throws ExecutionProblem some nice message, with input exception as cause
     * @see #generalRemoteException(RemoteException) 
     */
    public static void generalRemoteExceptionHandler(RemoteException e)

            throws ExecutionProblem {

        throw generalRemoteException(e);
    }
}
