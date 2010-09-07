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

import org.nimbustools.messaging.gt4_0.generated.metadata.VirtualWorkspace_Type;
import org.nimbustools.messaging.gt4_0.generated.negotiable.WorkspaceDeployment_Type;
import org.nimbustools.messaging.gt4_0.generated.types.OptionalParameters_Type;
import org.nimbustools.ctxbroker.generated.gt4_0.description.BrokerContactType;
import org.nimbustools.ctxbroker.generated.gt4_0.description.Cloudcluster_Type;
import org.globus.workspace.common.print.Print;
import org.globus.workspace.client_core.print.PrCodes;
import org.globus.wsrf.encoding.DeserializationException;
import org.globus.wsrf.encoding.ObjectDeserializer;
import org.globus.wsrf.encoding.ObjectSerializer;
import org.globus.wsrf.encoding.SerializationException;
import org.xml.sax.InputSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.axis.message.addressing.EndpointReferenceType;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.io.FileWriter;

public class FileUtils {

    private static final Log logger =
            LogFactory.getLog(FileUtils.class.getName());

    public static String readFileAsString(String path) throws IOException {

        final File file = new File(path);

        if (!file.exists()) {
            String err = "File does not exist, path: '" + path + "'";
            if (!file.isAbsolute()) {
                err += " (absolute path: '" + file.getAbsolutePath() + "')";
            }
            throw new IOException(err);
        }

        if (!file.canRead()) {
            String err = "Cannot read file: \"" + path + "\"";
            if (!file.isAbsolute()) {
                err += " (absolute path: \"" + file.getAbsolutePath() + "\")";
            }
            throw new IOException(err);
        }

        final StringBuffer sb = new StringBuffer(1024);

        BufferedReader in = null;
        FileReader fr = null;
        String s;
        try {
            fr = new FileReader(file);
            in = new BufferedReader(fr);
            s = in.readLine();
            while (s != null) {
                sb.append(s);
                sb.append("\n");
                s = in.readLine();
            }
        } finally {
            if (fr != null) {
                fr.close();
            }
            if (in != null) {
                in.close();
            }
        }
        return sb.toString();
    }

    public static VirtualWorkspace_Type getMetadataFromFile(
                                              Print print,
                                              String metadatapath)
                    throws IOException, DeserializationException {

        if (print.enabled()) {
            final String msg =
                    "Attempting to get metadata from '" + metadatapath + "'";
            if (print.useThis()) {
                print.dbg(msg);
            } else if (print.useLogging()) {
                logger.debug(msg);
            }
        }

        VirtualWorkspace_Type vw = null;
        BufferedInputStream in = null;

        try {
            in = new BufferedInputStream(new FileInputStream(metadatapath));
            vw = (VirtualWorkspace_Type) ObjectDeserializer.deserialize(
                             new InputSource(in), VirtualWorkspace_Type.class);
        } finally {
            if (in != null) {
                in.close();
            }
        }

        if (print.enabled()) {
            final String msg = "Read metadata file: \"" + metadatapath + "\"";
            if (print.useThis()) {
                print.infoln(PrCodes.METADATA__FILE_READ, msg);
            } else if (print.useLogging()) {
                logger.info(msg);
            }
        }
        return vw;
    }


    public static WorkspaceDeployment_Type getRequestFromFile(
                                                        Print print,
                                                        String requestpath)
                    throws IOException, DeserializationException {

        if (print.enabled()) {
            final String msg = "Attempting to get deployment request from '" +
                    requestpath + "'";
            if (print.useThis()) {
                print.dbg(msg);
            } else if (print.useLogging()) {
                logger.debug(msg);
            }
        }

        WorkspaceDeployment_Type req = null;
        BufferedInputStream in = null;
        try {
            in = new BufferedInputStream(new FileInputStream(requestpath));
            req = (WorkspaceDeployment_Type)
                        ObjectDeserializer.deserialize(
                                new InputSource(in),
                                WorkspaceDeployment_Type.class);
        } finally {
            if (in != null) {
                in.close();
            }
        }

        if (print.enabled()) {
            final String msg =
                    "Read deployment request file: \"" + requestpath + "\"";
            if (print.useThis()) {
                print.infoln(PrCodes.DEPREQ__FILE_READ, msg);
            } else if (print.useLogging()) {
                logger.info(msg);
            }
        }

        return req;
    }

    public static OptionalParameters_Type getOptionalFromFile(
                                                        Print print,
                                                        String optpath)
                    throws IOException, DeserializationException {

        if (print.enabled()) {
            final String msg = "Attempting to get optional parameters from '" +
                    optpath + "'";
            if (print.useThis()) {
                print.dbg(msg);
            } else if (print.useLogging()) {
                logger.debug(msg);
            }
        }

        OptionalParameters_Type opt = null;
        BufferedInputStream in = null;
        try {
            in = new BufferedInputStream(new FileInputStream(optpath));
            opt = (OptionalParameters_Type)
                        ObjectDeserializer.deserialize(
                                new InputSource(in),
                                OptionalParameters_Type.class);
        } finally {
            if (in != null) {
                in.close();
            }
        }

        if (print.enabled()) {
            final String msg =
                    "Read optional parameters file: \"" + optpath + "\"";
            if (print.useThis()) {
                print.infoln(PrCodes.OPTIONALPARAM__FILE_READ, msg);
            } else if (print.useLogging()) {
                logger.info(msg);
            }
        }

        return opt;
    }

