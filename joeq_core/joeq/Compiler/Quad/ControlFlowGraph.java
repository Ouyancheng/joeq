/*
 * ControlFlowGraph.java
 *
 * Created on April 21, 2001, 11:25 PM
 *
 */

package Compil3r.Quad;
import java.util.HashMap;
import java.util.Map;

import Bootstrap.PrimordialClassLoader;
import Clazz.jq_Method;
import Clazz.jq_Primitive;
import Clazz.jq_Type;
import Compil3r.Quad.Operand.BasicBlockTableOperand;
import Compil3r.Quad.Operand.ParamListOperand;
import Compil3r.Quad.Operand.RegisterOperand;
import Compil3r.Quad.Operand.TargetOperand;
import Compil3r.Quad.RegisterFactory.Register;
import Util.Assert;
import Util.Strings;
import Util.Collections.FilterIterator;
import Util.Templates.List;
import Util.Templates.ListIterator;
import Util.Templates.ListWrapper;
import Util.Templates.UnmodifiableList;

/**
 * Control flow graph for the Quad format.
 * The control flow graph is a fundamental part of the quad intermediate representation.
 * The control flow graph organizes the basic blocks for a method.
 * 
 * Control flow graphs always include an entry basic block and an exit basic block.
 * These basic blocks are always empty and have id numbers 0 and 1, respectively.
 * 
 * A control flow graph includes references to the entry and exit nodes, and the
 * set of exception handlers for the method.
 * 
 * @author  John Whaley
 * @version $Id$
 */

public class ControlFlowGraph {

    /* Method that this control flow graph represents. May be null for synthetic methods. */
    private final jq_Method method;
    /* Reference to the start node of this control flow graph. */
    private final BasicBlock start_node;
    /* Reference to the end node of this control flow graph. */
    private final BasicBlock end_node;
    /* List of exception handlers for this control flow graph. */
    private final java.util.List/*<ExceptionHandler>*/ exception_handlers;
    
    /* Register factory that we use on this control flow graph. */
    private final RegisterFactory rf;
    
    /* Current number of basic blocks, used to generate unique id's. */
    private int bb_counter;
    /* Current number of quads, used to generate unique id's. */
    private int quad_counter;
    
    /** Creates a new ControlFlowGraph.
     * @param numOfExits  the expected number of branches to the exit node.
     * @param numOfExceptionHandlers  the expected number of exception handlers. */
    public ControlFlowGraph(jq_Method method, int numOfExits, int numOfExceptionHandlers, RegisterFactory rf) {
        this.method = method;
        start_node = BasicBlock.createStartNode();
        end_node = BasicBlock.createEndNode(numOfExits);
        exception_handlers = new java.util.ArrayList(numOfExceptionHandlers);
        this.rf = rf;
        bb_counter = 1; quad_counter = 0;
    }

    /** Returns the entry node.
     * @return  the entry node. */
    public BasicBlock entry() { return start_node; }
    /** Returns the exit node.
     * @return  the exit node. */
    public BasicBlock exit() { return end_node; }

    /** Returns the method this control flow graph represents.
     *  May be null for synthetic methods.
     *  @return method this control flow graph represents, or null for synthetic. */
    public jq_Method getMethod() { return method; }

    /** Returns the register factory used by this control flow graph.
     * @return  the register factory used by this control flow graph. */
    public RegisterFactory getRegisterFactory() { return rf; }

    /** Create a new basic block in this control flow graph.  The new basic block
     * is given a new, unique id number.
     * @param numOfPredecessors  number of predecessor basic blocks that this
                                 basic block is expected to have.
     * @param numOfSuccessors  number of successor basic blocks that this
                               basic block is expected to have.
     * @param numOfInstructions  number of instructions that this basic block
                                 is expected to have.
     * @param ehs  set of exception handlers for this basic block.
     * @return  the newly created basic block. */
    public BasicBlock createBasicBlock(int numOfPredecessors, int numOfSuccessors, int numOfInstructions,
                                       ExceptionHandlerList ehs) {
        return BasicBlock.createBasicBlock(++bb_counter, numOfPredecessors, numOfSuccessors, numOfInstructions, ehs);
    }
    /** Use with care after renumbering basic blocks. */
    void updateBBcounter(int value) { bb_counter = value-1; }
    /** Returns a maximum on the number of basic blocks in this control flow graph.
     * @return  a maximum on the number of basic blocks in this control flow graph. */
    public int getNumberOfBasicBlocks() { return bb_counter+1; }

    /** Returns a new id number for a quad. */
    public int getNewQuadID() { return ++quad_counter; }
    
    Map jsr_map;
    
    public void addJSRInfo(JSRInfo info) {
        if (jsr_map == null) jsr_map = new HashMap();
        jsr_map.put(info.entry_block, info);
        jsr_map.put(info.exit_block, info);
    }
    
