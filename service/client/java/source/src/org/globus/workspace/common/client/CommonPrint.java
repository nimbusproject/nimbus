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

package org.globus.workspace.common.client;

import org.globus.workspace.common.print.Print;

public class CommonPrint {

    private static final String defaultDbgTopFill = "=";
    private static final String defaultDbgBottomFill = "-";
    private static final int defaultNumCols = 80; // always set to even number

    public static void logArgs(String[] args, Print pr) {
        
        if (args == null || pr == null) {
            return; // *** EARLY RETURN ***
        }

        final String sectionTitle = "GIVEN ARGUMENTS";
        printDebugSection(pr, sectionTitle);

        for (int i = 0; i < args.length; i++) {
            pr.debugln("  " + args[i]);
        }

        printDebugSectionEnd(pr, sectionTitle);
    }

    public static void printDebugSection(Print pr, String title) {

        if (pr == null || !pr.enabled()) {
            return;
        }

        String sending = " [ " + title + " ] ";
        if (title == null || title.trim().length() == 0) {
            sending = null;
        } 

        printMarker(pr, sending, defaultDbgTopFill, defaultNumCols, true);
    }

    public static void printDebugSectionEnd(Print pr, String title) {
        
        if (pr == null) {
            return;
        }

        printMarker(pr, null, defaultDbgBottomFill, defaultNumCols, true);
    }

    public static String textDebugSection(String title) {

        String sending = " [ " + title + " ] ";
        if (title == null || title.trim().length() == 0) {
            sending = null;
        }

        return textMarker(sending, defaultDbgTopFill, defaultNumCols, true);
    }

    public static String textDebugSectionEnd(String title) {
        return textMarker(null, defaultDbgBottomFill, defaultNumCols, true);
    }

    private static void printMarker(Print pr,
                                    String text,
                                    String fill,
                                    int numColumns,
                                    boolean moreToRight) {
        
        pr.debugln("\n" +
                   getMarker(text, fill, numColumns, moreToRight) +
                   "\n");
    }

    private static String textMarker(String text,
                                     String fill,
                                     int numColumns,
                                     boolean moreToRight) {

        return "\n" + getMarker(text, fill, numColumns, moreToRight) + "\n";
    }

    private static String getMarker(String text,
                                    String fill,
                                    int numColumns,
                                    boolean moreToLeft) {

        final StringBuffer buf = new StringBuffer(numColumns);

        if (text == null || text.trim().length() == 0) {

            for (int i = 0; i < numColumns; i++) {
                if (fill != null) {
                    buf.append(fill);
                }
            }

        } else {

            final int len = text.length();

            int before = numColumns/2;
            int after = numColumns/2;

            final int remainder = len % 2;
            final int half = len / 2;

            before -= half;
            after -= half;
            
            if (moreToLeft) {
                after -= remainder;
            } else {
                before -= remainder;
            }

            for (int i = 0; i < before; i++) {
                if (fill != null) {
                    buf.append(fill);
                }
            }

            buf.append(text);

            for (int i = 0; i < after; i++) {
                if (fill != null) {
                    buf.append(fill);
                }
            }

        }

        return buf.toString();
    }
}
