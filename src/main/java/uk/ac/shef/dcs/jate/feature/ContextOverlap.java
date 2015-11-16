package uk.ac.shef.dcs.jate.feature;

import java.util.List;

/**
 * For contextual features, it is possible that some context can have overlap (e.g., contextual window).
 *
 * It is critical to know what terms are present in these overlap, such that when co-occurrence stats are computed,
 * they are not double counted.
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