    public JSRInfo getJSRInfo(BasicBlock bb) {
        return (JSRInfo) jsr_map.get(bb);
    }
    
    /** Returns an iteration of the basic blocks in this graph in reverse post order.
     * @return  an iteration of the basic blocks in this graph in reverse post order. */
    public ListIterator.BasicBlock reversePostOrderIterator() {
        return reversePostOrderIterator(start_node);
    }
    
    /** Returns an iteration of the basic blocks in the reversed graph in reverse post order.
     * The reversed graph is the graph where all edges are reversed.
     * @return  an iteration of the basic blocks in the reversed graph in reverse post order. */
    public ListIterator.BasicBlock reversePostOrderOnReverseGraphIterator() {
        return reversePostOrderOnReverseGraph(end_node).basicBlockIterator();
    }
    
    /** Returns an iteration of the basic blocks in the reversed graph in post order.
     * The reversed graph is the graph where all edges are reversed.
     * @return  an iteration of the basic blocks in the reversed graph in post order. */
    public ListIterator.BasicBlock postOrderOnReverseGraphIterator() {
        return postOrderOnReverseGraph(end_node).basicBlockIterator();
    }
    
    /** Returns an iteration of the basic blocks in this graph reachable from the given
     * basic block in reverse post order, starting from the given basic block.
     * @param start_bb  basic block to start reverse post order from.
     * @return  an iteration of the basic blocks in this graph reachable from the given basic block in reverse post order. */
    public ListIterator.BasicBlock reversePostOrderIterator(BasicBlock start_bb) {
        return reversePostOrder(start_bb).basicBlockIterator();
    }

    /** Visits all of the basic blocks in this graph with the given visitor.
     * @param bbv  visitor to visit each basic block with. */
    public void visitBasicBlocks(BasicBlockVisitor bbv) {
        for (ListIterator.BasicBlock i=reversePostOrderIterator(); i.hasNext(); ) {
            BasicBlock bb = i.nextBasicBlock();
            bbv.visitBasicBlock(bb);
        }
    }
    
    /** Returns a list of basic blocks in reverse post order, starting at the given basic block.
     * @param start_bb  basic block to start from.
     * @return  a list of basic blocks in reverse post order, starting at the given basic block. */
    public List.BasicBlock reversePostOrder(BasicBlock start_bb) {
        java.util.LinkedList/*<BasicBlock>*/ result = new java.util.LinkedList();
        boolean[] visited = new boolean[bb_counter+1];
        reversePostOrder_helper(start_bb, visited, result, true);
        BasicBlock[] bb = new BasicBlock[result.size()];
        bb = (BasicBlock[])result.toArray(bb);
        return new UnmodifiableList.BasicBlock(bb);
    }

    /** Returns a list of basic blocks of the reversed graph in reverse post order, starting at the given basic block.
     * @param start_bb  basic block to start from.
     * @return  a list of basic blocks of the reversed graph in reverse post order, starting at the given basic block. */
    public List.BasicBlock reversePostOrderOnReverseGraph(BasicBlock start_bb) {
        java.util.LinkedList/*<BasicBlock>*/ result = new java.util.LinkedList();
        boolean[] visited = new boolean[bb_counter+1];
        reversePostOrder_helper(start_bb, visited, result, false);
        BasicBlock[] bb = new BasicBlock[result.size()];
        bb = (BasicBlock[])result.toArray(bb);
        return new UnmodifiableList.BasicBlock(bb);
    }
    
    /** Returns a list of basic blocks of the reversed graph in post order, starting at the given basic block.
     * @param start_bb  basic block to start from.
     * @return  a list of basic blocks of the reversed graph in post order, starting at the given basic block. */
    public List.BasicBlock postOrderOnReverseGraph(BasicBlock start_bb) {
        java.util.LinkedList/*<BasicBlock>*/ result = new java.util.LinkedList();
        boolean[] visited = new boolean[bb_counter+1];
        reversePostOrder_helper(start_bb, visited, result, false);
        java.util.Collections.reverse(result);
        BasicBlock[] bb = new BasicBlock[result.size()];
        bb = (BasicBlock[])result.toArray(bb);
        return new UnmodifiableList.BasicBlock(bb);
    }
    
