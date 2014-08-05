package uk.ac.shef.dcs.oak.jate.util.counter;

import uk.ac.shef.dcs.oak.jate.model.CorpusImpl;
import uk.ac.shef.dcs.oak.jate.model.Document;
import uk.ac.shef.dcs.oak.jate.model.DocumentImpl;

import java.util.StringTokenizer;
import java.io.File;
import java.net.MalformedURLException;

/**
 * Count number of words in corpus/document/text.
 *
 * @author <a href="mailto:z.zhang@dcs.shef.ac.uk">Ziqi Zhang</a>
 */


public class WordCounter {

	public WordCounter(){
	}

	/**
	 * Count number of words in a corpus, delimited by a space character
	 * @param c
	 * @return
	 */
	public int countWords(final CorpusImpl c){
		int total=0;
		for(Document doc:c){
			StringTokenizer tokenizer = new StringTokenizer(doc.getContent().replaceAll("\\s+"," ")
					.replaceAll("\\s+"," ")," ");
			total+=tokenizer.countTokens();
		}
		return total;
	}

	/**
	 * Count number of words in a document, delimited by a space character
	 * @param d
	 * @return
	 */
	public int countWords(final Document d){
		
		
		/*StringTokenizer tokenizer = new StringTokenizer(d.getContent().replaceAll("\\s+"," ")
					.replaceAll("\\s+"," ")," ");
		*/
		//modified part
		
		StringTokenizer tokenizer = new StringTokenizer(d.getContent(), "\r\n|\n");
		
		return tokenizer.countTokens();			
	}

	/**
	 * Main testing method.
	 * @param args args[0] is the path to directory containing files
	 */
	public static void main(String[] args) {
		File targetFolder = new File(args[0]);
      File[] files = targetFolder.listFiles();
		CorpusImpl c =new CorpusImpl();
      for (File f : files) {
	      try {
		      c.add(new DocumentImpl(f.toURI().toURL()));
	      } catch (MalformedURLException e) {
		      e.printStackTrace();
	      }
      }//for

		System.out.println(new WordCounter().countWords(c));
	}
}
