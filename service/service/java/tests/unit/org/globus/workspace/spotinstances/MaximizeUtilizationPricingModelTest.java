package org.globus.workspace.spotinstances;

import static org.junit.Assert.*;

import java.util.LinkedList;

import org.globus.workspace.service.binding.vm.VirtualMachine;
import org.globus.workspace.spotinstances.MaximizeUtilizationPricingModel;
import org.globus.workspace.spotinstances.AsyncRequest;
import org.junit.Test;

public class MaximizeUtilizationPricingModelTest { 
   
    
    @Test
    public void testGetNextPriceNoDemand() {
        
        MaximizeUtilizationPricingModel pricingModel = new MaximizeUtilizationPricingModel();
        
        LinkedList<AsyncRequest> requests = new LinkedList<AsyncRequest>();
        
        Double nextPrice = pricingModel.getNextPrice(0, requests, null);
        assertEquals(pricingModel.getMinPrice(), nextPrice);
        assertTrue(PricingModelTestUtils.checkPricingModelConstraints(pricingModel.getMinPrice(), nextPrice, 0, requests));
        
        
        nextPrice = pricingModel.getNextPrice(5, requests, null);
        assertEquals(pricingModel.getMinPrice(), nextPrice);
        assertTrue(PricingModelTestUtils.checkPricingModelConstraints(pricingModel.getMinPrice(), nextPrice, 5, requests));
        
        nextPrice = pricingModel.getNextPrice(2500, requests, null);
        assertEquals(pricingModel.getMinPrice(), nextPrice);
        assertTrue(PricingModelTestUtils.checkPricingModelConstraints(pricingModel.getMinPrice(), nextPrice, 2500, requests));
    }
    
    @Test
    public void testGetNextPriceNoOffer() {
       
        MaximizeUtilizationPricingModel pricingModel = new MaximizeUtilizationPricingModel();

        
        //case 1
        LinkedList<AsyncRequest> requests = new LinkedList<AsyncRequest>();
        requests.add(new AsyncRequest("a", 2.0, getBindings(1)));
     
        Double nextPrice = pricingModel.getNextPrice(0, requests, null);
        assertEquals(new Double(2.0+0.1), nextPrice);
        assertTrue(PricingModelTestUtils.checkPricingModelConstraints(pricingModel.getMinPrice(), nextPrice, 0, requests));
        
        //case 2
        requests = new LinkedList<AsyncRequest>();
        requests.add(new AsyncRequest("a", 4.0, getBindings(1)));
        requests.add(new AsyncRequest("b", 2.0, getBindings(4)));  
        requests.add(new AsyncRequest("c", 3.5, getBindings(4)));     
        
        nextPrice = pricingModel.getNextPrice(0, requests, null);
        assertEquals(new Double(4.0+0.1), nextPrice);
        assertTrue(PricingModelTestUtils.checkPricingModelConstraints(pricingModel.getMinPrice(), nextPrice, 0, requests));
    }    
    
    @Test
    public void testGetNextPriceCase1a() {

        MaximizeUtilizationPricingModel pricingModel = new MaximizeUtilizationPricingModel(true);

        
        LinkedList<AsyncRequest> requests = new LinkedList<AsyncRequest>();
        requests.add(new AsyncRequest("a", 2.0, getBindings(3)));
        
        Double nextPrice = pricingModel.getNextPrice(5, requests, null);
        assertEquals(new Double(pricingModel.getMinPrice()), nextPrice);
        assertTrue(PricingModelTestUtils.checkPricingModelConstraints(pricingModel.getMinPrice(), nextPrice, 5, requests));
    }
    
    @Test
    public void testGetNextPriceCase1b() {

        MaximizeUtilizationPricingModel pricingModel = new MaximizeUtilizationPricingModel(false);

        
        LinkedList<AsyncRequest> requests = new LinkedList<AsyncRequest>();
        requests.add(new AsyncRequest("a", 2.0, getBindings(3)));
        
        Double nextPrice = pricingModel.getNextPrice(5, requests, null);
        assertEquals(new Double(2.0), nextPrice);
        assertTrue(PricingModelTestUtils.checkPricingModelConstraints(pricingModel.getMinPrice(), nextPrice, 5, requests));
    }    
    
