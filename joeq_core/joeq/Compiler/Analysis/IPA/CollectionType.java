// CollectionType.java, created Nov 26, 2003 by gback
// Copyright (C) 2003 Godmar Back <gback@stanford.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package Compil3r.Analysis.IPA;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.sf.javabdd.BDD;
import org.sf.javabdd.BDDDomain;
import org.sf.javabdd.BDDFactory;
import org.sf.javabdd.BDDPairing;
import org.sf.javabdd.TypedBDDFactory.TypedBDD;

import Clazz.jq_Class;
import Clazz.jq_Field;
import Clazz.jq_Method;
import Clazz.jq_Member;
import Clazz.jq_NameAndDesc;
import Clazz.jq_Reference;
import Clazz.jq_Type;
import Util.Assert;
import Util.Strings;
import Util.Collections.Pair;
import Util.Collections.HashWorklist;
import Util.Collections.UnmodifiableIterator;
import Util.Graphs.PathNumbering;

/**
 * Do analysis of types stored in collections.
 * 
 * @author Godmar Back
 * @version $Id$
 */
public class CollectionType {

    PAResults res;
    boolean TRACE = false;
    
    public CollectionType(PAResults res, boolean TRACE) {
        this.res = res;
        this.TRACE = TRACE;
    }

    public HashMap cmethods = new HashMap();
    {	// not static so file is reread everytime you invoke this command 

	// Fileformat see below: type mname mdesc #pidx
        File f = new File("collectionmethods");
	DataInput in = null;
        if (f.exists()) {
	    try {
		in = new DataInputStream(new FileInputStream(f));
	    } catch (FileNotFoundException e) {
		e.printStackTrace();
	    }
	} else {
	    // some examples
	    String fContent = 
		"Ljava/util/List; add (ILjava/lang/Object;)V 2\n" +
		"Ljava/util/List; set (ILjava/lang/Object;)Ljava/lang/Object; 2\n" +
		"Ljava/util/Collection; add (Ljava/lang/Object;)Z 1\n" +
		"Ljava/util/Vector; addElement (Ljava/lang/Object;)V 1\n" +
		"Ljava/util/Map; put (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object; 2\n" +
		"Ljava/util/Collection; addAll (Ljava/util/Collection;)Z 1 ALL\n" +
		"Ljava/util/List; addAll (ILjava/util/Collection;)Z 2 ALL\n" +
		"Ljava/util/LinkedList; <init> (Ljava/util/Collection;)V 1 ALL\n";
	    in = new DataInputStream(new ByteArrayInputStream(fContent.getBytes()));
	}

	try {
	    for (;;) {
		String s = in.readLine();
		if (s == null) break;
		if (s.startsWith("#"))
		    continue;
		StringTokenizer st = new StringTokenizer(s);
		jq_Member m = jq_Member.read(st);
		if (m == null) {
		    System.out.println("Could not resolve `" + s + "', ignoring it");
		    continue;
		}
		boolean addsAll = false;
		int pidx = Integer.parseInt(st.nextToken());
		if (st.hasMoreTokens()) {
		    String mod = st.nextToken();
		    if ("all".equalsIgnoreCase(mod))
			addsAll = true;
		}
		cmethods.put(m.getDeclaringClass(), new CollCall((jq_Method)m, pidx, addsAll));
	    }
	} catch (IOException e) {
	    e.printStackTrace();
        }
    }

    static class CollCall {
	jq_Method meth;
	int pidx;
	boolean addsAll;

	CollCall(jq_Method meth, int pidx, boolean addsAll) {
	    this.meth = meth;
	    this.pidx = pidx;
	    this.addsAll = addsAll;
	}
    }

    /**
     * Check if m is a method that adds to a collection, if so, return idx of parameter.
     *
     * @return collection call or null if it isn't one
     */
    private CollCall isCollectionCall(jq_Method m) {
	// see if any superclass or implemented interface declares the method 
	// that is being invoked as a collection-add method
	jq_Class mclass = m.getDeclaringClass();
	while (mclass != null) {
	    CollCall cc = checkMethodForType(mclass, m); 
	    if (cc != null)
		return cc;
	    mclass = mclass.getSuperclass();
	}
	jq_Class []ifs = m.getDeclaringClass().getInterfaces();
	for (int i = 0; i < ifs.length; i++) {
	    CollCall cc = checkMethodForType(ifs[i], m); 
	    if (cc != null)
		return cc;
	}
	return null;
    }

    private CollCall checkMethodForType(jq_Class type, jq_Method m) {
	CollCall cc = (CollCall)cmethods.get(type);
	if (cc != null && cc.meth.getNameAndDesc().equals(m.getNameAndDesc()))
	    return cc;
	return null;
    }

