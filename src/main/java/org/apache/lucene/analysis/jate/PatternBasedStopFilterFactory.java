package org.apache.lucene.analysis.jate;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.vfs2.FileUtil;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoaderAware;
import org.apache.lucene.analysis.util.TokenFilterFactory;
import org.apache.solr.core.SolrResourceLoader;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by zqz on 24/05/17.
 */
public class PatternBasedStopFilterFactory extends TokenFilterFactory implements ResourceLoaderAware {

    private boolean removeDigits; //any candidates contain digits are removed
    private String patternFile=null;
    private Set<String> patterns = new HashSet<>(); //patterns to apply

    /**
     * Initialize this factory via a set of key-value pairs.
     *
     * @param args
     */
    public PatternBasedStopFilterFactory(Map<String, String> args) {
        super(args);
        removeDigits = Boolean.valueOf(args.get("removeDigits"));
        patternFile=args.get("patternFile");
        if (patternFile == null)
            throw new IllegalArgumentException("Parameter 'patternFile' for stop filter patterns is missing.");

    }

    @Override
    public void inform(ResourceLoader loader) throws IOException {
        if (patternFile != null ) {
            try {
                String path=((SolrResourceLoader) loader).getConfigDir();
                if(!path.endsWith(File.separator))
                    path=path+File.separator;
                List<String> lines = FileUtils.readLines(new File(path+patternFile));
                patterns.addAll(lines);
            } catch (Exception e) {
                StringBuilder sb = new StringBuilder("Initiating ");
                sb.append(this.getClass().getName()).append(" failed due to:\n");
                sb.append(ExceptionUtils.getFullStackTrace(e));
                throw new IllegalArgumentException(sb.toString());
            }
        }
    }

    @Override
    public TokenStream create(TokenStream input) {

        return new PatternBasedStopFilter(removeDigits, patterns, input);
    }
}