    public static String getSshPolicyFromFile(Print print,
                                               String sshpath)
                    throws IOException {

        if (print.enabled()) {
            final String msg = "Attempting to get SSH policy from '" +
                    sshpath + "'";
            if (print.useThis()) {
                print.dbg(msg);
            } else if (print.useLogging()) {
                logger.debug(msg);
            }
        }

        final String ret = readFileAsString(sshpath);

        if (print.enabled()) {
            final String msg =
                    "Read SSH policy: \"" + sshpath + "\"";
            if (print.useThis()) {
                print.infoln(PrCodes.SSH__FILE_READ, msg);
            } else if (print.useLogging()) {
                logger.info(msg);
            }
        }

        return ret;
    }

    public static String getUserDataFromFile(Print print,
                                             String userDataPath)
                    throws IOException {

        if (print.enabled()) {
            final String msg = "Attempting to get user data from '" +
                    userDataPath + "'";
            if (print.useThis()) {
                print.dbg(msg);
            } else if (print.useLogging()) {
                logger.debug(msg);
            }
        }

        final String ret = readFileAsString(userDataPath);

        if (print.enabled()) {
            final String msg =
                    "Read user data: \"" + userDataPath + "\"";
            if (print.useThis()) {
                print.infoln(PrCodes.MD_USERDATA__FILE_READ, msg);
            } else if (print.useLogging()) {
                logger.info(msg);
            }
        }

        return ret;
    }

    public static Cloudcluster_Type getClusterDocForRetrieve(Print print,
                                                             String path)
            throws IOException, DeserializationException {

        if (print.enabled()) {
            final String msg = "Attempting to get cluster ctx doc " +
                    "from '" + path + "'";
            if (print.useThis()) {
                print.dbg(msg);
            } else if (print.useLogging()) {
                logger.debug(msg);
            }
        }

        Cloudcluster_Type clusta = null;
        BufferedInputStream in = null;
        try {
            in = new BufferedInputStream(new FileInputStream(path));
            clusta = (Cloudcluster_Type)
                        ObjectDeserializer.deserialize(
                                new InputSource(in),
                                Cloudcluster_Type.class);
        } finally {
            if (in != null) {
                in.close();
            }
        }

        if (print.enabled()) {
            final String msg =
                    "Read cluster ctx doc: \"" +
                            path + "\"";
            if (print.useThis()) {
                print.infoln(PrCodes.CTX_DOC__FILE_READ, msg);
            } else if (print.useLogging()) {
                logger.info(msg);
            }
        }

        return clusta;
    }

    public static BrokerContactType getBrokerContactFromFile(Print print,
                                                             String path)
                    throws IOException, DeserializationException {

        if (print.enabled()) {
            final String msg =
                    "Attempting to get broker contact info from '" + path + "'";
            if (print.useThis()) {
                print.dbg(msg);
            } else if (print.useLogging()) {
                logger.debug(msg);
            }
        }

        BrokerContactType brokerContact = null;
        BufferedInputStream in = null;

        try {
            in = new BufferedInputStream(new FileInputStream(path));
            brokerContact =
                    (BrokerContactType) ObjectDeserializer.deserialize(
                             new InputSource(in), BrokerContactType.class);
        } finally {
            if (in != null) {
                in.close();
            }
        }

        if (print.enabled()) {
            final String msg = "Read broker contact file: \"" + path + "\"";
            if (print.useThis()) {
                print.debugln(msg);
            } else if (print.useLogging()) {
                logger.info(msg);
            }
        }
        return brokerContact;
    }

    public static EndpointReferenceType getEPRfromFile(File f)
            throws IOException, DeserializationException {
        if (f == null) {
            throw new IllegalArgumentException("File may not be null");
        }
        return getEPRfromFile(f, null);
    }

    public static EndpointReferenceType getEPRfromFile(String path)
            throws IOException, DeserializationException {
        if (path == null) {
            throw new IllegalArgumentException("path may not be null");
        }
        return getEPRfromFile(null, path);
    }

    private static EndpointReferenceType getEPRfromFile(File f,
                                                        String path)
            throws IOException, DeserializationException {


        final EndpointReferenceType epr;
        FileInputStream in = null;
        try {
            if (f != null) {
                in = new FileInputStream(f);
            } else {
                in = new FileInputStream(path);
            }
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

    public static void writeEprToFile(EndpointReferenceType epr,
                                      String path,
                                      QName qName)
            
                throws IOException, SerializationException {

        if (epr == null) {
            throw new IllegalArgumentException("epr may not be null");
        }

        writeStringToFile(ObjectSerializer.toString(epr, qName), path);
    }

    public static void writeStringToFile(String string,
                                         String path)

                throws IOException, SerializationException {
        writeStringToFile(string, path, false);
    }

    public static void writeStringToFile(String string,
                                         String path,
                                         boolean append)

                throws IOException, SerializationException {

        if (path == null) {
            throw new IllegalArgumentException("path may not be null");
        }

        FileWriter writer = null;
        try {
            writer = new FileWriter(path, append);
            writer.write(string);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }
}
