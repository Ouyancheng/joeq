/*
 * StackWalker.java
 *
 * Created on January 11, 2001, 10:34 PM
 *
 * @author  jwhaley
 * @version 
 */

package Run_Time;

import Allocator.CodeAllocator;
import Clazz.jq_CompiledCode;
import Run_Time.Unsafe;
import Run_Time.SystemInterface;
import jq;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class StackWalker implements Iterator {

    public static /*final*/ boolean TRACE = false;
    
    int/*Address*/ ip, fp;
    
    public int/*Address*/ getFP() { return fp; }
    public int/*Address*/ getIP() { return ip; }
    public jq_CompiledCode getCode() { return CodeAllocator.getCodeContaining(ip); }

    public StackWalker(int/*Address*/ ip, int/*Address*/ fp) {
        this.ip = ip;
        this.fp = fp;
        if (TRACE) SystemInterface.debugmsg("StackWalker init: fp="+jq.hex8(fp)+" ip="+jq.hex8(ip)+" "+getCode());
    }
    
    public boolean hasNext() {
        int/*CodeAddress*/ addr = Unsafe.peek(fp+4);
        if (TRACE) SystemInterface.debugmsg("StackWalker hasnext: next ip="+jq.hex8(addr)+" "+CodeAllocator.getCodeContaining(addr));
        if (TRACE) SystemInterface.debugmsg("Min code addr="+jq.hex8(CodeAllocator.getLowAddress())+" max code addr="+jq.hex8(CodeAllocator.getHighAddress()));
        return (addr >= CodeAllocator.getLowAddress()) &&
               (addr < CodeAllocator.getHighAddress());
    }
    
    public Object next() throws NoSuchElementException {
        ip = Unsafe.peek(fp+4);
        fp = Unsafe.peek(fp);
        if (TRACE) SystemInterface.debugmsg("StackWalker next: fp="+jq.hex8(fp)+" ip="+jq.hex8(ip)+" "+getCode());
        return new CodeAllocator.InstructionPointer(ip);
    }
    
    public void remove() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }
}
