/*
 * Finalizer.java
 *
 * Created on February 26, 2001, 9:01 PM
 *
 */

package ClassLib.Common.java.lang.ref;

import Clazz.jq_InstanceMethod;
import Clazz.jq_NameAndDesc;
import Clazz.jq_Reference;
import Run_Time.Reflection;
import UTF.Utf8;
import Util.Assert;

/*
 * @author  John Whaley
 * @version $Id$
 */
public abstract class Finalizer {

    public static native void runFinalization();
    public static native void runAllFinalizers();
    
    static void invokeFinalizeMethod(Object o) throws Throwable {
        jq_Reference c = jq_Reference.getTypeOf(o);
        jq_InstanceMethod m = c.getVirtualMethod(new jq_NameAndDesc(Utf8.get("finalize"), Utf8.get("()V")));
        Assert._assert(m != null);
        Reflection.invokeinstance_V(m, o);
    }
}
