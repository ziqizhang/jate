package uk.ac.shef.dcs.jate.app;

public enum AppParams {
    /**
     * Params used at runtime term candidate filtering (scoring, ranking, cutoff)
     * map from parameter key (for abbv.) to parameter name (for the solr config
     * setting)
     * <p>
     * see also {@code uk.ac.shef.dcs.jate.solr.TermRecognitionRequestHandler}
     */


    //used only in the embedded mode. tells the system whether you want to save offset information of extracted terms
    COLLECT_TERM_INFO("-c", "collect_offsets"),
    // cut off threshold to filter term candidates list by scores
    CUTOFF_THRESHOLD("-cf.t", "cutoff_threshold"),
    // top K terms to filter term candidates
    CUTOFF_TOP_K("-cf.k", "cutoff_top_k"),

    // top K% terms to filter term candidates
    CUTOFF_TOP_K_PERCENT("-cf.kp", "cutoff_top_k_percent"),

    //used only in the embedded mode. Output file to export final filtered term list
    OUTPUT_FILE("-o", "output_file"),

    // Min total fequency of a term for it to be considered for scoring and ranking
    // see {@code uk.ac.shef.dcs.jate.app.AppChiSquare}
    // see also {@code uk.ac.shef.dcs.jate.JATEProperties}
    PREFILTER_MIN_TERM_TOTAL_FREQUENCY("-pf.mttf", "min_term_total_freq"),
    // Min frequency of a term appearing in different context for it
    // to be considered for co-occurrence computation.
    // see {@code uk.ac.shef.dcs.jate.app.AppChiSquare}
    // see also {@code uk.ac.shef.dcs.jate.JATEProperties}
    PREFILTER_MIN_TERM_CONTEXT_FREQUENCY("-pf.mtcf", "min_term_context_freq"),

    CHISQUERE_FREQ_TERM_CUTOFF_PERCENTAGE("-ft", "ChiSquare only: frequent term cutoff percentage. " +
            "Value must be within (0,1.0]"),

    // file path to the reference corpus statistics (unigram
    // distribution) file.
    // see bnc_unifrqs.normal default file in /resource directory
    // see also {@code uk.ac.shef.dcs.jate.app.AppTermEx}
    // see also {@code uk.ac.shef.dcs.jate.app.AppWeirdness})
    REFERENCE_FREQUENCY_FILE("-r", "reference_frequency_file");

    private final String paramKey;
    private final String paramName;

    AppParams(String paramKey, String paramName) {
        this.paramKey = paramKey;
        this.paramName = paramName;
    }

    public String getParamKey() {
        return this.paramKey;
    }

    public String getParamName() {
        return this.paramName;
    }

}
