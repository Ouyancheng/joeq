/*
 * Shutdown.java
 *
 * Created on February 26, 2001, 8:56 PM
 *
 * @author  John Whaley
 * @version 
 */

package ClassLib.Common.java.lang;

import Bootstrap.PrimordialClassLoader;
import Clazz.jq_Class;
import Run_Time.Reflection;
import Run_Time.SystemInterface;
import Run_Time.Unsafe;
import jq;

abstract class Shutdown {
    
    static void halt(int status) {
        SystemInterface.die(status);
        jq.UNREACHABLE();
    }
    private static void runAllFinalizers() {
        try {
            ClassLib.Common.java.lang.ref.Finalizer.runAllFinalizers();
        } catch (java.lang.Throwable x) {
        }
    }
    
}