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

package org.globus.workspace.cloud.client.util;

import org.globus.ftp.GridFTPClient;
import org.globus.ftp.Session;
import org.globus.ftp.MlsxEntry;
import org.globus.ftp.exception.ServerException;
import org.globus.util.GlobusURL;
import org.globus.gsi.gssapi.auth.HostAuthorization;
import org.globus.gsi.gssapi.auth.IdentityAuthorization;
import org.globus.gsi.gssapi.auth.Authorization;
import org.globus.workspace.client_core.ExecutionProblem;
import org.globus.workspace.cloud.client.tasks.CopyTask;
import org.globus.workspace.cloud.client.tasks.CopyWatchTask;
import org.globus.io.urlcopy.UrlCopy;

import java.io.PrintStream;
import java.io.IOException;
import java.util.Vector;
import java.util.ArrayList;

import edu.emory.mathcs.backport.java.util.concurrent.FutureTask;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import edu.emory.mathcs.backport.java.util.concurrent.ExecutionException;
import edu.emory.mathcs.backport.java.util.concurrent.TimeoutException;
import edu.emory.mathcs.backport.java.util.concurrent.ExecutorService;

public class RepositoryUtil {


    // -------------------------------------------------------------------------
    // SEND FILE
    // -------------------------------------------------------------------------

    public static void sendFile(String sourceUrlString,
                                String destUrlString,
                                long timeoutMinutes,
                                String identityAuthorization,
                                PrintStream info,
                                PrintStream debug,
                                ExecutorService executorService)

            throws ExecutionProblem {

        if (executorService == null) {
            throw new IllegalArgumentException("executorService may not be null");
        }

        final UrlCopy urlcopy = new UrlCopy();

        try {
            final GlobusURL source = new GlobusURL(sourceUrlString);
            urlcopy.setSourceUrl(source);
        } catch (Exception e) {
            throw new ExecutionProblem("Problem constructing source URL: " +
                                                e.getMessage());
        }

        try {
            final GlobusURL dest = new GlobusURL(destUrlString);
            urlcopy.setDestinationUrl(dest);
        } catch (Exception e) {
            throw new ExecutionProblem("Problem constructing destination " +
                                       "URL: " + e.getMessage());
        }

        if (identityAuthorization == null) {
            urlcopy.setDestinationAuthorization(
                            HostAuthorization.getInstance());

            if (debug != null) {
                debug.println(
                        "Using host-based authorization of remote server");
            }
        } else {
            final IdentityAuthorization idA =
                    new IdentityAuthorization(identityAuthorization);
            urlcopy.setDestinationAuthorization(idA);
            if (debug != null) {
                debug.println("Using identity-based authorization of remote " +
                        "server: '" + identityAuthorization + "'");
            }
        }

        PrintStream pr = null;
        if (info != null) {
            pr = info;
        } else if (debug != null) {
            pr = debug;
        }

        if (pr != null) {
            pr.println("\nTransferring");
            pr.println("  - Source: " + sourceUrlString);
            pr.println("  - Destination: " + destUrlString);
            pr.println();
        }

        final FutureTask[] tasks = new FutureTask[2];
        tasks[0] = new FutureTask(new CopyTask(urlcopy));

        if (info != null) {
            tasks[1] = new FutureTask(new CopyWatchTask(info, debug));
        } else {
            tasks[1] = null;
        }

        for (int i = 0; i < tasks.length; i++) {
            if (tasks[i] != null) {
                executorService.submit(tasks[i]);
            }
        }

        for (int i = 0; i < tasks.length; i++) {
            if (tasks[i] != null) {
                // all tasks currently return null
                try {
                    if (timeoutMinutes < 1) {
                        tasks[i].get();
                    } else {
                        tasks[i].get(timeoutMinutes, TimeUnit.MINUTES);
                    }
                } catch (InterruptedException e) {
                    throw new ExecutionProblem(e);
                } catch (ExecutionException e) {
                    throw new ExecutionProblem("Problem transferring: " +
                                                        e.getMessage());
                } catch (TimeoutException e) {
                    throw new ExecutionProblem("Timeout limit exceeded.");
                }
            }
        }

        if (info != null) {
            info.println("Copy complete.\n");
        } else if (debug != null) {
            debug.println("Copy complete.\n");
        }
    }


