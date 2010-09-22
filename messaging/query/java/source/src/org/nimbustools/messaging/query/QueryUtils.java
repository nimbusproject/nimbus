/*
 * Copyright 1999-2009 University of Chicago
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
package org.nimbustools.messaging.query;

import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.MultivaluedMap;
import java.util.List;
import java.util.ArrayList;

public class QueryUtils {

    /**
     * Extracts an AWS parameter list from a Uri. Lists are stored as query
     * parameters of the form paramName.1, paramName.2, ... paramName.N.
     * Indexing starts at one.
     * @param uriInfo JAX-RS object containing Uri info
     * @param paramName Name of the parameter list to extract
     * @return Ordered list of parameters, never null. 
     */
    public static List<String> getParameterList(UriInfo uriInfo, String paramName) {

        if (uriInfo == null) {
            throw new IllegalArgumentException("uriInfo may not be null");
        }
        if (paramName == null) {
            throw new IllegalArgumentException("paramName may not be null");
        }

        final MultivaluedMap<String,String> queryParams =
                uriInfo.getQueryParameters();
        final ArrayList<String> list = new ArrayList<String>();

        String id;
        int i = 1;
        while ((id = queryParams.getFirst(paramName+"."+i)) != null) {
            id = id.trim();
            if (id.length() != 0) {
                list.add(id);
            }
            i++;
        }
        return list;
    }

    public static void assureRequiredParameter(String name, String value)
        throws QueryException {

        if (value == null || value.trim().length() == 0) {
            throw new QueryException(QueryError.InvalidArgument,
                    "The "+name+" parameter must be specified for this action");
        }
    }

    public static int getIntParameter(String name, String value)
        throws QueryException {

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new QueryException(QueryError.InvalidParameterValue,
                    "The "+name+" parameter must be an integer");
        }

    }

    public static boolean safeStringEquals(String s1, String s2) {

        // string comparison that is safe from timing attacks
        // when strings are of equal length, compares all chars

        if (s1 == null || s2 == null) {
            return s1 == s2;
        }
        if (s1.length() != s2.length()) {
            return false;
        }
        byte result = 0;
        for (int i=0; i< s1.length(); i++) {
            result |= s1.charAt(i) ^ s2.charAt(i);
        }
        return result == 0;
    }
}
