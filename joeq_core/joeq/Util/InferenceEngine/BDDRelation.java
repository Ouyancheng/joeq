// BDDRelation.java, created Mar 16, 2004 12:40:26 PM 2004 by jwhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package joeq.Util.InferenceEngine;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

import org.sf.javabdd.BDD;
import org.sf.javabdd.BDDDomain;
import org.sf.javabdd.BDDFactory;

/**
 * BDDRelation
 * 
 * @author jwhaley
 * @version $Id$
 */
public class BDDRelation extends Relation {
    
    BDDSolver solver;
    BDD relation;
    List/*<BDDDomain>*/ domains;
    BDD domainSet;
    
    public BDDRelation(BDDSolver solver, String name, List fieldNames, List fieldDomains, List fieldOptions) {
        super(name, fieldNames, fieldDomains, fieldOptions);
        this.solver = solver;
    }
    
    public void initialize() {
        this.relation = solver.bdd.zero();
        this.domains = new LinkedList();
        if (solver.TRACE)
            solver.out.println("Initializing BDDRelation "+name+" with domains "+fieldDomains+" names "+fieldNames.size()+" options "+fieldOptions);
        this.domainSet = solver.bdd.one();
        for (int i = 0; i < fieldDomains.size(); ++i) {
            FieldDomain fd = (FieldDomain) fieldDomains.get(i);
            Collection doms = solver.getBDDDomains(fd);
            BDDDomain d = null;
            String option = (String) fieldOptions.get(i);
            if (option != null && option.length() > 0) {
                // use the given domain.
                if (!option.startsWith(fd.name))
                    throw new IllegalArgumentException("Field "+fieldNames.get(i)+" has domain "+fd+", but tried to assign "+option);
                //int index = Integer.parseInt(option.substring(fd.name.length()));
                for (Iterator j = doms.iterator(); j.hasNext(); ) {
                    BDDDomain dom = (BDDDomain) j.next();
                    if (dom.getName().equals(option)) {
                        if (domains.contains(dom)) {
                            System.out.println("Cannot assign "+dom+" to field "+fieldNames.get(i)+": "+dom+" is already assigned");
                            option = "";
                            break;
                        } else {
                            d = dom;
                            break;
                        }
                    }
                }
                if (option.length() > 0) {
                    while (d == null) {
                        BDDDomain dom = solver.allocateBDDDomain(fd);
                        if (dom.getName().equals(option)) {
                            d = dom;
                            break;
                        }
                    }
                }
            }
            if (d == null) {
                // find an applicable domain.
                for (Iterator j = doms.iterator(); j.hasNext(); ) {
                    BDDDomain dom = (BDDDomain) j.next();
                    if (!domains.contains(dom)) {
                        d = dom;
                        break;
                    }
                }
                if (d == null) {
                    d = solver.allocateBDDDomain(fd);
                }
            }
            if (solver.TRACE) solver.out.println("Field "+fieldNames.get(i)+" ("+fieldDomains.get(i)+") assigned to BDDDomain "+d);
            domains.add(d);
            domainSet.andWith(d.set());
        }
        boolean is_equiv = solver.equivalenceRelations.values().contains(this);
        boolean is_nequiv = solver.notequivalenceRelations.values().contains(this);
        if (is_equiv || is_nequiv) {
            BDDDomain d1 = (BDDDomain) domains.get(0);
            BDDDomain d2 = (BDDDomain) domains.get(1);
            relation.free();
            BDD b = d1.buildEquals(d2);
            if (is_nequiv) {
                relation = b.not(); b.free();
            } else {
                relation = b;
            }
        }
        if (negated != null) {
            BDDRelation bddn = (BDDRelation) negated;
            bddn.relation = solver.bdd.one();
            bddn.domains = this.domains;
            bddn.domainSet = this.domainSet;
        }
    }
    
    public void load() throws IOException {
        load(name+".bdd");
        if (solver.NOISY) solver.out.println("Loaded BDD from file: "+name+".bdd "+relation.nodeCount()+" nodes, "+size()+" elements.");
        if (solver.NOISY) solver.out.println("Domains of loaded relation:"+activeDomains(relation));
    }
    
