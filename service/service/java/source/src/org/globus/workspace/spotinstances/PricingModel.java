package org.globus.workspace.spotinstances;

import java.util.Collection;

public interface PricingModel {

    Double getNextPrice(Integer totalReservedResources,
            Collection<SIRequest> requests, Double currentPrice);
    
}
