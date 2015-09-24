package uk.ac.shef.dcs.jate.v2.nlp;

import uk.ac.shef.dcs.jate.v2.nlp.opennlp.SentenceSplitterOpenNLP;

import java.lang.reflect.InvocationTargetException;

/**
 * Created by zqz on 24/09/2015.
 */
public class InstanceCreator {
    public static SentenceSplitter create(String className, String params)
            throws ClassNotFoundException, NoSuchMethodException,
            IllegalAccessException, InvocationTargetException, InstantiationException {
        if(className.equals(SentenceSplitterOpenNLP.class.getName())){
            return (SentenceSplitter) Class.forName(className).getDeclaredConstructor(String.class).newInstance(params);
        }
        return null;
    }
}
