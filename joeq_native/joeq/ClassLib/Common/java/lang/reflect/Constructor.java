/*
 * Constructor.java
 *
 * Created on April 14, 2001, 3:16 PM
 *
 */

package ClassLib.Common.java.lang.reflect;

import ClassLib.ClassLibInterface;
import ClassLib.Common.ClassUtils;
import Clazz.jq_Class;
import Clazz.jq_Initializer;
import Clazz.jq_NameAndDesc;
import Clazz.jq_Type;
import Main.jq;
import Run_Time.Reflection;
import UTF.Utf8;
import Util.Assert;

/*
 * @author  John Whaley
 * @version $Id$
 */
public class Constructor extends AccessibleObject {

    // additional instance field.
    public final jq_Initializer jq_init;
    
    private java.lang.Class clazz;
    private java.lang.Class[] parameterTypes;
    private java.lang.Class[] exceptionTypes;
    private int modifiers;
    private int slot;
    
    private Constructor() {
        Assert.UNREACHABLE();
        this.jq_init = null;
    }
    
    private Constructor(jq_Initializer i) {
        this.jq_init = i;
    }
    private Constructor(java.lang.Class clazz,
                        java.lang.Class[] parameterTypes,
                        java.lang.Class[] exceptionTypes,
                        int modifiers,
                        int slot) {
        this.clazz = clazz;
        this.parameterTypes = parameterTypes;
        this.exceptionTypes = exceptionTypes;
        this.modifiers = modifiers;
        this.slot = slot;
        jq_Class k = (jq_Class) ClassLibInterface.DEFAULT.getJQType(clazz);
        
        StringBuffer desc = new StringBuffer();
        desc.append('(');
        for (int i=0; i<parameterTypes.length; ++i) {
            desc.append(Reflection.getJQType(parameterTypes[i]).getDesc().toString());
        }
        desc.append(")V");
        jq_NameAndDesc nd = new jq_NameAndDesc(Utf8.get("<init>"), Utf8.get(desc.toString()));
        nd = ClassLib.ClassLibInterface.convertClassLibNameAndDesc(k, nd);
        jq_Initializer init = (jq_Initializer) k.getDeclaredMember(nd);
        if (init == null) {
            init = (jq_Initializer) k.getOrCreateInstanceMethod(nd);
        }
        this.jq_init = init;
    }
    
    public java.lang.Object newInstance(java.lang.Object[] initargs)
        throws java.lang.InstantiationException, java.lang.IllegalAccessException,
               java.lang.IllegalArgumentException, java.lang.reflect.InvocationTargetException
    {
        jq_Initializer jq_i = this.jq_init;
        jq_Class k = jq_i.getDeclaringClass();
        if (k.isAbstract()) throw new InstantiationException();
        if (!this.isAccessible()) ClassUtils.checkCallerAccess(jq_i, 2);
        jq_Type[] argtypes = jq_i.getParamTypes();
        int nargs = initargs == null ? 0 : initargs.length;
        if (nargs != argtypes.length-1)
            throw new java.lang.IllegalArgumentException("Constructor takes "+(argtypes.length-1)+" arguments, but "+nargs+" arguments passed in");
        Object o = k.newInstance();
        Reflection.invoke(jq_i, o, initargs);
        return o;
    }
    
    // additional methods.
    // ONLY TO BE CALLED BY jq_Member CONSTRUCTOR!!!
    public static java.lang.reflect.Constructor createNewConstructor(jq_Initializer jq_init) {
        Object o = new Constructor(jq_init);
        return (java.lang.reflect.Constructor)o;
    }
    
    public static void initNewConstructor(Constructor o, jq_Initializer jq_init) {
        if (!jq.RunningNative) return;
        Assert._assert(jq_init == o.jq_init);
        java.lang.Class clazz = jq_init.getDeclaringClass().getJavaLangClassObject();
        o.clazz = clazz;
        jq_Type[] paramTypes = jq_init.getParamTypes();
        java.lang.Class[] parameterTypes = new java.lang.Class[paramTypes.length-1];
        for (int i=1; i<paramTypes.length; ++i) {
            parameterTypes[i-1] = Reflection.getJDKType(paramTypes[i]);
        }
        o.parameterTypes = parameterTypes;
        // TODO: exception types
        java.lang.Class[] exceptionTypes = new java.lang.Class[0];
        o.exceptionTypes = exceptionTypes;
        int modifiers = jq_init.getAccessFlags();
        o.modifiers = modifiers;
    }
}
