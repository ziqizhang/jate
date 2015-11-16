package uk.ac.shef.dcs.jate.feature;

import java.util.List;

/**
 * For contextual features, it is possible that some context can have overlap (e.g., contextual window).
 *
 * It is critical to know what terms are present in these overlap, such that when co-occurrence stats are computed,
 * they are not double counted.
 */
class ContextOverlap {
    private Context context1;
    private Context context2;
    private List<String> terms;

    ContextOverlap(Context context1, Context context2, List<String> terms) {
        this.context1 = context1;
        this.context2 = context2;
        this.terms = terms;
    }

    public Context getPrevContext() {
        return context1;
    }

    public Context getNextContext() {
        return context2;
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
            if(c.context2.equals(context2)&& c.context1.equals(context1))
                return true;
        }
        return false;
    }
}
