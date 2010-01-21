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
package org.nimbustools.messaging.rest;

import com.google.gson.*;

import javax.ws.rs.core.Response;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nimbustools.messaging.rest.repr.ErrorMessage;
import org.joda.time.DateTime;

import java.net.URI;
import java.lang.reflect.Type;
import java.util.Date;

public class ResponseUtil {

    public static final String JSON_CONTENT_TYPE = "application/json";

    private static final Log logger =
            LogFactory.getLog(ResponseUtil.class.getName());

    final Gson gson;

    public ResponseUtil() {
        final GsonBuilder gsonBuilder = new GsonBuilder();

        gsonBuilder.registerTypeAdapter(DateTime.class,
                new DateTimeTypeConverter());

        this.gson = gsonBuilder.create();
    }

    public Response createJsonResponse(Object obj) {

        if (obj == null) {
            throw new IllegalArgumentException("obj may not be null");
        }
        final String json = gson.toJson(obj);

        return Response.ok(json, JSON_CONTENT_TYPE).build();
    }

    public String createJsonString(Object obj) {
        if (obj == null) {
            throw new IllegalArgumentException("obj may not be null");
        }

        return gson.toJson(obj);
    }

    public Response createServerErrorResponse() {
        return Response.serverError().build();
    }

    public Response createErrorResponse(ErrorMessage error, Response.Status status) {

        if (error == null) {
            throw new IllegalArgumentException("error may not be null");
        }

        final String json = this.gson.toJson(error);

        return Response.status(status).entity(json).build();
    }

    public Response createCreatedResponse(URI uri) {
        if (uri == null) {
            throw new IllegalArgumentException("uri may not be null");
        }
        return Response.created(uri).build();
    }

    public <T> T fromJson(String json, Class<T> classOfT) {
        try {
            return gson.fromJson(json, classOfT);
        } catch (Exception e) {
            throw new NimbusWebException("Failed to parse JSON message as a "+
                    classOfT.getSimpleName(), e);
        }
    }

    // sample JodaTime adapter from:
    // http://sites.google.com/site/gson/gson-type-adapters-for-common-classes-1
    private static class DateTimeTypeConverter
      implements JsonSerializer<DateTime>, JsonDeserializer<DateTime> {
    public JsonElement serialize(DateTime src, Type srcType, JsonSerializationContext context) {
      return new JsonPrimitive(src.toString());
    }

    public DateTime deserialize(JsonElement json, Type type, JsonDeserializationContext context)
        throws JsonParseException {
      try {
        return new DateTime(json.getAsString());
      } catch (IllegalArgumentException e) {
        // May be it came in formatted as a java.util.Date, so try that
        Date date = context.deserialize(json, Date.class);
        return new DateTime(date);
      }
    }
  }

    
}
