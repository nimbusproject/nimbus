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

package org.globus.workspace.scheduler.defaults;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Random;

import javax.sql.DataSource;

import org.apache.derby.jdbc.EmbeddedDataSource;
import org.junit.AfterClass;
import org.junit.BeforeClass;

public class NimbusDatabaseTestCase {

    private static File derbyDir;
    private static DataSource dataSource;
    private static DataSource shutdownDataSource;

    @BeforeClass
    public static void createDatabase() throws Exception {
        // create a temp dir for holding the database
        derbyDir = makeTempDir();
        File dbDir = new File(derbyDir, "db");

        // create database and initialize it
        dataSource = getCreateDataSource(dbDir);
        createNimbusDB();

        // create a data source for shutting down later
        shutdownDataSource = getShutdownDataSource(dbDir);
    }

    private static void createNimbusDB() throws SQLException, IOException {

        Connection connection = dataSource.getConnection();
        
        
        BufferedReader d = new BufferedReader(new FileReader("./service/service/java/source/share/lib/workspace_service_derby_schema.sql"));

        String thisLine, sqlQuery;
        sqlQuery = "";
        while ((thisLine = d.readLine()) != null) {
            //Skip comments and empty lines
            if(thisLine.trim().length() > 0 && thisLine.trim().charAt(0) == '-' || thisLine.trim().length() == 0 )
                continue;     

            sqlQuery = sqlQuery + " " + thisLine.trim();

            //If command is complete
            if(sqlQuery.endsWith(";")) {
                sqlQuery = sqlQuery.replace(";" , ""); //Remove the ; since jdbc complains
                
                Statement statement = connection.createStatement();
                
                statement.execute(sqlQuery.trim());
                
                sqlQuery = "";
                
                statement.close();
            }

        }
        
        connection.close();
    }

    /**
     * Shuts down and deletes the temporary database after all tests have 
     * completed.
     */
    @AfterClass
    public static void deleteDatabase() throws Exception {
        try {
            shutdownDataSource.getConnection();
        }
        catch(SQLException sqle) {
            // successful shutdown throws an exception
        }
        System.gc();
        assertTrue("failed to delete temp dir", deleteRecursively(derbyDir));
        derbyDir = null;
    }

    /**
     * Gets a data source for the derby database at the given path, and which 
     * creates the database if it doesn't already exist.  The data source will
     * connect to a local derby database using embedded mode.
     * @param path the path to the directory containing the database
     * @return a data source
     */
    private static DataSource getCreateDataSource(File path) throws Exception {
        if (path == null) { 
            throw new IllegalArgumentException("path is null");
        }
                
        EmbeddedDataSource dataSource = new EmbeddedDataSource();
        dataSource.setDatabaseName(path.getPath());
        dataSource.setCreateDatabase("create");
        return dataSource;
    }

    /**
     * Gets a data source for the derby database at the given path, and which 
     * shuts down the database when it is connected to.  The data source will
     * connect to a local derby database using embedded mode.
     * @param path the path to the directory containing the database
     * @return a data source
     */
    private static DataSource getShutdownDataSource(File path) {
        if (path == null) { 
            throw new IllegalArgumentException("path is null");
        }
        EmbeddedDataSource dataSource = new EmbeddedDataSource();
        dataSource.setDatabaseName(path.getPath());
        dataSource.setShutdownDatabase("shutdown");
        return dataSource;
    }


    private static final String RANDOM_FILENAME_CHARS =
        "abcdefghijklmnopqrstuvwxyz";
    private static final int DEFAULT_RANDOM_FILENAME_LENGTH = 12;

    /**
     * Gets the system temporary directory.
     * @return the system temporary directory
     */
    private static File getSystemTempDir() {
        return new File(System.getProperty("java.io.tmpdir"));
    }

    /**
     * Creates and returns a temporary directory under the system temporary 
     * directory.  The directory will be deleted on system exit.
     * @return a newly created temporary directory which will be deleted on 
     *         system exit
     */
    private static File makeTempDir() {
        File sysDir = getSystemTempDir();
        File tempDir = null;
        while (tempDir == null || tempDir.exists()) {
            tempDir = new File(sysDir, generateRandomFilename());
        }
        tempDir.mkdir();
        tempDir.deleteOnExit();
        return tempDir;
    }

    /**
     * Deletes the given file or directory, and all of its contained files and
     * directories.  If deletion does not complete successfully, some files
  may
     * have been deleted.
     * @param file the file or directory to delete 
     * @return whether the file and its children were deleted successfully
     */
    private static boolean deleteRecursively(File file) {
        File[] files = file.listFiles();
        if (files != null) {
            for (File f : files) {
                if (!deleteRecursively(f)) {
                    return false;
                }
            }
        }
        return file.delete();
    }

    /**
     * Generates a random string suitable for use as a filename.  Because the
     * string is randomly generated, it is unlikely though possible that a 
     * matching file exists.
     * @return a random string suitable for use as a filename
     */
    private static String generateRandomFilename() {
        return generateRandomFilename(DEFAULT_RANDOM_FILENAME_LENGTH);
    }

    /**
     * Generates a random string suitable for use as a filename.  Because the
     * string is randomly generated, it is unlikely though possible that a 
     * matching file exists.
     * @param length the filename length, in characters; must be positive
     * @return a random string suitable for use as a filename
     */
    private static String generateRandomFilename(int length) {
        if (length < 1) {
            throw new IllegalArgumentException("length is not positive");
        }
        Random rand = new Random();
        char[] filename = new char[length];
        for (int i = 0; i < filename.length; i++) {
            int index = (rand.nextInt() & Integer.MAX_VALUE) %
            RANDOM_FILENAME_CHARS.length();
            filename[i] = RANDOM_FILENAME_CHARS.charAt(index);
        }
        return new String(filename);
    }

    public static File getDerbyDir() {
        return derbyDir;
    }

    public static DataSource getDataSource() {
        return dataSource;
    }

}
