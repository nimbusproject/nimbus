package org.nimbustools.querygeneral.security;

import org.springframework.core.io.Resource;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataRetrievalFailureException;
import org.mortbay.util.QuotedStringTokenizer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.HashMap;
import java.util.NoSuchElementException;

/**
 * UserDetailsService which loads users from a file.
 *
 * The file format is one user per line: DN accessID secret
 * Any token may be quoted and they are separated by whitespace.
 * Lines starting with # are ignored
 */
public class FileUserDetailsService implements QueryUserDetailsService {

    private static final Log logger =
            LogFactory.getLog(FileUserDetailsService.class.getName());
    private static final String DELIMS = " \t\n\r\f";

    final private Object lock = new Object();

    private File file;
    private long lastModified;

    final private Map<String, QueryUser> mapByID;
    final private Map<String, QueryUser> mapByDN;


    public FileUserDetailsService(Resource fileResource) throws IOException {
        if (fileResource == null) {
            throw new IllegalArgumentException("fileResource may not be null");
        }

        mapByID = new HashMap<String, QueryUser>();
        mapByDN = new HashMap<String, QueryUser>();

        load(fileResource.getFile());
    }

    public QueryUser loadUserByUsername(String username)
            throws UsernameNotFoundException, DataAccessException {

        if (username == null) {
            throw new IllegalArgumentException("username may not be null");
        }

        synchronized (this.lock) {

            this.refreshIfNeeded();

            final QueryUser user = mapByID.get(username);
            if (user != null) {
                return user;
            }

            throw new UsernameNotFoundException("access identifier '"
                    +username+"' unknown");
        }
    }

    public QueryUser loadUserByDn(String dn)
            throws UsernameNotFoundException, DataAccessException {
        if (dn == null) {
            throw new IllegalArgumentException("dn may not be null");
        }
        synchronized (this.lock) {

            this.refreshIfNeeded();

            final QueryUser user = mapByDN.get(dn);
            if (user != null) {
                return user;
            }

            throw new UsernameNotFoundException("DN '"
                    +dn+"' unknown");
        }
    }

    public boolean refreshIfNeeded() throws DataAccessException {
        synchronized (this.lock) {

            if (this.file.lastModified() > this.lastModified) {
                try {
                    load(this.file);
                } catch (IOException e) {
                    throw new DataRetrievalFailureException(
                        "Error refreshing user file", e);
                }
                return true;
            }
            return false;
        }
    }

    private void load(File file) throws IOException {

        if (file == null) {
            throw new IllegalArgumentException("file may not be null");
        }

        InputStream in = null;
        try {
            in = new FileInputStream(file);
            this.file = file;
            this.lastModified = file.lastModified();
            load(in);
        } finally {
            if (in != null) {
                try { in.close(); }
                catch(Exception e) {
                    logger.warn("Failed to close (??)", e);
                }
            }
        }
    }

    private void load(InputStream stream) throws IOException {
        final BufferedReader reader =
                new BufferedReader(new InputStreamReader(stream));

        mapByID.clear();
        mapByDN.clear();

        String line;
        while ((line = reader.readLine()) != null) {

            // format is DN accessID secret
            // each token can be quoted and is seperated by whitespace

            line = line.trim();

            if (line.length() == 0 || line.charAt(0) == '#') {
                continue;
            }

            final QuotedStringTokenizer tokenizer =
                    new QuotedStringTokenizer(line, DELIMS);

            final QueryUser user;
            try {
                final String dn = tokenizer.nextToken();
                final String accessID = tokenizer.nextToken();
                final String secret = tokenizer.nextToken();

                user = new QueryUser(accessID, secret, dn);

            } catch (NoSuchElementException e) {
                logger.warn("encountered illegal query user entry in file: '"+
                        line+"'. Ignoring.");
                continue;
            }

            mapByID.put(user.getAccessID(), user);
            mapByDN.put(user.getDn(), user);
        }
    }
}
