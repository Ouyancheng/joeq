// SimpleGCWorkQueue.java, created Aug 3, 2004 3:29:21 AM by joewhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package joeq.Allocator;

import joeq.Memory.Address;
import joeq.Memory.HeapAddress;
import joeq.Runtime.SystemInterface;
import joeq.Util.Assert;

/**
 * SimpleGCWorkQueue
 * 
 * @author John Whaley
 * @version $Id$
 */
public class SimpleGCWorkQueue {
    
    public static int WORKQUEUE_SIZE = 65536;
    
    Address queueStart, queueEnd;
    Address blockStart, blockEnd;
    
    /**
     * 
     */
    public SimpleGCWorkQueue() {
        super();
    }
    
    public void free() {
        if (!blockStart.isNull()) {
            SystemInterface.sysfree(blockStart);
            blockStart = blockEnd = queueStart = queueEnd = HeapAddress.getNull();
        }
    }
    
    public void growQueue(int newSize) {
        // todo: use realloc here.
        Address new_queue = SystemInterface.syscalloc(newSize);
        if (new_queue.isNull())
            HeapAllocator.outOfMemory();
        int size = queueEnd.difference(queueStart);
        if (size > 0) {
            Assert._assert(newSize > size);
            SystemInterface.mem_cpy(new_queue, queueStart, size);
        } else {
            int size2 = blockEnd.difference(queueStart);
            SystemInterface.mem_cpy(new_queue, queueStart, size2);
            int size3 = queueEnd.difference(blockStart);
            SystemInterface.mem_cpy(new_queue.offset(size2), blockStart, size3);
            size = size2 + size3;
        }
        SystemInterface.sysfree(blockStart);
        queueStart = blockStart = new_queue;
        blockEnd = blockStart.offset(newSize);
        queueEnd = queueStart.offset(size);
    }
    
    public int size() {
        int size = queueEnd.difference(queueStart);
        if (size < 0) {
            size = blockEnd.difference(queueStart) +
                   queueEnd.difference(blockStart);
        }
        return size;
    }
    
    public int space() {
        int size = queueEnd.difference(queueStart);
        if (size < 0) {
            return -size;
        } else {
            return blockEnd.difference(queueEnd) +
                   queueStart.difference(blockStart);
        }
    }
    
    public boolean addToQueue(Object o, boolean b) {
        HeapAddress a = HeapAddress.addressOf(o);
        return addToQueue(a, b);
    }
    public boolean addToQueue(HeapAddress a, boolean b) {
        int statusWord = a.offset(ObjectLayout.STATUS_WORD_OFFSET).peek4();
        if (b) {
            if ((statusWord & ObjectLayout.GC_BIT) != 0) return false;
            a.offset(ObjectLayout.STATUS_WORD_OFFSET).poke4(statusWord | ObjectLayout.GC_BIT);
        } else {
            if ((statusWord & ObjectLayout.GC_BIT) == 0) return false;
            a.offset(ObjectLayout.STATUS_WORD_OFFSET).poke4(statusWord & ~ObjectLayout.GC_BIT);
        }
        if (space() <= HeapAddress.size()) {
            // need a bigger work queue!
            int size = blockEnd.difference(blockStart);
            if (size == 0) size = WORKQUEUE_SIZE;
            else size = (WORKQUEUE_SIZE *= 2);
            growQueue(size);
        }
        queueEnd.poke(a);
        queueEnd = queueEnd.offset(HeapAddress.size());
        if (queueEnd.difference(blockEnd) == 0) {
            queueEnd = blockStart;
        }
        Assert._assert(queueEnd.difference(queueStart) != 0);
        return true;
    }
    
    public Object pull() {
        if (queueEnd.difference(queueStart) == 0) return null;
        HeapAddress a = (HeapAddress) queueStart.peek();
        queueStart = queueStart.offset(HeapAddress.size());
        if (queueStart.difference(blockEnd) == 0) {
            queueStart = blockStart;
        }
        return a.asObject();
    }
}
