package uk.ac.shef.dcs.jate.lucene.filter;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Given a string "(' delete + all ) the symbols.+"
 *
 * stripAnySymbols will return "delete all the symbols"
 * stripLeadingSymbols will return "delete + all ) the symbols.+"
 * stripTrailingSymbols will return "(' delete + all ) the symbols"
 *
 *
 */
public class PunctuationRemover extends TokenFilter {

    public static boolean DEFAULT_STRIP_LEADING_SYMBOLS=false;
    public static boolean DEFAULT_STRIP_TRAILING_SYMBOLS=false;
    public static boolean DEFAULT_STRIP_ANY_SYMBOLS=false;

    private Pattern leadingSymbolPattern = Pattern.compile("^[\\p{Punct}]+[\\s]*");
    private Pattern trailingSymbolPattern = Pattern.compile("[\\s]*[\\p{Punct}]+$");
    private boolean stripLeadingSymbols;
    private boolean stripTrailingSymbols;
    private boolean stripAnySymbols;


    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

    /**
     * Construct a token stream filtering the given input.
     *
     * @param input
     */
    protected PunctuationRemover(TokenStream input, boolean stripAnySymbols,
                                 boolean stripLeadingSymbols, boolean stripTrailingSymbols) {
        super(input);
        this.stripAnySymbols=stripAnySymbols;
        this.stripLeadingSymbols=stripLeadingSymbols;
        this.stripTrailingSymbols=stripTrailingSymbols;
    }

    @Override
    public boolean incrementToken() throws IOException {
        if (input.incrementToken()) {
            String tok = new String(termAtt.buffer(),0, termAtt.length());
            if(tok.length()>0) {
                if (stripAnySymbols) {
                    tok = tok.replaceAll("\\p{Punct}", " ").replaceAll("\\s+", " ").trim();
                    if (tok.length() == 0)
                        clearAttributes();
                    else
                        termAtt.setEmpty().append(tok);
                } else {
                    if (stripLeadingSymbols) {
                        Matcher m = leadingSymbolPattern.matcher(tok);
                        if (m.find())
                            tok = tok.substring(m.end());
                    }
                    if (stripTrailingSymbols) {
                        Matcher m = trailingSymbolPattern.matcher(tok);
                        if (m.find())
                            tok = tok.substring(0, m.start());
                    }
                    tok = tok.trim();
                    termAtt.setEmpty().append(tok);
                }
            }
            return true;
        } else {
            return false;
        }
    }
}
