/* JayFX - A Fact Extractor Plug-in for Eclipse
 * Copyright (C) 2006  McGill University (http://www.cs.mcgill.ca/~swevo/jayfx)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * $Revision: 1.3 $
 */

package ca.mcgill.cs.swevo.jayfx.test;

import java.util.Set;

import ca.mcgill.cs.swevo.jayfx.ElementNotFoundException;
import ca.mcgill.cs.swevo.jayfx.ProgramDatabase;
import ca.mcgill.cs.swevo.jayfx.model.Category;
import ca.mcgill.cs.swevo.jayfx.model.FlyweightElementFactory;
import ca.mcgill.cs.swevo.jayfx.model.IElement;
import ca.mcgill.cs.swevo.jayfx.model.Relation;
import junit.framework.Assert;
import junit.framework.TestCase;

public class TestProgramDatabase extends TestCase {
	private IElement aClass1;
	private IElement aClass2;
	private IElement aField1;
	private IElement aMethod1;
	private IElement aMethod2;
	private ProgramDatabase aDB;

	@SuppressWarnings("unchecked")
	public void testAddElement() {
		this.aDB.addElement(this.aClass1, 0);
		Assert.assertTrue(this.aDB.contains(this.aClass1));
		this.aDB.addElement(this.aClass2, 0);
		Assert.assertTrue(this.aDB.contains(this.aClass2));
		this.aDB.addRelation(this.aClass1, Relation.DECLARES_TYPE, this.aClass2);
		this.aDB.addElement(this.aClass1, 0);
		final Set lSet = this.aDB.getRange(this.aClass1, Relation.DECLARES_TYPE);
		Assert.assertTrue(lSet.size() == 1);
	}

	@SuppressWarnings("unchecked")
	public void testAddRelation() {
		try {
			this.aDB.addRelation(this.aClass1, Relation.CALLS, this.aMethod1);
			Assert.fail("Expected ElementNotFoundException");
		} catch (final ElementNotFoundException pException) {
		}
		this.aDB.addElement(this.aClass1, 0);
		try {
			this.aDB.addRelation(this.aClass1, Relation.CALLS, this.aMethod1);
			Assert.fail("Expected ElementNotFoundException");
		} catch (final ElementNotFoundException pException) {
		}
		this.aDB.addElement(this.aMethod1, 0);
		this.aDB.addRelation(this.aClass1, Relation.CALLS, this.aMethod1);
		Set lSet = this.aDB.getRange(this.aClass1, Relation.CALLS);
		Assert.assertTrue(lSet.size() == 1);
		Assert.assertTrue(lSet.contains(this.aMethod1));
		this.aDB.addElement(this.aMethod2, 0);
		this.aDB.addRelation(this.aClass1, Relation.CALLS, this.aMethod2);
		lSet = this.aDB.getRange(this.aClass1, Relation.CALLS);
		Assert.assertTrue(lSet.size() == 2);
		Assert.assertTrue(lSet.contains(this.aMethod1));
		Assert.assertTrue(lSet.contains(this.aMethod2));
	}

	@SuppressWarnings("unchecked")
	public void testAddRelationAndTranspose() {
		try {
			this.aDB.addRelationAndTranspose(this.aClass1, Relation.CALLS, this.aMethod1);
			Assert.fail("Expected ElementNotFoundException");
		} catch (final ElementNotFoundException pException) {
		}
		this.aDB.addElement(this.aClass1, 0);
		try {
			this.aDB.addRelation(this.aClass1, Relation.CALLS, this.aMethod1);
			Assert.fail("Expected ElementNotFoundException");
		} catch (final ElementNotFoundException pException) {
		}
		this.aDB.addElement(this.aMethod1, 0);
		this.aDB.addRelationAndTranspose(this.aClass1, Relation.CALLS, this.aMethod1);
		Set lSet = this.aDB.getRange(this.aClass1, Relation.CALLS);
		Assert.assertTrue(lSet.size() == 1);
		Assert.assertTrue(lSet.contains(this.aMethod1));
		lSet = this.aDB.getRange(this.aMethod1, Relation.CALLS);
		Assert.assertTrue(lSet.size() == 0);
		lSet = this.aDB.getRange(this.aMethod1, Relation.T_CALLS);
		Assert.assertTrue(lSet.size() == 1);
		Assert.assertTrue(lSet.contains(this.aClass1));
		this.aDB.addElement(this.aMethod2, 0);
		this.aDB.addRelationAndTranspose(this.aMethod2, Relation.T_CALLS, this.aClass1);
		lSet = this.aDB.getRange(this.aClass1, Relation.CALLS);
		Assert.assertTrue(lSet.size() == 2);
		Assert.assertTrue(lSet.contains(this.aMethod1));
		Assert.assertTrue(lSet.contains(this.aMethod2));
		lSet = this.aDB.getRange(this.aMethod2, Relation.T_CALLS);
		Assert.assertTrue(lSet.size() == 1);
		Assert.assertTrue(lSet.contains(this.aClass1));
	}

