package uk.ac.shef.dcs.jate.solr;

import java.util.Collection;

/**
 * Term recognition Processor Factory where all the supported TR algorithms are registered
 *
 * @author jieg
 */
public class TermRecognitionProcessorFactory {
    public static CompositeTermRecognitionProcessor createTermRecognitionProcessor() {
        final CompositeTermRecognitionProcessor termRecognitionProcessor = new CompositeTermRecognitionProcessor();

        final Collection<TermRecognitionProcessor> processors = termRecognitionProcessor.getProcessors();

        //register all the supported term recognition algorithms
        processors.add(new ATTFProcessor());
        processors.add(new ChiSquareProcessor());
        processors.add(new CValueProcessor());
        processors.add(new GlossExProcessor());
        processors.add(new RAKEProcessor());
        processors.add(new TermExProcessor());
        processors.add(new TFIDFProcessor());
        processors.add(new TTFProcessor());
        processors.add(new WeirdnessProcessor());
        processors.add(new RIDFProcessor());

        return termRecognitionProcessor;
    }

}
