package uk.ac.shef.dcs.jate.nlp;

import uk.ac.shef.dcs.jate.nlp.opennlp.ChunkerOpenNLP;
import uk.ac.shef.dcs.jate.nlp.opennlp.POSTaggerOpenNLP;
import uk.ac.shef.dcs.jate.nlp.opennlp.SentenceSplitterOpenNLP;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;

/**
 * Created by zqz on 24/09/2015.
 */
public class InstanceCreator {
    public static SentenceSplitter createSentenceSplitter(String className, InputStream model)
            throws ClassNotFoundException, NoSuchMethodException,
            IllegalAccessException, InvocationTargetException, InstantiationException {
        if(className.equals(SentenceSplitterOpenNLP.class.getName())){
            return (SentenceSplitter) Class.forName(className).
                    getDeclaredConstructor(InputStream.class).newInstance(model);
        }
        return null;
    }

    public static POSTagger createPOSTagger(String className, InputStream model)
            throws ClassNotFoundException, NoSuchMethodException,
            IllegalAccessException, InvocationTargetException, InstantiationException {
        if(className.equals(POSTaggerOpenNLP.class.getName())){
            return (POSTagger) Class.forName(className).getDeclaredConstructor(InputStream.class).newInstance(model);
        }
        return null;
    }

    public static Chunker createChunker(String className, InputStream model)
            throws ClassNotFoundException, NoSuchMethodException,
            IllegalAccessException, InvocationTargetException, InstantiationException {
        if(className.equals(ChunkerOpenNLP.class.getName())){
            return (Chunker) Class.forName(className).getDeclaredConstructor(InputStream.class).newInstance(model);
        }
        return null;
    }
}
