/*
 * Created on Mar 28, 2003
 * 
 */
package Util.Collections;

import java.util.AbstractList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author John Whaley
 * @version $Id$
 */
public class HashWorklist extends AbstractList implements Worklist, Set {

    private final Set set;
    private final List list;

    public HashWorklist(SetFactory sf, ListFactory lf) {
        set = sf.makeSet();
        list = lf.makeList();
    }
    public HashWorklist(ListFactory lf) {
        this(Factories.hashSetFactory(), lf);
    }
    public HashWorklist(SetFactory sf) {
        this(sf, Factories.linkedListFactory());
    }
    public HashWorklist() {
        this(Factories.hashSetFactory());
    }

    /* (non-Javadoc)
     * @see Util.Collections.Worklist#push(java.lang.Object)
     */
    public void push(Object item) {
        add(item);
    }

    /* (non-Javadoc)
     * @see Util.Collections.Worklist#pull()
     */
    public Object pull() {
        return list.remove(0);
    }

    /* (non-Javadoc)
     * @see java.util.AbstractList#get(int)
     */
    public Object get(int index) {
        return list.get(index);
    }

    /* (non-Javadoc)
     * @see java.util.AbstractCollection#size()
     */
    public int size() {
        return list.size();
    }

    /* (non-Javadoc)
     * @see java.util.Collection#add(java.lang.Object)
     */
    public boolean add(Object item) {
        if (set.add(item))
            return list.add(item);
        else
            return false;
    }

    /* (non-Javadoc)
     * @see java.util.Collection#contains(java.lang.Object)
     */
    public boolean contains(Object o) {
        return set.contains(o);
    }

    public Set getVisitedSet() {
        if (false) {
            return Collections.unmodifiableSet(set);
        } else {
            return set;
        }
    }

}