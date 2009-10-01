/*
 * Copyright 2008 University of Victoria
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

-- Version 1.0
-- Author: Michael Paterson (mhp@uvic.ca)
-- Query for Workspace Information for all workspaces
connect 'jdbc:derby:/tmp/WorkspacePersistenceDB';
SELECT p.vmid, r.creator_dn, v.node, (r.start_time + d.min_duration*1000) as shutdown_time, r.state
FROM app.vm_partitions as p, app.vms as v, app.vm_deployment as d, app.resources as r
WHERE p.vmid = v.id AND v.id = d.vmid AND d.vmid = r.id
ORDER BY r.creator_dn;
disconnect;
exit;
