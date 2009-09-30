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

package org.globus.workspace.cloud.meta.client;

import org.nimbustools.ctxbroker.generated.gt4_0.description.Cloudcluster_Type;
import org.nimbustools.ctxbroker.generated.gt4_0.description.Clouddeploy_Type;
import org.globus.wsrf.encoding.ObjectDeserializer;
import org.globus.wsrf.encoding.DeserializationException;
import org.xml.sax.InputSource;

import java.io.*;
import java.util.Properties;

public class TestUtil {

    public static Cloudcluster_Type getSampleCluster()
        throws IOException, DeserializationException {

        InputStream is = null;
        try {
            is = getSampleClusterStream();
            return (Cloudcluster_Type)
                            ObjectDeserializer.deserialize(
                                new InputSource(is), Cloudcluster_Type.class);
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    public static void writeSampleClusterToFile(File file)
        throws IOException {

        InputStream is = null;
        try {
            is = getSampleClusterStream();
            writeInputStreamToFile(is, file);

        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    public static Clouddeploy_Type getSampleDeploy()
        throws IOException, DeserializationException {

        InputStream is = null;
        try {
            is = getSampleDeployStream();
            return (Clouddeploy_Type)
                            ObjectDeserializer.deserialize(
                                new InputSource(is), Cloudcluster_Type.class);
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    private static InputStream getSampleDeployStream() {
       InputStream is;
        is = TestUtil.class.getResourceAsStream("test-deploy.xml");
        return is;
    }

    public static void writeSampleDeployToFile(File file)
        throws IOException {

        InputStream is = null;
        try {
            is = getSampleDeployStream();
            writeInputStreamToFile(is, file);

        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    private static void writeInputStreamToFile(InputStream is, File file)
        throws IOException {
        FileOutputStream os = null;

        try {
        os = new FileOutputStream(file);

        byte buf[] = new byte[1024];
        int len;
        while((len=is.read(buf))>0) {
            os.write(buf,0,len);
        }
        } finally {
            if (os != null) {
                os.close();
            }
        }
    }

    private static InputStream getSampleClusterStream() {
        InputStream is;
        is = TestUtil.class.getResourceAsStream("test-cluster.xml");
        return is;
    }

    static void writePropertiesToFile(Properties props, File f)
        throws IOException {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(f);
            props.store(fos,"");
        } finally {
            if (fos != null)
                fos.close();
        }
    }
}
