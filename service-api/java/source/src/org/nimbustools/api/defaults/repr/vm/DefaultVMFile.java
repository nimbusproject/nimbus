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

    // -------------------------------------------------------------------------
    // EQUALS AND HASHCODE
    // -------------------------------------------------------------------------       
   
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + blankSpaceSize;
        result = prime * result
                + ((blankspaceName == null) ? 0 : blankspaceName.hashCode());
        result = prime * result
                + ((diskPerms == null) ? 0 : diskPerms.hashCode());
        result = prime * result + ((mountAs == null) ? 0 : mountAs.hashCode());
        result = prime * result + (rootFile ? 1231 : 1237);
        result = prime * result
                + ((unpropURI == null) ? 0 : unpropURI.hashCode());
        result = prime * result + ((uri == null) ? 0 : uri.hashCode());
        return result;
    }

    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DefaultVMFile other = (DefaultVMFile) obj;
        if (blankSpaceSize != other.blankSpaceSize)
            return false;
        if (blankspaceName == null) {
            if (other.blankspaceName != null)
                return false;
        } else if (!blankspaceName.equals(other.blankspaceName))
            return false;
        if (diskPerms == null) {
            if (other.diskPerms != null)
                return false;
        } else if (!diskPerms.equals(other.diskPerms))
            return false;
        if (mountAs == null) {
            if (other.mountAs != null)
                return false;
        } else if (!mountAs.equals(other.mountAs))
            return false;
        if (rootFile != other.rootFile)
            return false;
        if (unpropURI == null) {
            if (other.unpropURI != null)
                return false;
        } else if (!unpropURI.equals(other.unpropURI))
            return false;
        if (uri == null) {
            if (other.uri != null)
                return false;
        } else if (!uri.equals(other.uri))
            return false;
        return true;
    }    
}
