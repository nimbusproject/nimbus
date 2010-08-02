package org.globus.workspace.spotinstances;

import java.util.Collection;

public interface PricingModel {

    Double getNextPrice(Integer totalReservedResources,
            Collection<AsyncRequest> aliveRequests, Double currentPrice);
    
    void setMinPrice(Double minPrice);
    
}
