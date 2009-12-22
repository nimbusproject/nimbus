-- Copyright 1999-2007 University of Chicago
--
-- Licensed under the Apache License, Version 2.0 (the "License"); you may not
-- use this file except in compliance with the License. You may obtain a copy
-- of the License at
--
--    http://www.apache.org/licenses/LICENSE-2.0
--
--  Unless required by applicable law or agreed to in writing, software
--  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
--  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
--  License for the specific language governing permissions and limitations
--  under the License.

-- connect 'jdbc:derby:workspace_service/WorkspaceAccountingDB;create=true';

-- workspaceid does not need to be unique (allows this data to persist beyond
-- one service deployment, service version, or configuration)

CREATE TABLE deployments
(
uuid CHAR(36) NOT NULL PRIMARY KEY,
workspaceid INT NOT NULL,
creator_dn VARCHAR(512) NOT NULL,
creation_time FLOAT NOT NULL,
requested_duration INT NOT NULL,
active SMALLINT NOT NULL,
elapsed_minutes FLOAT
);

