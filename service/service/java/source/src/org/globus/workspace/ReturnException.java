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
 *  Used to carry back the return code of a forked process, and
 *  also the stdin/stdout/stderr if needed.
 */
public class ReturnException extends Exception {
    public int retval;
    public String stdout;
    public String stderr;

    ReturnException() {
        super();
    }

    public ReturnException(int ret) {
        super();
        this.retval = ret;
    }

    public ReturnException(int ret, String stderr) {
        super();
        this.retval = ret;
        this.stderr = stderr;
    }

    public ReturnException(int ret, String stderr, String stdout) {
        super();
        this.retval = ret;
        this.stdout = stdout;
        this.stderr = stderr;
    }
}
