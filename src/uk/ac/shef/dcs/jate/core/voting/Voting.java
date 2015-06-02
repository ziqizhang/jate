package uk.ac.shef.dcs.jate.core.voting;

import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.model.Term;

import java.util.*;
import java.io.*;



public class Voting {

	/**
	 * Load standard jate term recognition output into a list of ranked terms. The standard output has one term on a line,
	 * in the format of:
	 * <p>
	 * lemma |variantX |variantY        confidence
	 * </p>
	 *
	 * @param path
	 * @return
	 */
	public List<Term> load(String path) {
		List<Term> result = new ArrayList<Term>();
		try {
			final BufferedReader reader = new BufferedReader(new FileReader(path));
			String line;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (line.equals("") || line.startsWith("//")) continue;
				String[] elements = line.split("\\\\|");
				//if (elements.length < 2) continue;
				result.add(new Term(elements[0],0));
				//result.add(new Term(elements[0], Double.valueOf(elements[1])));
			}
			reader.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		Collections.sort(result);

		return result;
	}

	/**
	 * Produce the voting result
	 *
	 * @param outputs
	 * @return
	 * @throws uk.ac.shef.dcs.jate.JATEException
	 */
	public List<Term> calculate(WeightedOutput... outputs) throws JATEException {
/*		for (WeightedOutput output : outputs) {
			List<Term> test = outputs[1].getOutputList();
			test.removeAll(output.getOutputList());
			if (test.size() != 0)
				throw new TEXTractorException("Data inconsistency! The results do not share identical concepts.");
		}*/

		List<Term> result = new ArrayList<Term>();
		int count=0;
		for (Term t : outputs[0].getOutputList()) {
			double score = 0.0;
			for (WeightedOutput output : outputs) {
				score += output.getWeight() * (output.getOutputList().size() - output.getOutputList().indexOf(t));
			}
			result.add(new Term(t.getConcept(), score));
			count++;
			if(count%1000==0)System.out.println(t.getConcept()+count);
		}

		Collections.sort(result);
		return result;
	}

	/**
	 * Output the result, one term on a line, in the format of
	 * <p>
	 * lemma       confidence
	 * </P>
	 * @param target
	 * @param filename
	 */
	public void output(List<Term> target, String filename) {
		Term[] result = target.toArray(new Term[0]);
		Arrays.sort(result);

		try {
			PrintWriter pw = new PrintWriter(new FileWriter(new File(filename)));
			for (Term c : result) {
				pw.println(c.getConcept() + "\t\t\t" + c.getConfidence());
			}
			pw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

/*

	public static void main(String[] args) {
		try {
			Voting v = new Voting();


			List<Term> tfidf = v.load("X:\\Work_stuff\\project_remote\\JTEC_SDK\\JTEC\\TEXTractor_v2.1\\test\\animal/TfIdf_ATR_ALGORITHM.txt");
			List<Term> weridness =v.load("X:\\Work_stuff\\project_remote\\JTEC_SDK\\JTEC\\TEXTractor_v2.1\\test\\animal/Weirdness_ATR_ALGORITHM.txt");
			List<Term> glossex = v.load("X:\\Work_stuff\\project_remote\\JTEC_SDK\\JTEC\\TEXTractor_v2.1\\test\\animal/IBM_GlossEx_ATR_ALGORITHM.txt");
			List<Term> termex = v.load("X:\\Work_stuff\\project_remote\\JTEC_SDK\\JTEC\\TEXTractor_v2.1\\test\\animal/TermEx_ATR_ALGORITHM.txt");
			List<Term> cvalue = v.load("X:\\Work_stuff\\project_remote\\JTEC_SDK\\JTEC\\TEXTractor_v2.1\\test\\animal/CValue_ALGORITHM.txt");

			List<Term> output = v.calculate(new WeightedOutput(tfidf,0.2),new WeightedOutput(weridness,0.2),
					new WeightedOutput(glossex,0.2), new WeightedOutput(termex, 0.2), new WeightedOutput(cvalue,0.2));
			v.output(output, "voted.txt");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}*/
}