    @Test
    public void testGetNextPriceCase2a() {

        MaximizeUtilizationPricingModel pricingModel = new MaximizeUtilizationPricingModel(true);

        
        LinkedList<AsyncRequest> requests = new LinkedList<AsyncRequest>();
        requests.add(new AsyncRequest("a", 2.0, getBindings(10)));
        
        Double nextPrice = pricingModel.getNextPrice(5, requests, null);
        assertEquals(new Double(2.0), nextPrice);
        assertTrue(PricingModelTestUtils.checkPricingModelConstraints(pricingModel.getMinPrice(), nextPrice, 5, requests));
    }
    
    @Test
    public void testGetNextPriceCase2b() {

        MaximizeUtilizationPricingModel pricingModel = new MaximizeUtilizationPricingModel(false);

        
        LinkedList<AsyncRequest> requests = new LinkedList<AsyncRequest>();
        requests.add(new AsyncRequest("a", 2.0, getBindings(10)));
        
        Double nextPrice = pricingModel.getNextPrice(5, requests, null);
        assertEquals(new Double(2.0), nextPrice);
        assertTrue(PricingModelTestUtils.checkPricingModelConstraints(pricingModel.getMinPrice(), nextPrice, 5, requests));        
    }    
    
    @Test
    public void testGetNextPriceCase3a() {

        MaximizeUtilizationPricingModel pricingModel = new MaximizeUtilizationPricingModel(true);

        
        LinkedList<AsyncRequest> requests = new LinkedList<AsyncRequest>();
        requests.add(new AsyncRequest("a", 2.0, getBindings(10)));
        requests.add(new AsyncRequest("b", 1.0, getBindings(5)));        
        
        Double nextPrice = pricingModel.getNextPrice(5, requests, null);
        assertEquals(new Double(2.0), nextPrice);
        assertTrue(PricingModelTestUtils.checkPricingModelConstraints(pricingModel.getMinPrice(), nextPrice, 5, requests));        
    } 
    
    @Test
    public void testGetNextPriceCase3b() {

        MaximizeUtilizationPricingModel pricingModel = new MaximizeUtilizationPricingModel(false);

        
        LinkedList<AsyncRequest> requests = new LinkedList<AsyncRequest>();
        requests.add(new AsyncRequest("a", 2.0, getBindings(10)));
        requests.add(new AsyncRequest("b", 1.0, getBindings(5)));        
        
        Double nextPrice = pricingModel.getNextPrice(5, requests, null);
        assertEquals(new Double(2.0), nextPrice);
        assertTrue(PricingModelTestUtils.checkPricingModelConstraints(pricingModel.getMinPrice(), nextPrice, 5, requests));
    }     
    
    @Test
    public void testGetNextPriceCase4a() {

        MaximizeUtilizationPricingModel pricingModel = new MaximizeUtilizationPricingModel(true);
        
        LinkedList<AsyncRequest> requests = new LinkedList<AsyncRequest>();
        requests.add(new AsyncRequest("a", 2.0, getBindings(5)));
        requests.add(new AsyncRequest("b", 1.0, getBindings(5)));
        requests.add(new AsyncRequest("c", 1.6, getBindings(5)));
        
        Double nextPrice = pricingModel.getNextPrice(15, requests, null);
        assertEquals(new Double(1.0), nextPrice);
        assertTrue(PricingModelTestUtils.checkPricingModelConstraints(pricingModel.getMinPrice(), nextPrice, 15, requests));
    }
    
