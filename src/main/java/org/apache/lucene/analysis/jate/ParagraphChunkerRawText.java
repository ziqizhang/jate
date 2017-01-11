package org.apache.lucene.analysis.jate;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
class ParagraphChunkerRawText implements ParagraphChunker {

    private static Pattern separator = Pattern.compile("\\p{Alnum}+\n+");

    public ParagraphChunkerRawText(){}

    public List<Paragraph> chunk(String inputText) {
        List<Paragraph> paragraphs = new ArrayList<>();
        Matcher m = separator.matcher(inputText);

        int index=0;
        while(m.find()){
            int start=m.start();
            int end=m.end();

            Paragraph p = new Paragraph(start,end, index);
            index++;
            paragraphs.add(p);
        }

        return null;
    }
}
