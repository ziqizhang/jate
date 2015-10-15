package org.apache.lucene.analysis.jate;

import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;

/**
 * Classes implementing this interface must record the following information, in the following format:
 *
 * 1. which sentence id this lexical unit (token, phrase) is from, in the form of: "sent{tab_delimiter}id1,id2..."
 * 2. the order of the first token and the order of the last token in the sentence, in the form of: "first{tab_delimiter}x" where x is the order
 *
 * For example, given a phrase "the cat" in the sentences "I saw the cat sat on the mat. The dog has left."
 * The following information should be stored for "the cat":
 *
 * <br></br>
 * sent{tab_delimiter}0
 * first{tab_delimiter}2
 * last{tab_delimiter}3
 *
 *
 * The string value must be parseable by Sentencecontext.
 *
 * The best option is probably calling SentenceContext.createString, passing the three int values
 */
public interface SentenceContextAware {

    void addSentenceContext(PayloadAttribute attribute, String firstTokenIndex, String lastTokenIndex, String sentenceIndexes);

}
