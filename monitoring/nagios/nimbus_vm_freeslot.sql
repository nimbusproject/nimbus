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
-- Query to count all non-private IPs remaining that are un-used
connect 'jdbc:derby:/tmp/WorkspacePersistenceDB';
SELECT count(ae.ipaddress) AS free 
FROM app.association_entries AS ae 
WHERE ae.association <> 'private' AND ae.used=0; 
disconnect;
exit;
