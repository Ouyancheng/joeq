/*
 * jq_Reference.java
 *
 * Created on December 19, 2000, 8:38 AM
 *
 */

package Clazz;

import Allocator.ObjectLayout;
import Bootstrap.PrimordialClassLoader;
import Compil3r.Quad.AndersenInterface.AndersenReference;
import Main.jq;
import Memory.HeapAddress;
import Run_Time.Reflection;
import UTF.Utf8;

/*
 * @author  John Whaley
 * @version $Id$
 */
public abstract class jq_Reference extends jq_Type implements jq_ClassFileConstants, AndersenReference {

    public static final jq_Reference getTypeOf(Object o) {
        if (!jq.RunningNative) return Reflection.getTypeOf(o);
        return ((HeapAddress)HeapAddress.addressOf(o).offset(ObjectLayout.VTABLE_OFFSET).peek().peek()).asReferenceType();
    }

    public final int getState() { return state; }
    public final boolean isLoaded() { return state >= STATE_LOADED; }
    public final boolean isVerified() { return state >= STATE_VERIFIED; }
    public final boolean isPrepared() { return state >= STATE_PREPARED; }
    public final boolean isSFInitialized() { return state >= STATE_SFINITIALIZED; }
    public final boolean isCompiled() { return state >= STATE_COMPILED; }
    public final boolean isClsInitRunning() { return state >= STATE_CLSINITRUNNING; }
    public final boolean isClsInitialized() { return state >= STATE_CLSINITIALIZED; }
    
    public final boolean isPrimitiveType() { return false; }
    public final boolean isIntLike() { return false; }
    
    public final ClassLoader getClassLoader() { return class_loader; }
    public final int getReferenceSize() { return 4; }
    public final Object getVTable() { chkState(STATE_PREPARED); return vtable; }
    
    public abstract String getJDKName();
    public abstract jq_Class[] getInterfaces();
    public abstract jq_Class getInterface(Utf8 desc);
    public abstract boolean implementsInterface(jq_Class k);

    public abstract jq_InstanceMethod getVirtualMethod(jq_NameAndDesc nd);
    
    public abstract jq_Reference getDirectPrimarySupertype();
    
    public boolean isInstance(Object o) {
        if (o == null) return false;
        jq_Reference that = jq_Reference.getTypeOf(o);
        return that.isSubtypeOf(this);
    }
    
    public static final boolean TRACE = false;
    
    public final void chkState(byte s) {
        if (state >= s) return;
        jq.UNREACHABLE(this+" actual state: "+state+" expected state: "+s);
    }

    protected jq_Reference(Utf8 desc, ClassLoader class_loader) {
        super(desc, class_loader);
        jq.Assert(class_loader != null);
        this.class_loader = class_loader;
    }
    protected final ClassLoader class_loader;
    protected int/*byte*/ state; // use an 'int' so we can do cas4 on it
    protected Object vtable;

    public static class jq_NullType extends jq_Reference {
        private jq_NullType() { super(Utf8.get("L&NULL;"), PrimordialClassLoader.loader); }
        public boolean isAddressType() { return false; }
        public String getJDKName() { return desc.toString(); }
        public String getJDKDesc() { return getJDKName(); }
        public jq_Class[] getInterfaces() { jq.UNREACHABLE(); return null; }
        public jq_Class getInterface(Utf8 desc) { jq.UNREACHABLE(); return null; }
        public boolean implementsInterface(jq_Class k) { jq.UNREACHABLE(); return false; }
        public jq_InstanceMethod getVirtualMethod(jq_NameAndDesc nd) { jq.UNREACHABLE(); return null; }
        public String getName() { jq.UNREACHABLE(); return null; }
        public String shortName() { return "NULL_TYPE"; }
        public boolean isClassType() { jq.UNREACHABLE(); return false; }
        public boolean isArrayType() { jq.UNREACHABLE(); return false; }
        public boolean isFinal() { jq.UNREACHABLE(); return false; }
        public boolean isInstance(Object o) { return o == null; }
        public int getDepth() { jq.UNREACHABLE(); return 0; }
        public jq_Reference getDirectPrimarySupertype() { jq.UNREACHABLE(); return null; }
        public void load() { jq.UNREACHABLE(); }
        public void verify() { jq.UNREACHABLE(); }
        public void prepare() { jq.UNREACHABLE(); }
        public void sf_initialize() { jq.UNREACHABLE(); }
        public void compile() { jq.UNREACHABLE(); }
        public void cls_initialize() { jq.UNREACHABLE(); }
        public String toString() { return "NULL_TYPE"; }
        public static final jq_NullType NULL_TYPE = new jq_NullType();
    }
    
    public static final jq_Class _class;
    public static final jq_InstanceField _vtable;
    public static /*final*/ jq_InstanceField _state; // set after PrimordialClassLoader finishes initialization
    static {
        _class = (jq_Class)PrimordialClassLoader.loader.getOrCreateBSType("LClazz/jq_Reference;");
        _vtable = _class.getOrCreateInstanceField("vtable", "Ljava/lang/Object;");
        // primitive types have not yet been created!
        _state = _class.getOrCreateInstanceField("state", "I");
    }
}