    /** Helper function to compute reverse post order. */
    private void reversePostOrder_helper(BasicBlock b, boolean[] visited, java.util.LinkedList result, boolean direction) {
        if (visited[b.getID()]) return;
        visited[b.getID()] = true;
        List.BasicBlock bbs = direction ? b.getSuccessors() : b.getPredecessors();
        ListIterator.BasicBlock bbi = bbs.basicBlockIterator();
        while (bbi.hasNext()) {
            BasicBlock b2 = bbi.nextBasicBlock();
            reversePostOrder_helper(b2, visited, result, direction);
        }
        if (direction) {
            ListIterator.ExceptionHandler ehi = b.getExceptionHandlers().exceptionHandlerIterator();
            while (ehi.hasNext()) {
                ExceptionHandler eh = ehi.nextExceptionHandler();
                BasicBlock b2 = eh.getEntry();
                reversePostOrder_helper(b2, visited, result, direction);
            }
        } else {
            if (b.isExceptionHandlerEntry()) {
                java.util.Iterator ex_handlers = getExceptionHandlersMatchingEntry(b);
                while (ex_handlers.hasNext()) {
                    ExceptionHandler eh = (ExceptionHandler)ex_handlers.next();
                    ListIterator.BasicBlock handled = eh.getHandledBasicBlocks().basicBlockIterator();
                    while (handled.hasNext()) {
                        BasicBlock bb = handled.nextBasicBlock();
                        reversePostOrder_helper(bb, visited, result, direction);
                    }
                }
            }
        }
        result.addFirst(b);
    }

    void addExceptionHandler(ExceptionHandler eh) {
        exception_handlers.add(eh);
    }
    
    /** Return the list of exception handlers in this control flow graph.
     */
    public List.ExceptionHandler getExceptionHandlers() {
        return new ListWrapper.ExceptionHandler(exception_handlers);
    }

    /** Return an iterator of the exception handlers with the given entry point.
     * @param b  basic block to check exception handlers against.
     * @return  an iterator of the exception handlers with the given entry point. */
    public java.util.Iterator getExceptionHandlersMatchingEntry(BasicBlock b) {
        final BasicBlock bb = b;
        return new FilterIterator(exception_handlers.iterator(),
            new FilterIterator.Filter() {
                public boolean isElement(Object o) {
                    ExceptionHandler eh = (ExceptionHandler)o;
                    return eh.getEntry() == bb;
                }
        });
    }
    
    /** Returns a verbose string of every basic block in this control flow graph.
     * @return  a verbose string of every basic block in this control flow graph. */
    public String fullDump() {
        StringBuffer sb = new StringBuffer();
        sb.append("Control flow graph for "+method+":"+Strings.lineSep);
        ListIterator.BasicBlock i = reversePostOrderIterator();
        while (i.hasNext()) {
            BasicBlock bb = i.nextBasicBlock();
            sb.append(bb.fullDump());
        }
        sb.append("Exception handlers: "+exception_handlers);
        sb.append(Strings.lineSep+"Register factory: "+rf);
        return sb.toString();
    }

    private ExceptionHandler copier(HashMap map, ExceptionHandler this_eh) {
        ExceptionHandler that_eh = (ExceptionHandler)map.get(this_eh);
        if (that_eh != null) return that_eh;
        map.put(this_eh, that_eh = new ExceptionHandler(this_eh.getExceptionType()));
        that_eh.setEntry(copier(map, this_eh.getEntry()));
        for (ListIterator.BasicBlock li =
                 this_eh.getHandledBasicBlocks().basicBlockIterator();
             li.hasNext(); ) {
            that_eh.addHandledBasicBlock(copier(map, li.nextBasicBlock()));
        }
        return that_eh;
    }

    private ExceptionHandlerList copier(HashMap map, ExceptionHandlerList this_ehl) {
        if (this_ehl == null || this_ehl.size() == 0) return null;
        ExceptionHandlerList that_ehl = (ExceptionHandlerList)map.get(this_ehl);
        if (that_ehl != null) return that_ehl;
        map.put(this_ehl, that_ehl = new ExceptionHandlerList());
        that_ehl.setHandler(copier(map, this_ehl.getHandler()));
        that_ehl.setParent(copier(map, this_ehl.getParent()));
        return that_ehl;
    }

    private void updateOperand(HashMap map, Operand op) {
        if (op == null) return;
        if (op instanceof TargetOperand) {
            ((TargetOperand)op).setTarget(copier(map, ((TargetOperand)op).getTarget()));
        } else if (op instanceof BasicBlockTableOperand) {
            BasicBlockTableOperand bt = (BasicBlockTableOperand)op;
            for (int i=0; i<bt.size(); ++i) {
                bt.set(i, copier(map, bt.get(i)));
            }
        } else if (op instanceof RegisterOperand) {
            RegisterOperand rop = (RegisterOperand)op;
            Register r = (Register)map.get(rop.getRegister());
            if (r == null) {
                if (rop.getRegister().getNumber() == -1) {
                    r = RegisterFactory.makeGuardReg().getRegister();
                    map.put(rop.getRegister(), r);
                } else {
                    Assert.UNREACHABLE(rop.toString());
                }
            } else {
                rop.setRegister(r);
            }
        } else if (op instanceof ParamListOperand) {
            ParamListOperand plo = (ParamListOperand)op;
            for (int i=0; i<plo.length(); ++i) {
                updateOperand(map, plo.get(i));
            }
        }
    }