	public void testContains() {
		this.aDB.addElement(this.aClass1, 0);
		this.aDB.addElement(this.aClass2, 0);
		this.aDB.contains(this.aClass1);
		this.aDB.contains(this.aClass2);
	}

	@SuppressWarnings("unchecked")
	public void testCopyRelations() {
		// Invalid case
		try {
			this.aDB.copyRelations(this.aClass1, this.aClass2);
			Assert.fail("Expected ElementNotFoundException");
		} catch (final ElementNotFoundException pException) {
		}
		this.aDB.addElement(this.aClass1, 0);
		try {
			this.aDB.addRelation(this.aClass1, Relation.CALLS, this.aMethod1);
			Assert.fail("Expected ElementNotFoundException");
		} catch (final ElementNotFoundException pException) {
		}
		// Empty case
		this.aDB.addElement(this.aClass2, 0);
		this.aDB.copyRelations(this.aClass1, this.aClass2);
		// Unit case
		this.aDB.addElement(this.aMethod1, 0);
		this.aDB.addElement(this.aField1, 0);
		this.aDB.addRelationAndTranspose(this.aClass1, Relation.DECLARES_FIELD, this.aField1);
		this.aDB.addRelationAndTranspose(this.aClass1, Relation.CALLS, this.aMethod1);
		this.aDB.copyRelations(this.aClass1, this.aClass2);
		Set lSet = this.aDB.getRange(this.aClass1, Relation.DECLARES_FIELD);
		Assert.assertTrue(lSet.size() == 1);
		Assert.assertTrue(lSet.contains(this.aField1));
		lSet = this.aDB.getRange(this.aClass1, Relation.CALLS);
		Assert.assertTrue(lSet.size() == 1);
		Assert.assertTrue(lSet.contains(this.aMethod1));
		lSet = this.aDB.getRange(this.aField1, Relation.T_DECLARES);
		Assert.assertTrue(lSet.size() == 2);
		Assert.assertTrue(lSet.contains(this.aClass1));
		Assert.assertTrue(lSet.contains(this.aClass2));
		lSet = this.aDB.getRange(this.aMethod1, Relation.T_CALLS);
		Assert.assertTrue(lSet.size() == 2);
		Assert.assertTrue(lSet.contains(this.aClass1));
		Assert.assertTrue(lSet.contains(this.aClass2));
		lSet = this.aDB.getRange(this.aClass2, Relation.DECLARES_FIELD);
		Assert.assertTrue(lSet.size() == 1);
		Assert.assertTrue(lSet.contains(this.aField1));
		lSet = this.aDB.getRange(this.aClass2, Relation.CALLS);
		Assert.assertTrue(lSet.size() == 1);
		Assert.assertTrue(lSet.contains(this.aMethod1));
		// Adding on to existing relations
		this.aDB.addRelationAndTranspose(this.aField1, Relation.T_ACCESSES, this.aMethod1);
		this.aDB.copyRelations(this.aField1, this.aClass2);
		lSet = this.aDB.getRange(this.aMethod1, Relation.ACCESSES);
		Assert.assertTrue(lSet.size() == 2);
		Assert.assertTrue(lSet.contains(this.aField1));
		Assert.assertTrue(lSet.contains(this.aClass2));
		lSet = this.aDB.getRange(this.aClass2, Relation.T_ACCESSES);
		Assert.assertTrue(lSet.size() == 1);
		Assert.assertTrue(lSet.contains(this.aMethod1));
	}

	@SuppressWarnings("unchecked")
	public void testGetAllElements() {
		Set lSet = this.aDB.getAllElements();
		Assert.assertTrue(lSet.size() == 0);
		this.aDB.addElement(this.aClass1, 0);
		lSet = this.aDB.getAllElements();
		Assert.assertTrue(lSet.size() == 1);
		Assert.assertTrue(lSet.contains(this.aClass1));
		this.aDB.addElement(this.aClass2, 0);
		lSet = this.aDB.getAllElements();
		Assert.assertTrue(lSet.size() == 2);
		Assert.assertTrue(lSet.contains(this.aClass1));
		Assert.assertTrue(lSet.contains(this.aClass2));
	}

