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

import org.apache.commons.codec.binary.Base64;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.nimbustools.querygeneral.security.QueryUser;
import org.nimbustools.querygeneral.security.QueryUserDetailsService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.filter.GenericFilterBean;
import org.springframework.dao.DataAccessException;
import org.nimbustools.messaging.query.QueryException;
import org.nimbustools.messaging.query.QueryError;
import org.nimbustools.messaging.query.QueryUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.Collator;
import java.util.Enumeration;
import java.util.Set;
import java.util.TreeSet;

public class QueryAuthenticationFilter extends GenericFilterBean {

    private static final String PARAM_ACCESSID = "AWSAccessKeyId";
    private static final String PARAM_SIGNATURE = "Signature";
    private static final String PARAM_SIGNATURE_VERSION = "SignatureVersion";
    private static final String PARAM_SIGNATURE_METHOD = "SignatureMethod";
    private static final String PARAM_TIMESTAMP = "Timestamp";
    private static final String PARAM_EXPIRES = "Expires";

    private static final int EXPIRATION_SECONDS = 300;

    private static final String SIGNATURE_VERSION_1 = "1";
    private static final String SIGNATURE_VERSION_2 = "2";
    private static final String HMACSHA256 = "HmacSHA256";
    private static final String HMACSHA1 = "HmacSHA1";


    private QueryUserDetailsService userDetailsService;

    private String accessIdParameter;
    private String signatureParameter;
    private String signatureVersionParameter;
    private String signatureMethodParameter;
    private String timestampParameter;
    private String expiresParameter;

    private int expirationSeconds;

    public QueryAuthenticationFilter() {

        this.accessIdParameter = PARAM_ACCESSID;
        this.signatureParameter = PARAM_SIGNATURE;
        this.signatureMethodParameter = PARAM_SIGNATURE_METHOD;
        this.signatureVersionParameter = PARAM_SIGNATURE_VERSION;
        this.timestampParameter = PARAM_TIMESTAMP;
        this.expiresParameter = PARAM_EXPIRES;
        this.expirationSeconds = EXPIRATION_SECONDS;

        addRequiredProperty("userDetailsService");
    }


