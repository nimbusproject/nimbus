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

import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

import org.globus.workspace.Lager;
import org.globus.workspace.persistence.DerbyLoad;
import org.globus.workspace.persistence.PersistenceAdapter;
import org.globus.workspace.persistence.PersistenceAdapterImpl;
import org.globus.workspace.persistence.WorkspaceDatabaseException;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

public class ResourcepoolUtilProfiling extends NimbusDatabaseTestCase {

    private static final int NUM_EXECUTIONS = 100;
    private static final int NUM_ENTRIES = 100;
    private static final int REQUESTED_MEM = 40;
    public static Lager lagerImpl = new Lager();
    public static DerbyLoad loader = new DerbyLoad();
    public static Hashtable<String, ResourcepoolEntry> entries1 = new Hashtable<String, ResourcepoolEntry>();
    public static Hashtable<String, ResourcepoolEntry> entries2 = new Hashtable<String, ResourcepoolEntry>();
    public static Hashtable<String, ResourcepoolEntry> entries3 = new Hashtable<String, ResourcepoolEntry>();
    public static PersistenceAdapter adapter;

    @BeforeClass
    public static void init() throws Exception {
        
        //dataset generation
        for (int i = 0; i < NUM_ENTRIES; i++) {
            String hostname = "host-" + i;
            int mem1 = (int)(Math.random()*NUM_ENTRIES);
            int mem2 = (int)(Math.random()*NUM_ENTRIES);
            
            //creating 3 entries' tables because getResourcePoolEntry modifies cached objects
            entries1.put(hostname, new ResourcepoolEntry("testrp1", hostname, Math.max(mem1, mem2), Math.min(mem1, mem2), 0, ""));
            entries2.put(hostname, new ResourcepoolEntry("testrp1", hostname, Math.max(mem1, mem2), Math.min(mem1, mem2), 0, "")); 
            entries3.put(hostname, new ResourcepoolEntry("testrp1", hostname, Math.max(mem1, mem2), Math.min(mem1, mem2), 0, ""));
        }
        
        //objects creation
        loader.setDerbySystemHome(getDerbyDir().getAbsolutePath());
        loader.setDerbySystemProperty();
        adapter = new PersistenceAdapterImpl(getDataSource(), lagerImpl, loader);
    }    
   
    

    @Test
    public void compareMethods1() throws Exception{
            
        populateDB(entries1);
        
        double normalTime = profileGetResourcePoolEntryNormal(REQUESTED_MEM, NUM_EXECUTIONS);
        
        cleanDB();
        
        populateDB(entries2);
        
        double improvedTime = profileGetResourcePoolEntryImproved(REQUESTED_MEM, NUM_EXECUTIONS);
        
        System.out.println("Sequence: Normal->Improved");
        System.out.println("Average execution time for normal method: " + normalTime + "ns");
        System.out.println("Average execution time for improved method: " + improvedTime + "ns");
        System.out.println("improved/normal ratio: " + (improvedTime/normalTime)*100 + "%");            
    }
    
    @Test
    public void compareMethods2() throws Exception{
            
        populateDB(entries2);
        
        double improvedTime = profileGetResourcePoolEntryImproved(REQUESTED_MEM, NUM_EXECUTIONS);
        
        cleanDB();
        
        populateDB(entries3);
        
        double normalTime = profileGetResourcePoolEntryNormal(REQUESTED_MEM, NUM_EXECUTIONS);
        
        System.out.println("Sequence: Improved->Normal");
        System.out.println("Average execution time for normal method: " + normalTime + "ns");
        System.out.println("Average execution time for improved method: " + improvedTime + "ns");
        System.out.println("improved/normal ratio: " + (improvedTime/normalTime)*100 + "%");            
    }    
    
    
    public double profileGetResourcePoolEntryNormal(int requestedMem, int executions) throws Exception {    
        
        long init, end;

        List<Long> times = new LinkedList<Long>();

        for(int i=0; i<executions; i++){
            init = System.nanoTime();
            String hostname = ResourcepoolUtil.getResourcepoolEntry(requestedMem, new String[0], adapter, lagerImpl, 1, true, false);
            end = System.nanoTime();
            //System.out.println("norm: " + hostname);
            ResourcepoolUtil.retireMem(hostname, requestedMem, adapter, lagerImpl.eventLog, lagerImpl.traceLog, 1, false);
            times.add(end-init);
        }

        long total = 0;
        for (Long time : times) {
            total += time;
        }
        
        return total/executions;
    }     
    
    public double profileGetResourcePoolEntryImproved(int requestedMem, int executions) throws Exception {    

        long init, end;

        List<Long> timings = new LinkedList<Long>();

        for(int i=0; i<executions; i++){
            init = System.nanoTime();
            String hostname = ResourcepoolUtil.getResourcePoolEntryImproved(requestedMem, new String[0], adapter, lagerImpl, 1, true, false);
            end = System.nanoTime();
            //System.out.println("impr: " + hostname);
            ResourcepoolUtil.retireMem(hostname, requestedMem, adapter, lagerImpl.eventLog, lagerImpl.traceLog, 1, false);
            timings.add(end-init);
        }

        long total = 0;
        for (Long timing : timings) {
            total += timing;
        }
        
        return total/executions;
    }
    
    private void populateDB(Hashtable<String, ResourcepoolEntry> entries) throws WorkspaceDatabaseException {
        Resourcepool resourcepool = new Resourcepool();
        resourcepool.setEntries(entries);
        Hashtable<String, Resourcepool> resourcepools = new Hashtable<String, Resourcepool>();
        resourcepools.put("testrp1", resourcepool);
        adapter.replaceResourcepools(resourcepools);
    }


    @After
    public void cleanDB() throws WorkspaceDatabaseException {
        adapter.replaceResourcepools(new Hashtable<String, Resourcepool>());
    }

}
