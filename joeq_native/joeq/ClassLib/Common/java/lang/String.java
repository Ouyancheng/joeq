/*
 * String.java
 *
 * Created on January 29, 2001, 10:31 AM
 *
 * @author  John Whaley
 * @version 
 */

package ClassLib.Common.java.lang;

import UTF.Utf8;

public abstract class String {

    public java.lang.String intern() {
        // note: this relies on the caching of String objects in Utf8 class
        java.lang.Object o = this;
        return Utf8.get((java.lang.String)o).toString();
    }

}