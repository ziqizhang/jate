package uk.ac.shef.dcs.jate.deprecated.model;

/**
 * @author <a href="mailto:z.zhang@dcs.shef.ac.uk">Ziqi Zhang</a>
 */


public interface Corpus extends Iterable<Document>{

	boolean add (Document d);

	boolean remove(Document d);

	boolean contains(Document d);

   int size();

}
