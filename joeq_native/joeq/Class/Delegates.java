package Clazz;

import Bootstrap.BootstrapCodeAddress;
import Bootstrap.BootstrapHeapAddress;
import Memory.CodeAddress;
import Memory.HeapAddress;
import Memory.StackAddress;
import Compil3r.Reference.x86.x86ReferenceCompiler;
import Compil3r.Reference.x86.x86ReferenceLinker;
import ClassLib.ClassLibInterface;
import Compil3r.Compil3rInterface;
import Main.jq;

class Delegates implements jq_ClassFileConstants {
    static class Field implements jq_Field.Delegate {
	public final boolean isCodeAddressType(jq_Field f) {
	    return f.getType() == CodeAddress._class ||
		f.getType() == BootstrapCodeAddress._class;
	}
	public final boolean isHeapAddressType(jq_Field f) {
	    return f.getType() == HeapAddress._class ||
		f.getType() == BootstrapHeapAddress._class;
	}
	public final boolean isStackAddressType(jq_Field f) {
	    return f.getType() == StackAddress._class;
	}
    }
    static class Method implements jq_Method.Delegate {
	public final jq_CompiledCode compile_stub (jq_Method m) {
	    return x86ReferenceCompiler.generate_compile_stub(m);
	}
	public final jq_CompiledCode compile (jq_Method m) {
	    jq_CompiledCode default_compiled_version;
            //System.out.println("Compiling: "+m);
            if (m.isNative() && m.getBytecode() == null) {
                System.out.println("Unimplemented native method! "+m);
                if (x86ReferenceLinker._nativeMethodError.getState() < STATE_CLSINITIALIZED) {
                    jq_Class k = x86ReferenceLinker._class;
                    k.verify(); //k.prepare();
                    if (x86ReferenceLinker._nativeMethodError.getState() != STATE_PREPARED)
                        x86ReferenceLinker._nativeMethodError.prepare();
                    default_compiled_version = x86ReferenceLinker._nativeMethodError.compile();
                    //if (k != getDeclaringClass() && getDeclaringClass().getSuperclass() != null) { k.cls_initialize(); }
                } else {
                    default_compiled_version = x86ReferenceLinker._nativeMethodError.getDefaultCompiledVersion();
                }
            } else if (m.isAbstract()) {
                if (x86ReferenceLinker._abstractMethodError.getState() < STATE_CLSINITIALIZED) {
                    jq_Class k = x86ReferenceLinker._class;
                    k.verify(); //k.prepare();
                    //default_compiled_version = x86ReferenceLinker._abstractMethodError.getDefaultCompiledVersion();
                    if (x86ReferenceLinker._abstractMethodError.getState() != STATE_PREPARED)
                        x86ReferenceLinker._abstractMethodError.prepare();
                    default_compiled_version = x86ReferenceLinker._abstractMethodError.compile();
                    //if (k != getDeclaringClass() && getDeclaringClass().getSuperclass() != null) { k.cls_initialize(); }
                } else {
                    default_compiled_version = x86ReferenceLinker._abstractMethodError.getDefaultCompiledVersion();
                }
            } else {
                Compil3rInterface c;
                if (true)
                    c = new x86ReferenceCompiler(m);
                //else
                //    c = new x86OpenJITCompiler(m);
                default_compiled_version = c.compile();
                if (jq.RunningNative)
                    default_compiled_version.patchDirectBindCalls();
            }
	    return default_compiled_version;
	}
    }
}