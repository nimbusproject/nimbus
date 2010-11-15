package org.nimbustools.api._repr;

import java.util.Calendar;

import org.nimbustools.api.repr.SpotPriceEntry;

public interface _SpotPriceEntry extends SpotPriceEntry {

    public void setTimeStamp(Calendar timeStamp);
    
    public void setSpotPrice(Double spotPrice);
}
