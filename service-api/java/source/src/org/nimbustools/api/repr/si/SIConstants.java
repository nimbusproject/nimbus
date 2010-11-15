package org.nimbustools.api.repr.si;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public abstract class SIConstants {

    // -------------------------------------------------------------------------
    // SPOT INSTANCE TYPES
    // -------------------------------------------------------------------------    

    public static final String SI_TYPE_BASIC = "basic";
    public static final Integer SI_TYPE_BASIC_MEM = 128;

    public static final List<String> SI_TYPES = Arrays.asList(new String[]{ SI_TYPE_BASIC });

    private static final HashMap<String, Integer> instanceMems = new HashMap<String, Integer>();    

    static {
        instanceMems.put(SI_TYPE_BASIC, SI_TYPE_BASIC_MEM);
    }

    public static Integer getInstanceMem(String instanceType){
        return instanceMems.get(instanceType);
    }
    
    public static final String SI_REQUEST_PREFIX = "si-";

}
