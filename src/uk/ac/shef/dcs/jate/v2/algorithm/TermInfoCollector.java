package uk.ac.shef.dcs.jate.v2.algorithm;

import uk.ac.shef.dcs.jate.v2.model.TermInfo;
import uk.ac.shef.dcs.jate.v2.model.TermInfoType;

import java.util.Map;

/**
 * Created by zqz on 19/09/2015.
 */
public interface TermInfoCollector{

    Map<TermInfoType, TermInfo> collect(String term);
}