    // -------------------------------------------------------------------------
    // DELETE FILE
    // -------------------------------------------------------------------------

    public static void deleteFile(String delUrlString,
                                  String identityAuthorization,
                                  PrintStream info,
                                  PrintStream debug)

            throws ExecutionProblem {

        final GlobusURL delURL;
        try {
            delURL = new GlobusURL(delUrlString);
        } catch (Exception e) {
            throw new ExecutionProblem("Problem constructing delete URL: " +
                                                e.getMessage());
        }

        final GridFTPClient ftp;
        try {
            ftp = new GridFTPClient(delURL.getHost(), delURL.getPort());
        } catch (Exception e) {
            throw new ExecutionProblem("Problem constructing GridFTPClient: " +
                                                e.getMessage());
        }

        final Authorization auth;
        if (identityAuthorization == null) {
            auth = HostAuthorization.getInstance();
            if (debug != null) {
                debug.println(
                        "Using host-based authorization of remote server");
            }
        } else {
            auth = new IdentityAuthorization(identityAuthorization);
            if (debug != null) {
                debug.println("Using identity-based authorization of remote " +
                        "server: '" + identityAuthorization + "'");
            }
        }
        ftp.setAuthorization(auth);

        PrintStream pr = null;
        if (info != null) {
            pr = info;
        } else if (debug != null) {
            pr = debug;
        }

        if (pr != null) {
            pr.println("\nDeleting: " + delUrlString);
            pr.println();
        }

        try {
            ftp.authenticate(null);
            ftp.deleteFile(delURL.getPath());
        } catch (IOException e) {
            throw new ExecutionProblem(e.getMessage(), e);
        } catch (ServerException e) {
            throw new ExecutionProblem(e.getMessage(), e);
        }

        if (info != null) {
            info.println("Deleted.\n");
        } else if (debug != null) {
            debug.println("Deleted.\n");
        }
    }


    // -------------------------------------------------------------------------
    // LIST FILES
    // -------------------------------------------------------------------------

    public static FileListing[] listFiles(String url,
                                          String identAuthz,
                                          PrintStream info,
                                          PrintStream err,
                                          PrintStream debug) 
              throws ExecutionProblem {

        try {
            return listFilesImpl(url, identAuthz, info, err, debug);
        } catch (Exception e) {
            if (debug != null) {
                debug.println("\n---------- Listing problem: ---------\n");
                e.printStackTrace(debug);
                debug.println("\n-------- (End Listing problem) ------\n");
            }

            throw new ExecutionProblem("Problem listing workspaces: " +
                                                e.getMessage());
        }
    }

