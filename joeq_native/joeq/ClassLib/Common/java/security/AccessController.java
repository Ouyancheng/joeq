/*
 * AccessController.java
 *
 * Created on January 29, 2001, 1:30 PM
 *
 * @author  John Whaley
 * @version 
 */

package ClassLib.Common.java.security;

import Bootstrap.PrimordialClassLoader;
import Clazz.jq_Class;

abstract class AccessController {

    public static Object doPrivileged(java.security.PrivilegedAction action) {
        // TODO: set privilege level
        return action.run();
    }
    public static Object doPrivileged(java.security.PrivilegedAction action,
                                      java.security.AccessControlContext context) {
        // TODO: set privilege level
        return action.run();
    }
    public static Object doPrivileged(java.security.PrivilegedExceptionAction action)
        throws java.security.PrivilegedActionException {
        // TODO: set privilege level
        try {
            return action.run();
        } catch (RuntimeException x) {
            throw x;
        } catch (Exception x) {
            throw new java.security.PrivilegedActionException(x);
        }
    }
    public static Object doPrivileged(java.security.PrivilegedExceptionAction action,
                                      java.security.AccessControlContext context)
        throws java.security.PrivilegedActionException {
        // TODO: set privilege level
        try {
            return action.run();
        } catch (RuntimeException x) {
            throw x;
        } catch (Exception x) {
            throw new java.security.PrivilegedActionException(x);
        }
    }
    private static java.security.AccessControlContext getStackAccessControlContext() {
        // TODO
        return null;
    }
    static java.security.AccessControlContext getInheritedAccessControlContext() {
        // TODO
        return null;
    }

    //public static final jq_Class _class = (jq_Class)PrimordialClassLoader.loader.getOrCreateBSType("Ljava/security/AccessController;");
}