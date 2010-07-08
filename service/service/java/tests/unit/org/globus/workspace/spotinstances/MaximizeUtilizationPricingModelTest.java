package org.globus.workspace.spotinstances;

import static org.junit.Assert.*;

import java.util.LinkedList;

import org.junit.Test;

public class MaximizeUtilizationPricingModelTest { 
   
    
    @Test
    public void testGetNextPriceNoDemand() {
        
        MaximizeUtilizationPricingModel pricingModel = new MaximizeUtilizationPricingModel();
        
        LinkedList<SIRequest> requests = new LinkedList<SIRequest>();
        
        Double nextPrice = pricingModel.getNextPrice(0, requests, null);
        assertEquals(PricingModelConstants.MINIMUM_PRICE, nextPrice);
        assertTrue(PricingModelTestUtils.checkPricingModelConstraints(nextPrice, 0, requests));
        
        
        nextPrice = pricingModel.getNextPrice(5, requests, null);
        assertEquals(PricingModelConstants.MINIMUM_PRICE, nextPrice);
        assertTrue(PricingModelTestUtils.checkPricingModelConstraints(nextPrice, 5, requests));
        
        nextPrice = pricingModel.getNextPrice(2500, requests, null);
        assertEquals(PricingModelConstants.MINIMUM_PRICE, nextPrice);
        assertTrue(PricingModelTestUtils.checkPricingModelConstraints(nextPrice, 2500, requests));
    }
    
    @Test
    public void testGetNextPriceNoOffer() {
       
        MaximizeUtilizationPricingModel pricingModel = new MaximizeUtilizationPricingModel();

        
        //case 1
        LinkedList<SIRequest> requests = new LinkedList<SIRequest>();
        requests.add(new SIRequest("a", 2.0, 1));
     
        Double nextPrice = pricingModel.getNextPrice(0, requests, null);
        assertEquals(new Double(2.0+0.1), nextPrice);
        assertTrue(PricingModelTestUtils.checkPricingModelConstraints(nextPrice, 0, requests));
        
        //case 2
        requests = new LinkedList<SIRequest>();
        requests.add(new SIRequest("a", 4.0, 1));
        requests.add(new SIRequest("b", 2.0, 4));  
        requests.add(new SIRequest("c", 3.5, 4));     
        
        nextPrice = pricingModel.getNextPrice(0, requests, null);
        assertEquals(new Double(4.0+0.1), nextPrice);
        assertTrue(PricingModelTestUtils.checkPricingModelConstraints(nextPrice, 0, requests));
    }    
    
    @Test
    public void testGetNextPriceCase1a() {

        MaximizeUtilizationPricingModel pricingModel = new MaximizeUtilizationPricingModel(true);

        
        LinkedList<SIRequest> requests = new LinkedList<SIRequest>();
        requests.add(new SIRequest("a", 2.0, 3));
        
        Double nextPrice = pricingModel.getNextPrice(5, requests, null);
        assertEquals(new Double(PricingModelConstants.MINIMUM_PRICE), nextPrice);
        assertTrue(PricingModelTestUtils.checkPricingModelConstraints(nextPrice, 5, requests));
    }
    
    @Test
    public void testGetNextPriceCase1b() {

        MaximizeUtilizationPricingModel pricingModel = new MaximizeUtilizationPricingModel(false);

        
        LinkedList<SIRequest> requests = new LinkedList<SIRequest>();
        requests.add(new SIRequest("a", 2.0, 3));
        
        Double nextPrice = pricingModel.getNextPrice(5, requests, null);
        assertEquals(new Double(2.0), nextPrice);
        assertTrue(PricingModelTestUtils.checkPricingModelConstraints(nextPrice, 5, requests));
    }    
    
    @Test
    public void testGetNextPriceCase2a() {

        MaximizeUtilizationPricingModel pricingModel = new MaximizeUtilizationPricingModel(true);

        
        LinkedList<SIRequest> requests = new LinkedList<SIRequest>();
        requests.add(new SIRequest("a", 2.0, 10));
        
        Double nextPrice = pricingModel.getNextPrice(5, requests, null);
        assertEquals(new Double(2.0), nextPrice);
        assertTrue(PricingModelTestUtils.checkPricingModelConstraints(nextPrice, 5, requests));
    }
    
    @Test
    public void testGetNextPriceCase2b() {

        MaximizeUtilizationPricingModel pricingModel = new MaximizeUtilizationPricingModel(false);

        
        LinkedList<SIRequest> requests = new LinkedList<SIRequest>();
        requests.add(new SIRequest("a", 2.0, 10));
        
        Double nextPrice = pricingModel.getNextPrice(5, requests, null);
        assertEquals(new Double(2.0), nextPrice);
        assertTrue(PricingModelTestUtils.checkPricingModelConstraints(nextPrice, 5, requests));        
    }    
    
    @Test
    public void testGetNextPriceCase3a() {

        MaximizeUtilizationPricingModel pricingModel = new MaximizeUtilizationPricingModel(true);

        
        LinkedList<SIRequest> requests = new LinkedList<SIRequest>();
        requests.add(new SIRequest("a", 2.0, 10));
        requests.add(new SIRequest("b", 1.0, 5));        
        
        Double nextPrice = pricingModel.getNextPrice(5, requests, null);
        assertEquals(new Double(2.0), nextPrice);
        assertTrue(PricingModelTestUtils.checkPricingModelConstraints(nextPrice, 5, requests));        
    } 
    
