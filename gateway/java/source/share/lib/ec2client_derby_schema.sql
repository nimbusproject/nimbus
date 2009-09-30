-- Copyright 1999-2008 University of Chicago
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

-- connect 'jdbc:derby:EC2AccountingDB;create=true';

-- account and account_instance are for AccountingManager

CREATE TABLE account
(
dn VARCHAR(512) NOT NULL PRIMARY KEY,
credits_used INT NOT NULL DEFAULT 0,
credits_max INT NOT NULL DEFAULT 0
);

CREATE TABLE account_instance
(
id VARCHAR(64) NOT NULL PRIMARY KEY,
dn VARCHAR(512) NOT NULL,
rate INT NOT NULL DEFAULT 0,
charge INT NOT NULL DEFAULT 0,
start_time TIMESTAMP,
stop_time TIMESTAMP
);

-- ec2_instance is for EC2GatewayManager

CREATE TABLE ec2_instance
(
id VARCHAR(64) NOT NULL PRIMARY KEY,
dn VARCHAR(512) NOT NULL,
access_id VARCHAR(64) NOT NULL
);
