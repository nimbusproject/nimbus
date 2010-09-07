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

package org.nimbustools.auto_config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.io.IOException;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;

//via Eckel
public class TextFile extends ArrayList {

    public TextFile(String fileName) throws IOException {
        super(Arrays.asList(gRead(fileName).split("\n")));
    }

    private static String gRead(String fileName) throws IOException {
        final StringBuffer sb = new StringBuffer(16);

        FileReader fr = null;
        BufferedReader br = null;
        try {
            fr = new FileReader(fileName);
            br = new BufferedReader(fr);
            String s = br.readLine();
            while(s != null) {
                sb.append(s);
                sb.append("\n");
                s = br.readLine();
            }
        } finally {
            if (fr != null) {
                fr.close();
            }
            if (br != null) {
                br.close();
            }
        }
        return sb.toString();
    }

    public void writeFile(File conf) throws IOException {

        if (conf == null) {
            throw new IOException("no target file");
        }
        if (!conf.canWrite()) {
            throw new IOException("cannot write to target file '" +
                    conf.getAbsolutePath() + "'");
        }

        final StringBuffer buf = new StringBuffer();
        final Iterator iter = this.iterator();
        while (iter.hasNext()) {
            buf.append((String)iter.next()).append("\n");
        }
        buf.append("\n");

        FileWriter fw = null;
        try {
            fw = new FileWriter(conf);
            fw.write(buf.toString());
            fw.flush();
        } finally {
            if (fw != null) {
                fw.close();
            }
        }
    }
}