    public void doFilter(ServletRequest servletRequest,
                         ServletResponse servletResponse,
                         FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        final String accessId = getAtMostOneParameter(request,
                this.accessIdParameter);
        if (accessId == null || accessId.length() == 0) {
            throw new QueryException(QueryError.MissingClientTokenId);
        }

        final String signature = getExactlyOneParameter(request,
                this.signatureParameter);
        final String signatureVersion = getExactlyOneParameter(request,
                this.signatureVersionParameter);

        // Note that signature version 1 has known vulnerabilities when used across plain
        // HTTP. This interface should only be offered over SSL.
        // see http://developer.amazonwebservices.com/connect/entry.jspa?externalID=1928
        //
        // if another version comes along, this library will need to be updated

        if (!(SIGNATURE_VERSION_2.equals(signatureVersion) ||
                SIGNATURE_VERSION_1.equals(signatureVersion))) {
            throw new QueryException(QueryError.InvalidParameterValue,
                    "Only signature versions 1 and 2 are supported");
        }

        String signatureMethod = getAtMostOneParameter(request,
                this.signatureMethodParameter);
        if (signatureMethod != null) {
            if (!(signatureMethod.equals(HMACSHA256) ||
                    signatureMethod.equals(HMACSHA1))) {
                throw new QueryException(QueryError.InvalidParameterValue,
                        "Only "+ HMACSHA256 +" or " +HMACSHA1 +
                                " are supported signature methods");
            }
        } else {
            signatureMethod = HMACSHA1;
        }

        final String timestamp = getAtMostOneParameter(request,
                this.timestampParameter);
        final String expires = getAtMostOneParameter(request,
                this.expiresParameter);

        final boolean hasTimestamp = timestamp != null;
        final boolean hasExpires = expires != null;
        if (hasTimestamp == hasExpires) {
            throw new QueryException(QueryError.InvalidArgument,
                    "Request must have timestamp or expiration, but not both");
        }

        final QueryUser user;
        try {
        user = userDetailsService.loadUserByUsername(accessId);
        } catch (UsernameNotFoundException e) {
            throw new QueryException(QueryError.InvalidClientTokenId, e);
        } catch (DataAccessException e) {
            throw new QueryException(QueryError.InternalError,
                    "Failed to retrieve user token for provided accessID", e);
        }
        final String secret = user.getSecret();

        final String stringToSign;
        if (SIGNATURE_VERSION_2.equals(signatureVersion)) {
            stringToSign = getStringToSign_v2(request);
        } else {
            stringToSign = getStringToSign_v1(request);
        }

        final String checkSig = createSignature(stringToSign,
                secret, signatureMethod);

        // Note that this comparison will succeed if both inputs are null.
        // (But checkSig can't be null in this implementation)

        if (!QueryUtils.safeStringEquals(signature, checkSig)) {
            logger.warn("Signature check failed on request for accessID: "+accessId);
            throw new QueryException(QueryError.SignatureDoesNotMatch,
                    "Signature check failed!");
        }

        // check for expiration of request-- replay attack prevention
        final DateTime expireTime;
        try {
            if (hasTimestamp) {

                // boto doesn't include the 'Z' in its timestamp
                // we force parsing to assume UTC

                final DateTime stamp = new DateTime(timestamp, DateTimeZone.UTC);
                expireTime = stamp.plusSeconds(this.expirationSeconds);
            } else {
                expireTime = new DateTime(expires, DateTimeZone.UTC);
            }
        } catch (IllegalArgumentException e) {
            throw new QueryException(QueryError.InvalidParameterValue, "Failed to parse "+
                    (hasTimestamp ? "timestamp" : "expiration") +
                    ". Must be in ISO8601 format.", e);
        }

        if (expireTime.isBeforeNow()) {
            throw new QueryException(QueryError.RequestExpired, "Request is expired");
        }

        final QueryAuthenticationToken auth = new QueryAuthenticationToken(user, true);

        // okay we have an authenticated request. set token on the SecurityContext
        SecurityContextHolder.getContext().setAuthentication(auth);

        // and continue along chain
        chain.doFilter(request, response);
    }


