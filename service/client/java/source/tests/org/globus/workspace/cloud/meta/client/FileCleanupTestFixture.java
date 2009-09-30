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

package org.globus.workspace.cloud.meta.client;

import org.junit.Before;
import org.junit.After;

import java.util.UUID;
import java.util.ArrayList;
import java.io.File;

/**
 * Base class for unit tests that create temp files. Provides an empty
 * directory and an empty list @tempFiles. At cleanup, every file in the
 * list will be deleted, followed by the directory.
 */
public class FileCleanupTestFixture {
    File tempDir;
    ArrayList<File> tempFiles = new ArrayList<File>();

    @Before
    public void setup() throws Exception {
        String tempPath = System.getProperty("java.io.tmpdir");

        if (tempPath == null || tempPath.length() == 0) {
            throw new Exception("could not find temp dir");
        }

        UUID uuid = UUID.randomUUID();

        this.tempDir = new File(tempPath+
            File.separator+
            "CloudMetaClient"+
            uuid);

        if (!this.tempDir.mkdir()) {
            throw new Exception("Failed to create temp dir!");
        }

    }

    @After
    public void teardown() {
        for (int i = tempFiles.size()-1; i >= 0; i--) {
            File f = tempFiles.get(i);
            if (f.exists()) {
                f.delete();
            }
        }
        if (tempDir != null) {
            tempDir.delete();
        }
    }
}
