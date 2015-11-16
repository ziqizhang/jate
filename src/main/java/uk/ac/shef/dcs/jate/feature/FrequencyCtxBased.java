package uk.ac.shef.dcs.jate.feature;

import java.util.*;

/**
 * Created by zqz on 21/09/2015.
 */
public class FrequencyCtxBased extends AbstractFeature {

    //context id to context objects
    private Map<String, Context> id2Ctx = new HashMap<>();

    //context and total frequency of all terms in that context
    private Map<Context, Integer> ctx2TTF = new HashMap<>();
    //context and its contained terms with their frequency In that context
    private Map<Context, Map<String, Integer>> ctx2TFIC = new HashMap<>();

    //term and set of contexts where it appears
    private Map<String, Set<Context>> term2Ctx = new HashMap<>();

    private Map<String, ContextOverlap> ctxOverlapZones = new HashMap<>();

    protected FrequencyCtxBased() {
    }


    public Map<Context, Integer> getMapCtx2TTF() {
        return ctx2TTF;
    }

    public Map<Context, Map<String, Integer>> getMapCtx2TFIC() {
        return ctx2TFIC;
    }

    protected Map<String, Set<Context>> getMapTerm2Ctx() {
        return term2Ctx;
    }

    public Map<String, Integer> getTFIC(Context ctx) {
        Map<String, Integer> result = ctx2TFIC.get(ctx);
        if (result == null)
            return new HashMap<>();
        return result;
    }

    public Set<Context> getContexts(String term) {
        return term2Ctx.get(term);
    }

    /**
     * increment the number of occurrences of term in the context (ctxid) by tf
     */
    protected synchronized void increment(Context ctx, String term, int tf) {
        Context c = id2Ctx.get(ctx.getContextId());
        if (c == null) {
            c = ctx;
            id2Ctx.put(ctx.getContextId(), c);
        }

        Map<String, Integer> tfidMap = ctx2TFIC.get(c);
        if (tfidMap == null)
            tfidMap = new HashMap<>();

        Integer f = tfidMap.get(term);
        if (f == null)
            f = 0;
        f += tf;
        tfidMap.put(term, f);
        ctx2TFIC.put(c, tfidMap);

        Set<Context> ctxs = term2Ctx.get(term);
        if (ctxs == null)
            ctxs = new HashSet<>();
        ctxs.add(c);
        term2Ctx.put(term, ctxs);
    }

    protected synchronized void increment(Context ctx, int freq) {
        Context c = id2Ctx.get(ctx.getContextId());
        if (c == null) {
            c = ctx;
            id2Ctx.put(ctx.getContextId(), c);
        }
        Integer f = ctx2TTF.get(c);
        if (f == null)
            f = 0;
        f += freq;
        ctx2TTF.put(c, f);
    }

    public Map<String, ContextOverlap> getCtxOverlapZones() {
        return ctxOverlapZones;
    }

    public void addCtxOverlapZone(ContextOverlap ctxOverlapZone) {
        this.ctxOverlapZones.put(ctxOverlapZone.getPrevContext().getContextId() + ":" +
                        ctxOverlapZone.getNextContext().getContextId(),
                ctxOverlapZone);
    }
}