	@SuppressWarnings("unchecked")
	public void testGetRange() {
		// Invalid case
		try {
			Set<IElement> range = this.aDB.getRange(this.aClass1, Relation.DECLARES_METHOD);
			assertTrue(range.isEmpty());
		} catch (final ElementNotFoundException pException) {
		}
		// Empty Case
		this.aDB.addElement(this.aClass1, 0);
		Set lSet = this.aDB.getRange(this.aClass1, Relation.DECLARES_METHOD);
		Assert.assertTrue(lSet.size() == 0);
		this.aDB.addElement(this.aMethod1, 0);
		this.aDB.addRelationAndTranspose(this.aClass1, Relation.DECLARES_METHOD, this.aMethod1);
		lSet = this.aDB.getRange(this.aClass1, Relation.DECLARES_METHOD);
		Assert.assertTrue(lSet.size() == 1);
		Assert.assertTrue(lSet.contains(this.aMethod1));
		lSet = this.aDB.getRange(this.aMethod1, Relation.T_DECLARES);
		Assert.assertTrue(lSet.size() == 1);
		Assert.assertTrue(lSet.contains(this.aClass1));
	}

	public void testHasRelations() {
		// Invalid case
		try {
			this.aDB.hasRelations(this.aClass1);
			Assert.fail("Expected ElementNotFoundException");
		} catch (final ElementNotFoundException pException) {
		}
		// Empty case
		this.aDB.addElement(this.aClass1, 0);
		Assert.assertFalse(this.aDB.hasRelations(this.aClass1));
		this.aDB.addElement(this.aMethod1, 0);
		this.aDB.addRelationAndTranspose(this.aClass1, Relation.DECLARES_METHOD, this.aMethod1);
		Assert.assertTrue(this.aDB.hasRelations(this.aClass1));
		Assert.assertTrue(this.aDB.hasRelations(this.aMethod1));
	}

	@SuppressWarnings("unchecked")
	public void testRemoveElement() {
		// Invalid case
		try {
			this.aDB.removeElement(this.aClass1);
			Assert.fail("Expected ElementNotFoundException");
		} catch (final ElementNotFoundException pException) {
		}
		this.aDB.addElement(this.aClass1, 0);
		this.aDB.removeElement(this.aClass1);
		Assert.assertFalse(this.aDB.contains(this.aClass1));
		this.aDB.addElement(this.aClass1, 0);
		this.aDB.addElement(this.aMethod1, 0);
		this.aDB.addRelationAndTranspose(this.aClass1, Relation.DECLARES_METHOD, this.aMethod1);
		this.aDB.addElement(this.aClass2, 0);
		this.aDB.addRelationAndTranspose(this.aClass2, Relation.ACCESSES, this.aClass1);
		this.aDB.addRelationAndTranspose(this.aMethod1, Relation.T_DECLARES, this.aClass2);
		this.aDB.removeElement(this.aClass1);
		Assert.assertFalse(this.aDB.contains(this.aClass1));
		Set lSet = this.aDB.getRange(this.aClass2, Relation.ACCESSES);
		Assert.assertTrue(lSet.size() == 0);
		lSet = this.aDB.getRange(this.aMethod1, Relation.T_DECLARES);
		Assert.assertTrue(lSet.size() == 1);
		Assert.assertTrue(lSet.contains(this.aClass2));
		lSet = this.aDB.getRange(this.aClass2, Relation.DECLARES_METHOD);
		Assert.assertTrue(lSet.size() == 1);
		Assert.assertTrue(lSet.contains(this.aMethod1));
	}

	@Override
	protected void setUp() throws Exception {
		this.aClass1 = FlyweightElementFactory.getElement(Category.CLASS, "a.b.c.Class1");
		this.aClass2 = FlyweightElementFactory.getElement(Category.CLASS, "a.b.c.Class2");
		this.aField1 = FlyweightElementFactory.getElement(Category.FIELD, "a.b.c.Class1.aField1");
		FlyweightElementFactory.getElement(Category.FIELD, "a.b.c.Class2.aField2");
		this.aMethod1 = FlyweightElementFactory.getElement(Category.METHOD, "a.b.c.Class1.method1()");
		this.aMethod2 = FlyweightElementFactory.getElement(Category.METHOD, "a.b.c.Class2.method2()");
		this.aDB = new ProgramDatabase();
	}

	@Override
	protected void tearDown() throws Exception {
	}
}