    private Quad copier(HashMap map, Quad this_q) {
        Quad that_q = (Quad)map.get(this_q);
        if (that_q != null) return that_q;
        map.put(this_q, that_q = this_q.copy(++quad_counter));
        updateOperand(map, that_q.getOp1());
        updateOperand(map, that_q.getOp2());
        updateOperand(map, that_q.getOp3());
        updateOperand(map, that_q.getOp4());
        return that_q;
    }

    private BasicBlock copier(HashMap map, BasicBlock this_bb) {
        BasicBlock that_bb = (BasicBlock)map.get(this_bb);
        if (that_bb != null) return that_bb;
        that_bb = BasicBlock.createBasicBlock(++this.bb_counter,
                                              this_bb.getNumberOfPredecessors(),
                                              this_bb.getNumberOfSuccessors(),
                                              this_bb.size());
        map.put(this_bb, that_bb);
        ExceptionHandlerList that_ehl = copier(map, this_bb.getExceptionHandlers());
        that_bb.setExceptionHandlerList(that_ehl);
        for (ListIterator.BasicBlock bbs = this_bb.getSuccessors().basicBlockIterator();
             bbs.hasNext(); ) {
            that_bb.addSuccessor(copier(map, bbs.nextBasicBlock()));
        }
        for (ListIterator.BasicBlock bbs = this_bb.getPredecessors().basicBlockIterator();
             bbs.hasNext(); ) {
            that_bb.addPredecessor(copier(map, bbs.nextBasicBlock()));
        }
        for (ListIterator.Quad qs = this_bb.iterator();
             qs.hasNext(); ) {
            that_bb.appendQuad(copier(map, qs.nextQuad()));
        }
        return that_bb;
    }

    static void addRegistersToMap(HashMap map, RegisterFactory from,
                                  RegisterFactory to, jq_Type type) {
        int n = from.getLocalSize(type);
        Assert._assert(n == to.getLocalSize(type));
        for (int i=0; i<n; ++i) {
            map.put(from.getLocal(i, type), to.getLocal(i, type));
        }
        n = from.getStackSize(type);
        Assert._assert(n == to.getStackSize(type));
        for (int i=0; i<n; ++i) {
            map.put(from.getStack(i, type), to.getStack(i, type));
        }
    }

    static void addRegistersToMap(HashMap map, RegisterFactory from,
                                  RegisterFactory to) {
        addRegistersToMap(map, from, to, jq_Primitive.INT);
        addRegistersToMap(map, from, to, jq_Primitive.FLOAT);
        addRegistersToMap(map, from, to, jq_Primitive.LONG);
        addRegistersToMap(map, from, to, jq_Primitive.DOUBLE);
        addRegistersToMap(map, from, to, PrimordialClassLoader.getJavaLangObject());
    }

    /** Merges the given control flow graph into this control flow graph.
     * Doesn't modify the given control flow graph.  A copy of the
     * given control flow graph (with appropriate renumberings) is
     * returned.
     */
    public ControlFlowGraph merge(ControlFlowGraph from) {
        RegisterFactory that_rf = this.rf.merge(from.rf);
        ControlFlowGraph that = new ControlFlowGraph(from.getMethod(),
                                                     from.exit().getNumberOfPredecessors(), from.exception_handlers.size(), that_rf);
        HashMap map = new HashMap();
        map.put(from.entry(), that.entry());
        map.put(from.exit(), that.exit());

        addRegistersToMap(map, from.getRegisterFactory(), that_rf);

        for (ListIterator.ExceptionHandler exs = from.getExceptionHandlers().exceptionHandlerIterator();
             exs.hasNext(); ) {
            that.addExceptionHandler(copier(map, exs.nextExceptionHandler()));
        }

        that.entry().addSuccessor(copier(map, from.entry().getFallthroughSuccessor()));
        for (ListIterator.BasicBlock bbs = from.exit().getPredecessors().basicBlockIterator();
             bbs.hasNext(); ) {
            that.exit().addPredecessor(copier(map, bbs.nextBasicBlock()));
        }

        that.bb_counter = this.bb_counter;
        that.quad_counter = this.quad_counter;

        return that;
    }

    public void appendExceptionHandlers(ExceptionHandlerList ehl) {
        if (ehl == null || ehl.size() == 0) return;
        ListIterator.BasicBlock l = reversePostOrderIterator();
        while (l.hasNext()) {
            BasicBlock bb = l.nextBasicBlock();
            if (bb.isEntry() || bb.isExit()) continue;
            bb.appendExceptionHandlerList(ehl);
        }
    }

}
