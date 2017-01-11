package uk.ac.shef.dcs.jate.feature;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class WordShapeFeature extends AbstractFeature {
    private Map<String, Boolean> hasAcronym = new HashMap<>();
    private Map<String, Boolean> hasNumber = new HashMap<>();
    private Map<String, Boolean> hasDigit = new HashMap<>();
    private Map<String, Boolean> hasUppercase=new HashMap<>();
    private Map<String, Boolean> hasSymbol = new HashMap<>();
    private Map<String, Boolean> hasIndicativeWord = new HashMap<>();

    public WordShapeFeature(){}

    public boolean getHasAcronymFeature(String mwe){
        return hasAcronym.get(mwe);
    }
    public boolean getHasNumberFeature(String mwe){
        return hasNumber.get(mwe);
    }
    public boolean getHasDigitFeature(String mwe){
        return hasDigit.get(mwe);
    }
    public boolean getHasUppercaseFeature(String mwe){
        return hasUppercase.get(mwe);
    }
    public boolean getHasSymbolFeature(String mwe){
        return hasSymbol.get(mwe);
    }
    public boolean getHasIndicativeWordFeature(String mwe){
        return hasIndicativeWord.get(mwe);
    }

    public synchronized void mweHasAcronym(String mwe, boolean truth){
        hasAcronym.put(mwe, truth);
    }

    public synchronized void mweHasNumber(String mwe, boolean truth){
        hasNumber.put(mwe, truth);
    }

    public synchronized void mweHasDigit(String mwe, boolean truth){
        hasDigit.put(mwe, truth);
    }

    public synchronized void mweHasSymbol(String mwe, boolean truth){
        hasSymbol.put(mwe, truth);
    }

    public synchronized void mweHasUppercase(String mwe, boolean truth){
        hasUppercase.put(mwe, truth);
    }
    public synchronized void mweHasIndicativeWord(String mwe, boolean truth){
        hasIndicativeWord.put(mwe, truth);
    }
}
