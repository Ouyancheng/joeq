/*
 * jq_Member.java
 *
 * Created on December 19, 2000, 11:29 AM
 *
 * @author  jwhaley
 * @version 
*/

package Clazz;

import java.lang.reflect.Member;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Iterator;
import java.io.DataInput;
import java.io.IOException;

import jq;
import Run_Time.StackWalker;
import Run_Time.TypeCheck;
import Run_Time.Unsafe;
import UTF.Utf8;

public abstract class jq_Member implements jq_ClassFileConstants {

    protected final void chkState(int s) {
        if (getState() >= s) return;
        jq.UNREACHABLE(this+" actual state: "+getState()+" expected state: "+s);
    }
    public final int getState() { return state; }
    
    //  Always available
    protected byte state;
    protected final jq_Class clazz;
    protected final jq_NameAndDesc nd;
    
    // Available after loading
    protected char access_flags;
    protected Map attributes;
    
    protected final Member member_object;  // pointer to our associated java.lang.reflect.Member object
    
    protected jq_Member(jq_Class clazz, jq_NameAndDesc nd) {
        jq.assert(clazz != null);
        jq.assert(nd != null);
        this.clazz = clazz; this.nd = nd;
        Member c = null;
        if (!jq.Bootstrapping) {
            if (this instanceof jq_Field) {
                c = ClassLib.sun13.java.lang.reflect.Field.createNewField(ClassLib.sun13.java.lang.reflect.Field._class, (jq_Field)this);
            } else if (this instanceof jq_Initializer) {
                c = ClassLib.sun13.java.lang.reflect.Constructor.createNewConstructor(ClassLib.sun13.java.lang.reflect.Constructor._class, (jq_Initializer)this);
            } else {
                c = ClassLib.sun13.java.lang.reflect.Method.createNewMethod(ClassLib.sun13.java.lang.reflect.Method._class, (jq_Method)this);
            }
        }
        this.member_object = c;
    }

    public final Member getJavaLangReflectMemberObject() {
        jq.assert(!jq.Bootstrapping);
        return member_object;
    }
    
    public void load(char access_flags, DataInput in) 
    throws IOException, ClassFormatError {
        state = STATE_LOADING1;
        this.access_flags = access_flags;
        attributes = new HashMap();
        char n_attributes = (char)in.readUnsignedShort();
        for (int i=0; i<n_attributes; ++i) {
            char attribute_name_index = (char)in.readUnsignedShort();
            if (clazz.getCPtag(attribute_name_index) != CONSTANT_Utf8)
                throw new ClassFormatError("constant pool entry "+attribute_name_index+", referred to by attribute "+i+
                                           ", is wrong type tag (expected="+CONSTANT_Utf8+", actual="+clazz.getCPtag(attribute_name_index));
            Utf8 attribute_desc = clazz.getCPasUtf8(attribute_name_index);
            int attribute_length = in.readInt();
            // todo: maybe we only want to read in attributes we care about...
            byte[] attribute_data = new byte[attribute_length];
            in.readFully(attribute_data);
            attributes.put(attribute_desc, attribute_data);
        }
        state = STATE_LOADING2;
    }

    public void load(char access_flags, Map attributes) {
        this.access_flags = access_flags;
        this.attributes = attributes;
        state = STATE_LOADING2;
    }

    // Always available
    public final jq_Class getDeclaringClass() { return clazz; }
    public final jq_NameAndDesc getNameAndDesc() { return nd; }
    public final Utf8 getName() { return nd.getName(); }
    public final Utf8 getDesc() { return nd.getDesc(); }
    public abstract boolean needsDynamicLink(jq_Method method);
    
    // Available after loading
    public final byte[] getAttribute(Utf8 name) {
        chkState(STATE_LOADING2);
        return (byte[])attributes.get(name);
    }
    public final byte[] getAttribute(String name) {
        return getAttribute(Utf8.get(name));
    }
    public final Map getAttributes() {
        chkState(STATE_LOADING2);
        return attributes;
    }
    public final boolean checkAccessFlag(char f) {
        chkState(STATE_LOADING2);
        return (access_flags & f) != 0;
    }
    public final char getAccessFlags() { chkState(STATE_LOADING2); return access_flags; }
    public final boolean isPublic() { return checkAccessFlag(ACC_PUBLIC); }
    public final boolean isPrivate() { return checkAccessFlag(ACC_PRIVATE); }
    public final boolean isProtected() { return checkAccessFlag(ACC_PROTECTED); }
    public final boolean isFinal() { return checkAccessFlag(ACC_FINAL); }
    public final boolean isSynthetic() { return getAttribute("Synthetic") != null; }
    public final boolean isDeprecated() { return getAttribute("Deprecated") != null; }

    public void checkCallerAccess(int depth)
        throws java.lang.IllegalAccessException
    {
        jq_Class field_class = this.getDeclaringClass();
        if (this.isPublic() && field_class.isPublic()) {
            // completely public!
            return;
        }
        StackWalker sw = new StackWalker(0, Unsafe.EBP());
        while (--depth >= 0) sw.gotoNext();
        jq_CompiledCode cc = sw.getCode();
        if (cc != null) {
            jq_Class caller_class = cc.getMethod().getDeclaringClass();
            if (caller_class == field_class) {
                // same class! access allowed!
                return;
            }
            if (field_class.isPublic() || caller_class.isInSamePackage(field_class)) {
                if (this.isPublic()) {
                    // class is accessible and field is public!
                    return;
                }
                if (this.isProtected()) {
                    if (TypeCheck.isAssignable(caller_class, field_class)) {
                        // field is protected and field_class is supertype of caller_class!
                        return;
                    }
                }
                if (!this.isPrivate()) {
                    if (caller_class.isInSamePackage(field_class)) {
                        // field is package-private and field_class and caller_class are in the same package!
                        return;
                    }
                }
            }
        }
        throw new java.lang.IllegalAccessException();
    }
    
    // available after resolution
    public abstract boolean isStatic();
}
