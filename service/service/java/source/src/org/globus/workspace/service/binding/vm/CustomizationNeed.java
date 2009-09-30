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

/**
 * right now limits are checked before hitting the DB for fail fast, sourcepath
 * is 32 chars (UUID), destpath is is 512 chars and is the path ON the VM.
 * On the VMM, the file is also the UUID.
 */
public class CustomizationNeed {

    public final static int srcMax = 36;
    public final static int dstMax = 512;

    private final static String legalDestCharsString =
          "./_-abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    public final static char[] legalDestChars =
                                    legalDestCharsString.toCharArray();

    public final String sourcePath;
    public final String destPath;

    boolean wasSent;

    public CustomizationNeed(String src, String dst)
            throws Exception {
        this(src, dst, false);
    }

    public CustomizationNeed(String src, String dst, boolean sent)
            throws Exception {
        
        if (src == null) {
            throw new IllegalArgumentException("source path may not be null");
        }
        if (src.length() > srcMax) {
            throw new Exception(
                    "customization source path is too long: " +
                                    src.length() + " > " + srcMax +
                            ".  Path: '" + src + "'");
        }
        if (dst == null) {
            throw new IllegalArgumentException(
                    "destination path may not be null");
        }
        if (dst.length() > dstMax) {
            throw new Exception(
                    "customization destination path is too long: " +
                                    dst.length() + " > " + dstMax +
                            ".  Path: '" + dst + "'");
        }

        // the mount tool would catch this too, but failfast
        final char[] dstChars = dst.toCharArray();
        for (int i = 0; i < dstChars.length; i++) {
            if (!legalChar(dstChars[i])) {
                throw new Exception(
                    "customization destination path contains illegal " +
                    "character '" + dstChars[i] + "'. Path: '" + dst + "'");
            }
        }

        if (dst.indexOf("../") >= 0) {
            throw new Exception(
                    "customization destination path contains illegal " +
                    "path expansion, for example '../'. Path: '" + dst + "'");
        }

        this.sourcePath = src;
        this.destPath = dst;
        this.wasSent = sent;
    }

    // for clone only
    // 'fake' is just there to get around java limitation
    private CustomizationNeed(String src, String dst, boolean sent, int fake) {
        this.sourcePath = src;
        this.destPath = dst;
        this.wasSent = sent;
    }

    public synchronized boolean isSent() {
        return this.wasSent;
    }

    public synchronized void setSent(boolean sent) {
        this.wasSent = sent;
    }

    private static boolean legalChar(char c) {
        for (int i = 0; i < legalDestChars.length; i++) {
            if (c == legalDestChars[i]) {
                return true;
            }
        }
        return false;
    }

    public static CustomizationNeed[] cloneArray(CustomizationNeed[] cur)
            throws Exception {

        if (cur == null) {
            return null;
        }

        final CustomizationNeed[] newArr = new CustomizationNeed[cur.length];
        for (int i = 0; i < cur.length; i++) {
            newArr[i] = cloneOne(cur[i]);
        }
        return newArr;
    }

    public static CustomizationNeed cloneOne(CustomizationNeed cur)
            throws Exception {

        if (cur == null) {
            return null;
        }
        
        return new CustomizationNeed(
                        cur.sourcePath, cur.destPath, cur.wasSent, 0);
    }
}
