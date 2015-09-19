package uk.ac.shef.dcs.jate.v2.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by zqz on 19/09/2015.
 */
public class JATETerm implements Comparable<JATETerm>{

    protected String string;
    protected double score;
    protected Map<TermInfoType, TermInfo> termInfo;

    public JATETerm(String string){
        this(string, 0.0);
    }
    public JATETerm(String string, double score){
        this.string=string;
        this.score=score;
        this.termInfo=new HashMap<>();
    }

    public String getString(){
        return string;
    }

    public void setString(String string){
        this.string=string;
    }

    public double getScore(){
        return score;
    }

    public void setScore(double score){
        this.score=score;
    }

    public Map<TermInfoType, TermInfo> getTermInfo(){
        return termInfo;
    }

    public void addTermInfo(TermInfoType type, TermInfo info){
        termInfo.put(type, info);
    }

    public String toString(){
        StringBuilder sb = new StringBuilder(string);
        sb.append("=").append(score);
        return sb.toString();
    }


    @Override
    public int compareTo(JATETerm o) {
        return Double.valueOf(o.getScore()).compareTo(getScore());
    }
}
