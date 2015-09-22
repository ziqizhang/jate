package uk.ac.shef.dcs.jate.deprecated;

import java.util.Properties;
import java.io.InputStream;
import java.io.IOException;

/**
 */


@Deprecated
public class JATEProperties {
    private Properties _properties = new Properties();
    private static JATEProperties _ref = null;

    //public static final String NP_FILTER_PATTERN = "[^a-zA-Z0-9\\-]";
    //replaced by the following var:
    public static final String TERM_CLEAN_PATTERN = "[^a-zA-Z0-9\\-]";
    public static final String REGEX_QUOTES="[\"]";
    

    public static final String NLP_PATH = "jate.system.nlp";
    public static final String TERM_MAX_WORDS = "jate.system.term.maxwords";
    public static final String TERM_IGNORE_DIGITS = "jate.system.term.ignore_digits";
    public static final String MULTITHREAD_COUNTER_NUMBERS="jate.system.term.frequency.counter.multithread";
    public static final String CONTEXT_PERCENTAGE = "jate.system.NCValue.percent";
    
    public static final String CORPUS_PATH = "jate.system.corpus_path";
    public static final String RESULT_PATH="jate.system.result_path";
    public static final String IGNORE_SINGLE_WORDS = "jate.system.term.ignore_singleWords";
    public static final String TERM_MIN_FREQ = "jate.system.term.minFreq";
    
    /*code modification RAKE : starts*/
    
    public static final String TERM_CLEAN_PATTERN_RAKE = "[^a-zA-Z0-9\\-.'&_]";
    
    /*code modification RAKE : ends*/

    private JATEProperties() {
        read();
    }

    public static JATEProperties getInstance() {
        if (_ref == null) {
            _ref = new JATEProperties();
        }
        return _ref;
    }

    private void read() {
        InputStream in = null;
        try {
            /*InputStream x= getClass().getResourceAsStream("/indexing.properties");*/
            in = getClass().getResourceAsStream("/jate.properties");
            _properties.load(in);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (in != null) try {
                in.close();
                in = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String getProperty(String key) {
        return _properties.getProperty(key);
    }

    public String getNLPPath() {
        return getProperty(NLP_PATH);
    }

    public int getMaxMultipleWords() {
        try {
            return Integer.valueOf(getProperty(TERM_MAX_WORDS));
        } catch (NumberFormatException e) {
            return 5;
        }
    }

    public int getMultithreadCounterNumbers() {
        try {
            return Integer.valueOf(getProperty(MULTITHREAD_COUNTER_NUMBERS));
        } catch (NumberFormatException e) {
            return 5;
        }
    }

    public boolean isIgnoringDigits() {
        try {
            return Boolean.valueOf(getProperty(TERM_IGNORE_DIGITS));
        } catch (Exception e) {
            return true;
        }
    }
    
    //code modification NCValue begins
    
    public int getPercentage() {
        try {
            return Integer.valueOf(getProperty(CONTEXT_PERCENTAGE));
        } catch (Exception e) {
            return 30;
        }
    }
    
    public String getCorpusPath() {
        return getProperty(CORPUS_PATH);
    }
    
    public String getResultPath() {
        return getProperty(RESULT_PATH);
    }
    
    public boolean isIgnoringSingleWords() {
        try {
            return Boolean.valueOf(getProperty(IGNORE_SINGLE_WORDS));
        } catch (Exception e) {
            return true;
        }
    }
    
    //code modification NCValue ends
     
    //code modification chi Square begins
    public int getMinFreq() {
        try {
            return Integer.valueOf(getProperty(TERM_MIN_FREQ));
        } catch (NumberFormatException e) {
            return 4;
        }
    }
    
    //code modification chi Square ends
    

}
