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

package org.globus.workspace.persistence;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;

public class ErrorUtil {

    private static final Log logger =
            LogFactory.getLog(PersistenceAdapterImpl.class.getName());

    public static Throwable getThrowable(byte[] faultBytes)
            throws IOException, ClassNotFoundException {

        ByteArrayInputStream bais = null;
        ObjectInputStream ois = null;
        try {
            bais = new ByteArrayInputStream(faultBytes);
            ois = new ObjectInputStream(bais);
            return (Throwable) ois.readObject();
        } finally {
            if (bais != null) {
                bais.close();
            }
            if (ois != null) {
                ois.close();
            }
        }
    }

    public static byte[] toByteArray(Throwable t) throws IOException {

        if (t == null) {
            return null;
        }

        byte[] ret = null;
        ByteArrayOutputStream baos = null;
        ObjectOutputStream oos = null;

        try {
            baos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(baos);
            oos.writeObject(t);
            oos.flush();
            baos.flush();
            ret = baos.toByteArray();
        } catch (IOException e) {
            logger.error("",e);
            throw e;
        } finally {
            if (baos != null) {
                baos.close();
            }
            if (oos != null) {
                oos.close();
            }
        }

        return ret;
    }

}
