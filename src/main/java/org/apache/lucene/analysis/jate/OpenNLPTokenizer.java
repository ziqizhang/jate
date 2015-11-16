package org.apache.lucene.analysis.jate;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import opennlp.tools.sentdetect.SentenceDetector;
import opennlp.tools.util.Span;

import org.apache.commons.io.IOUtils;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.FlagsAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.util.AttributeFactory;
import org.apache.lucene.util.BytesRef;
/**
 * Run OpenNLP SentenceDetector and Tokenizer.
 * Must have Sentence and/or Tokenizer.
 */
public final class OpenNLPTokenizer extends Tokenizer implements SentenceContextAware {
    private static final int DEFAULT_BUFFER_SIZE = 256;

    private int finalOffset;
    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);
    private final PayloadAttribute sentenceContextAtt = addAttribute(PayloadAttribute.class);

    //
    private Span[] sentences = null;
    private Span[][] words = null;
    private Span[] wordSet = null;
    boolean first = true;
    int indexSentence = 0;
    int indexWord = 0;
    private char[] fullText;

    private SentenceDetector sentenceOp = null;
    private opennlp.tools.tokenize.Tokenizer tokenizerOp = null;

    public OpenNLPTokenizer(AttributeFactory factory, SentenceDetector sentenceOp, opennlp.tools.tokenize.Tokenizer tokenizerOp) {
        super(factory);
        termAtt.resizeBuffer(DEFAULT_BUFFER_SIZE);
        if (sentenceOp == null && tokenizerOp == null) {
            throw new IllegalArgumentException("OpenNLPTokenizer: need one or both of Sentence Detector and Tokenizer");
        }
        this.sentenceOp = sentenceOp;
        this.tokenizerOp = tokenizerOp;
    }

    // OpenNLP ops run all-at-once. Have to cache sentence and/or word spans and feed them out.
    // Cache entire input buffer- don't know if this is the right implementation.
    // Of if the CharTermAttribute can cache it across multiple increments?

    @Override
    public final boolean incrementToken() throws IOException {
        if (first) {
            loadAll();
            restartAtBeginning();
            first = false;
        }
        if (sentences.length == 0) {
            first = true;
            return false;
        }
        int sentenceOffset = sentences[indexSentence].getStart();
        if (wordSet == null) {
            wordSet = words[indexSentence];
        }
        clearAttributes();
        while (indexSentence < sentences.length) {
            while (indexWord == wordSet.length) {
                indexSentence++;
                if (indexSentence < sentences.length) {
                    wordSet = words[indexSentence];
                    indexWord = 0;
                    sentenceOffset = sentences[indexSentence].getStart();
                } else {
                    first = true;
                    return false;
                }
            }
            // set termAtt from private buffer
            Span sentence = sentences[indexSentence];
            Span word = wordSet[indexWord];
            int spot = sentence.getStart() + word.getStart();
            termAtt.setEmpty();
            int termLength = word.getEnd() - word.getStart();
            if (termAtt.buffer().length < termLength) {
                termAtt.resizeBuffer(termLength);
            }
            termAtt.setLength(termLength);
            char[] buffer = termAtt.buffer();

            finalOffset = correctOffset(sentenceOffset + word.getEnd());
            int start = correctOffset(word.getStart() + sentenceOffset);
            offsetAtt.setOffset(start, finalOffset);
            for (int i = 0; i < termLength; i++) {
                buffer[i] = fullText[spot + i];
            }
            addSentenceContext(sentenceContextAtt, indexWord, indexWord,
                    null, indexSentence);
            //System.out.println(sentenceContextAtt.getPayload().utf8ToString()+","+new String(buffer,0, termAtt.length()));

            indexWord++;
            return true;
        }
        first = true;
        return false;
    }

    void restartAtBeginning() throws IOException {
        indexWord = 0;
        indexSentence = 0;
        indexWord = 0;
        finalOffset = 0;
        wordSet = null;
    }

    void loadAll() throws IOException {
        fillBuffer();
        detectSentences();
        words = new Span[sentences.length][];
        for (int i = 0; i < sentences.length; i++) {
            splitWords(i);
        }
    }

    void splitWords(int i) {
        Span current = sentences[i];
        String sentence = String.copyValueOf(fullText, current.getStart(), current.getEnd() - current.getStart());
        words[i] = tokenizerOp.tokenizePos(sentence);
    }

    // read all text, turn into sentences
    void detectSentences() throws IOException {
        fullText.hashCode();
        sentences = sentenceOp.sentPosDetect(new String(fullText));
    }

    void fillBuffer() throws IOException {
        fullText = IOUtils.toCharArray(input);
        /*int offset = 0;
        int size = 10000;
        fullText = new char[size];
        int length = input.read(fullText);
        while(length == size) {
//    fullText = IOUtils.toCharArray(input);
            fullText = Arrays.copyOf(fullText, offset + size);
            offset += size;
            length = input.read(fullText, offset, size);
        }
        fullText = Arrays.copyOf(fullText, offset + length);*/
    }

    @Override
    public final void end() {
        // set final offset
        offsetAtt.setOffset(finalOffset, finalOffset);
    }

//  public void reset(Reader input) throws IOException {
//    super.reset(input);
//    fullText = null;
//    sentences = null;
//    words = null;
//    first = true;
//  }

    @Override
    public void reset() throws IOException {
        super.reset();
        clearAttributes();
        restartAtBeginning();
    }


    @Override
    public void addSentenceContext(PayloadAttribute attribute, int firstTokenIndex, int lastTokenIndex,
                                   String posTag, int sentenceIndex) {
        StringBuilder s = new StringBuilder("f=").append(firstTokenIndex);
        s.append(",l=").append(lastTokenIndex)
                .append(",s=").append(sentenceIndex);
        try {
            attribute.setPayload(new BytesRef(s.toString().getBytes("UTF-8")));
        } catch (UnsupportedEncodingException uee) {
            attribute.setPayload(new BytesRef(s.toString().getBytes()));
        }
    }
}
