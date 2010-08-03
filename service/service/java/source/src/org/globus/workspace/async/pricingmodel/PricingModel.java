package org.globus.workspace.async.pricingmodel;

import java.util.Collection;

import org.globus.workspace.async.AsyncRequest;

public interface PricingModel {

    Double getNextPrice(Integer totalReservedResources,
            Collection<AsyncRequest> aliveRequests, Double currentPrice);
    
    void setMinPrice(Double minPrice);
    
}
