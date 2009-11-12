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
package org.nimbustools.messaging.query.security;

import org.springframework.web.filter.GenericFilterBean;
import org.nimbustools.messaging.query.QueryExceptionMapper;
import org.nimbustools.messaging.query.QueryException;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.PrintWriter;

public class QueryExceptionTranslationFilter extends GenericFilterBean {

    protected QueryExceptionMapper exceptionMapper;

    public QueryExceptionTranslationFilter() {
        this.addRequiredProperty("exceptionMapper");
    }

    // alright this isn't awesome. pushing spring security auth errors through our CXF handler.
    // TODO probably be best to go with one or the other

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        try {
            filterChain.doFilter(request, response);
        } catch (QueryException e) {

            final Response resp = exceptionMapper.toResponse(e);

            response.setStatus(resp.getStatus());
            response.setContentType("text/xml");
            final PrintWriter writer = response.getWriter();
            writer.print(resp.getEntity());
            writer.close();
        } //TODO what about other errors?

    }

    public QueryExceptionMapper getExceptionMapper() {
        return exceptionMapper;
    }

    public void setExceptionMapper(QueryExceptionMapper exceptionMapper) {
        this.exceptionMapper = exceptionMapper;
    }
}
