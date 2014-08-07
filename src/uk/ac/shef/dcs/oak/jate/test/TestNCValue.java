/**
Test file for NC-Value Algorithm.
 */

package uk.ac.shef.dcs.oak.jate.test;

import net.didion.jwnl.JWNLException;
import uk.ac.shef.dcs.oak.jate.JATEException;
import uk.ac.shef.dcs.oak.jate.core.algorithm.NCValueAlgorithm;
import uk.ac.shef.dcs.oak.jate.core.algorithm.NCValueFeatureWrapper;
import uk.ac.shef.dcs.oak.jate.JATEProperties;
import uk.ac.shef.dcs.oak.jate.core.context.ContextExtraction;
import java.io.IOException;
import java.util.Date;
import java.util.Map;

public class TestNCValue {

	public static void main(String[] args) throws IOException, JATEException, JWNLException {
		
			System.out.println("Started "+ TestNCValue.class+"at: " + new Date() + "... For detailed progress see log file jate.log.");
			String[] argument = new String[]{args[0], args[1]};
			
			/* Call the C-Value Algorithm first to get the resultant candidates with CValue score. */
			AlgorithmTester tester = TestCValue.CValueTester(argument);
			
			/* Get the list of context words by making use of the 'top candidates' of CValue resultant term candidates. */
			ContextExtraction contextExtract = new ContextExtraction(tester);
			Map<String, Double> contextWords = contextExtract.Extract(args[0]);
			
			/* Register the NC-Value Algorithm and get it executed. */
			AlgorithmTester NCTester = new AlgorithmTester();
			NCTester.registerAlgorithm(new NCValueAlgorithm(), new NCValueFeatureWrapper(contextWords, tester));
			NCTester.execute(tester.getIndex(), JATEProperties.getInstance().getResultPath());
			System.out.println("Ended at: " + new Date());		
		}	 
	}


