package uk.ac.shef.dcs.jate.feature;

import java.util.List;

/**
 * Context windows can have overlap. This class represent an overlap of two adjacent context windows.
 *
 * </p>This includes the previous and following context window objects, and the list of terms (multi-set) appearing
 * in the overlapping zone. So a context window in sentence 1, with start token index=1 and end token index=10
 * has an overlap with a context window in the same sentence with start token=6 and end token=15. The list of terms
 * stored along this object appear between token index=5 and token index=10 in sentence 1.
 */
class ContextOverlap {
    private ContextWindow contextWindow1;
    private ContextWindow contextWindow2;
    private List<String> terms;

    ContextOverlap(ContextWindow contextWindow1, ContextWindow contextWindow2, List<String> terms) {
        this.contextWindow1 = contextWindow1;
        this.contextWindow2 = contextWindow2;
        this.terms = terms;
    }

    public ContextWindow getPrevContext() {
        return contextWindow1;
    }

    public ContextWindow getNextContext() {
        return contextWindow2;
    }

    public List<String> getTerms() {
        return terms;
    }

    public void setTerms(List<String> terms) {
        this.terms = terms;
    }

    public boolean equals(Object o){
        if (o instanceof ContextOverlap){
            ContextOverlap c = (ContextOverlap) o;
            if(c.contextWindow2.equals(contextWindow2)&& c.contextWindow1.equals(contextWindow1))
                return true;
        }
        return false;
    }
}
