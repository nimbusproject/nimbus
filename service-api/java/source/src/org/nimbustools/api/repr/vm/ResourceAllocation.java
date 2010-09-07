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

package org.nimbustools.api.repr.vm;

public interface ResourceAllocation {

    public static final String ARCH_sparc = "sparc";
    public static final String ARCH_powerpc = "powerpc";
    public static final String ARCH_x86 = "x86";
    public static final String ARCH_x86_32 = "x86_32";
    public static final String ARCH_x86_64 = "x86_64";
    public static final String ARCH_parisc = "parisc";
    public static final String ARCH_mips = "mips";
    public static final String ARCH_ia64 = "ia64";
    public static final String ARCH_arm = "arm";
    public static final String ARCH_other = "other";

    public String getArchitecture();
    public int getIndCpuSpeed();
    public int getIndCpuCount();
    public int getCpuPercentage();
    public int getMemory();
    public int getNodeNumber();
}
