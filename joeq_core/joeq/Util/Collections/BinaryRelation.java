package Util.Collections;

/**
 * <code>BinaryRelation</code> represents a predicate on a 2-tuple.
 * It maps a set of pairs to a boolean.  Often
 * <code>BinaryRelation</code>s will be constrained in terms of what
 * types of arguments they accept; take care in documenting what
 * requirements your <code>BinaryRelation</code> needs.
 * Examples of <code>BinaryRelation</code>s include 
 * "less than" ( &lt; ) and "equals" ( == ).
 * @author  Felix S. Klock II <pnkfelix@mit.edu>
 * @version $Id$
 */
public interface BinaryRelation/*<A,B>*/ {
    
    /** Checks if this relation holds for a given pair.
    <BR> <B>requires:</B> (<code>a</code>, <code>b</code>) falls
         in the domain of <code>this</code>.
    <BR> <B>effects:</B> Returns <code>True</code> if this
         relation holds for (<code>a</code> , <code>b</code>).
         Else returns <code>False</code>.  
    */
    public boolean contains(Object/*A*/ a, Object/*B*/ b);
    
}