    @Test
    public void testGetNextPriceCase3b() {

        MaximizeUtilizationPricingModel pricingModel = new MaximizeUtilizationPricingModel(false);

        
        LinkedList<SIRequest> requests = new LinkedList<SIRequest>();
        requests.add(new SIRequest("a", 2.0, 10));
        requests.add(new SIRequest("b", 1.0, 5));        
        
        Double nextPrice = pricingModel.getNextPrice(5, requests, null);
        assertEquals(new Double(2.0), nextPrice);
        assertTrue(PricingModelTestUtils.checkPricingModelConstraints(nextPrice, 5, requests));
    }     
    
    @Test
    public void testGetNextPriceCase4a() {

        MaximizeUtilizationPricingModel pricingModel = new MaximizeUtilizationPricingModel(true);
        
        LinkedList<SIRequest> requests = new LinkedList<SIRequest>();
        requests.add(new SIRequest("a", 2.0, 5));
        requests.add(new SIRequest("b", 1.0, 5));
        requests.add(new SIRequest("c", 1.6, 5));
        
        Double nextPrice = pricingModel.getNextPrice(15, requests, null);
        assertEquals(new Double(1.0), nextPrice);
        assertTrue(PricingModelTestUtils.checkPricingModelConstraints(nextPrice, 15, requests));
    }
    
    @Test
    public void testGetNextPriceCase4b() {

        MaximizeUtilizationPricingModel pricingModel = new MaximizeUtilizationPricingModel(false);
        
        LinkedList<SIRequest> requests = new LinkedList<SIRequest>();
        requests.add(new SIRequest("a", 2.0, 5));
        requests.add(new SIRequest("b", 1.0, 5));
        requests.add(new SIRequest("c", 1.6, 5));
        
        Double nextPrice = pricingModel.getNextPrice(15, requests, null);
        assertEquals(new Double(1.0), nextPrice);
        assertTrue(PricingModelTestUtils.checkPricingModelConstraints(nextPrice, 15, requests));        
    }     
    
    @Test
    public void testGetNextPriceCase5a() {

        MaximizeUtilizationPricingModel pricingModel = new MaximizeUtilizationPricingModel(true);
        
        LinkedList<SIRequest> requests = new LinkedList<SIRequest>();
        requests.add(new SIRequest("a", 2.0, 5));
        requests.add(new SIRequest("b", 1.0, 5));
        requests.add(new SIRequest("c", 1.4, 5));
        
        Double nextPrice = pricingModel.getNextPrice(15, requests, null);
        assertEquals(new Double(1.0), nextPrice);
        assertTrue(PricingModelTestUtils.checkPricingModelConstraints(nextPrice, 15, requests));        
    } 
    
    @Test
    public void testGetNextPriceCase5b() {

        MaximizeUtilizationPricingModel pricingModel = new MaximizeUtilizationPricingModel(false);
        
        LinkedList<SIRequest> requests = new LinkedList<SIRequest>();
        requests.add(new SIRequest("a", 2.0, 5));
        requests.add(new SIRequest("b", 1.0, 5));
        requests.add(new SIRequest("c", 1.4, 5));
        
        Double nextPrice = pricingModel.getNextPrice(15, requests, null);
        assertEquals(new Double(1.0), nextPrice);
        assertTrue(PricingModelTestUtils.checkPricingModelConstraints(nextPrice, 15, requests));                
    }        
    
    @Test
    public void testGetNextPriceCase6a() {

        MaximizeUtilizationPricingModel pricingModel = new MaximizeUtilizationPricingModel(true);
        
        LinkedList<SIRequest> requests = new LinkedList<SIRequest>();
        requests.add(new SIRequest("a", 200.0, 1));
        requests.add(new SIRequest("b", 1.0, 25));
        requests.add(new SIRequest("c", 2.0, 25));
        requests.add(new SIRequest("d", 3.0, 25));
        requests.add(new SIRequest("e", 4.0, 25));
        
        Double nextPrice = pricingModel.getNextPrice(200, requests, null);
        assertEquals(new Double(PricingModelConstants.MINIMUM_PRICE), nextPrice);
        assertTrue(PricingModelTestUtils.checkPricingModelConstraints(nextPrice, 200, requests));        
    }  
    
    @Test
    public void testGetNextPriceCase6b() {

        MaximizeUtilizationPricingModel pricingModel = new MaximizeUtilizationPricingModel(false);
        
        LinkedList<SIRequest> requests = new LinkedList<SIRequest>();
        requests.add(new SIRequest("a", 200.0, 1));
        requests.add(new SIRequest("b", 1.0, 25));
        requests.add(new SIRequest("c", 2.0, 25));
        requests.add(new SIRequest("d", 3.0, 25));
        requests.add(new SIRequest("e", 4.0, 25));
        
        Double nextPrice = pricingModel.getNextPrice(200, requests, null);
        assertEquals(new Double(1.0), nextPrice);
        assertTrue(PricingModelTestUtils.checkPricingModelConstraints(nextPrice, 200, requests));        
    }        

}
