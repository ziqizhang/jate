package uk.ac.shef.dcs.jate.v2.model;

/**
 * Created by zqz on 19/09/2015.
 */
public class TermInfo<T> {

    protected T info;

    public T getInfo(){
        return info;
    }

    public void setInfo(T info){
        this.info=info;
    }
}
