package org.apache.lucene.analysis.jate;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
class ParagraphChunkerRawText implements ParagraphChunker {

    private static Pattern separator = Pattern.compile("\\n+");

    public ParagraphChunkerRawText(){}

    public List<Paragraph> chunk(String inputText) {
        List<Paragraph> paragraphs = new ArrayList<>();
        Matcher m = separator.matcher(inputText);

        int index=0, start=0;
        while(m.find()){
            int mstart=m.start();
            String content= inputText.substring(start, mstart);
            if(content.trim().length()==0)
                continue;
            Paragraph p = new Paragraph(start,mstart, index);
            index++;
            paragraphs.add(p);
            start=m.end();
        }
        if(start< inputText.length()){
            Paragraph p =new Paragraph(start, inputText.length(), index);
            paragraphs.add(p);
        }

        return paragraphs;
    }

    /*public static void main(String[] args) {
        String input="Cellular and molecular mechanisms of IFN-gamma production induced by IL-2 and IL-12 in a human NK cell line.\n" +
                "Interferon-gamma (IFN-gamma) is an important immunoregulatory protein produced predominantly by T cells and large granular lymphocytes (LGL) in response to different extracellular signals.In particular, two interleukins (ILs), IL-2 and IL-12, have been shown to be potent inducers of IFN-gamma gene expression in both T cells and LGL.Although it has been reported that there are some T cell lines that produce IFN-gamma in response to IL-2 and IL-12 stimulation, there has as yet been no report of a natural killer (NK) cell line that responds in a similar manner.In this report we present evidence that the cell line NK3.3 derived from human NK cells, responds to both IL-2 and IL-12, as measured by increases in IFN-gamma and granulocyte-macrophage colony-stimulating factor (GM-CSF) cytoplasmic mRNA and protein expression.In addition, when used together IL-2 and IL-12 synergized in the induction of IFN-gamma and GM-CSF and this synergy was attributed to an increased accumulation and stability of the IFN-gamma and GM-CSF mRNAs.To investigate the signaling pathways involved in the gene induction, five inhibitors, cyclosporin A (CsA), transforming growth factor-beta, cycloheximide, genistein, and staurosporine A, were used in analyzing the effects of IL-2 and IL-12 on NK3.3 cells.The results suggest that activation of protein kinase C, but not new protein synthesis, is required for IL-2 induction of IFN-gamma and GM-CSF cytoplasmic mRNA.In contrast, IL-12 induction of IFN-gamma cytoplasmic mRNA appears to only partially depend on activation of protein kinase C.Furthermore, both transforming growth factor-beta and genistein, a tyrosine kinase inhibitor, could suppress IL-2 and IL-12 signaling but CsA was generally inactive.It also was observed that suppression of cytokine gene expression by these agents was independent of the inhibition of proliferation.In addition, IL-2 but not IL-12 induced nuclear factors NF-kappa B and AP1, and regulation of the nuclear levels of these two DNA binding protein complexes is correlated with IFN-gamma and GM-CSF gene expression.These data indicate that IL-2 and IL-12 may have distinct signaling pathways leading to the induction of IFN-gamma and GM-CSF gene expression, and that the NK3.3 cell line may serve as a novel model for dissecting the biochemical and molecular events involved in these pathways.";
        ParagraphChunkerRawText chunker = new ParagraphChunkerRawText();
        List<Paragraph> pars=chunker.chunk(input);
        for(Paragraph p: pars){
            String txt = input.substring(p.startOffset, p.endOffset);
            System.out.println();
        }
    }*/
}
