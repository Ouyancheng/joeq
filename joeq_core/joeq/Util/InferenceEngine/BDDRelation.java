// BDDRelation.java, created Mar 16, 2004 12:40:26 PM 2004 by jwhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package joeq.Util.InferenceEngine;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import org.sf.javabdd.BDD;
import org.sf.javabdd.BDDDomain;

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
    
    /**
     * @param bdd
     */
    public BDDRelation(BDDSolver solver, String name, List fieldNames, List fieldDomains) {
        super(name, fieldNames, fieldDomains);
        this.solver = solver;
        this.relation = solver.bdd.zero();
        this.domains = new LinkedList();
        for (int i = 0; i < fieldDomains.size(); ++i) {
            FieldDomain fd = (FieldDomain) fieldDomains.get(i);
            Collection doms = solver.getBDDDomains(fd);
            BDDDomain d = null;
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
            if (solver.TRACE) solver.out.println("Field "+fieldNames.get(i)+" assigned to BDDDomain "+d);
            domains.add(d);
        }
    }
    
    public void load() {
        try {
            load(name+".bdd");
        } catch (IOException x) {
        }
        
        try {
            loadTuples(name+".tuples");
        } catch (IOException x) {
        }
    }
    
    public void load(String filename) throws IOException {
        relation.free();
        relation = solver.bdd.load(filename);
    }
    
    public void loadTuples(String filename) throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(filename));
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
                if (v.equals("*")) b.andWith(d.domain());
                else b.andWith(d.ithVar(Long.parseLong(v)));
            }
            relation.orWith(b);
        }
    }
}