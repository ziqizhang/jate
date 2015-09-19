package uk.ac.shef.dcs.jate.v2.model;

import java.util.*;

/**
 */
public class TermInfo {

    protected Map<JATEDocument, Set<int[]>> offsets = new HashMap<>();
    protected List<String> variants = new ArrayList<>();

    protected Map<String, Object> otherInfo = new HashMap<>();

    public Map<JATEDocument, Set<int[]>> getOffsets(){
        return offsets;
    }

    public List<String> getVariants(){
        return variants;
    }

    public Map<String, Object> getOtherInfo(){
        return otherInfo;
    }
}
