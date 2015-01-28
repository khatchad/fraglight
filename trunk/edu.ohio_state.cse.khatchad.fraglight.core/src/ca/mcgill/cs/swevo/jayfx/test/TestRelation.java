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

import junit.framework.Assert;
import junit.framework.TestCase;
import ca.mcgill.cs.swevo.jayfx.model.Relation;

public class TestRelation extends TestCase {

	private Relation[] aDirectRelations;
	private Relation[] aIndirectRelations;

	@Override
	public void setUp() {
		this.aDirectRelations = Relation.getAllRelations(true);
		this.aIndirectRelations = Relation.getAllRelations(false);
	}

	/**
	 * Check if there are equal number of direct and indirection relations.
	 * 
	 */
	public void test1() {
		Assert.assertEquals(this.aDirectRelations.length,
				this.aIndirectRelations.length);
	}

	/**
	 * Check if the inverse of a direction relation is equal to its copy in the
	 * indirection relation collections.
	 * 
	 */
	public void test2() {
		for (int i = 0; i < this.aDirectRelations.length; i++)
			Assert.assertEquals(this.aDirectRelations[i].getInverseRelation(),
					this.aIndirectRelations[i]);
	}
}
