package uk.ac.shef.dcs.jate.nlp;

import uk.ac.shef.dcs.jate.nlp.opennlp.ChunkerOpenNLP;
import uk.ac.shef.dcs.jate.nlp.opennlp.POSTaggerOpenNLP;
import uk.ac.shef.dcs.jate.nlp.opennlp.SentenceSplitterOpenNLP;

import java.lang.reflect.InvocationTargetException;

/**
 * Created by zqz on 24/09/2015.
 */
public class InstanceCreator {
    public static SentenceSplitter createSentenceSplitter(String className, String params)
            throws ClassNotFoundException, NoSuchMethodException,
            IllegalAccessException, InvocationTargetException, InstantiationException {
        if(className.equals(SentenceSplitterOpenNLP.class.getName())){
            return (SentenceSplitter) Class.forName(className).getDeclaredConstructor(String.class).newInstance(params);
        }
        return null;
    }

    public static POSTagger createPOSTagger(String className, String params)
            throws ClassNotFoundException, NoSuchMethodException,
            IllegalAccessException, InvocationTargetException, InstantiationException {
        if(className.equals(POSTaggerOpenNLP.class.getName())){
            return (POSTagger) Class.forName(className).getDeclaredConstructor(String.class).newInstance(params);
        }
        return null;
    }

    public static Chunker createChunker(String className, String params)
            throws ClassNotFoundException, NoSuchMethodException,
            IllegalAccessException, InvocationTargetException, InstantiationException {
        if(className.equals(ChunkerOpenNLP.class.getName())){
            return (Chunker) Class.forName(className).getDeclaredConstructor(String.class).newInstance(params);
        }
        return null;
    }
}
