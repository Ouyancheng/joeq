/*
 * Interface.java
 *
 * Created on December 12, 2001, 1:27 AM
 *
 * @author  John Whaley
 * @version 
 */

package ClassLib.sun13_win32;

import Bootstrap.PrimordialClassLoader;
import Clazz.*;
import Run_Time.Unsafe;
import Run_Time.Reflection;
import Allocator.DefaultCodeAllocator;
import jq;

public final class Interface extends ClassLib.ClassLibInterface {

    /** Creates new Interface */
    public Interface() {}

    public java.util.Iterator getImplementationClassDescs(UTF.Utf8 desc) {
        if (USE_JOEQ_CLASSLIB && desc.toString().startsWith("Ljava/")) {
            UTF.Utf8 u = UTF.Utf8.get("LClassLib/sun13_win32/"+desc.toString().substring(1));
            return new Util.SingletonIterator(u);
        }
        return Util.NullIterator.INSTANCE;
    }
    
    public java.util.Set bootstrapNullStaticFields() {
        java.util.Set nullStaticFields = new java.util.HashSet();
        nullStaticFields.add(Unsafe._remapper_object);
        nullStaticFields.add(PrimordialClassLoader.loader.getJavaLangSystem().getOrCreateStaticField("in", "Ljava/io/InputStream;"));
        nullStaticFields.add(PrimordialClassLoader.loader.getJavaLangSystem().getOrCreateStaticField("out", "Ljava/io/PrintStream;"));
        nullStaticFields.add(PrimordialClassLoader.loader.getJavaLangSystem().getOrCreateStaticField("err", "Ljava/io/PrintStream;"));
        nullStaticFields.add(PrimordialClassLoader.loader.getJavaLangSystem().getOrCreateStaticField("props", "Ljava/util/Properties;"));
        nullStaticFields.add(Reflection._obj_trav);
        nullStaticFields.add(DefaultCodeAllocator._default_allocator);
        nullStaticFields.add(PrimordialClassLoader.loader.getJavaLangClassLoader().getOrCreateStaticField("loadedLibraryNames", "Ljava/util/Vector;"));
        nullStaticFields.add(PrimordialClassLoader.loader.getJavaLangClassLoader().getOrCreateStaticField("systemNativeLibraries", "Ljava/util/Vector;"));
        nullStaticFields.add(PrimordialClassLoader.loader.getJavaLangClassLoader().getOrCreateStaticField("nativeLibraryContext", "Ljava/util/Stack;"));
        jq_Class jq_class = (jq_Class)PrimordialClassLoader.loader.getOrCreateBSType("Ljq;");
        nullStaticFields.add(jq_class.getOrCreateStaticField("on_vm_startup", "Ljava/util/List;"));
        return nullStaticFields;
    }
    
    public java.util.Set bootstrapNullInstanceFields() {
        java.util.Set nullInstanceFields = new java.util.HashSet();
        return nullInstanceFields;
    }
    
    public java.lang.Class createNewClass(Clazz.jq_Type f) {
        return ClassLib.Common.java.lang.Class.createNewClass(f);
    }
    
    public java.lang.reflect.Constructor createNewConstructor(Clazz.jq_Initializer f) {
        return ClassLib.Common.java.lang.reflect.Constructor.createNewConstructor(f);
    }
    
    public void initNewConstructor(java.lang.reflect.Constructor dis, Clazz.jq_Initializer f) {
        java.lang.Object o = dis;
        ClassLib.Common.java.lang.reflect.Constructor.initNewConstructor((ClassLib.Common.java.lang.reflect.Constructor)o, f);
    }
    
    public java.lang.reflect.Field createNewField(Clazz.jq_Field f) {
        return ClassLib.Common.java.lang.reflect.Field.createNewField(f);
    }
    
    public void initNewField(java.lang.reflect.Field dis, Clazz.jq_Field f) {
        java.lang.Object o = dis;
        ClassLib.Common.java.lang.reflect.Field.initNewField((ClassLib.Common.java.lang.reflect.Field)o, f);
    }
    
    public java.lang.reflect.Method createNewMethod(Clazz.jq_Method f) {
        return ClassLib.Common.java.lang.reflect.Method.createNewMethod(f);
    }
    
    public void initNewMethod(java.lang.reflect.Method dis, Clazz.jq_Method f) {
        java.lang.Object o = dis;
        ClassLib.Common.java.lang.reflect.Method.initNewMethod((ClassLib.Common.java.lang.reflect.Method)o, f);
    }
    
    public Clazz.jq_Field getJQField(java.lang.reflect.Field f) {
        java.lang.Object o = f;
        return ((ClassLib.Common.java.lang.reflect.Field)o).jq_field;
    }
    
    public Clazz.jq_Initializer getJQInitializer(java.lang.reflect.Constructor f) {
        java.lang.Object o = f;
        return ((ClassLib.Common.java.lang.reflect.Constructor)o).jq_init;
    }
    
    public Clazz.jq_Method getJQMethod(java.lang.reflect.Method f) {
        java.lang.Object o = f;
        return ((ClassLib.Common.java.lang.reflect.Method)o).jq_method;
    }
    
    public Scheduler.jq_Thread getJQThread(java.lang.Thread t) {
        if (jq.Bootstrapping) 
            return (Scheduler.jq_Thread)Reflection.obj_trav.getMappedInstanceFieldValue(t, java.lang.Thread.class, "jq_thread");
        java.lang.Object o = t;
        return ((ClassLib.Common.java.lang.Thread)o).jq_thread;
    }
    
    public Clazz.jq_Type getJQType(java.lang.Class k) {
        if (jq.Bootstrapping) 
            return Reflection.getJQType(k);
        java.lang.Object o = k;
        return ((ClassLib.Common.java.lang.Class)o).jq_type;
    }
    
    public Clazz.jq_Type getOrCreateType(java.lang.ClassLoader cl, UTF.Utf8 desc) {
        if (jq.Bootstrapping) {
            jq.assert(cl == PrimordialClassLoader.loader);
            return PrimordialClassLoader.loader.getOrCreateBSType(desc);
        }
        java.lang.Object o = cl;
        return ((ClassLib.Common.java.lang.ClassLoader)o).getOrCreateType(desc);
    }
    
    public void init_zipfile(java.util.zip.ZipFile dis, java.lang.String name) throws java.io.IOException {
        if (jq.Bootstrapping) {
            ClassLib.Common.java.util.zip.ZipFile.bootstrap_init(dis, name);
            return;
        }
        java.lang.Object o = dis;
        ((ClassLib.Common.java.util.zip.ZipFile)o).__init__(name);
    }
    
    public void init_inflater(java.util.zip.Inflater o, boolean nowrap) {
        //ClassLib.Common.java.util.zip.Inflater.__init__(o, nowrap);
    }
    
    public void initializeSystemClass() throws java.lang.Throwable {
        ClassLib.Common.java.lang.System.initializeSystemClass();
    }
    
    public void open(java.io.RandomAccessFile dis, java.lang.String name, boolean writeable) throws java.io.FileNotFoundException {
        java.lang.Object o = dis;
        ((ClassLib.Common.java.io.RandomAccessFile)o).open(name, writeable);
    }
    
    public void unloadType(java.lang.ClassLoader cl, Clazz.jq_Type t) {
        if (jq.Bootstrapping) {
            jq.assert(cl == PrimordialClassLoader.loader);
            PrimordialClassLoader.loader.unloadBSType(t);
            return;
        }
        java.lang.Object o = cl;
        ((ClassLib.Common.java.lang.ClassLoader)o).unloadType(t);
    }
    
}