    private String getStringToSign_v1(HttpServletRequest request) {

        // Request must mapped to into a canonical string format. See:
        // http://docs.amazonwebservices.com/AWSEC2/latest/DeveloperGuide/using-query-api.html#query-authentication

        final StringBuilder buf = new StringBuilder();

        Collator stringCollator = Collator.getInstance();
        stringCollator.setStrength(Collator.PRIMARY);

        final Set<String> sortedKeys =
                new TreeSet<String>(stringCollator);

        final Enumeration paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String param = (String) paramNames.nextElement();

            // don't include signature in canonical query string
            if (!param.equals(this.signatureParameter)) {
                sortedKeys.add(param);
            }
        }
        for (String key : sortedKeys) {
            String[] values = request.getParameterValues(key);

            for (String val : values) {
                buf.append(key).append(val);
            }
        }
        return buf.toString();
    }

    private String getStringToSign_v2(HttpServletRequest request) {

        // Request must mapped to into a canonical string format. See:
        // http://docs.amazonwebservices.com/AWSEC2/latest/DeveloperGuide/using-query-api.html#query-authentication

        final char newline = '\n';
        final StringBuilder buf = new StringBuilder();

        buf.append(request.getMethod()).append(newline); // GET or POST

        String host = request.getHeader("Host");
        if (host == null || host.length() == 0) {
            throw new QueryException(QueryError.InvalidArgument,
                    "Request is missing Host header");
        }
        buf.append(host.toLowerCase()).append(newline);

        String requestUri = request.getRequestURI();
        if (requestUri.length() == 0) {
            requestUri = "/";
        }
        buf.append(requestUri).append(newline);

        appendCanonicalQueryString_v2(request, buf);

        return buf.toString();
    }

    private void appendCanonicalQueryString_v2(ServletRequest request,
                                           StringBuilder buf) {
        final Set<String> sortedKeys =
                new TreeSet<String>();
        final Enumeration paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String param = (String) paramNames.nextElement();

            // don't include signature in canonical query string
            if (!param.equals(this.signatureParameter)) {
                sortedKeys.add(param);
            }
        }
        boolean first = true;
        for (String key : sortedKeys) {
            String[] values = request.getParameterValues(key);

            for (String val : values) {
                if (first) {
                    first = false;
                } else {
                    buf.append('&');
                }

                buf.append(urlEncode(key)).append('=');
                buf.append(urlEncode(val));
            }
        }
    }

    private String createSignature(String s, String secretKey, String method) {

        final SecretKeySpec spec = new SecretKeySpec(secretKey.getBytes(), method);

        // these Macs may be expensive to create? perhaps need a caching scheme.
        // careful though, thread safety of Mac#doFinal() is unclear..

        final byte[] bytes;
        try {
            final Mac mac = Mac.getInstance(method);
            mac.init(spec);
            bytes = mac.doFinal(s.getBytes("UTF-8"));

        } catch (NoSuchAlgorithmException e) {
            throw new QueryException(QueryError.SignatureDoesNotMatch,
                    "Request used an unsupported signature method: "+method,
                    e);
        } catch (InvalidKeyException e) {
            // I don't think this should happen..
            throw new QueryException(QueryError.SignatureDoesNotMatch,
                    "Secret key is invalid", e);
        } catch (UnsupportedEncodingException e) {
            throw new QueryException(QueryError.SignatureDoesNotMatch,
                    "Signature generation failed", e);
        }

        return new String(Base64.encodeBase64(bytes));
    }

    private static String urlEncode(String s) {
        try {

            // URLEncoder sorta sucks..
            return URLEncoder.encode(s, "UTF-8").
                    replace("+", "%20").
                    replace("*", "%2A").
                    replace("%7E", "~");

        } catch (UnsupportedEncodingException e) {
            throw new QueryException(QueryError.SignatureDoesNotMatch,
                    "Failed to URL encode a value (??)", e);
        }

    }

    private static String getExactlyOneParameter(ServletRequest request,
                                                 String paramName) {
        String[] values = request.getParameterValues(paramName);
        if (values == null || values.length != 1) {
            throw new QueryException(QueryError.InvalidArgument,
                    "Request must have exactly one "+paramName+" parameter");
        }
        return values[0];
    }

    private static String getAtMostOneParameter(ServletRequest request,
                                                String paramName) {

        String[] values = request.getParameterValues(paramName);
        if (values == null || values.length == 0) {
            return null;
        }
        if (values.length > 1) {
            throw new QueryException(QueryError.InvalidArgument,
                    "Request must have at most one "+ paramName+" parameter");
        }
        return values[0];
    }

    public QueryUserDetailsService getUserDetailsService() {
        return userDetailsService;
    }

    public void setUserDetailsService(QueryUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    public String getAccessIdParameter() {
        return accessIdParameter;
    }

    public void setAccessIdParameter(String accessIdParameter) {
        this.accessIdParameter = accessIdParameter;
    }

    public String getSignatureParameter() {
        return signatureParameter;
    }

    public void setSignatureParameter(String signatureParameter) {
        this.signatureParameter = signatureParameter;
    }

    public String getSignatureVersionParameter() {
        return signatureVersionParameter;
    }

    public void setSignatureVersionParameter(String signatureVersionParameter) {
        this.signatureVersionParameter = signatureVersionParameter;
    }

    public String getSignatureMethodParameter() {
        return signatureMethodParameter;
    }

    public void setSignatureMethodParameter(String signatureMethodParameter) {
        this.signatureMethodParameter = signatureMethodParameter;
    }

    public String getTimestampParameter() {
        return timestampParameter;
    }

    public void setTimestampParameter(String timestampParameter) {
        this.timestampParameter = timestampParameter;
    }

    public String getExpiresParameter() {
        return expiresParameter;
    }

    public void setExpiresParameter(String expiresParameter) {
        this.expiresParameter = expiresParameter;
    }

    public int getExpirationSeconds() {
        return expirationSeconds;
    }

    public void setExpirationSeconds(int expirationSeconds) {
        this.expirationSeconds = expirationSeconds;
    }
}
