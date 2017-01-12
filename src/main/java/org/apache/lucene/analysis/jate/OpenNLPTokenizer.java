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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import opennlp.tools.sentdetect.SentenceDetector;
import opennlp.tools.util.Span;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.util.AttributeFactory;
import org.apache.lucene.util.BytesRef;

/**
 * Run OpenNLP SentenceDetector and Tokenizer.
 * Must have Sentence and/or Tokenizer.
 * <p>This class will split a text into sentences, the tokenize each sentence. For each token, it will record its sentence context information. See SentenceContext class.
 * The sentence context information is recorded as PayloadAttribute</p>
 */
public final class OpenNLPTokenizer extends Tokenizer implements SentenceContextAware {
    private static final int DEFAULT_BUFFER_SIZE = 256;
    private static final Logger LOG = Logger.getLogger(OpenNLPTokenizer.class.getName());

    private int finalOffset;
    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);
    private final PayloadAttribute tokenMetadataAtt = addAttribute(PayloadAttribute.class);

    //
    private Map<Integer, Paragraph> sentsInParagraph = new HashMap<>(); //the key is the startoffset of a sentence
    private Map<Paragraph, Integer> paragraphHasSents = new HashMap<>(); //key is the paragraph, value is the number of sentences in that paragraph
    private Map<Integer, Integer> sentIdsInParagraph = new HashMap<>(); //key is the setnecen startoffset, value is the sentence's id in its source paragraph

    private Span[] sentences = null;
    private Span[][] words = null;
    private Span[] wordSet = null;
    boolean first = true;
    int indexSentence = 0;
    int indexWord = 0;
    private char[] fullText;

    private ParagraphChunker paragraphOp = null;
    private SentenceDetector sentenceOp = null;
    private opennlp.tools.tokenize.Tokenizer tokenizerOp = null;

    public OpenNLPTokenizer(AttributeFactory factory, SentenceDetector sentenceOp,
                            opennlp.tools.tokenize.Tokenizer tokenizerOp) {
        super(factory);
        termAtt.resizeBuffer(DEFAULT_BUFFER_SIZE);
        if (sentenceOp == null && tokenizerOp == null) {
            throw new IllegalArgumentException("OpenNLPTokenizer: need one or both of Sentence Detector and Tokenizer");
        }
        this.sentenceOp = sentenceOp;
        this.tokenizerOp = tokenizerOp;
    }

    public OpenNLPTokenizer(AttributeFactory factory, SentenceDetector sentenceOp,
                            opennlp.tools.tokenize.Tokenizer tokenizerOp,
                            ParagraphChunker paragraphOp) {
        super(factory);
        termAtt.resizeBuffer(DEFAULT_BUFFER_SIZE);
        if (sentenceOp == null && tokenizerOp == null) {
            throw new IllegalArgumentException("OpenNLPTokenizer: need one or both of Sentence Detector and Tokenizer");
        }
        this.sentenceOp = sentenceOp;
        this.tokenizerOp = tokenizerOp;
        this.paragraphOp = paragraphOp;
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

            for (int i = 0; i < termLength; i++) {
                buffer[i] = fullText[spot + i];
            }

            //safeguard tweak to avoid invalid token offsets, see issue 26 on github
            if (finalOffset - start > termLength) {
                offsetAtt.setOffset(start, start + termLength);
                LOG.warn("Invalid token start and end offsets diff greater than term length. End offset is reset to be start+tokenlength. " +
                        "start=" + start + ", invalid end=" + finalOffset + ", termlength=" + termLength + ". See Issue 26 on JATE webpage");
/*                String wordStr =  new String(buffer, 0, offsetAtt.endOffset() - offsetAtt.startOffset());
                System.out.println(wordStr);*/
            } else
                offsetAtt.setOffset(start, finalOffset);

            MWEMetadata ctx = addSentenceContext(new MWEMetadata(), indexWord, indexWord,
                    null, indexSentence);
            if (paragraphOp != null) {
                Paragraph sourcePar = sentsInParagraph.get(sentence.getStart());
                int sentenceIdInParagraph= sentIdsInParagraph.get(sentences[indexSentence].getStart());
                addOtherMetadata(ctx,
                        sourcePar.indexInDoc,
                        paragraphHasSents.get(sourcePar),
                        paragraphHasSents.size(),
                        sentenceIdInParagraph,
                        sentences.length);
            }
            addPayloadAttribute(tokenMetadataAtt, ctx);
            //System.out.println(tokenMetadataAtt.getPayload().utf8ToString()+","+new String(buffer,0, termAtt.length()));

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
        String txtStr = new String(fullText);
        detectSentences(txtStr);
        if (paragraphOp != null) {
            detectParagraphs(txtStr);
        }
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
    void detectSentences(String fulltext) throws IOException {
        //fullText.hashCode();
        sentences = sentenceOp.sentPosDetect(fulltext);

       /* Span[] revised = new Span[sentences.length];
        //correct offsets, in case charfilters have been used and will change offsets

        for(int i=0; i<sentences.length; i++){
            Span span = sentences[i];
            int newStart =correctOffset(span.getStart());
            int newEnd = correctOffset(span.getEnd());
            revised[i] = new Span(newStart, newEnd, span.getType(), span.getProb());
        }
        sentences=revised;*/
    }

    //split paragraphs and also create containment relation with sentences
    void detectParagraphs(String txtStr) {
        sentsInParagraph.clear();
        paragraphHasSents.clear();
        sentIdsInParagraph.clear();
        List<Paragraph>
                paragraphs = paragraphOp.chunk(txtStr);

        if (paragraphs != null) {
            int parCursor = 0;
            Paragraph par = paragraphs.get(parCursor);
            int sentenceIdInPar=0;
            for (Span sent : sentences) {
                if (sent.getStart() >= par.startOffset && sent.getStart() <= par.endOffset) {
                    sentsInParagraph.put(sent.getStart(), par);
                    Integer c = paragraphHasSents.get(par);
                    if (c == null) c = 0;
                    c++;
                    paragraphHasSents.put(par, c);
                    sentIdsInParagraph.put(sent.getStart(), sentenceIdInPar);
                    sentenceIdInPar++;
                } else {
                    for (int i = parCursor + 1; i < paragraphs.size(); i++) {
                        par = paragraphs.get(i);
                        sentenceIdInPar=0;
                        if (sent.getStart() >= par.startOffset && sent.getStart() <= par.endOffset) {
                            sentsInParagraph.put(sent.getStart(), par);
                            Integer c = paragraphHasSents.get(par);
                            if (c == null) c = 0;
                            c++;
                            paragraphHasSents.put(par, c);
                            parCursor = i;
                            sentIdsInParagraph.put(sent.getStart(), sentenceIdInPar);
                            sentenceIdInPar++;
                            break;
                        }
                    }
                }
            }
        }
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

    public MWEMetadata addSentenceContext(MWEMetadata ctx, int firstTokenIndex, int lastTokenIndex,
                                          String posTag, int sentenceIndex) {
        ctx.addMetaData(MWEMetadataType.FIRST_COMPOSING_TOKEN_ID_IN_SENT, String.valueOf(firstTokenIndex));
        ctx.addMetaData(MWEMetadataType.LAST_COMPOSING_TOKEN_ID_IN_SENT, String.valueOf(lastTokenIndex));
        ctx.addMetaData(MWEMetadataType.POS, posTag);
        ctx.addMetaData(MWEMetadataType.SOURCE_SENTENCE_ID_IN_DOC, String.valueOf(sentenceIndex));
        return ctx;
    }

    protected void addOtherMetadata(MWEMetadata ctx, int paragraphId,
                                    int totalSentencesInParagraph,
                                    int totalParagraphsInDoc,
                                    int sentenceIdInParagraph,
                                    int totalSentencesInDoc) {
        ctx.addMetaData(MWEMetadataType.SOURCE_PARAGRAPH_ID_IN_DOC, String.valueOf(paragraphId));
        ctx.addMetaData(MWEMetadataType.SENTENCES_IN_PARAGRAPH, String.valueOf(totalSentencesInParagraph));
        ctx.addMetaData(MWEMetadataType.PARAGRAPHS_IN_DOC, String.valueOf(totalParagraphsInDoc));
        ctx.addMetaData(MWEMetadataType.SOURCE_SENTENCE_ID_IN_PARAGRAPH, String.valueOf(sentenceIdInParagraph));
        ctx.addMetaData(MWEMetadataType.SENTENCES_IN_DOC, String.valueOf(totalSentencesInDoc));
    }

    public void addPayloadAttribute(PayloadAttribute attribute, MWEMetadata ctx) {
        byte[] data = MWEMetadata.serialize(ctx);
        attribute.setPayload(new BytesRef(data));
    }
}
