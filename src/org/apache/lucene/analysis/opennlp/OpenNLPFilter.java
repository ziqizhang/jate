package org.apache.lucene.analysis.opennlp;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import opennlp.tools.chunker.Chunker;
import opennlp.tools.postag.POSTagger;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.util.Attribute;
import org.apache.lucene.util.AttributeSource;
import org.apache.lucene.util.BytesRef;

/**
 * Run OpenNLP sentence-processing tools
 * OpenNLP Tokenizer- removed sentence detection
 * Optional: POS tagger or phrase chunker. These tag all terms.
 * Optional: one or more Named Entity Resolution taggers. These tag only some terms.
 *
 * Use file names as keys for cached models.
 *
 * TODO: a) do positionincr attrs b) implement all attr types
 *
 * Hacks:
 * hack #1: EN POS tagger sometimes tags last word as a period if no period at the end
 * hack #2: tokenizer needs to split words with punctuation and it doesn't
 */
public final class OpenNLPFilter extends TokenFilter {

    // TODO: if there's an ICU for this, that's great
    private static String SENTENCE_BREAK = "[.?!]";

    private final boolean doPOS;
    private final boolean doChunking;

    private int finalOffset;

    // cloned attrs of all tokens
    private List<AttributeSource> tokenAttrs = new ArrayList<>();
    private boolean first = true;
    private int indexToken = 0;
    //  private char[] fullText;
    // hack #1: have to remove final term if we added one
    private boolean stripFinal = false;

    private POSTagger posTaggerOp = null;
    private Chunker chunkerOp = null;

    public OpenNLPFilter(
            TokenStream input,
            POSTagger posTaggerOp,
            Chunker chunkerOp){
        super(input);
        this.posTaggerOp = posTaggerOp;
        this.chunkerOp = chunkerOp;

        boolean havePOS = (posTaggerOp != null);
        doChunking = (chunkerOp != null);
        doPOS = doChunking ? false : havePOS;
    }

    @Override
    public final boolean incrementToken() throws IOException {
        clearAttributes();
        if (first) {
            String[] words = walkTokens();
            if (words.length == 0) {
                return false;
            }
            createTags(words);
            first = false;
            indexToken = 0;
        }
        if (stripFinal && indexToken == tokenAttrs.size() - 1) {
            first = true;
            indexToken = 0;
            return false;
        }
        if (indexToken == tokenAttrs.size()) {
            first = true;
            indexToken = 0;
            return false;
        }
        AttributeSource as = tokenAttrs.get(indexToken);
        Iterator<? extends Class<? extends Attribute>> it = as.getAttributeClassesIterator();
        while(it.hasNext()) {
            Class<? extends Attribute> attrClass = it.next();
            if (! hasAttribute(attrClass)) {
                addAttribute(attrClass);
            }
        }
        as.copyTo(this);
        indexToken++;
        return true;
    }

    private String[] walkTokens() throws IOException {
        List<String> wordList = new ArrayList<>();
        while (input.incrementToken()) {
            CharTermAttribute textAtt = input.getAttribute(CharTermAttribute.class);
            OffsetAttribute offsetAtt = input.getAttribute(OffsetAttribute.class);
            char[] buffer = textAtt.buffer();
            String word = new String(buffer, 0, offsetAtt.endOffset() - offsetAtt.startOffset());
            wordList.add(word);
            AttributeSource attrs = input.cloneAttributes();
            tokenAttrs.add(attrs);
        }
        String[] words = new String[wordList.size()];
        for(int i = 0; i < words.length; i++) {
            words[i] = wordList.get(i);
        }
        return words;
    }

    private void createTags(String[] words) {
        String[] appended = appendDot(words);
        if (doPOS) {
            String[] tags = assignPOS(appended);
            appendPayloads(tags, words.length);
        }
        else if (doChunking) {
            String[] pos = assignPOS(appended);
            String[] tags = createChunks(words, pos);
            appendPayloads(tags, words.length);
        }
    }

    // Hack #1: taggers expect a sentence break as the final term.
    // This does not make it into the attribute set lists.
    private String[] appendDot(String[] words) {
        int nWords = words.length;
        String lastWord = words[nWords - 1];
        if (lastWord.length() != 1) {
            return words;
        }
        if (lastWord.matches(SENTENCE_BREAK)) {
            return words;
        }
        words = Arrays.copyOf(words, nWords + 1);
        words[nWords] = ".";
        return words;
    }

    private void appendPayloads(String[] tags, int length) {
        for(int i = 0; i < length; i++) {
            AttributeSource attrs = tokenAttrs.get(i);
            if (tags[i] != null) {
                try {
                    PayloadAttribute payloadAtt = attrs.hasAttribute(PayloadAttribute.class) ? attrs.getAttribute(PayloadAttribute.class) : attrs.addAttribute(PayloadAttribute.class);
                    BytesRef p = new BytesRef(tags[i].toUpperCase(Locale.getDefault()).getBytes("UTF-8"));
                    payloadAtt.setPayload(p);
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private String[] assignPOS(String[] words) {
        return posTaggerOp.tag(words);
    }

    private String[] createChunks(String[] words, String[] pos) {
        return chunkerOp.chunk(words, pos);
    }

    @Override
    public final void end() {
        clearAttributes();
        OffsetAttribute offsetAtt = getAttribute(OffsetAttribute.class);
        offsetAtt.setOffset(finalOffset, finalOffset);
        tokenAttrs.clear();
    }

    @Override
    public void reset() throws IOException {
        super.reset();
        clearAttributes();
        restartAtBeginning();
    }

    private  void restartAtBeginning() throws IOException {
        indexToken = 0;
        finalOffset = 0;
    }

}