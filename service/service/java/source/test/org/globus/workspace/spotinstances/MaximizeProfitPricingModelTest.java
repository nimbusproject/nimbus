package org.globus.workspace.spotinstances;

import static org.junit.Assert.*;
import static org.globus.workspace.spotinstances.MaximizeProfitPricingModel.*;

import java.util.LinkedList;

import org.junit.Test;

public class MaximizeProfitPricingModelTest {

    private MaximizeProfitPricingModel pricingModel = new MaximizeProfitPricingModel();
   
    
    @Test
    public void testGetNextPriceNoDemand() {
        
        LinkedList<SIRequest> requests = new LinkedList<SIRequest>();
        
        assertEquals(MINIMUM_PRICE, pricingModel.getNextPrice(0, requests, null));
        assertEquals(MINIMUM_PRICE, pricingModel.getNextPrice(5, requests, null));
        assertEquals(MINIMUM_PRICE, pricingModel.getNextPrice(2500, requests, null));
    }
    
    @Test
    public void testGetNextPriceNoOffer() {
       
        //case 1
        LinkedList<SIRequest> requests = new LinkedList<SIRequest>();
        requests.add(new SIRequest(MINIMUM_PRICE*2, 1));
     
        assertEquals(new Double(MINIMUM_PRICE*2+1), pricingModel.getNextPrice(0, requests, null));
        
        //case 2
        requests = new LinkedList<SIRequest>();
        requests.add(new SIRequest(MINIMUM_PRICE*4, 1));
        requests.add(new SIRequest(MINIMUM_PRICE*2, 4));  
        requests.add(new SIRequest(MINIMUM_PRICE*3.5, 4));        
        
        assertEquals(new Double(MINIMUM_PRICE*4+1), pricingModel.getNextPrice(0, requests, null));
    }    
    
    @Test
    public void testGetNextPriceCase1() {

        LinkedList<SIRequest> requests = new LinkedList<SIRequest>();
        requests.add(new SIRequest(MINIMUM_PRICE*2, 3));
        
        assertEquals(new Double(MINIMUM_PRICE*2), pricingModel.getNextPrice(5, requests, null));
    }  
    
    @Test
    public void testGetNextPriceCase2() {

        LinkedList<SIRequest> requests = new LinkedList<SIRequest>();
        requests.add(new SIRequest(MINIMUM_PRICE*2, 10));
        
        assertEquals(new Double(MINIMUM_PRICE*2), pricingModel.getNextPrice(5, requests, null));
    }
    
    @Test
    public void testGetNextPriceCase3() {

        LinkedList<SIRequest> requests = new LinkedList<SIRequest>();
        requests.add(new SIRequest(MINIMUM_PRICE*2, 10));
        requests.add(new SIRequest(MINIMUM_PRICE*1, 5));
        
        
        assertEquals(new Double(MINIMUM_PRICE*2), pricingModel.getNextPrice(5, requests, null));
    } 
    
    @Test
    public void testGetNextPriceCase4() {

        LinkedList<SIRequest> requests = new LinkedList<SIRequest>();
        requests.add(new SIRequest(MINIMUM_PRICE*2, 5));
        requests.add(new SIRequest(MINIMUM_PRICE*1, 5));
        requests.add(new SIRequest(MINIMUM_PRICE*1.6, 5));
        
        assertEquals(new Double(MINIMUM_PRICE*1.6), pricingModel.getNextPrice(15, requests, null));
    }   
    
    @Test
    public void testGetNextPriceCase5() {

        LinkedList<SIRequest> requests = new LinkedList<SIRequest>();
        requests.add(new SIRequest(MINIMUM_PRICE*2, 5));
        requests.add(new SIRequest(MINIMUM_PRICE*1, 5));
        requests.add(new SIRequest(MINIMUM_PRICE*1.4, 5));
        
        assertEquals(new Double(MINIMUM_PRICE*1), pricingModel.getNextPrice(15, requests, null));
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

        LinkedList<SIRequest> requests = new LinkedList<SIRequest>();
        requests.add(new SIRequest(MINIMUM_PRICE*100, 1));
        requests.add(new SIRequest(MINIMUM_PRICE*1, 25));
        requests.add(new SIRequest(MINIMUM_PRICE*2, 25));
        requests.add(new SIRequest(MINIMUM_PRICE*3, 25));
        requests.add(new SIRequest(MINIMUM_PRICE*4, 25));
        
        assertEquals(new Double(MINIMUM_PRICE*100), pricingModel.getNextPrice(200, requests, null));
    }    

}
