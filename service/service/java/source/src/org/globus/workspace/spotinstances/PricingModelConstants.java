package org.globus.workspace.spotinstances;

public class PricingModelConstants {

    protected static final double BASE_PRICE = 1.0;
    
    protected static final double DISCOUNT_FACTOR = 0.6;
    
    public static final Double MINIMUM_PRICE = (1 - DISCOUNT_FACTOR)*BASE_PRICE;

    protected static final Double NEGATIVE_INFINITY = -1.0;
    
}
