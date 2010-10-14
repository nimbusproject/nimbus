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
package org.globus.workspace.remoting.admin.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.*;

class Reporter {

    public static final String DEFAULT_DELIMITER = " ";

    private final Gson gson;

    private final OutputMode mode;
    private final String[] fields;
    private final String delimiter;

    public Reporter(OutputMode mode, String[] fields, String delimiter) {
        this.mode = mode;
        this.fields = new String[fields.length];
        System.arraycopy(fields, 0, this.fields, 0, fields.length);

        if (delimiter != null) {
            if (mode != OutputMode.Batch) {
                throw new IllegalArgumentException("delimiter only supported in " +
                        OutputMode.Batch.toString() + " mode");
            }
            this.delimiter = delimiter;
        } else if (mode == OutputMode.Batch) {
            this.delimiter = DEFAULT_DELIMITER;
        } else {
            this.delimiter = null;
        }

        if (mode == OutputMode.Json) {
            this.gson = new GsonBuilder().setPrettyPrinting().
                    serializeNulls().create();
        } else {
            this.gson = null;
        }
    }

    public void report(Collection<Map<String,String>> entries, OutputStream stream)
            throws IOException {
        final OutputStreamWriter writer = new OutputStreamWriter(stream);

        if (this.mode == OutputMode.Json) {
            List<LinkedHashMap<String,String>> filtered =
                    new ArrayList<LinkedHashMap<String, String>>(entries.size());
            for (Map<String,String> entry : entries) {
                filtered.add(_filterMap(entry,
                        new LinkedHashMap<String, String>(fields.length),
                        fields)
                );
            }
            gson.toJson(filtered, writer);
        } else {
            final int fieldMaxLength = _findMaxLength(fields);

            final LinkedHashMap<String,String> filtered =
                    new LinkedHashMap<String, String>(fields.length);

            for (Map<String, String> entry : entries) {
                _filterMap(entry, filtered, fields);
                this._report(filtered, writer, fieldMaxLength);

                filtered.clear();

                if (this.mode == OutputMode.Friendly) {
                    writer.append("\n");
                }
            }
        }
        writer.flush();
    }

    public void report(Map<String,String> entry, OutputStream stream)
            throws IOException {
        final OutputStreamWriter writer = new OutputStreamWriter(stream);

        final LinkedHashMap<String,String> filtered = _filterMap(entry,
                new LinkedHashMap<String, String>(fields.length),
                fields);

        if (mode == OutputMode.Json) {
            gson.toJson(filtered, writer);

        } else {
            final int fieldMaxLength = _findMaxLength(fields);
            _report(filtered, writer, fieldMaxLength);
        }
        writer.flush();
    }


    private void _report(Map<String, String> entry, OutputStreamWriter writer, int keyLength)
            throws IOException {
        if (this.mode == OutputMode.Friendly) {
            final String formatString = "%1$-" + keyLength + "s :  ";
            for (Map.Entry<String, String> pair : entry.entrySet()) {
                // skip output of null value entries
                if (pair.getValue() != null) {
                    final String key = String.format(formatString, pair.getKey());
                    writer.append(key).append(pair.getValue()).append("\n");
                }
            }
        } else if (this.mode == OutputMode.Batch) {
            boolean first = true;
            for (String value : entry.values()) {
                if (value == null) {
                    value = "";
                }
                if (first) {
                    first = false;
                } else {
                    writer.append(this.delimiter);
                }
                writer.append(value);
            }
            writer.append("\n");
        } else {
            throw new UnsupportedOperationException("programmer error: mode " +
                    mode.toString() + "is unsupported by this method");
        }

        writer.flush();
    }


    private static LinkedHashMap<String, String> _filterMap(Map<String,String> src,
                                                            LinkedHashMap<String,String> dest,
                                                            String[] fields) {
        if (!dest.isEmpty()) {
             throw new IllegalArgumentException("destination map is not empty!");
        }
        for (String field : fields) {
            if (!src.containsKey(field)) {
                throw new IllegalArgumentException("map is missing '"+field+"' field");
            }
            final String value = src.get(field);
            dest.put(field, value);
        }
        return dest;
    }

    private static int _findMaxLength(String[] strings) {
        int len = 0;
        for (String s : strings) {
            if (s.length() > len) {
                len = s.length();
            }
        }
        return len;
    }

    public enum OutputMode {Friendly, Batch, Json}

}
