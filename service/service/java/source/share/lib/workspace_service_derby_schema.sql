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

-- connect 'jdbc:derby:workspace_service/WorkspacePersistenceDB;create=true';

--
-- Persistence for WorkspaceResource:

CREATE TABLE resources
(
id INT NOT NULL PRIMARY KEY,
name VARCHAR(100) NOT NULL,
state SMALLINT NOT NULL,
target_state SMALLINT NOT NULL,
term_time BIGINT NOT NULL,
ops_enabled SMALLINT NOT NULL,
creator_dn VARCHAR(512),
start_time BIGINT NOT NULL,
vmm_access_ok SMALLINT NOT NULL,
ensembleid CHAR(36),
groupid CHAR(36),
groupsize INT,
last_in_group SMALLINT,
launch_index INT,
error_fault BLOB
);

--
-- Persistence for GroupResource/CoschedResource:

CREATE TABLE groupresources
(
groupid CHAR(36) NOT NULL PRIMARY KEY,
creator_dn VARCHAR(512)
);


--
-- Persistence for virtual machines:

CREATE TABLE vms
(
id INT NOT NULL PRIMARY KEY,
name VARCHAR(128) NOT NULL,
node VARCHAR(128),
prop_required SMALLINT NOT NULL,
unprop_required SMALLINT NOT NULL,
network VARCHAR(1024),
kernel_parameters VARCHAR(128),
vmm VARCHAR(32),
vmm_version VARCHAR(32),
assocs_needed VARCHAR(256),
md_user_data VARCHAR(30720),
credential_name VARCHAR(128)
);

--
-- VM partitions

CREATE TABLE vm_partitions
(
vmid INT NOT NULL,
image VARCHAR(4096) NOT NULL,
imagemount VARCHAR(128) NOT NULL,
readwrite SMALLINT NOT NULL,
rootdisk SMALLINT NOT NULL,
blankspace INT NOT NULL,
prop_required SMALLINT NOT NULL,
unprop_required SMALLINT NOT NULL,
alternate_unprop VARCHAR(128)
);

--
-- Persistence for vm deployment-time data:

CREATE TABLE vm_deployment
(
vmid INT NOT NULL,
requested_state SMALLINT,
requested_shutdown SMALLINT,
min_duration INT,
ind_physmem INT,
ind_physcpu INT
);

--
-- Persistence for association tracking:

CREATE TABLE associations
(
association VARCHAR(128) NOT NULL PRIMARY KEY,
dns VARCHAR(32),
file_time BIGINT NOT NULL
);

CREATE TABLE association_entries
(
association VARCHAR(128) NOT NULL,
ipaddress VARCHAR(32) NOT NULL,
mac VARCHAR(32),
hostname VARCHAR(128),
gateway VARCHAR(32),
broadcast VARCHAR(32),
subnetmask VARCHAR(32),
used SMALLINT,
PRIMARY KEY(association,ipaddress)
);

--
-- Persistence for file copy tasks

CREATE TABLE file_copy
(
vmid INT NOT NULL,
sourcepath VARCHAR(36) NOT NULL,
destpath VARCHAR(512),
on_image SMALLINT NOT NULL
);

--
-- For DefaultSchedulerAdapter

CREATE TABLE default_scheduler_current_tasks
(
id INT NOT NULL,
shutdown_time BIGINT NOT NULL,
shutdown SMALLINT NOT NULL
);

CREATE TABLE default_scheduler_workspid
(
id INT NOT NULL PRIMARY KEY DEFAULT 0
);

CREATE TABLE default_scheduler_pending_ensemb
(
coschedid CHAR(36) NOT NULL,
groupid CHAR(36),
id INT,
min_duration INT NOT NULL,
ind_physmem INT NOT NULL,
assocs_needed VARCHAR(256)
);

CREATE TABLE default_scheduler_done_ensemb
(
coschedid CHAR(36) NOT NULL
);

-- using REAL for memory attributs to allow
-- real division operations in ORDER BY statements

CREATE TABLE resourcepool_entries
(
resourcepool VARCHAR(128) NOT NULL,
hostname VARCHAR(128) NOT NULL PRIMARY KEY,
associations VARCHAR(512) NOT NULL,
maximum_memory REAL,
available_memory REAL,
active SMALLINT NOT NULL DEFAULT 1
);

--
-- Pilot:

CREATE TABLE pilot_slots
(
id CHAR(36) NOT NULL,
vmid INT NOT NULL,
pending SMALLINT NOT NULL,
terminal SMALLINT NOT NULL,
lrmhandle VARCHAR(128) NOT NULL,
duration BIGINT NOT NULL,
partofgroup SMALLINT NOT NULL,
pendingremove SMALLINT NOT NULL,
nodename VARCHAR(128)
);

CREATE TABLE pilot_groups
(
groupid CHAR(36) NOT NULL,
vmid INT NOT NULL
);

CREATE TABLE pilot_notification_position
(
position BIGINT
);

--
-- Other:

CREATE TABLE counter
(
id SMALLINT,
pending INT
);

CREATE TABLE notification_position
(
position BIGINT
);
