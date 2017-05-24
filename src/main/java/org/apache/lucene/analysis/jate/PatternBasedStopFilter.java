package org.apache.lucene.analysis.jate;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.util.FilteringTokenFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 *
 */
public class PatternBasedStopFilter extends FilteringTokenFilter {

    private final CharTermAttribute termAtt = (CharTermAttribute)this.addAttribute(CharTermAttribute.class);

    private boolean removeDigits=false;
    private Pattern digitPattern = Pattern.compile("[\\d]");
    private Set<String> patterns = new HashSet<>();
    public PatternBasedStopFilter(boolean removeDigits, Set<String> patterns, TokenStream stream) {
        super(stream);
        this.removeDigits=removeDigits;
        this.patterns=patterns;
    }

    @Override
    protected boolean accept() throws IOException {
        String tok = new String(this.termAtt.buffer(), 0, this.termAtt.length());
        if(removeDigits&&digitPattern.matcher(tok).find()){
            return false;
        }
        Set<String> elements=new HashSet<>(Arrays.asList(tok.split(" ")));
        elements.retainAll(patterns);
        if(elements.size()>0)
            return false;
        return true;
    }

    /*public static void main(String[] args) {
        Pattern digitPattern = Pattern.compile("[\\d]");
        System.out.println(digitPattern.matcher("this is a").find());

    }*/
}
