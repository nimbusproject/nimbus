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

package org.globus.workspace.service.binding.vm;

public class VirtualMachinePartition {
    private String image;
    private String imagemount;
    private boolean readwrite;
    private boolean rootdisk;
    private int blankspace;
    private boolean propRequired;
    private boolean unPropRequired;
    private String alternateUnpropTarget;

    public String getImage() {
        return this.image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getImagemount() {
        return this.imagemount;
    }

    public void setImagemount(String imagemount) {
        this.imagemount = imagemount;
    }

    public boolean isReadwrite() {
        return this.readwrite;
    }

    public void setReadwrite(boolean readwrite) {
        this.readwrite = readwrite;
    }

    public boolean isRootdisk() {
        return this.rootdisk;
    }

    public void setRootdisk(boolean rootdisk) {
        this.rootdisk = rootdisk;
    }

    public int getBlankspace() {
        return this.blankspace;
    }

    public void setBlankspace(int blankspace) {
        this.blankspace = blankspace;
    }

    public boolean isPropRequired() {
        return this.propRequired;
    }

    public void setPropRequired(boolean propRequired) {
        this.propRequired = propRequired;
    }

    public boolean isUnPropRequired() {
        return this.unPropRequired;
    }

    public void setUnPropRequired(boolean unPropRequired) {
        this.unPropRequired = unPropRequired;
    }

    public String getAlternateUnpropTarget() {
        return alternateUnpropTarget;
    }

    public void setAlternateUnpropTarget(
            String alternateUnpropTarget) {
        this.alternateUnpropTarget = alternateUnpropTarget;
    }


    public String toString() {
        return "VirtualMachinePartition{" +
                "image='" + this.image + '\'' +
                ", imagemount='" + this.imagemount + '\'' +
                ", readwrite=" + this.readwrite +
                ", rootdisk=" + this.rootdisk +
                ", blankspace=" + this.blankspace +
                ", propRequired=" + this.propRequired +
                ", unPropRequired=" + this.unPropRequired +
                ", alternateUnpropTarget='" + this.alternateUnpropTarget + '\'' +
                '}';
    }

    // don't use clone()
    // be sure to differentiate afterwards if that's what is desired...
    public static VirtualMachinePartition cloneOne(
                            final VirtualMachinePartition p) {

        if (p == null) {
            return null;
        }

        final VirtualMachinePartition vmp = new VirtualMachinePartition();

        vmp.blankspace = p.blankspace;
        vmp.image = p.image;
        vmp.imagemount = p.imagemount;
        vmp.propRequired = p.propRequired;
        vmp.readwrite = p.readwrite;
        vmp.rootdisk = p.rootdisk;
        vmp.unPropRequired = p.unPropRequired;
        vmp.alternateUnpropTarget = p.alternateUnpropTarget;

        return vmp;
    }
}
