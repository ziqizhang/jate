package uk.ac.shef.dcs.jate.eval;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import uk.ac.shef.dcs.jate.JATEException;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jieg on 30/01/2016.
 */
public class ScorerTest {
    @Before
    public void setup() throws Exception {
    }

    private static DecimalFormat decFormat = new DecimalFormat(".##");

    @Test
    public void testComputePrecisionAtRank1(){
        List<String> gsTerms = new ArrayList<>();
        List<String> extractedTerms = new ArrayList<>();

        gsTerms.add("\"B-subunit knock-out\" (BKO) construct");
        gsTerms.add("\"N-end rule\" ligase");
        gsTerms.add("\"Plus\" clone");
        gsTerms.add("\"adult\" patterne");
        gsTerms.add("\"basic helix-loop-helix\" protein");
        gsTerms.add("\"delayed-early\" IL-3-responsive gene");
        gsTerms.add("\"giant\" platelet");
        gsTerms.add("\"housekeeping\" promoter");
        gsTerms.add("\"minus\" clone");
        gsTerms.add("\"nonresponsive\" clone");
        gsTerms.add("\"normal\" lymphokine");

        extractedTerms.add("\"B-subunit knock-out\" (BKO) construct");
        extractedTerms.add("\"N-end rule\" ligase");
        extractedTerms.add("\"nonresponsive\" clone");
        extractedTerms.add("N-end rule");
        extractedTerms.add("\"basic helix-loop-helix\" protein");
        extractedTerms.add("construct");
        extractedTerms.add("clone");
        extractedTerms.add("N-end rule");
        extractedTerms.add("\"giant\" platelet");
        extractedTerms.add("platelet");
        extractedTerms.add("lymphokine");

        double top5Precision1 = Scorer.precision(gsTerms, extractedTerms.subList(0, 5));
        assert 0.8 == top5Precision1;


        double top5Precision3 = Scorer.precision(gsTerms, extractedTerms.subList(0, 10));
        assert 0.5 == top5Precision3;

    }

    @Test
    public void testComputePrecision() throws JATEException {
        List<String> gsTerms = new ArrayList<>();
        List<String> extractedTerms = new ArrayList<>();

        gsTerms.add("\"B-subunit knock-out\" (BKO) construct");
        gsTerms.add("\"N-end rule\" ligase");
        gsTerms.add("\"Plus\" clone");
        gsTerms.add("\"adult\" patterne");
        gsTerms.add("\"basic helix-loop-helix\" protein");
        gsTerms.add("\"delayed-early\" IL-3-responsive gene");
        gsTerms.add("\"giant\" platelet");
        gsTerms.add("\"housekeeping\" promoter");
        gsTerms.add("\"minus\" clone");
        gsTerms.add("\"nonresponsive\" clone");
        gsTerms.add("\"normal\" lymphokine");

        extractedTerms.add("B subunit knock out (bko) construct");
        extractedTerms.add("n end rule ligase");
        extractedTerms.add("nonresponsive clone");
        extractedTerms.add("N end rule");
        extractedTerms.add("basic helix-loop-helix protein");
        extractedTerms.add("construct");
        extractedTerms.add("clone");
        extractedTerms.add("N end rule");
        extractedTerms.add("giant platelet");
        extractedTerms.add("platelet");
        extractedTerms.add("lymphokine");
        extractedTerms.add("platelet");
        extractedTerms.add("end rule");
        extractedTerms.add("clone");
        extractedTerms.add("Normal Lymphokine");

        double[] scores = Scorer.computePrecisionAtRank(gsTerms, extractedTerms,
                true, false, true,
                1, 100, 1, 10,
                5, 7, 10, 14);
        assert 0.8 == scores[0];
        assert 0.57 == scores[1];
        assert 0.5 == scores[2];
        assert 0.36 == scores[3];
    }

    @Test
    public void testTermSetPrune(){
        List<String> gsTerms = new ArrayList<>();
        gsTerms.add("\"B-subunit knock-out\" (BKO) construct");
        gsTerms.add("\"N-end rule\" ligase");
        gsTerms.add("\"Plus\" clone");
        gsTerms.add("\"adult\" patterne");
        gsTerms.add("\"basic helix-loop-helix\" protein");
        gsTerms.add("\"delayed-early\" IL-3-responsive gene");
        gsTerms.add("\"giant\" platelet");
        gsTerms.add("\"housekeeping\" promoter");
        gsTerms.add("\"minus\" clone");
        gsTerms.add("\"nonresponsive\" clone");
        gsTerms.add("\"normal\" lymphokine");

        boolean ignoreSymbols = true;
        boolean ignoreDigits = false;
        boolean lowercase = true;
        int minChar = 1;
        int maxChar = 1000;
        int minTokens = 1;
        int maxTokens = 10;

        List<String> normedGSTerms = Scorer.prune(gsTerms,ignoreSymbols, ignoreDigits, lowercase, minChar,
                maxChar, minTokens, maxTokens);

        assert "b subunit knock out bko construct".equals(normedGSTerms.get(0)) ;
        assert "n end rule ligase".equals(normedGSTerms.get(1)) ;
        assert "plus clone".equals(normedGSTerms.get(2)) ;
        assert "adult patterne".equals(normedGSTerms.get(3)) ;
        assert "basic helix loop helix protein".equals(normedGSTerms.get(4)) ;
        assert "delayed early il 3 responsive gene".equals(normedGSTerms.get(5)) ;
        assert "giant platelet".equals(normedGSTerms.get(6)) ;
        assert "housekeeping promoter".equals(normedGSTerms.get(7)) ;
        assert "minus clone".equals(normedGSTerms.get(8)) ;
        assert "nonresponsive clone".equals(normedGSTerms.get(9)) ;
        assert "normal lymphokine".equals(normedGSTerms.get(10)) ;
    }

    @After
    public void tearDown() throws IOException {}
}
