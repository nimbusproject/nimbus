/*
 * Copyright 1999-2010 University of Chicago
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
package org.globus.workspace.remoting;

import org.apache.commons.io.FileUtils;
import static org.testng.Assert.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.rmi.Remote;
import java.util.Collections;
import java.util.Map;

public class RemotingTests {

    private File socketDir;

    public RemotingTests() {
        //hmmmm, not sure how to do this..
        System.setProperty("org.newsclub.net.unix.library.path",
                "/Users/david/Code/nimbus/lib/native");
    }

    @Before
    public void setUp() throws IOException {
        socketDir = createTempDirectory();
    }

    @After
    public void tearDown() throws IOException {
        FileUtils.deleteDirectory(socketDir);
    }

    @Test
    public void testRemoting() throws Exception {
        final Counter localCounter = new CounterImpl();
        Map<String,Remote> bindings = Collections.singletonMap("counter",
                (Remote)localCounter);
        
        RemotingServer server = new RemotingServer();
        server.setSocketDirectory(socketDir);
        server.setBindings(bindings);
        server.initialize();

        // okay, not really remote
        RemotingClient client = new RemotingClient();
        client.setSocketDirectory(socketDir);
        client.initialize();
        Counter remoteCounter = (Counter) client.lookup("counter");
        assertNotSame(remoteCounter, localCounter);

        localCounter.increment();
        assertEquals(remoteCounter.getCount(), 1);
        assertEquals(remoteCounter.getCount(), localCounter.getCount());

        remoteCounter.increment();
        assertEquals(remoteCounter.getCount(), 2);
        assertEquals(remoteCounter.getCount(), localCounter.getCount());
    }

    public static File createTempDirectory() throws IOException {
        final File temp = File.createTempFile("temp", null);
        if (!(temp.delete())) {
            throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
        }
        if (!(temp.mkdir())) {
            throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
        }
        return temp;
    }
}

interface Counter extends Remote {
    int getCount() throws IOException;
    void increment() throws IOException;
}

class CounterImpl implements Counter {

    private int count = 0;

    public int getCount() {
        return count;
    }

    public void increment() {
        count++;
    }
}