    public static FileListing[] listFilesImpl(String url,
                                              String identityAuthorization,
                                              PrintStream info,
                                              PrintStream err,
                                              PrintStream debug) 
               throws Exception {

        final GlobusURL listdir = new GlobusURL(url);

        if (debug != null) {
            debug.println("Listing:\n" + listdir.toString());
        }

        final GridFTPClient client =
                new GridFTPClient(listdir.getHost(), listdir.getPort());

        if (identityAuthorization == null) {
            client.setAuthorization(HostAuthorization.getInstance());
            if (debug != null) {
                debug.println(
                        "Using host-based authorization of remote server");
            }
        } else {
            final IdentityAuthorization idA =
                    new IdentityAuthorization(identityAuthorization);
            client.setAuthorization(idA);
            if (debug != null) {
                debug.println("Using identity-based authorization of remote " +
                        "server: '" + identityAuthorization + "'");
            }
        }

        if (debug != null) {
            debug.println("Authenticating.");
        }
        client.authenticate(null);
        client.setType(Session.TYPE_ASCII);
        client.setPassive();
        client.setLocalActive();

        if (debug != null) {
            debug.println("Changing directory (CWD).");
        }
        client.changeDir(listdir.getPath());

        if (debug != null) {
            debug.println("Listing.");
        }
        
        final Vector v = client.mlsd(null);

        int len = v.size();
        if (debug != null) {
            debug.println("Size of list return vector: " + len);
        }

        final ArrayList files = new ArrayList(len);
        while (! v.isEmpty()) {
            final MlsxEntry f = (MlsxEntry)v.remove(0);
            if (f == null) {
                if (debug != null) {
                    debug.println("null MlsxEntry received (?)");
                }
                continue;
            }

            final String fileName = f.getFileName();
            if (fileName == null) {
                if (debug != null) {
                    debug.println("no MlsxEntry filename (?)");
                }
                continue;
            }

            if (fileName.equals(".")) {
                len -= 1;
                if (debug != null) {
                    debug.println("'.' is not an interesting file");
                }
                continue;
            }
            if (fileName.equals("..")) {
                len -= 1;
                if (debug != null) {
                    debug.println("'..' is not an interesting file");
                }
                continue;
            }

            final FileListing fl = new FileListing();

            fl.setName(f.getFileName());

            final String sizeStr = f.get("size");
            if (sizeStr == null) {
                fl.setSize(-1);
            } else {
                long x = -1;
                try {
                    x = Long.parseLong(sizeStr);
                } catch (NumberFormatException e) {
                    // pass.
                }
                fl.setSize(x);
            }

            final String modified = f.get("modify");
            // 20080522161726
            if (modified == null || modified.length() != 14) {
                throw new Exception("cannot parse modified time");
            }
            fl.setDate(parseDate(modified));
            fl.setTime(parseTime(modified));

            final String type = f.get("type");
            if (type != null && type.equals("dir")) {
                fl.setDirectory(true);
            }


            // If user is root and perms are group no-write and all no-write,
            // we can, because of several conventions, safely say that the user
            // only has read-only access.
            final String owner = f.get("unix.owner");
            final String mode = f.get("unix.mode");
            if (mode == null) {
                fl.setReadWrite(false);
            } else if (mode.substring(3,4).equals("6")) {
                fl.setReadWrite(true);
            } else if (mode.substring(1,2).equals("6")) {
                if (owner != null && owner.equals("root")) {
                    fl.setReadWrite(false);
                } else {
                    fl.setReadWrite(true);
                }
            } else if (mode.substring(2,3).equals("6")) {
                fl.setReadWrite(true); // unknown to be actually true.
            }

            files.add(fl);
        }

        client.close();

        return (FileListing[]) files.toArray(new FileListing[files.size()]);
    }

    private static String parseDate(String modified) {
        if (modified == null || modified.length() != 14) {
            throw new IllegalArgumentException("invalid modified arg");
        }
        final String monthNum = modified.substring(4,6);
        final String month = getMonthStr(Integer.parseInt(monthNum));
        final String day = modified.substring(6,8);
        return month + " " + day;
    }

    private static String getMonthStr(int month) {
        switch (month) {
            case 1: return "Jan";
            case 2: return "Feb";
            case 3: return "Mar";
            case 4: return "Apr";
            case 5: return "May";
            case 6: return "Jun";
            case 7: return "Jul";
            case 8: return "Aug";
            case 9: return "Sep";
            case 10: return "Oct";
            case 11: return "Nov";
            case 12: return "Dec";
            default: return "???";
        }
    }

    private static String parseTime(String modified) {
        if (modified == null || modified.length() != 14) {
            throw new IllegalArgumentException("invalid modified arg");
        }
        final String hours = modified.substring(8,10);
        final String minutes = modified.substring(10,12);
        return hours + ":" + minutes;
    }

    public static void main(String[] args) {
        System.out.println(parseDate("20080522161726"));
        System.out.println(parseTime("20080522161726"));
    }
}
