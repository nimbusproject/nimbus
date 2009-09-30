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

package org.globus.workspace;

/**
 * Chained exceptions imply cause.  This exception is for when a handler in a
 * lower layer is a) cleaning up after a previous problem, b) runs into an
 * exception during the backout, and c) can not fully handle that situation
 * itself.
 *
 * It needs to throw something, but which exception to throw.  Now there is this
 * one to throw.  It is for very severe and/or bizarre situations.
 */
public class ExceptionDuringBackoutHandlerException extends Exception {

    private Throwable originalProblem;
    private Throwable backoutProblem;

    public ExceptionDuringBackoutHandlerException() {
        super();
    }

    public ExceptionDuringBackoutHandlerException(String message) {
        super(message);
    }

    public ExceptionDuringBackoutHandlerException(String message, Exception e) {
        super(message, e);
    }

    public ExceptionDuringBackoutHandlerException(String message, Throwable t) {
        super(message, t);
    }

    public ExceptionDuringBackoutHandlerException(Exception e) {
        super("", e);
    }

    public Throwable getOriginalProblem() {
        return this.originalProblem;
    }

    public void setOriginalProblem(Throwable originalThrowable) {
        this.originalProblem = originalThrowable;
    }

    public Throwable getBackoutProblem() {
        return this.backoutProblem;
    }

    public void setBackoutProblem(Throwable backoutThrowable) {
        this.backoutProblem = backoutThrowable;
    }


    public String toString() {
        String err =
            "ExceptionDuringBackoutHandlerException is severe " +
                "(or at least bizarre).  " +
            "You should never be reading this message, please report this " +
                "event including as much information as possible.\n\n" +
                "MESSAGE: " + super.toString();

        err += "\n\nORIGINAL ISSUE: ";
        if (this.originalProblem != null) {
            err += this.originalProblem.toString();
        } else {
            err += "not set?";
        }

        err += "\n\nBACKOUT ISSUE: ";
        if (this.backoutProblem != null) {
            err += this.backoutProblem.toString();
        } else {
            err += "not set?";
        }

        // todo: append stack trace
        // todo: append thread dump
        
        return err;
    }
}
