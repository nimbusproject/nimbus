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

package org.globus.workspace.common.print;

import java.io.PrintStream;

public abstract class ProxyPrintStream extends PrintStream {

    private static final PrintStream bitBucket =
                        new PrintStream(new NullOutputStream());

    final Print pr;

    ProxyPrintStream(Print print) {

        // For expediency, this just overrides close, flush, and some of the
        // print/println methods.  Any other calls are ignored because of the
        // NullOutputStream implementation.
        super(bitBucket, true);

        if (print == null) {
            throw new IllegalArgumentException("pr may not be null");
        }

        this.pr = print;
    }

    public void close() {
        // ignore
    }

    public void flush() {
        this.pr.flush();
    }

    public abstract void println();

    public abstract void print(Object obj);

    public abstract void println(Object x);

    public abstract void print(String s);

    public abstract void println(String x);
}