    public void loadTuples() throws IOException {
        loadTuples(name+".tuples");
        if (solver.NOISY) solver.out.println("Loaded tuples from file: "+name+".tuples");
        if (solver.NOISY) solver.out.println("Domains of loaded relation:"+activeDomains(relation));
    }
    
    void updateNegated() {
        if (negated != null) {
            BDDRelation bddn = (BDDRelation) negated;
            bddn.relation.free();
            bddn.relation = relation.not();
        }
    }
    
    public void load(String filename) throws IOException {
        BDD r2 = solver.bdd.load(filename);
        if (r2 != null) {
            if (r2.isZero()) {
                System.out.println("Warning: "+filename+" is zero.");
            } else if (r2.isOne()) {
                System.out.println("Warning: "+filename+" is one.");
            } else {
                BDD s = r2.support();
                BDD t = domainSet.and(s);
                s.free();
                boolean b = !t.equals(domainSet);
                t.free();
                if (b) {
                    throw new IOException("Expected domains for loaded BDD "+filename+" to be "+domains+", but found "+activeDomains(r2)+" instead");
                }
            }
            
            relation.free();
            relation = r2;
        }
        updateNegated();
    }
    
    public void loadTuples(String filename) throws IOException {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(filename));
            for (;;) {
                String s = in.readLine();
                if (s == null) break;
                if (s.length() == 0) continue;
                if (s.startsWith("#")) continue;
                StringTokenizer st = new StringTokenizer(s);
                BDD b = solver.bdd.one();
                for (int i = 0; i < domains.size(); ++i) {
                    BDDDomain d = (BDDDomain) domains.get(i);
                    String v = st.nextToken();
                    if (v.equals("*")) {
                        b.andWith(d.domain());
                    } else {
                        int x = v.indexOf('-');
                        if (x < 0) {
                            long l = Long.parseLong(v);
                            b.andWith(d.ithVar(l));
                            if (solver.TRACE_FULL) solver.out.print(fieldNames.get(i)+": "+l+", ");
                        } else {
                            long l = Long.parseLong(v.substring(0, x));
                            long m = Long.parseLong(v.substring(x+1));
                            b.andWith(d.varRange(l, m));
                            if (solver.TRACE_FULL) solver.out.print(fieldNames.get(i)+": "+l+"-"+m+", ");
                        }
                    }
                }
                if (solver.TRACE_FULL) solver.out.println();
                relation.orWith(b);
            }
        } finally {
            if (in != null) in.close();
        }
        updateNegated();
    }
    
    public void save() throws IOException {
        save(name+".rbdd");
    }
    
    public void save(String filename) throws IOException {
        System.out.println("Relation "+this+": "+relation.nodeCount()+" nodes, "+size()+" elements");
        solver.bdd.save(filename, relation);
    }

    public void saveNegated() throws IOException {
        System.out.println("Relation "+this+": "+relation.not().nodeCount()+" nodes");
        solver.bdd.save("not"+name+".rbdd", relation.not());
    }
    
    public void saveTuples() throws IOException {
        System.out.println("Relation "+this+": "+relation.nodeCount()+" nodes, "+size()+" elements");
        saveTuples(name+".rtuples", relation);
    }
    
    public void saveNegatedTuples() throws IOException {
        System.out.println("Relation "+this+": "+relation.nodeCount()+" nodes");
        saveTuples("not"+name+".rtuples", relation.not());
    }
    
    public void saveTuples(String fileName, BDD relation) throws IOException {
        DataOutputStream dos = null;
        try {
            dos = new DataOutputStream(new FileOutputStream(fileName));
            if (relation.isZero()) {
                return;
            }
            BDD ss = relation.support();
            int[] a = ss.scanSetDomains();
            ss.free();
            BDD allDomains = solver.bdd.one();
            dos.writeBytes("#");
            System.out.print(fileName+" domains {");
            for (Iterator i = domains.iterator(); i.hasNext(); ) {
                BDDDomain d = (BDDDomain) i.next();
                System.out.print(" "+d.toString());
                dos.writeBytes(" "+d.toString()+":"+d.varNum());
            }
            dos.writeBytes("\n");
            System.out.println(" ) = "+relation.nodeCount()+" nodes, "+size()+" elements");
            for (int i = 0; i < a.length; ++i) {
                BDDDomain d = solver.bdd.getDomain(a[i]);
                allDomains.andWith(d.set());
            }
            BDDDomain iterDomain = (BDDDomain) domains.get(0);
            BDD foo = relation.exist(allDomains.exist(iterDomain.set()));
            int lines = 0;
            for (Iterator i = foo.iterator(iterDomain.set()); i.hasNext(); ) {
                BDD q = (BDD) i.next();
                q.andWith(relation.id());
                while (!q.isZero()) {
                    BDD sat = q.satOne(allDomains, solver.bdd.zero());
                    BDD sup = q.support();
                    int[] b = sup.scanSetDomains();
                    sup.free();
                    long[] v = sat.scanAllVar();
                    sat.free();
                    BDD t = solver.bdd.one();
                    for (Iterator j = domains.iterator(); j.hasNext(); ) {
                        BDDDomain d = (BDDDomain) j.next();
                        int jj = d.getIndex();
                        if (Arrays.binarySearch(b, jj) < 0) {
                            dos.writeBytes("* ");
                            t.andWith(d.domain());
                        } else {
                            dos.writeBytes(v[jj]+" ");
                            t.andWith(d.ithVar(v[jj]));
                        }
                    }
                    q.applyWith(t, BDDFactory.diff);
                    dos.writeBytes("\n");
                    ++lines;
                }
                q.free();
            }
            System.out.println("Done writing "+lines+" lines.");
        } finally {
            if (dos != null) dos.close();
        }
    }
    
    public String activeDomains(BDD relation) {
        BDD s = relation.support();
        int[] a = s.scanSetDomains();
        s.free();
        if (a == null) return "(none)";
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < a.length; ++i) {
            sb.append(solver.bdd.getDomain(a[i]));
            if (i < a.length-1) sb.append(',');
        }
        return sb.toString();
    }
    
    public double size() {
        return relation.satCount(domainSet);
    }
    
    public TupleIterator iterator() {
        final Iterator i = relation.iterator(domainSet);
        return new MyTupleIterator(i, domains);
    }
    
    static class MyTupleIterator extends TupleIterator {
        Iterator i;
        List domains;
        MyTupleIterator(Iterator i, List domains) {
            this.i = i;
            this.domains = domains;
        }
        public long[] nextTuple() {
            BDD b = (BDD) i.next();
            long[] r = new long[domains.size()];
            long[] q = b.scanAllVar();
            int j = 0;
            for (Iterator k = domains.iterator(); k.hasNext(); ++j) {
                BDDDomain d = (BDDDomain) k.next();
                r[j] = q[d.getIndex()];
            }
            return r;
        }

        public boolean hasNext() {
            return i.hasNext();
        }
        
        public void remove() {
            i.remove();
        }
    }
    
    public TupleIterator iterator(int k) {
        final BDDDomain d = (BDDDomain) domains.get(k);
        BDD s = d.set();
        final Iterator i = relation.iterator(s);
        return new TupleIterator() {

            public long[] nextTuple() {
                BDD b = (BDD) i.next();
                long v = b.scanVar(d);
                return new long[] { v };
            }

            public boolean hasNext() {
                return i.hasNext();
            }
            
            public void remove() {
                i.remove();
            }
            
        };
    }

    public TupleIterator iterator(int k, long j) {
        final BDDDomain d = (BDDDomain) domains.get(k);
        BDD val = d.ithVar(j);
        val.andWith(relation.id());
        final Iterator i = val.iterator(domainSet);
        return new MyTupleIterator(i, domains);
    }
}