    /**
     * Implements Vladimir's idea of finding out what types go in a collection.
     *
     * @return BDD H2 x T1 that maps collection objects to the shared supertypes of their elements.
     */
    public TypedBDD findCollectionTypes() {
	PA r = res.r;
	TypedBDD storedin = (TypedBDD)r.bdd.zero(); 		// H1xH1cxH2xH2c
	TypedBDD copiedin = (TypedBDD)r.bdd.zero(); 		// H1xH2
	if (!r.CONTEXT_SENSITIVE) {
            System.out.println("Sorry, this analysis has only been debugged in context-sensitivity mode");
	    return storedin;
	}
	res.initializeExtraDomains();	// for H3
	BDD V1cset = r.V1c.set();
	BDD V1set = r.V1.set();
	BDD H1set = r.H1.set();
	BDD H2set = r.H2.set();

	// iterate over all callsites (XXX use a BDD-filter for this instead?)
	for (int iidx = 0; iidx < r.Imap.size(); iidx++) {
	    ProgramLocation call = (ProgramLocation)r.Imap.get(iidx);

	    // is this a call that adds to a collection?
	    CollCall cc = isCollectionCall(call.getTargetMethod());
	    if (cc == null)
		continue;
	    // if so, find the parameter index of the item or collection being added
	    int pidx = cc.pidx;
	    
	    if (TRACE) {
		System.out.println("I(" + iidx + ") Z(" + cc.pidx + ") method " 
		    + cc.meth + " all=" + cc.addsAll + " " + call.toStringLong());
	    }
	    BDD isite = r.I.ithVar(iidx);
	    BDDPairing V2toV1 = r.bdd.makePair(r.V2, r.V1);
	    BDD actuals = r.actual.restrict(isite);		// V2xZ
	    isite.free();
	    BDD z0 = r.Z.ithVar(0);
	    BDD v0 = actuals.restrict(z0);			// V2
	    z0.free();
	    v0.replaceWith(V2toV1);				// V1

	    BDD v0pt = r.vP.relprod(v0, V1set);			// V1cxH1xH1c
	    v0.free(); 
	    if (r.NNfilter != null) v0pt.andWith(r.NNfilter.id());
	    v0pt.replaceWith(r.H1toH2);				// V1cxH2xH2c

	    BDD vp = actuals.restrictWith(r.Z.ithVar(pidx));	// V2
	    vp.replaceWith(V2toV1);				// V1
	    BDD vppt = r.vP.relprod(vp, V1set);			// V1cxH1xH1c
	    vp.free();
	    if (r.NNfilter != null) vppt.andWith(r.NNfilter.id());
	    BDD h0hp = v0pt.relprod(vppt, V1cset);		// H1xH1cxH2xH2c
	    v0pt.free();
	    vppt.free();
	    if (cc.addsAll) {
		copiedin.orWith(h0hp);	// H2 has collection, H1 has collection being added
	    } else {
		storedin.orWith(h0hp);	// H2 has collection, H1 has items being added
	    }
	}

	if (true) {	// does this make sense?
	    BDD one_to_one = r.H1c.buildEquals(r.H2c);
	    storedin.andWith(one_to_one.id());
	    copiedin.andWith(one_to_one);
	}

	TypedBDD tmp = null;
	if (r.CONTEXT_SENSITIVE) {	
	    // project away H1c, H2c
	    tmp = (TypedBDD)storedin.exist(r.H1cH2cset);
	    storedin.free();
	    storedin = tmp;
	    tmp = (TypedBDD)copiedin.exist(r.H1cH2cset);
	    copiedin.free();
	    copiedin = tmp;
	}

	tmp = (TypedBDD)storedin.exist(H1set);
	if (tmp.isZero()) {
	    System.out.println("Didn't find any collections");
	    return tmp;
	}
	// determine the supertype of all inserted items for each collection
	BDD supertypes = r.bdd.zero();				// H2 x T1
	for (Iterator collections = tmp.iterator(); collections.hasNext(); ) {
	    BDD c = (BDD)collections.next();
	    BDD items = storedin.restrict(c);			// H1xH2 -> H1
	    BDD itemtypes = items.relprod(r.hT, H1set);		// H1 x H1xT2 -> T2
	    items.free();
	    BDD stypes = res.calculateCommonSupertype(itemtypes);// T2 -> T1
	    itemtypes.free();
	    c.andWith(stypes);					// H2 x T1
	    supertypes.orWith(c);
	}
	tmp.free();

	// propagate the supertype of all copied collections to the copy destination
	tmp = (TypedBDD)copiedin.exist(H1set);
	BDD old_s = supertypes.id();
	for (int cnt = 1;;++cnt) {
	    for (Iterator collections = tmp.iterator(); collections.hasNext(); ) {
		BDD this_col = (BDD)collections.next();			// H2
		BDD other_cols = copiedin.restrict(this_col);		// H1xH2 -> H1
		BDDPairing H1toH2 = r.bdd.makePair(r.H1, r.H2);
		other_cols.replaceWith(H1toH2);				// H1 -> H2
		BDD othertypes = supertypes.relprod(other_cols, H2set);	// H2xT1 x H2 -> T1
		if (othertypes.isZero())
		    continue;
		BDD thistype = supertypes.relprod(this_col, H2set);	// H2 x H2xT1 -> T1
		othertypes.orWith(thistype);
		othertypes.replaceWith(r.T1toT2);
		BDD newtypeforthis = res.calculateCommonSupertype(othertypes);
		othertypes.free();
		// supertypes.applyWith(this_col.and(r.T1.domain()), BDDFactory.diff);
		supertypes.applyWith(this_col.id(), BDDFactory.diff);
		this_col.andWith(newtypeforthis);
		supertypes.orWith(this_col);
	    }
	    boolean nochange = supertypes.equals(old_s);
	    old_s.free();
	    if (nochange)
		break;
	    old_s = supertypes.id();
	    System.out.println("iteration #" + cnt);
	}
	tmp.free();
	return (TypedBDD)supertypes;
    }
}