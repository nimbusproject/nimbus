/*
 * Copyright 1999-2008 University of Chicago
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

package org.globus.workspace.scheduler.defaults;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Hashtable;

import org.globus.workspace.Lager;
import org.globus.workspace.persistence.DerbyLoad;
import org.globus.workspace.persistence.PersistenceAdapter;
import org.globus.workspace.persistence.PersistenceAdapterImpl;
import org.globus.workspace.persistence.WorkspaceDatabaseException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class ResourcepoolUtilTest extends NimbusDatabaseTestCase {

    public static Lager lagerImpl = new Lager();
    public static DerbyLoad loader = new DerbyLoad();
    public static PersistenceAdapter adapter;
    public static Hashtable<String, ResourcepoolEntry> entries = new Hashtable<String, ResourcepoolEntry>();

    @BeforeClass
    public static void init() throws Exception {
        
        //objects creation
        loader.setDerbySystemHome(getDerbyDir().getAbsolutePath());
        loader.setDerbySystemProperty();
        adapter = new PersistenceAdapterImpl(getDataSource(), lagerImpl, loader);
        
        populateDB();
    }

    @AfterClass
    public static void cleanDB() throws WorkspaceDatabaseException {
        adapter.replaceResourcepools(new Hashtable<String, Resourcepool>());
    }

    
    private static void populateDB() throws WorkspaceDatabaseException {
        entries.put("hostA", new ResourcepoolEntry("testrp", "hostA", 1000, 1000, 0, "netC"));
        entries.put("hostB", new ResourcepoolEntry("testrp", "hostB", 1000, 100, 0, "netB"));
        entries.put("hostC", new ResourcepoolEntry("testrp", "hostC", 1000, 900, 0, "netA,netB"));
        entries.put("hostD", new ResourcepoolEntry("testrp", "hostD", 1000, 100, 0, "*"));
        entries.put("hostE", new ResourcepoolEntry("testrp", "hostE", 1000, 0, 0, "*"));
        entries.put("hostF", new ResourcepoolEntry("testrp", "hostF", 1000, 800, 0, "netA"));
        entries.put("hostG", new ResourcepoolEntry("testrp", "hostG", 1000, 900, 0, "netB"));
        entries.put("hostH", new ResourcepoolEntry("testrp", "hostH", 1000, 300, 0, "netA,netB,netC"));
        Resourcepool resourcepool = new Resourcepool();
        resourcepool.setEntries(entries);
        Hashtable<String, Resourcepool> resourcepools = new Hashtable<String, Resourcepool>();
        resourcepools.put("testrp", resourcepool);
        adapter.replaceResourcepools(resourcepools);
    }    
   
    /**
     * Test if getResourcepoolEntry and getResourcepoolEntryImproved return the same result (greedy)
     * @throws Exception
     */
    @Test
    public void compareMethodsGreedy() throws Exception{
        String hostNorm = ResourcepoolUtil.getResourcepoolEntry(200, new String[0], adapter, lagerImpl, 1, true, false);
        ResourcepoolUtil.retireMem(hostNorm, 200, adapter, lagerImpl.eventLog, lagerImpl.traceLog, 1);
        
        String hostImpr = ResourcepoolUtil.getResourcepoolEntry(200, new String[0], adapter, lagerImpl, 1, true, false);
        ResourcepoolUtil.retireMem(hostImpr, 200, adapter, lagerImpl.eventLog, lagerImpl.traceLog, 1);
        
        assertEquals(hostNorm, hostImpr);
        assertEquals("hostH", hostNorm);
    }

    
    /**
     * Test if getResourcepoolEntry and getResourcepoolEntryImproved return the same result (roundrobin)
     * @throws Exception
     */
    @Test
    public void compareMethodsRoundRobin() throws Exception{
        String hostNorm = ResourcepoolUtil.getResourcepoolEntry(200, new String[0], adapter, lagerImpl, 1, false, false);
        ResourcepoolUtil.retireMem(hostNorm, 200, adapter, lagerImpl.eventLog, lagerImpl.traceLog, 1);
        
        String hostImpr = ResourcepoolUtil.getResourcePoolEntryImproved(200, new String[0], adapter, lagerImpl, 1, false, false);
        ResourcepoolUtil.retireMem(hostImpr, 200, adapter, lagerImpl.eventLog, lagerImpl.traceLog, 1);
        
        assertEquals(hostNorm, hostImpr);
        assertEquals("hostA", hostNorm);
    }
    
    
    /**
     * Test if getResourcepoolEntry and getResourcepoolEntryImproved return the same result, 
     * with associations (greedy)
     * @throws Exception
     */
    @Test
    public void compareMethodsAssociationsGreedy() throws Exception{
        String[] neededAssociations = new String[1];
        neededAssociations[0] = "netA";
        
        String hostNorm = ResourcepoolUtil.getResourcepoolEntry(100, neededAssociations, adapter, lagerImpl, 1, true, false);
        ResourcepoolUtil.retireMem(hostNorm, 100, adapter, lagerImpl.eventLog, lagerImpl.traceLog, 1);
        
        String hostImpr = ResourcepoolUtil.getResourcePoolEntryImproved(100, neededAssociations, adapter, lagerImpl, 1, true, false);
        ResourcepoolUtil.retireMem(hostImpr, 100, adapter, lagerImpl.eventLog, lagerImpl.traceLog, 1);
        
        assertEquals(hostNorm, hostImpr);
        assertEquals("hostD", hostNorm);
    }  
    
    /**
     * Test if getResourcepoolEntry and getResourcepoolEntry return the same result, 
     * with associations (round robin)
     * @throws Exception
     */
    @Test
    public void compareMethodsAssociationsRoundRobin() throws Exception{
        String[] neededAssociations = new String[1];
        neededAssociations[0] = "netA";        
        
        String hostNorm = ResourcepoolUtil.getResourcepoolEntry(200, neededAssociations, adapter, lagerImpl, 1, false, false);
        ResourcepoolUtil.retireMem(hostNorm, 200, adapter, lagerImpl.eventLog, lagerImpl.traceLog, 1);
        
        String hostImpr = ResourcepoolUtil.getResourcePoolEntryImproved(200, neededAssociations, adapter, lagerImpl, 1, false, false);
        ResourcepoolUtil.retireMem(hostImpr, 200, adapter, lagerImpl.eventLog, lagerImpl.traceLog, 1);

        assertEquals(hostNorm, hostImpr);
        assertEquals("hostC", hostNorm);
    }   
    
    /**
     * Test if getResourcepoolEntry and getResourcepoolEntryImproved can return the same results, 
     * when there is more than one candidate (greedy)
     * In this case, the selected entry will be randomly chosen
     * @throws Exception
     */
    @Test
    public void compareMethodsRandomGreedy() throws Exception{
        String[] neededAssociations = new String[1];
        neededAssociations[0] = "netB";
        
        String hostNorm = ResourcepoolUtil.getResourcepoolEntry(100, neededAssociations, adapter, lagerImpl, 1, true, false);
        ResourcepoolUtil.retireMem(hostNorm, 100, adapter, lagerImpl.eventLog, lagerImpl.traceLog, 1);
        
        String hostImpr = ResourcepoolUtil.getResourcePoolEntryImproved(100, neededAssociations, adapter, lagerImpl, 1, true, false);
        ResourcepoolUtil.retireMem(hostImpr, 100, adapter, lagerImpl.eventLog, lagerImpl.traceLog, 1);
        
        assertTrue(hostNorm.equals("hostB") || hostNorm.equals("hostD"));
        assertTrue(hostImpr.equals("hostB") || hostImpr.equals("hostD"));
    }  
    
    /**
     * Test if getResourcepoolEntry and getResourcepoolEntryImproved can return the same results, 
     * when there is more than one candidate (round robin)
     * In this case, the selected entry will be randomly chosen
     * @throws Exception
     */
    @Test
    public void compareMethodsRandomRoundRobin() throws Exception{
        String[] neededAssociations = new String[1];
        neededAssociations[0] = "netB";        
        
        String hostNorm = ResourcepoolUtil.getResourcepoolEntry(200, neededAssociations, adapter, lagerImpl, 1, false, false);
        ResourcepoolUtil.retireMem(hostNorm, 100, adapter, lagerImpl.eventLog, lagerImpl.traceLog, 1);
        
        String hostImpr = ResourcepoolUtil.getResourcePoolEntryImproved(200, neededAssociations, adapter, lagerImpl, 1, false, false);
        ResourcepoolUtil.retireMem(hostImpr, 100, adapter, lagerImpl.eventLog, lagerImpl.traceLog, 1);
        
        assertTrue(hostNorm.equals("hostC") || hostNorm.equals("hostG"));
        assertTrue(hostImpr.equals("hostC") || hostImpr.equals("hostG"));
    }    

}
