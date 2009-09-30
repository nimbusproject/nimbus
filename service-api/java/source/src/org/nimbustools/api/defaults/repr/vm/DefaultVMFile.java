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

package org.nimbustools.api.defaults.repr.vm;

import org.nimbustools.api._repr.vm._VMFile;

import java.net.URI;

public class DefaultVMFile implements _VMFile {

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------
    
    private boolean rootFile;
    private URI uri;
    private String mountAs;
    private String diskPerms;
    private URI unpropURI;
    private String blankspaceName;
    private int blankSpaceSize = -1;


    // -------------------------------------------------------------------------
    // implements org.nimbustools.api.repr.vm.VMFile
    // -------------------------------------------------------------------------

    public boolean isRootFile() {
        return this.rootFile;
    }

    public URI getURI() {
        return this.uri;
    }

    public String getMountAs() {
        return this.mountAs;
    }

    public String getDiskPerms() {
        return this.diskPerms;
    }

    public URI getUnpropURI() {
        return this.unpropURI;
    }

    public String getBlankSpaceName() {
        return this.blankspaceName;
    }

    public int getBlankSpaceSize() {
        return this.blankSpaceSize;
    }
    

    // -------------------------------------------------------------------------
    // implements org.nimbustools.api._repr.vm._VMFile
    // -------------------------------------------------------------------------

    public void setRootFile(boolean rootFile) {
        this.rootFile = rootFile;
    }

    public void setURI(URI uri) {
        this.uri = uri;
    }

    public void setMountAs(String mountAs) {
        this.mountAs = mountAs;
    }

    public void setDiskPerms(String diskPerms) {
        this.diskPerms = diskPerms;
    }

    public void setUnpropURI(URI uri) {
        this.unpropURI = uri;
    }

    public void setBlankSpaceName(String name) {
        this.blankspaceName = name;
    }

    public void setBlankSpaceSize(int space) {
        this.blankSpaceSize = space;
    }


    // -------------------------------------------------------------------------
    // DEBUG STRING
    // -------------------------------------------------------------------------

    public String toString() {
        return "DefaultVMFile{" +
                "rootFile=" + rootFile +
                ", uri='" + uri +
                "', mountAs='" + mountAs + '\'' +
                "', diskPerms='" + diskPerms + '\'' +
                ", unpropURI=" + unpropURI +
                ", blankspaceName='" + blankspaceName + '\'' +
                ", blankSpaceSize=" + blankSpaceSize +
                '}';
    }
}
