package uk.ac.shef.dcs.jate.deprecated.core.feature.indexer;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;


/**
 * Created by zqz on 11/12/2014.
 */
abstract class Index<T> implements Serializable{
    protected List<T> objects;

    protected Index(){
        objects=new LinkedList<T>();
    }

    /**
     * @param object
     * @return the id or -1 if not indexed
     */
    protected synchronized int retrieveId(T object){
        return objects.indexOf(object);
    }

    /**
     * Given an id, retrieve the candidate term's canonical form
     *
     * @param id
     * @return the object or null if not indexed
     */
    protected synchronized T retrieveObject(int id){
        return objects.get(id);
    }

    protected synchronized int indexObject(T object){
        int index = objects.indexOf(object);
        //if (index == null) _termIdMap.put(term, index = _termCounter++);
        if (index == -1) {
            int nextId=objects.size();
            objects.add(object);
        }
        return index;
    }


}
