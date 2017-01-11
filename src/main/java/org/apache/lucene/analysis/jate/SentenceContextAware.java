package org.apache.lucene.analysis.jate;

/**
 * Classes implementing this interface must record the following information, in the following format:
 *
 * 1. which sentence id this lexical unit (token, phrase) is from
 * 2. the index number of the first token and the index of the last token in the sentence
 *
 * For example, given a phrase "the cat" in the sentences "I saw the cat sat on the mat. The dog has left."
 * The following information should be stored for "the cat":
 *
 * <br></br>
 * 2,3,0
 *
 *
 * The string value must be parseable by Sentencecontext.
 *
 * The best option is probably calling SentenceContext.createString, passing the three int values
 */

public interface SentenceContextAware {

    /**
     *
     * @param firstTokenIndex index of the first token of the MWE in the sentence
     * @param lastTokenIndex index of the last token of the MWE in the sentence
     * @param posTag PoS of the lexical unit (only valid for single token)
     * @param sentenceIndex the index (id) of the sentence
     */
    MWEMetadata addSentenceContext(MWEMetadata ctx, int firstTokenIndex, int lastTokenIndex,
                                   String posTag, int sentenceIndex);

}
