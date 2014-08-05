package uk.ac.shef.dcs.oak.jate.model;

import java.util.HashSet;
import java.util.Set;
import java.util.Iterator;
import java.net.MalformedURLException;
import java.io.File;

/**
 * @author <a href="mailto:z.zhang@dcs.shef.ac.uk">Ziqi Zhang</a>
 */


public class CorpusImpl implements Corpus {

	protected Set<Document> _docs;

	public CorpusImpl(){
		_docs = new HashSet<Document>();
	}

	public Iterator<Document> iterator(){
		return _docs.iterator();
	}

	public CorpusImpl(String path){
		_docs=new HashSet<Document>();
		File targetFolder = new File(path);
		
		File[] files = targetFolder.listFiles();
		for (File f : files) {
			try {
				this.add(new DocumentImpl(f.toURI().toURL()));
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}
	}

   public boolean add(Document document){
	   return _docs.add(document);
   }

   public boolean contains(Document document){
	   return _docs.contains(document);
   }

   public int size() {
      return _docs.size();
   }

   public boolean remove(Document doc){
	   return _docs.remove(doc);
   }

}
