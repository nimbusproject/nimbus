/*
 * Copyright 1999-2007 University of Chicago
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

package org.globus.workspace.client_common;

import org.apache.axis.message.addressing.EndpointReferenceType;
import org.globus.wsrf.encoding.DeserializationException;
import org.globus.wsrf.encoding.ObjectDeserializer;
import org.globus.wsrf.utils.FaultHelper;
import org.xml.sax.InputSource;

import java.io.IOException;
import java.io.FileInputStream;

public class BaseClientUtil {

    public static EndpointReferenceType getEPRfromFile(String path)
            throws IOException, DeserializationException {

        EndpointReferenceType epr;
        FileInputStream in = null;
        try {
            in = new FileInputStream(path);
            epr = (EndpointReferenceType) ObjectDeserializer.deserialize(
                             new InputSource(in), EndpointReferenceType.class);
            return epr;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    throw e;
                }
            }
        }
    }

    public static short die(Throwable t, String type, BaseClient client) {
        dieMsg(t, type, client);
        return BaseClient.APPLICATION_EXIT_CODE;
    }

    public static void dieMsg(Throwable t,
                              String type,
                              BaseClient client) {

        String description = null;
        if (t != null) {
            FaultHelper helper = new FaultHelper(FaultHelper.toBaseFault(t));
            description = helper.getDescriptionAsString();
        }

        if (description == null || description.trim().equals("")) {
            description = recurseCauses(t, "");
        } else {
            description = recurseCauses(t, description);
        }

        if (description != null && description.trim().equals("")) {
            description = null;
        }

        System.err.println("\n------\nError:\n------");

        if (description == null && type == null) {
            System.err.println("\nUnknown.\n");
        } else if (description != null && type == null) {
            System.err.println(description + "\n");
        } else if (description == null) {
            System.err.println("\n" + type + "\n");
        } else {
            System.err.println("\n" + type + ":\n" + description + "\n");
        }

        if (client.isDebugMode() && t != null) {
            System.err.println("\n-----------\nStacktrace:\n-----------\n");
            FaultHelper.printStackTrace(t);
        }
    }

    // look for any kind of message
    // TODO: use the new replacement for this in all cases (see client core) 
    private static String recurseCauses(Throwable t, String sofar) {
        if (t == null) {
            return "\n";
        }

        String sofarStr;
        if (sofar == null) {
            sofarStr = "";
        } else {
            sofarStr = sofar;
        }

        String a = "\n---- ";
        String b = " ----\n";

        boolean message = false;

        if (t.getMessage() != null && t.getMessage().trim().length() != 0) {
            message = true;
        }

        final String thisThr = a + t.getClass().toString() + b;

        if (message) {
            sofarStr = thisThr + t.getMessage() + sofar;
        } else {
            sofarStr = thisThr + sofar;
        }

        if (t.getCause() != null) {
            return recurseCauses(t.getCause(), sofarStr);
        } else {
            return sofarStr;
        }
    }
}
