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

package org.nimbus.authz;

public interface RepositoryImageLocator {

    /**
     * @param identity The DN of the caller
     * @param vmname The VM of the caller
     * @return The proper URL for this file in the configured repository
     * @throws Exception issue finding the user, user repository bucket, or file
     */
    public String getImageLocation(String identity, String vmname) throws Exception;
}
