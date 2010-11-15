package org.globus.workspace.spotinstances;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.LinkedList;

import org.globus.workspace.async.AsyncRequest;
import org.globus.workspace.async.pricingmodel.MaximizeProfitPricingModel;
import org.globus.workspace.service.binding.vm.VirtualMachine;
import org.junit.Test;

public class MaximizeProfitPricingModelTest {

    private MaximizeProfitPricingModel pricingModel = new MaximizeProfitPricingModel();
    
    @Test
    public void testGetNextPriceNoDemand() {
        
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
    public void testGetNextPriceCase1() {

        LinkedList<AsyncRequest> requests = new LinkedList<AsyncRequest>();
        requests.add(new AsyncRequest("a", 2.0, getBindings(3)));
        
        Double nextPrice = pricingModel.getNextPrice(5, requests, null);
        assertEquals(new Double(2.0), nextPrice);
        assertTrue(PricingModelTestUtils.checkPricingModelConstraints(pricingModel.getMinPrice(), nextPrice, 5, requests));
        
    }  
    
    @Test
    public void testGetNextPriceCase2() {

        LinkedList<AsyncRequest> requests = new LinkedList<AsyncRequest>();
        requests.add(new AsyncRequest("a", 2.0, getBindings(10)));
        
        Double nextPrice = pricingModel.getNextPrice(5, requests, null);
        assertEquals(new Double(2.0), nextPrice);
        assertTrue(PricingModelTestUtils.checkPricingModelConstraints(pricingModel.getMinPrice(), nextPrice, 5, requests));
    }
    
    @Test
    public void testGetNextPriceCase3() {

        LinkedList<AsyncRequest> requests = new LinkedList<AsyncRequest>();
        requests.add(new AsyncRequest("a", 2.0, getBindings(10)));
        requests.add(new AsyncRequest("b", 1.0, getBindings(5)));
        
        
        Double nextPrice = pricingModel.getNextPrice(5, requests, null);
        assertEquals(new Double(2.0), nextPrice);
        assertTrue(PricingModelTestUtils.checkPricingModelConstraints(pricingModel.getMinPrice(), nextPrice, 5, requests));

    } 
    
    @Test
    public void testGetNextPriceCase4() {

        LinkedList<AsyncRequest> requests = new LinkedList<AsyncRequest>();
        requests.add(new AsyncRequest("a", 2.0, getBindings(5)));
        requests.add(new AsyncRequest("a", 1.0, getBindings(5)));
        requests.add(new AsyncRequest("a", 1.6, getBindings(5)));
        
        Double nextPrice = pricingModel.getNextPrice(15, requests, null);
        assertEquals(new Double(1.6), nextPrice);
        assertTrue(PricingModelTestUtils.checkPricingModelConstraints(pricingModel.getMinPrice(), nextPrice, 15, requests));
    }   
    
    @Test
    public void testGetNextPriceCase5() {

        LinkedList<AsyncRequest> requests = new LinkedList<AsyncRequest>();
        requests.add(new AsyncRequest("a", 2.0, getBindings(5)));
        requests.add(new AsyncRequest("b", 1.0, getBindings(5)));
        requests.add(new AsyncRequest("c", 1.4, getBindings(5)));
        
        Double nextPrice = pricingModel.getNextPrice(15, requests, null);
        assertEquals(new Double(1.0), nextPrice);
        assertTrue(PricingModelTestUtils.checkPricingModelConstraints(pricingModel.getMinPrice(), nextPrice, 15, requests));        
    }    
    
    /*
     * This case shows the biggest problem of this model:
     * There are available slots to satisfy all requests, but the highest bid
     * has such a high profit that it's more advantageous (according to this model)
     * to take just one request (not very interesting for science clouds though)
     * 
     */
    @Test
    public void testGetNextPriceCase6() {

        LinkedList<AsyncRequest> requests = new LinkedList<AsyncRequest>();
        requests.add(new AsyncRequest("a", 200.0, getBindings(1)));
        requests.add(new AsyncRequest("b", 1.0, getBindings(25)));
        requests.add(new AsyncRequest("c", 2.0, getBindings(25)));
        requests.add(new AsyncRequest("d", 3.0, getBindings(25)));
        requests.add(new AsyncRequest("e", 4.0, getBindings(25)));
        
        Double nextPrice = pricingModel.getNextPrice(200, requests, null);
        assertEquals(new Double(200.0), nextPrice);
        assertTrue(PricingModelTestUtils.checkPricingModelConstraints(pricingModel.getMinPrice(), nextPrice, 200, requests));        
    }    

    public VirtualMachine[] getBindings(int number){
        return new VirtualMachine[number];
    }
}
