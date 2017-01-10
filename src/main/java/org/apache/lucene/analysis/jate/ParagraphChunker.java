package org.apache.lucene.analysis.jate;

import java.util.List;

/**
 *
 */
interface ParagraphChunker {
    List<Paragraph> chunk(String inputText);
}
