package uk.ac.shef.dcs.jate.deprecated.core.feature;

import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uk.ac.shef.dcs.jate.deprecated.JATEException;
import uk.ac.shef.dcs.jate.deprecated.core.feature.indexer.GlobalIndex;

/**
 * A specific type of feature builder that builds an instance of FeatureTermNest from a GlobalIndex.
 * Counting of term nested frequency is based on <b>canonical forms</b>.
 *
 *  <br>Also credits to <b>pmclachlan@gmail.com</b> for revision for performance tweak </br>
 *
 * @author <a href="mailto:z.zhang@dcs.shef.ac.uk">Ziqi Zhang</a>
 */


public class FeatureBuilderTermNest extends AbstractFeatureBuilder {

    private static Logger _logger = Logger.getLogger(FeatureBuilderTermNest.class.getName());

    /**
     * Default constructor
     */
    public FeatureBuilderTermNest() {
        super(null, null, null);
    }

    /**
     * Build an instance of FeatureTermNest from an instance of GlobalIndexMem
     *
     * @param index
     * @return
     * @throws uk.ac.shef.dcs.jate.deprecated.JATEException
     *
     */
    public FeatureTermNest build(GlobalIndex index) throws JATEException {
        FeatureTermNest _feature = new FeatureTermNest(index);
        if (index.getTermsCanonical().size() == 0 || index.getDocuments().size() == 0) throw new
                JATEException("No resource indexed!");

        _logger.info("About to build FeatureTermNest...");
        int counter = 0;
        for (String np : index.getTermsCanonical()) {
          
        	/*for (String anp : index.getTermsCanonical()) {
                   if (anp.length() <= np.length()) continue;
                   if (anp.indexOf(" " + np) != -1 || anp.indexOf(np + " ") != -1) //e.g., np=male, anp=the male
                       _feature.termNestIn(np, anp);
               }
               counter++;

            Expensive string operations pulled out for performance of the inner loop
            final String spacePrefixed = " " + np;
            final String spaceSuffixed = np + " "; */
        	
        	
        	/*modified code begins */
        	
        	Set<String> variants_np = index.retrieveVariantsOfTermCanonical(np);
        	variants_np.add(np);
        	
        	 StringBuilder pattern = new StringBuilder("\\b(");
             Iterator<String> it = variants_np.iterator();
             while(it.hasNext())
             {
             	pattern.append(it.next()+ "|");                        		
             }
             
             pattern.deleteCharAt(pattern.length()-1);
             pattern.append(")\\b");
             
             Pattern p = Pattern.compile(pattern.toString());
        	 
        	 
        	/*modified code ends*/ 
            
        	
        	for (String anp : index.getTermsCanonical()) {
                if (anp.length() <= np.length()) continue;
                //modified code begins              
               
                Matcher m = p.matcher(anp);

                if (m.find()) {
                	_feature.termNestIn(np, anp);                	
                }
                
        	}
            counter++;


            if (counter % 500 == 0) _logger.info("Batch done" + counter + " end: " + np);
        }
        return _feature;
    }
}

               
                
                /*              
                if(anp.contains(" "))
                { 
                	String[] anp_split = anp.split(" ");
                    for(String s : anp_split)
                    {
                    	if(s.equals(np))
                    	{
                    		_feature.termNestIn(np, anp);
                    		break;
                    	}                    	
                    }
                }
                */
                
                /* if np = "table content" and anp = "remote table content"...this nesting is not identified by the above code*/
              
                /*
                if(anp.indexOf(np)==-1)
                	continue;
                else 
                {
                	int startIdx = 0;
                
                	for(;startIdx<anp.length() && (anp.length()-startIdx) >= np.length();)
                	{
                		
                		if(anp.indexOf(np, startIdx)==-1)
                			break;
                		if((anp.indexOf(np, startIdx)==0 || anp.charAt(anp.indexOf(np, startIdx)-1)==' ') && (anp.indexOf(np, startIdx)+np.length()==anp.length() || anp.charAt(anp.indexOf(np, startIdx)+np.length()) == ' ' ))
                		{
                			_feature.termNestIn(np, anp);
                			break;
                		}
                		else
                		{
                			startIdx = anp.indexOf(np, startIdx)+np.length();
                			continue;
                		}
                	}
                
                }*/
               // if (anp.indexOf(spacePrefixed) != -1 || anp.indexOf(spaceSuffixed) != -1) //e.g., np=male, anp=the male
               //     _feature.termNestIn(np, anp);
                //modified code ends
     /*       }
            counter++;


            if (counter % 500 == 0) _logger.info("Batch done" + counter + " end: " + np);
        }
        return _feature;
    }
}
*/