    @Test
    public void testGetNextPriceCase4b() {

        MaximizeUtilizationPricingModel pricingModel = new MaximizeUtilizationPricingModel(false);
        
        LinkedList<AsyncRequest> requests = new LinkedList<AsyncRequest>();
        requests.add(new AsyncRequest("a", 2.0, getBindings(5)));
        requests.add(new AsyncRequest("b", 1.0, getBindings(5)));
        requests.add(new AsyncRequest("c", 1.6, getBindings(5)));
        
        Double nextPrice = pricingModel.getNextPrice(15, requests, null);
        assertEquals(new Double(1.0), nextPrice);
        assertTrue(PricingModelTestUtils.checkPricingModelConstraints(pricingModel.getMinPrice(), nextPrice, 15, requests));        
    }     
    
    @Test
    public void testGetNextPriceCase5a() {

        MaximizeUtilizationPricingModel pricingModel = new MaximizeUtilizationPricingModel(true);
        
        LinkedList<AsyncRequest> requests = new LinkedList<AsyncRequest>();
        requests.add(new AsyncRequest("a", 2.0, getBindings(5)));
        requests.add(new AsyncRequest("b", 1.0, getBindings(5)));
        requests.add(new AsyncRequest("c", 1.4, getBindings(5)));
        
        Double nextPrice = pricingModel.getNextPrice(15, requests, null);
        assertEquals(new Double(1.0), nextPrice);
        assertTrue(PricingModelTestUtils.checkPricingModelConstraints(pricingModel.getMinPrice(), nextPrice, 15, requests));        
    } 
    
    @Test
    public void testGetNextPriceCase5b() {

        MaximizeUtilizationPricingModel pricingModel = new MaximizeUtilizationPricingModel(false);
        
        LinkedList<AsyncRequest> requests = new LinkedList<AsyncRequest>();
        requests.add(new AsyncRequest("a", 2.0, getBindings(5)));
        requests.add(new AsyncRequest("b", 1.0, getBindings(5)));
        requests.add(new AsyncRequest("c", 1.4, getBindings(5)));
        
        Double nextPrice = pricingModel.getNextPrice(15, requests, null);
        assertEquals(new Double(1.0), nextPrice);
        assertTrue(PricingModelTestUtils.checkPricingModelConstraints(pricingModel.getMinPrice(), nextPrice, 15, requests));                
    }        
    
    @Test
    public void testGetNextPriceCase6a() {

        MaximizeUtilizationPricingModel pricingModel = new MaximizeUtilizationPricingModel(true);
        
        LinkedList<AsyncRequest> requests = new LinkedList<AsyncRequest>();
        requests.add(new AsyncRequest("a", 200.0, getBindings(1)));
        requests.add(new AsyncRequest("b", 1.0, getBindings(25)));
        requests.add(new AsyncRequest("c", 2.0, getBindings(25)));
        requests.add(new AsyncRequest("d", 3.0, getBindings(25)));
        requests.add(new AsyncRequest("e", 4.0, getBindings(25)));
        
        Double nextPrice = pricingModel.getNextPrice(200, requests, null);
        assertEquals(new Double(pricingModel.getMinPrice()), nextPrice);
        assertTrue(PricingModelTestUtils.checkPricingModelConstraints(pricingModel.getMinPrice(), nextPrice, 200, requests));        
    }  
    
    @Test
    public void testGetNextPriceCase6b() {

        MaximizeUtilizationPricingModel pricingModel = new MaximizeUtilizationPricingModel(false);
        
        LinkedList<AsyncRequest> requests = new LinkedList<AsyncRequest>();
        requests.add(new AsyncRequest("a", 200.0, getBindings(1)));
        requests.add(new AsyncRequest("b", 1.0, getBindings(25)));
        requests.add(new AsyncRequest("c", 2.0, getBindings(25)));
        requests.add(new AsyncRequest("d", 3.0, getBindings(25)));
        requests.add(new AsyncRequest("e", 4.0, getBindings(25)));
        
        Double nextPrice = pricingModel.getNextPrice(200, requests, null);
        assertEquals(new Double(1.0), nextPrice);
        assertTrue(PricingModelTestUtils.checkPricingModelConstraints(pricingModel.getMinPrice(), nextPrice, 200, requests));        
    }        

    public VirtualMachine[] getBindings(int number){
        return new VirtualMachine[number];
    }
       
}
