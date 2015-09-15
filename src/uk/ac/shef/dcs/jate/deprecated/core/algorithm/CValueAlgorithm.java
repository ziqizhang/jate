package uk.ac.shef.dcs.jate.deprecated.core.algorithm;
import uk.ac.shef.dcs.jate.deprecated.JATEException;
import uk.ac.shef.dcs.jate.deprecated.model.Term;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;

/**
 * An implementation of the CValue term recognition algorithm. See Frantzi et. al 2000, <i>
 * Automatic recognition of multi-word terms:. the C-value/NC-value method</i>
 * 
 * @author <a href="mailto:z.zhang@dcs.shef.ac.uk">Ziqi Zhang</a>
 */
public class CValueAlgorithm implements Algorithm {

	/**
	 *
	 * @param store
	 * @return
	 * @throws uk.ac.shef.dcs.jate.deprecated.JATEException
	 */
	public Term[] execute(AbstractFeatureWrapper store) throws JATEException {
		if (!(store instanceof CValueFeatureWrapper)) throw new JATEException("" +
				"Required: CValueFeatureWrapper");
		CValueFeatureWrapper cFeatureStore = (CValueFeatureWrapper) store;
		Set<Term> result = new HashSet<Term>();

		for(String s:cFeatureStore.getTerms()){
			double score;
			double log2a=Math.log((double)s.split(" ").length  + 0.1)/Math.log(2.0); //Anurag mods for log (a), log(a + 0.1)
			double freqa=(double)cFeatureStore.getTermFreq(s);
			int[] nest = cFeatureStore.getNestsOf(s);
			double pTa = (double)nest.length;
			double sumFreqb=0.0;
			
			/* code modification begins */
			
			for(Integer i:nest){
				if(i!=null)
				{
					sumFreqb+=(double)cFeatureStore.getTermFreq(i);		
					//String x= cFeatureStore.getTerm(i);
					int[] nest_i = cFeatureStore.getNestsOf(cFeatureStore.getTerm(i));
					List<Integer> i_already_considered = new ArrayList<Integer>();
					for(Integer j:nest_i)
					{
						if (j!=null && (!i_already_considered.contains(j)|| i_already_considered.size()==0))
						{
							sumFreqb-=(double)cFeatureStore.getTermFreq(j);
							int[] nest_j = cFeatureStore.getNestsOf(cFeatureStore.getTerm(j));
							for(int x1: nest_j)
								i_already_considered.add(x1);
						}
						
					}		
				}				
			}
			
		
			//Commented LOCs in original version
			
			/*
			for(Integer i:nest){
				if(i!=null) sumFreqb+=(double)cFeatureStore.getTermFreq(i);
			}
			*/
			
			/*code modification ends*/

			score = pTa==0?log2a*freqa:log2a*(freqa-(sumFreqb/pTa));

			result.add(new Term(s,score));
		}

		Term[] all = result.toArray(new Term[0]);
		Arrays.sort(all);
		return all;
	}
	
	

	public String toString(){
		return "CValue_ALGORITHM";
	}
}