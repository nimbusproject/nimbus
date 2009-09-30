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

package org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.image;

/**
 * Hold information about repository files.
 *
 * todo: code dup from cloud client.
 */
public class FileListing {

 // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    private String name;
    private boolean readWrite;
    private long size;
    private boolean directory;
    private String date;
    private String time;

    // -------------------------------------------------------------------------
    // GET/SET
    // -------------------------------------------------------------------------

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isReadWrite() {
        return this.readWrite;
    }

    public void setReadWrite(boolean readWrite) {
        this.readWrite = readWrite;
    }

    public long getSize() {
        return this.size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public boolean isDirectory() {
        return this.directory;
    }

    public void setDirectory(boolean directory) {
        this.directory = directory;
    }

    public String getDate() {
        return this.date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return this.time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}
