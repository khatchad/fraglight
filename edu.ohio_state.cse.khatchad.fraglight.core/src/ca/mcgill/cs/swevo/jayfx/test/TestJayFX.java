/* JayFX - A Fact Extractor Plug-in for Eclipse
 * Copyright (C) 2006  McGill University (http://www.cs.mcgill.ca/~swevo/jayfx)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * $Revision: 1.9 $
 */

package ca.mcgill.cs.swevo.jayfx.test;

import java.util.Set;

import org.eclipse.jdt.core.IJavaElement;

import ca.mcgill.cs.swevo.jayfx.ConversionException;
import ca.mcgill.cs.swevo.jayfx.JayFX;
import ca.mcgill.cs.swevo.jayfx.RelationNotSupportedException;
import ca.mcgill.cs.swevo.jayfx.model.Category;
import ca.mcgill.cs.swevo.jayfx.model.FlyweightElementFactory;
import ca.mcgill.cs.swevo.jayfx.model.IElement;
import ca.mcgill.cs.swevo.jayfx.model.Relation;
import junit.framework.Assert;
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

public class TestJayFX extends TestCase {
	private JayFX aDB;

	public TestJayFX() {

	}

	@SuppressWarnings("unchecked")
	public void testAccesses() {
		Set lRange = this.aDB.getRange(this.getElement("a.b.A.<clinit>()", false), Relation.ACCESSES);
		Assert.assertEquals(3, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.A.aTest0", false)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.B.aTest1", false)));
		Assert.assertTrue(lRange.contains(this.getElement("java.lang.System.out", false)));

		lRange = this.aDB.getRange(this.getElement("a.b.A.<init>()", false), Relation.ACCESSES);
		Assert.assertEquals(2, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("java.lang.System.out", false)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.A.aTest1", false)));

		lRange = this.aDB.getRange(
				this.getElement("a.b.A.test1(I,Ljava.lang.String;,[Ljava.lang.String;,[[Ljava.lang.String;)", false),
				Relation.ACCESSES);
		Assert.assertEquals(5, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.A$AB.aTest1", false)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.A.aTest1", false)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.A.aTest2", false)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.A.aTest3", false)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.B.aTest1", false)));
	}

	@SuppressWarnings("unchecked")
	public void testCalls() {
		Set lRange = this.aDB.getRange(this.getElement("a.b.c.C1.main([Ljava.lang.String;)", false), Relation.CALLS);
		Assert.assertEquals(1, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.A.<init>(Ljava.lang.String;)", false)));

		// Calls to default super constructors are not included in the database
		lRange = this.aDB.getRange(this.getElement("a.b.A.<init>()", false), Relation.CALLS);
		Assert.assertTrue(lRange.contains(this.getElement("java.io.PrintStream.println(Ljava.lang.String;)", false)));
		Assert.assertEquals(1, lRange.size());

		lRange = this.aDB.getRange(this.getElement("a.b.c.C2.<init>()", false), Relation.CALLS);
		Assert.assertEquals(1, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.c.C1.<init>(Ljava.lang.String;)", false)));

		lRange = this.aDB.getRange(this.getElement("a.b.c.C2.<init>(Ljava.lang.String;)", false), Relation.CALLS);
		Assert.assertEquals(2, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.c.C1.<init>(Ljava.lang.String;)", false)));
		Assert.assertTrue(lRange.contains(this.getElement("java.io.PrintStream.println(Ljava.lang.String;)", false)));

		lRange = this.aDB.getRange(this.getElement("a.b.c.C2.<init>(Ljava.lang.String;,I)", false), Relation.CALLS);
		Assert.assertEquals(1, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.c.C2.<init>(Ljava.lang.String;)", false)));

		lRange = this.aDB.getRange(this.getElement("a.b.c.C2.doit(Ljava.lang.String;)", false), Relation.CALLS);
		Assert.assertEquals(2, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.c.C1.doit(Ljava.lang.String;)", false)));
		Assert.assertTrue(lRange.contains(this.getElement("java.io.PrintStream.println(Ljava.lang.String;)", false)));

		lRange = this.aDB.getRange(this.getElement("a.b.d.D1.doit()", false), Relation.CALLS);
		Assert.assertEquals(3, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.H.<init>()", false)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.H.toString()", false)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.K.toString()", false)));

		lRange = this.aDB.getRange(this.getElement("a.b.c.C3.doit()", false), Relation.CALLS);
		Assert.assertEquals(8, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.I2.doit()", false)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.J.doit()", false)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.D.doit()", false)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.E.doit()", false)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.F.doit()", false)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.G.doit()", false)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.K.doit()", false)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.D.<init>()", false)));
	}

	@SuppressWarnings("unchecked")
	public void testClassDeclares2() {
		final Set lRange = this.aDB.getRange(this.getElement("a.b.D", true), Relation.DECLARES_METHOD);
		Assert.assertEquals(2, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.D.doit()", false)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.D.equals(Ljava.lang.Object;)", false)));
	}

	public void testConverter() throws ConversionException {
		// Valid two-way conversions
		IElement[] lInitial = new IElement[18];
		lInitial[0] = this.getElement("a.b.A", true);
		lInitial[1] = this.getElement("a.b.A.aTest0", false);
		lInitial[2] = this.getElement("a.b.A.aTest1", false);
		lInitial[3] = this.getElement("a.b.A.aTest2", false);
		lInitial[4] = this.getElement("a.b.A.aTest3", false);
		lInitial[5] = this.getElement("a.b.A.aTest4", false);
		lInitial[6] = this.getElement("a.b.A.test1(I,Ljava.lang.String;,[Ljava.lang.String;,[[Ljava.lang.String;)",
				false);
		lInitial[7] = this.getElement("a.b.A.<init>(Ljava.lang.String;)", false);
		lInitial[8] = this.getElement("a.b.A$AA", true);
		lInitial[9] = this.getElement("a.b.A$AA$AAA", true);
		lInitial[10] = this.getElement("a.b.A$AA.doit(La.b.A$AA$AAA;)", false);
		lInitial[11] = this.getElement("a.b.A$AB", true);
		lInitial[12] = this.getElement("a.b.A$AB.aTest1", false);
		lInitial[13] = this.getElement("a.b.B", true);
		lInitial[14] = this.getElement("a.b.B.aTest2", false);
		lInitial[15] = this.getElement("java.lang.Runnable", true);
		lInitial[16] = this.getElement("java.io.PrintStream.println(Ljava.lang.String;)", false);
		lInitial[17] = this.getElement("a.b.I2", true);

		for (final IElement element : lInitial) {
			final IJavaElement lJavaElement = this.aDB.convertToJavaElement(element);
			final IElement lFinal = this.aDB.convertToElement(lJavaElement);
			Assert.assertTrue(element == lFinal);
		}

		// Invalid two-way conversions
		// To make sure these raise the proper exception and do not case a crash
		lInitial = new IElement[6];
		lInitial[0] = this.getElement("a.b.A$AA$1.doit(La.b.A$AA$1AAAA;)", false);
		lInitial[1] = this.getElement("a.b.A$AB.<clinit>()", false);
		lInitial[2] = this.getElement("a.b.A$1$AAAB", false);
		lInitial[3] = this.getElement("a.b.A$1", false);
		lInitial[4] = this.getElement("a.b.B.<init>()", false);
		lInitial[5] = this.getElement("a.b.A.<init>()", false);

		for (final IElement element : lInitial)
			try {
				@SuppressWarnings("unused")
				final IJavaElement lJavaElement = this.aDB.convertToJavaElement(element);
				Assert.fail("Expected ConversionException");
			} catch (final ConversionException pException) {
			}

	}

	@SuppressWarnings("unchecked")
	public void testDeclares() {
		final Set lRange = this.aDB.getRange(this.getElement("a.b.A", true), Relation.DECLARES_TYPE);
		lRange.addAll(this.aDB.getRange(this.getElement("a.b.A", true), Relation.DECLARES_METHOD));
		lRange.addAll(this.aDB.getRange(this.getElement("a.b.A", true), Relation.DECLARES_FIELD));

		Assert.assertEquals(11, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.A.aTest3", false)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.A$AA", true)));
		Assert.assertTrue(lRange.contains(
				this.getElement("a.b.A.test1(I,Ljava.lang.String;,[Ljava.lang.String;,[[Ljava.lang.String;)", false)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.A.aTest1", false)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.A.<init>()", false)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.A.<init>(Ljava.lang.String;)", false)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.A.<clinit>()", false)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.A.aTest2", false)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.A$AB", true)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.A.aTest0", false)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.A.aTest4", false)));
		//
		// lRange = aDB.getRange( getElement( "a.b.A$AA$1", true),
		// Relation.DECLARES );
		// assertEquals( 2, lRange.size() );
		// for (Object i : lRange)
		// {
		// System.out.println(i.toString());
		// }
		// assertTrue( lRange.contains( getElement(
		// "a.b.A$AA$1.doit(La.b.A$AA$1$AAAA;)", false)));
		// assertTrue( lRange.contains( getElement( "a.b.A$AA$1.doit()",
		// false)));

	}

	@SuppressWarnings("unchecked")
	public void testEnumAccess() {
		final Set lRange = this.aDB.getRange(this.getElement(
				"ca.cs.mcgill.swevo.jayfxbenchmark.enumtest.Planet.<init>(D,D,Lca.cs.mcgill.swevo.jayfxbenchmark.enumtest.Planet$Composition;)",
				false), Relation.ACCESSES);

		Assert.assertEquals(3, lRange.size());
		Assert.assertTrue(
				lRange.contains(this.getElement("ca.cs.mcgill.swevo.jayfxbenchmark.enumtest.Planet.mass", false)));
	}

	@SuppressWarnings("unchecked")
	public void testEnumConstantCreate() {
		try {
			@SuppressWarnings("unused")
			final Set lRange = this.aDB.getRange(this.getElement(
					"ca.cs.mcgill.swevo.jayfxbenchmark.enumtest.Planet.<init>(D,D,Lca.cs.mcgill.swevo.jayfxbenchmark.enumtest.Planet$Composition;)",
					false), Relation.CREATES);
			throw new AssertionFailedError("Previously unsupported relationship now found!");
		} catch (final RelationNotSupportedException e) {
		}
	}

	@SuppressWarnings("unchecked")
	public void testEnumDeclares() {
		Set lRange = this.aDB.getRange(this.getElement("ca.cs.mcgill.swevo.jayfxbenchmark.enumtest.Planet", true),
				Relation.DECLARES_METHOD);
		lRange.addAll(this.aDB.getRange(this.getElement("ca.cs.mcgill.swevo.jayfxbenchmark.enumtest.Planet", true),
				Relation.DECLARES_FIELD));
		for (final Object i : lRange)
			System.out.println(i.toString());
		Assert.assertEquals(21, lRange.size()); // isn't it 22?
		Assert.assertTrue(
				lRange.contains(this.getElement("ca.cs.mcgill.swevo.jayfxbenchmark.enumtest.Planet.MERCURY", false)));
		Assert.assertTrue(
				lRange.contains(this.getElement("ca.cs.mcgill.swevo.jayfxbenchmark.enumtest.Planet.VENUS", false)));
		Assert.assertTrue(
				lRange.contains(this.getElement("ca.cs.mcgill.swevo.jayfxbenchmark.enumtest.Planet.PLUTO", false)));
		Assert.assertTrue(
				lRange.contains(this.getElement("ca.cs.mcgill.swevo.jayfxbenchmark.enumtest.Planet.mass", false)));
		Assert.assertTrue(
				lRange.contains(this.getElement("ca.cs.mcgill.swevo.jayfxbenchmark.enumtest.Planet.mass()", false)));
		Assert.assertTrue(
				lRange.contains(this.getElement("ca.cs.mcgill.swevo.jayfxbenchmark.enumtest.Planet.G", false)));
		Assert.assertTrue(lRange
				.contains(this.getElement("ca.cs.mcgill.swevo.jayfxbenchmark.enumtest.Planet.<clinit>()", false)));
		Assert.assertFalse(
				lRange.contains(this.getElement("ca.cs.mcgill.swevo.jayfxbenchmark.enumtest.Planet.<init>()", false)));

		lRange = this.aDB.getRange(
				this.getElement("ca.cs.mcgill.swevo.jayfxbenchmark.enumtest.Planet$Composition", true),
				Relation.DECLARES_FIELD);
		Assert.assertEquals(3, lRange.size());
		Assert.assertFalse(lRange.contains(
				this.getElement("ca.cs.mcgill.swevo.jayfxbenchmark.enumtest.Planet$Composition.SOLID", false)));

		lRange = this.aDB.getRange(this.getElement("ca.cs.mcgill.swevo.jayfxbenchmark.enumtest.Card", true),
				Relation.DECLARES_FIELD);
		Assert.assertEquals(5, lRange.size());
		Assert.assertFalse(lRange
				.contains(this.getElement("ca.cs.mcgill.swevo.jayfxbenchmark.enumtest.Planet.<clinit>()", false)));
	}

	@SuppressWarnings("unchecked")
	public void testEnumTAccess() {
		final Set lRange = this.aDB.getRange(
				this.getElement("ca.cs.mcgill.swevo.jayfxbenchmark.enumtest.Planet.JUPITER", false),
				Relation.T_ACCESSES);
		// for (Object i : lRange)
		// {
		// System.out.println(i.toString());
		// }
		// System.out.println();
		// for (Object i : lRange)
		// {
		// System.out.println(i.toString());
		// }
		// assertEquals( 1, lRange.size() ); // not sure yet...
		Assert.assertTrue(lRange.contains(
				this.getElement("ca.cs.mcgill.swevo.jayfxbenchmark.enumtest.Planet.getLargestPlanet()", false)));

		// lRange = aDB.getRange( getElement(
		// "ca.cs.mcgill.swevo.jayfxbenchmark.enumtest.Planet$Composition.ROCK"
		// , false), Relation.T_ACCESSES );
		// System.out.println();
		// for (Object i : lRange)
		// {
		// System.out.println(i.toString());
		// }
		// assertEquals( 4, lRange.size() );
	}

	@SuppressWarnings("unchecked")
	public void testExtendsClass() {
		Set lRange = this.aDB.getRange(this.getElement("a.b.A", true), Relation.EXTENDS_CLASS);
		Assert.assertEquals(1, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("java.lang.Object", true)));

		lRange = this.aDB.getRange(this.getElement("a.b.B", true), Relation.EXTENDS_CLASS);
		Assert.assertEquals(1, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("java.lang.Object", true)));

		lRange = this.aDB.getRange(this.getElement("a.b.A$AA", true), Relation.EXTENDS_CLASS);
		Assert.assertEquals(1, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("java.lang.Object", true)));

		lRange = this.aDB.getRange(this.getElement("a.b.A$AB", true), Relation.EXTENDS_CLASS);
		Assert.assertEquals(0, lRange.size());

		lRange = this.aDB.getRange(this.getElement("a.b.A$AA$1", true), Relation.EXTENDS_CLASS);
		Assert.assertEquals(1, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("java.lang.Object", true)));

		lRange = this.aDB.getRange(this.getElement("a.b.A$AA$AAA", true), Relation.EXTENDS_CLASS);
		Assert.assertEquals(1, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("java.lang.Object", true)));

		lRange = this.aDB.getRange(this.getElement("a.b.A$AA$1AAAB", true), Relation.EXTENDS_CLASS);
		Assert.assertEquals(1, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("java.lang.Object", true)));

		lRange = this.aDB.getRange(this.getElement("a.b.C", true), Relation.EXTENDS_CLASS);
		Assert.assertEquals(1, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("java.lang.Object", true)));

		lRange = this.aDB.getRange(this.getElement("a.b.D", true), Relation.EXTENDS_CLASS);
		Assert.assertEquals(1, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("java.lang.Object", true)));

		lRange = this.aDB.getRange(this.getElement("a.b.E", true), Relation.EXTENDS_CLASS);
		Assert.assertEquals(1, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.D", true)));

		lRange = this.aDB.getRange(this.getElement("a.b.F", true), Relation.EXTENDS_CLASS);
		Assert.assertEquals(1, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.D", true)));

		lRange = this.aDB.getRange(this.getElement("a.b.G", true), Relation.EXTENDS_CLASS);
		Assert.assertEquals(1, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.F", true)));

		lRange = this.aDB.getRange(this.getElement("a.b.H", true), Relation.EXTENDS_CLASS);
		Assert.assertEquals(1, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.F", true)));

		lRange = this.aDB.getRange(this.getElement("a.b.J", true), Relation.EXTENDS_CLASS);
		Assert.assertEquals(1, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.C", true)));

		lRange = this.aDB.getRange(this.getElement("a.b.I1", true), Relation.EXTENDS_CLASS);
		Assert.assertEquals(0, lRange.size());

		lRange = this.aDB.getRange(this.getElement("java.lang.Object", true), Relation.EXTENDS_CLASS);
		Assert.assertEquals(0, lRange.size());

		lRange = this.aDB.getRange(this.getElement("java.lang.String", true), Relation.EXTENDS_CLASS);
		Assert.assertEquals(1, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("java.lang.Object", true)));
	}

	@SuppressWarnings("unchecked")
	public void testExtendsInterfaces() {
		Set lRange = this.aDB.getRange(this.getElement("a.b.A", true), Relation.EXTENDS_INTERFACES);
		Assert.assertEquals(0, lRange.size());

		lRange = this.aDB.getRange(this.getElement("a.b.I1", true), Relation.EXTENDS_INTERFACES);
		Assert.assertEquals(1, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.I2", true)));

		lRange = this.aDB.getRange(this.getElement("a.b.I2", true), Relation.EXTENDS_INTERFACES);
		Assert.assertEquals(0, lRange.size());

		lRange = this.aDB.getRange(this.getElement("a.b.I3", true), Relation.EXTENDS_INTERFACES);
		Assert.assertEquals(1, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.I2", true)));

		lRange = this.aDB.getRange(this.getElement("a.b.I4", true), Relation.EXTENDS_INTERFACES);
		Assert.assertEquals(0, lRange.size());

		lRange = this.aDB.getRange(this.getElement("a.b.I5", true), Relation.EXTENDS_INTERFACES);
		Assert.assertEquals(2, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("java.lang.Runnable", true)));
		Assert.assertTrue(lRange.contains(this.getElement("java.io.Serializable", true)));

	}

	@SuppressWarnings("unchecked")
	public void testGetRangeInProject() {
		Set lRange = this.aDB.getRangeInProject(this.getElement("a.b.c.C1.main([Ljava.lang.String;)", false),
				Relation.CALLS);
		Assert.assertEquals(1, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.A.<init>(Ljava.lang.String;)", false)));

		lRange = this.aDB.getRangeInProject(this.getElement("a.b.A.<init>()", false), Relation.CALLS);
		Assert.assertEquals(0, lRange.size());

		lRange = this.aDB.getRangeInProject(this.getElement("a.b.c.C2.<init>()", false), Relation.CALLS);
		Assert.assertEquals(1, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.c.C1.<init>(Ljava.lang.String;)", false)));

		lRange = this.aDB.getRangeInProject(this.getElement("a.b.c.C2.<init>(Ljava.lang.String;)", false),
				Relation.CALLS);
		Assert.assertEquals(1, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.c.C1.<init>(Ljava.lang.String;)", false)));

		lRange = this.aDB.getRangeInProject(this.getElement("a.b.c.C2.<init>(Ljava.lang.String;,I)", false),
				Relation.CALLS);
		Assert.assertEquals(1, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.c.C2.<init>(Ljava.lang.String;)", false)));

		lRange = this.aDB.getRangeInProject(this.getElement("a.b.c.C2.doit(Ljava.lang.String;)", false),
				Relation.CALLS);
		Assert.assertEquals(1, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.c.C1.doit(Ljava.lang.String;)", false)));

		lRange = this.aDB.getRangeInProject(this.getElement("a.b.d.D1.doit()", false), Relation.CALLS);
		Assert.assertEquals(3, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.H.<init>()", false)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.H.toString()", false)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.K.toString()", false)));

		lRange = this.aDB.getRangeInProject(this.getElement("a.b.c.C3.doit()", false), Relation.CALLS);
		Assert.assertEquals(8, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.I2.doit()", false)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.J.doit()", false)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.D.doit()", false)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.E.doit()", false)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.F.doit()", false)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.G.doit()", false)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.K.doit()", false)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.D.<init>()", false)));
	}

	@SuppressWarnings("unchecked")
	public void testImplementsInterface() {
		Set lRange = this.aDB.getRange(this.getElement("a.b.A", true), Relation.IMPLEMENTS_INTERFACE);
		Assert.assertEquals(0, lRange.size());

		lRange = this.aDB.getRange(this.getElement("a.b.A$AA$1", true), Relation.IMPLEMENTS_INTERFACE);
		Assert.assertEquals(1, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.A$AB", true)));

		lRange = this.aDB.getRange(this.getElement("a.b.C", true), Relation.IMPLEMENTS_INTERFACE);
		Assert.assertEquals(1, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.I1", true)));

		lRange = this.aDB.getRange(this.getElement("a.b.C", true), Relation.IMPLEMENTS_INTERFACE);
		Assert.assertEquals(1, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.I1", true)));

		lRange = this.aDB.getRange(this.getElement("a.b.D", true), Relation.IMPLEMENTS_INTERFACE);
		Assert.assertEquals(1, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.I2", true)));

		lRange = this.aDB.getRange(this.getElement("a.b.E", true), Relation.IMPLEMENTS_INTERFACE);
		Assert.assertEquals(1, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.I3", true)));

		lRange = this.aDB.getRange(this.getElement("a.b.F", true), Relation.IMPLEMENTS_INTERFACE);
		Assert.assertEquals(0, lRange.size());

		lRange = this.aDB.getRange(this.getElement("a.b.G", true), Relation.IMPLEMENTS_INTERFACE);
		Assert.assertEquals(0, lRange.size());

		lRange = this.aDB.getRange(this.getElement("a.b.H", true), Relation.IMPLEMENTS_INTERFACE);
		Assert.assertEquals(2, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.I4", true)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.I5", true)));

		lRange = this.aDB.getRange(this.getElement("a.b.I1", true), Relation.IMPLEMENTS_INTERFACE);
		Assert.assertEquals(0, lRange.size());

		lRange = this.aDB.getRange(this.getElement("a.b.I2", true), Relation.IMPLEMENTS_INTERFACE);
		Assert.assertEquals(0, lRange.size());

		lRange = this.aDB.getRange(this.getElement("a.b.I3", true), Relation.IMPLEMENTS_INTERFACE);
		Assert.assertEquals(0, lRange.size());

		lRange = this.aDB.getRange(this.getElement("a.b.I4", true), Relation.IMPLEMENTS_INTERFACE);
		Assert.assertEquals(0, lRange.size());

		lRange = this.aDB.getRange(this.getElement("a.b.I5", true), Relation.IMPLEMENTS_INTERFACE);
		Assert.assertEquals(0, lRange.size());

		lRange = this.aDB.getRange(this.getElement("a.b.J", true), Relation.IMPLEMENTS_INTERFACE);
		Assert.assertEquals(1, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.I6", true)));

		lRange = this.aDB.getRange(this.getElement("java.lang.String", true), Relation.IMPLEMENTS_INTERFACE);
		Assert.assertEquals(3, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("java.io.Serializable", true)));
		Assert.assertTrue(lRange.contains(this.getElement("java.lang.Comparable<Ljava.lang.String;>", true)));
		Assert.assertTrue(lRange.contains(this.getElement("java.lang.CharSequence", true)));
	}

	@SuppressWarnings("unchecked")
	public void testImportStatic() {
		final Set lRange = this.aDB.getRange(
				this.getElement("ca.cs.mcgill.swevo.jayfxbenchmark.generictest.TLA_QandA.answer(Ljava.lang.String;)",
						false),
				Relation.ACCESSES);
		Assert.assertEquals(3, lRange.size());
		Assert.assertTrue(
				lRange.contains(this.getElement("ca.cs.mcgill.swevo.jayfxbenchmark.generictest.TLA.FUD", false)));
		Assert.assertTrue(
				lRange.contains(this.getElement("ca.cs.mcgill.swevo.jayfxbenchmark.generictest.TLA.IBM", false)));
	}

	@SuppressWarnings("unchecked")
	public void testInnerClassDeclares() {
		final Set lRange = this.aDB.getRange(this.getElement("a.b.A$AA", true), Relation.DECLARES_TYPE);
		lRange.addAll(this.aDB.getRange(this.getElement("a.b.A$AA", true), Relation.DECLARES_METHOD));
		lRange.addAll(this.aDB.getRange(this.getElement("a.b.A$AA", true), Relation.DECLARES_FIELD));
		Assert.assertEquals(2, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.A$AA$AAA", true)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.A$AA.doit(La.b.A$AA$AAA;)", false)));
	}

	@SuppressWarnings("unchecked")
	public void testInterfaceDeclares() {
		final Set lRange = this.aDB.getRange(this.getElement("a.b.A$AB", true), Relation.DECLARES_TYPE);
		lRange.addAll(this.aDB.getRange(this.getElement("a.b.A$AB", true), Relation.DECLARES_METHOD));
		lRange.addAll(this.aDB.getRange(this.getElement("a.b.A$AB", true), Relation.DECLARES_FIELD));
		Assert.assertEquals(3, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.A$AB.<clinit>()", false)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.A$AB.aTest1", false)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.A$AB.doit()", false)));
	}

	public void testIsProjectElement() {
		Assert.assertTrue(this.aDB.isProjectElement(this.getElement("a.b.c.C2.<init>()", false)));
		Assert.assertTrue(this.aDB.isProjectElement(this.getElement("a.b.E", true)));
		Assert.assertTrue(this.aDB.isProjectElement(this.getElement("a.b.c.C1.doit(Ljava.lang.String;)", false)));
		Assert.assertTrue(this.aDB.isProjectElement(this.getElement("a.b.A.aTest2", false)));
		Assert.assertTrue(this.aDB.isProjectElement(this.getElement("a.b.A$1.doit(La.b.A$1$AAAA;)", false)));
		Assert.assertTrue(this.aDB.isProjectElement(this.getElement("a.b.D.doit()", false)));
		Assert.assertTrue(this.aDB.isProjectElement(this.getElement("a.b.F", true)));
		Assert.assertTrue(this.aDB.isProjectElement(this.getElement("a.b.I2", true)));
		Assert.assertTrue(this.aDB.isProjectElement(this.getElement("a.b.c.C1.main([Ljava.lang.String;)", false)));
		Assert.assertTrue(this.aDB.isProjectElement(this.getElement("a.b.D.equals(Ljava.lang.Object;)", false)));
		Assert.assertTrue(this.aDB.isProjectElement(
				this.getElement("a.b.A.test1(I,Ljava.lang.String;,[Ljava.lang.String;,[[Ljava.lang.String;)", false)));
		Assert.assertTrue(this.aDB.isProjectElement(this.getElement("a.b.A$AB.doit()", false)));
		Assert.assertTrue(this.aDB.isProjectElement(this.getElement("a.b.A$1$AAAA", true)));

		Assert.assertFalse(this.aDB.isProjectElement(this.getElement("java.lang.Object", true)));
		Assert.assertFalse(this.aDB.isProjectElement(this.getElement("java.lang.Runnable", true)));
		Assert.assertFalse(
				this.aDB.isProjectElement(this.getElement("java.io.PrintStream.println(Ljava.lang.String;)", false)));
		Assert.assertFalse(this.aDB.isProjectElement(this.getElement("java.io.PrintStream.println(I)", false)));
	}

	@SuppressWarnings("unchecked")
	public void testLocalClassDeclares() {
		final Set lRange = this.aDB.getRange(this.getElement("a.b.A$AA.doit(La.b.A$AA$AAA;)", false),
				Relation.DECLARES_TYPE);
		lRange.addAll(
				this.aDB.getRange(this.getElement("a.b.A$AA.doit(La.b.A$AA$AAA;)", false), Relation.DECLARES_METHOD));
		lRange.addAll(
				this.aDB.getRange(this.getElement("a.b.A$AA.doit(La.b.A$AA$AAA;)", false), Relation.DECLARES_FIELD));
		Assert.assertEquals(3, lRange.size());

		Assert.assertTrue(lRange.contains(this.getElement("a.b.A$AA$1AAAA", true)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.A$AA$1AAAB", true)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.A$AA$1", true)));

	}

	@SuppressWarnings("unchecked")
	public void testNonProjectExtendsInterfaces() {
		Set lRange = this.aDB.getRange(this.getElement("javax.imageio.stream.ImageInputStream", true),
				Relation.EXTENDS_INTERFACES);
		Assert.assertEquals(1, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("java.io.DataInput", true)));

		lRange = this.aDB.getRange(this.getElement("java.io.Externalizable", true), Relation.EXTENDS_INTERFACES);
		Assert.assertEquals(1, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("java.io.Serializable", true)));

		lRange = this.aDB.getRange(this.getElement("java.util.SortedSet", true), Relation.EXTENDS_INTERFACES);
		Assert.assertEquals(1, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("java.util.Set<TE;>", true)));
	}

	@SuppressWarnings("unchecked")
	public void testNonProjectTExtendInterfaces() {
		Set lRange = this.aDB.getRange(this.getElement("java.lang.Runnable", true), Relation.T_EXTENDS_INTERFACES);
		Assert.assertEquals(1, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.I5", true)));

		lRange = this.aDB.getRange(this.getElement("java.io.Serializable", true), Relation.T_EXTENDS_INTERFACES);
		Assert.assertEquals(1, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.I5", true)));
	}

	@SuppressWarnings("unchecked")
	public void testOverrides() {
		Set lRange = this.aDB.getRange(this.getElement("a.b.C.doit()", false), Relation.OVERRIDES);
		Assert.assertEquals(0, lRange.size());

		lRange = this.aDB.getRange(this.getElement("a.b.D.doit()", false), Relation.OVERRIDES);
		Assert.assertEquals(1, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.I2.doit()", false)));

		lRange = this.aDB.getRange(this.getElement("a.b.D.equals(Ljava.lang.Object;)", false), Relation.OVERRIDES);
		Assert.assertEquals(1, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("java.lang.Object.equals(Ljava.lang.Object;)", false)));

		lRange = this.aDB.getRange(this.getElement("a.b.E.doit()", false), Relation.OVERRIDES);
		Assert.assertEquals(2, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.D.doit()", false)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.I2.doit()", false)));

		lRange = this.aDB.getRange(this.getElement("a.b.F.doit()", false), Relation.OVERRIDES);
		Assert.assertEquals(2, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.D.doit()", false)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.I2.doit()", false)));

		lRange = this.aDB.getRange(this.getElement("a.b.G.doit()", false), Relation.OVERRIDES);
		Assert.assertEquals(3, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.D.doit()", false)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.I2.doit()", false)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.F.doit()", false)));

		lRange = this.aDB.getRange(this.getElement("a.b.H.run()", false), Relation.OVERRIDES);
		Assert.assertEquals(1, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("java.lang.Runnable.run()", false)));

		lRange = this.aDB.getRange(this.getElement("a.b.H.toString()", false), Relation.OVERRIDES);
		Assert.assertEquals(1, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("java.lang.Object.toString()", false)));

		lRange = this.aDB.getRange(this.getElement("a.b.I2.doit()", false), Relation.OVERRIDES);
		Assert.assertEquals(0, lRange.size());

		lRange = this.aDB.getRange(this.getElement("a.b.J.doit()", false), Relation.OVERRIDES);
		Assert.assertEquals(4, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.C.doit()", false)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.I1.doit()", false)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.I2.doit()", false)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.I6.doit()", false)));

		lRange = this.aDB.getRange(this.getElement("a.b.K.doit()", false), Relation.OVERRIDES);
		Assert.assertEquals(3, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.F.doit()", false)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.D.doit()", false)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.I2.doit()", false)));
	}

	@SuppressWarnings("unchecked")
	public void testSuperClassDeclares() {
		Set lRange = this.aDB.getRange(this.getElement("java.lang.Object", true), Relation.DECLARES_METHOD);
		Assert.assertEquals(14, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("java.lang.Object.registerNatives()", false)));
		Assert.assertTrue(lRange.contains(this.getElement("java.lang.Object.wait(J)", false)));
		Assert.assertTrue(lRange.contains(this.getElement("java.lang.Object.finalize()", false)));
		Assert.assertTrue(lRange.contains(this.getElement("java.lang.Object.toString()", false)));
		Assert.assertTrue(lRange.contains(this.getElement("java.lang.Object.hashCode()", false)));
		Assert.assertTrue(lRange.contains(this.getElement("java.lang.Object.equals(Ljava.lang.Object;)", false)));
		Assert.assertTrue(lRange.contains(this.getElement("java.lang.Object.getClass()", false)));
		Assert.assertTrue(lRange.contains(this.getElement("java.lang.Object.notifyAll()", false)));
		Assert.assertTrue(lRange.contains(this.getElement("java.lang.Object.clone()", false)));
		Assert.assertTrue(lRange.contains(this.getElement("java.lang.Object.notify()", false)));
		Assert.assertTrue(lRange.contains(this.getElement("java.lang.Object.wait()", false)));
		Assert.assertTrue(lRange.contains(this.getElement("java.lang.Object.<clinit>()", false)));
		Assert.assertTrue(lRange.contains(this.getElement("java.lang.Object.<init>()", false)));
		Assert.assertTrue(lRange.contains(this.getElement("java.lang.Object.wait(J,I)", false)));

		lRange = this.aDB.getRange(this.getElement("java.io.FileFilter", true), Relation.DECLARES_METHOD);
		Assert.assertEquals(1, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("java.io.FileFilter.accept(Ljava.io.File;)", false)));

		lRange = this.aDB.getRange(this.getElement("java.awt.BufferCapabilities", true), Relation.DECLARES_METHOD);
		Assert.assertEquals(12, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("java.awt.BufferCapabilities$FlipContents", true)));
		Assert.assertTrue(lRange.contains(this.getElement(
				"java.awt.BufferCapabilities.<init>(Ljava.awt.ImageCapabilities;,Ljava.awt.ImageCapabilities;,Ljava.awt.BufferCapabilities$FlipContents;)",
				false)));
		Assert.assertTrue(lRange.contains(this.getElement("java.awt.BufferCapabilities.frontCaps", false)));
		Assert.assertTrue(lRange.contains(this.getElement("java.awt.BufferCapabilities.flipContents", false)));
		Assert.assertTrue(
				lRange.contains(this.getElement("java.awt.BufferCapabilities.getFrontBufferCapabilities()", false)));
		Assert.assertTrue(lRange.contains(this.getElement("java.awt.BufferCapabilities.getFlipContents()", false)));
		Assert.assertTrue(lRange.contains(this.getElement("java.awt.BufferCapabilities.isPageFlipping()", false)));
		Assert.assertTrue(lRange.contains(this.getElement("java.awt.BufferCapabilities.backCaps", false)));
		Assert.assertTrue(lRange.contains(this.getElement("java.awt.BufferCapabilities.clone()", false)));
		Assert.assertTrue(
				lRange.contains(this.getElement("java.awt.BufferCapabilities.getBackBufferCapabilities()", false)));
		Assert.assertTrue(
				lRange.contains(this.getElement("java.awt.BufferCapabilities.isFullScreenRequired()", false)));
		Assert.assertTrue(
				lRange.contains(this.getElement("java.awt.BufferCapabilities.isMultiBufferAvailable()", false)));

		lRange = this.aDB.getRange(this.getElement("java.awt.BufferCapabilities$FlipContents", true),
				Relation.DECLARES_FIELD);
		lRange.addAll(this.aDB.getRange(this.getElement("java.awt.BufferCapabilities$FlipContents", true),
				Relation.DECLARES_METHOD));
		Assert.assertEquals(11, lRange.size());
		Assert.assertTrue(
				lRange.contains(this.getElement("java.awt.BufferCapabilities$FlipContents.I_UNDEFINED", false)));
		Assert.assertTrue(
				lRange.contains(this.getElement("java.awt.BufferCapabilities$FlipContents.BACKGROUND", false)));
		Assert.assertTrue(
				lRange.contains(this.getElement("java.awt.BufferCapabilities$FlipContents.UNDEFINED", false)));
		Assert.assertTrue(lRange.contains(this.getElement("java.awt.BufferCapabilities$FlipContents.PRIOR", false)));
		Assert.assertTrue(lRange.contains(this.getElement("java.awt.BufferCapabilities$FlipContents.COPIED", false)));
		Assert.assertTrue(lRange.contains(this.getElement("java.awt.BufferCapabilities$FlipContents.I_COPIED", false)));
		Assert.assertTrue(lRange.contains(this.getElement("java.awt.BufferCapabilities$FlipContents.I_PRIOR", false)));
		Assert.assertTrue(lRange.contains(this.getElement("java.awt.BufferCapabilities$FlipContents.NAMES", false)));
		Assert.assertTrue(
				lRange.contains(this.getElement("java.awt.BufferCapabilities$FlipContents.I_BACKGROUND", false)));
		Assert.assertTrue(
				lRange.contains(this.getElement("java.awt.BufferCapabilities$FlipContents.<init>(I)", false)));
		Assert.assertTrue(
				lRange.contains(this.getElement("java.awt.BufferCapabilities$FlipContents.<clinit>()", false)));
	}

	@SuppressWarnings("unchecked")
	public void testTAccesses() {
		Set lRange = this.aDB.getRange(this.getElement("a.b.A.aTest0", false), Relation.T_ACCESSES);
		Assert.assertEquals(1, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.A.<clinit>()", false)));

		lRange = this.aDB.getRange(this.getElement("a.b.A.aTest1", false), Relation.T_ACCESSES);
		Assert.assertEquals(3, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.A.<init>()", false)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.A.<init>(Ljava.lang.String;)", false)));
		Assert.assertTrue(lRange.contains(
				this.getElement("a.b.A.test1(I,Ljava.lang.String;,[Ljava.lang.String;,[[Ljava.lang.String;)", false)));

		lRange = this.aDB.getRange(this.getElement("a.b.A.aTest2", false), Relation.T_ACCESSES);
		Assert.assertEquals(2, lRange.size());
		Assert.assertTrue(lRange.contains(
				this.getElement("a.b.A.test1(I,Ljava.lang.String;,[Ljava.lang.String;,[[Ljava.lang.String;)", false)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.A.<init>(Ljava.lang.String;)", false)));

		lRange = this.aDB.getRange(this.getElement("a.b.A.aTest3", false), Relation.T_ACCESSES);
		Assert.assertEquals(1, lRange.size());
		Assert.assertTrue(lRange.contains(
				this.getElement("a.b.A.test1(I,Ljava.lang.String;,[Ljava.lang.String;,[[Ljava.lang.String;)", false)));

		lRange = this.aDB.getRange(this.getElement("a.b.A.aTest4", false), Relation.T_ACCESSES);
		Assert.assertEquals(0, lRange.size());

		lRange = this.aDB.getRange(this.getElement("a.b.A$AB.aTest1", false), Relation.T_ACCESSES);
		Assert.assertEquals(2, lRange.size());
		Assert.assertTrue(lRange.contains(
				this.getElement("a.b.A.test1(I,Ljava.lang.String;,[Ljava.lang.String;,[[Ljava.lang.String;)", false)));

		lRange = this.aDB.getRange(this.getElement("a.b.B.aTest1", false), Relation.T_ACCESSES);
		Assert.assertEquals(3, lRange.size());
		Assert.assertTrue(lRange.contains(
				this.getElement("a.b.A.test1(I,Ljava.lang.String;,[Ljava.lang.String;,[[Ljava.lang.String;)", false)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.A.<clinit>()", false)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.B.<init>()", false)));
	}

	@SuppressWarnings("unchecked")
	public void testTCalls() {
		Set lRange = this.aDB.getRange(this.getElement("a.b.B.<init>()", false), Relation.T_CALLS);
		Assert.assertEquals(2, lRange.size());
		Assert.assertTrue(lRange.contains(
				this.getElement("a.b.A.test1(I,Ljava.lang.String;,[Ljava.lang.String;,[[Ljava.lang.String;)", false)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.A.<clinit>()", false)));

		lRange = this.aDB.getRange(this.getElement("a.b.I2.doit()", false), Relation.T_CALLS);
		Assert.assertEquals(1, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.c.C3.doit()", false)));

		lRange = this.aDB.getRange(this.getElement("a.b.K.doit()", false), Relation.T_CALLS);
		Assert.assertEquals(2, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.c.C3.doit()", false)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.d.D1.m1()", false)));
	}

	@SuppressWarnings("unchecked")
	public void testTExtendInterfaces() {
		Set lRange = this.aDB.getRange(this.getElement("a.b.A", true), Relation.T_EXTENDS_INTERFACES);
		Assert.assertEquals(0, lRange.size());

		lRange = this.aDB.getRange(this.getElement("a.b.I1", true), Relation.T_EXTENDS_INTERFACES);
		Assert.assertEquals(0, lRange.size());

		lRange = this.aDB.getRange(this.getElement("a.b.I2", true), Relation.T_EXTENDS_INTERFACES);
		Assert.assertEquals(3, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.I1", true)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.I3", true)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.I6", true)));

		lRange = this.aDB.getRange(this.getElement("a.b.I3", true), Relation.T_EXTENDS_INTERFACES);
		Assert.assertEquals(0, lRange.size());

		lRange = this.aDB.getRange(this.getElement("a.b.I4", true), Relation.T_EXTENDS_INTERFACES);
		Assert.assertEquals(0, lRange.size());

		lRange = this.aDB.getRange(this.getElement("a.b.I5", true), Relation.T_EXTENDS_INTERFACES);
		Assert.assertEquals(0, lRange.size());

	}

	@SuppressWarnings("unchecked")
	public void testTExtendsClass() {
		Set lRange = this.aDB.getRange(this.getElement("a.b.A", true), Relation.T_EXTENDS_CLASS);
		Assert.assertEquals(0, lRange.size());

		lRange = this.aDB.getRange(this.getElement("a.b.B", true), Relation.T_EXTENDS_CLASS);
		Assert.assertEquals(0, lRange.size());

		lRange = this.aDB.getRange(this.getElement("a.b.A$AA", true), Relation.T_EXTENDS_CLASS);
		Assert.assertEquals(0, lRange.size());

		lRange = this.aDB.getRange(this.getElement("a.b.A$AB", true), Relation.T_EXTENDS_CLASS);
		Assert.assertEquals(0, lRange.size());

		lRange = this.aDB.getRange(this.getElement("a.b.A$AA$1", true), Relation.T_EXTENDS_CLASS);
		Assert.assertEquals(0, lRange.size());

		lRange = this.aDB.getRange(this.getElement("a.b.A$AA$AAA", true), Relation.T_EXTENDS_CLASS);
		Assert.assertEquals(0, lRange.size());

		lRange = this.aDB.getRange(this.getElement("a.b.A$AA$1AAAB", true), Relation.T_EXTENDS_CLASS);
		Assert.assertEquals(0, lRange.size());

		lRange = this.aDB.getRange(this.getElement("a.b.C", true), Relation.T_EXTENDS_CLASS);
		Assert.assertEquals(1, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.J", true)));

		lRange = this.aDB.getRange(this.getElement("a.b.D", true), Relation.T_EXTENDS_CLASS);
		Assert.assertEquals(2, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.E", true)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.F", true)));

		lRange = this.aDB.getRange(this.getElement("a.b.E", true), Relation.T_EXTENDS_CLASS);
		Assert.assertEquals(0, lRange.size());

		lRange = this.aDB.getRange(this.getElement("a.b.F", true), Relation.T_EXTENDS_CLASS);
		Assert.assertEquals(2, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.G", true)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.H", true)));

		lRange = this.aDB.getRange(this.getElement("a.b.G", true), Relation.T_EXTENDS_CLASS);
		Assert.assertEquals(0, lRange.size());

		lRange = this.aDB.getRange(this.getElement("a.b.H", true), Relation.T_EXTENDS_CLASS);
		Assert.assertEquals(1, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.K", true)));

		lRange = this.aDB.getRange(this.getElement("a.b.J", true), Relation.T_EXTENDS_CLASS);
		Assert.assertEquals(0, lRange.size());

		lRange = this.aDB.getRange(this.getElement("a.b.I1", true), Relation.T_EXTENDS_CLASS);
		Assert.assertEquals(0, lRange.size());
	}

	@SuppressWarnings("unchecked")
	public void testTImplementsInterface() {
		Set lRange = this.aDB.getRange(this.getElement("a.b.A", true), Relation.T_IMPLEMENTS_INTERFACE);
		Assert.assertEquals(0, lRange.size());

		lRange = this.aDB.getRange(this.getElement("a.b.A$AB", true), Relation.T_IMPLEMENTS_INTERFACE);
		Assert.assertEquals(1, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.A$AA$1", true)));

		lRange = this.aDB.getRange(this.getElement("a.b.I", true), Relation.T_IMPLEMENTS_INTERFACE);
		Assert.assertEquals(0, lRange.size());

		lRange = this.aDB.getRange(this.getElement("a.b.I1", true), Relation.T_IMPLEMENTS_INTERFACE);
		Assert.assertEquals(1, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.C", true)));

		lRange = this.aDB.getRange(this.getElement("a.b.I2", true), Relation.T_IMPLEMENTS_INTERFACE);
		Assert.assertEquals(1, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.D", true)));

		lRange = this.aDB.getRange(this.getElement("a.b.I3", true), Relation.T_IMPLEMENTS_INTERFACE);
		Assert.assertEquals(1, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.E", true)));

		lRange = this.aDB.getRange(this.getElement("a.b.I4", true), Relation.T_IMPLEMENTS_INTERFACE);
		Assert.assertEquals(1, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.H", true)));

		lRange = this.aDB.getRange(this.getElement("a.b.I5", true), Relation.T_IMPLEMENTS_INTERFACE);
		Assert.assertEquals(1, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.H", true)));
	}

	@SuppressWarnings("unchecked")
	public void testTOverrides() {
		Set lRange = this.aDB.getRange(this.getElement("a.b.A$AA.doit(La.b.A$AA$AAA;)", false), Relation.T_OVERRIDES);
		Assert.assertEquals(0, lRange.size());

		lRange = this.aDB.getRange(this.getElement("a.b.C.doit()", false), Relation.T_OVERRIDES);
		Assert.assertEquals(1, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.J.doit()", false)));

		lRange = this.aDB.getRange(this.getElement("a.b.D.doit()", false), Relation.T_OVERRIDES);
		Assert.assertEquals(4, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.E.doit()", false)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.F.doit()", false)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.G.doit()", false)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.K.doit()", false)));

		lRange = this.aDB.getRange(this.getElement("java.lang.Object.equals(Ljava.lang.Object;)", false),
				Relation.T_OVERRIDES);
		// for (Object i : lRange)
		// {
		// System.out.println(i.toString());
		// }
		Assert.assertEquals(2, lRange.size()); // TODO: it overrides itself as
												// well??
		Assert.assertTrue(lRange.contains(this.getElement("a.b.D.equals(Ljava.lang.Object;)", false)));

		lRange = this.aDB.getRange(this.getElement("a.b.E.doit()", false), Relation.T_OVERRIDES);
		Assert.assertEquals(0, lRange.size());

		lRange = this.aDB.getRange(this.getElement("a.b.F.doit()", false), Relation.T_OVERRIDES);
		Assert.assertEquals(2, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.K.doit()", false)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.G.doit()", false)));

		lRange = this.aDB.getRange(this.getElement("a.b.G.doit()", false), Relation.T_OVERRIDES);
		Assert.assertEquals(0, lRange.size());

		lRange = this.aDB.getRange(this.getElement("java.lang.Object.toString()", false), Relation.T_OVERRIDES);
		Assert.assertEquals(2, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.K.toString()", false)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.H.toString()", false)));

		lRange = this.aDB.getRange(this.getElement("a.b.I1.doit()", false), Relation.T_OVERRIDES);
		Assert.assertEquals(1, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.J.doit()", false)));

		lRange = this.aDB.getRange(this.getElement("a.b.I2.doit()", false), Relation.T_OVERRIDES);
		Assert.assertEquals(6, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.D.doit()", false)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.E.doit()", false)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.F.doit()", false)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.G.doit()", false)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.J.doit()", false)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.K.doit()", false)));
	}

	@SuppressWarnings("unchecked")
	public void testTransExtends() {
		Set lRange = this.aDB.getRange(this.getElement("a.b.A$AB", true), Relation.TRANS_EXTENDS);
		Assert.assertEquals(0, lRange.size());

		lRange = this.aDB.getRange(this.getElement("a.b.I1", true), Relation.TRANS_EXTENDS);
		Assert.assertEquals(0, lRange.size());

		lRange = this.aDB.getRange(this.getElement("a.b.A", true), Relation.TRANS_EXTENDS);
		Assert.assertEquals(1, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("java.lang.Object", true)));

		lRange = this.aDB.getRange(this.getElement("a.b.B", true), Relation.TRANS_EXTENDS);
		Assert.assertEquals(1, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("java.lang.Object", true)));

		lRange = this.aDB.getRange(this.getElement("a.b.C", true), Relation.TRANS_EXTENDS);
		Assert.assertEquals(1, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("java.lang.Object", true)));

		lRange = this.aDB.getRange(this.getElement("a.b.D", true), Relation.TRANS_EXTENDS);
		Assert.assertEquals(1, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("java.lang.Object", true)));

		lRange = this.aDB.getRange(this.getElement("a.b.E", true), Relation.TRANS_EXTENDS);
		Assert.assertEquals(2, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("java.lang.Object", true)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.D", true)));

		lRange = this.aDB.getRange(this.getElement("a.b.F", true), Relation.TRANS_EXTENDS);
		Assert.assertEquals(2, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("java.lang.Object", true)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.D", true)));

		lRange = this.aDB.getRange(this.getElement("a.b.G", true), Relation.TRANS_EXTENDS);
		Assert.assertEquals(3, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("java.lang.Object", true)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.D", true)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.F", true)));

		lRange = this.aDB.getRange(this.getElement("a.b.H", true), Relation.TRANS_EXTENDS);
		Assert.assertEquals(3, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("java.lang.Object", true)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.D", true)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.F", true)));

		lRange = this.aDB.getRange(this.getElement("a.b.J", true), Relation.TRANS_EXTENDS);
		Assert.assertEquals(2, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("java.lang.Object", true)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.C", true)));

		lRange = this.aDB.getRange(this.getElement("a.b.K", true), Relation.TRANS_EXTENDS);
		Assert.assertEquals(4, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("java.lang.Object", true)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.D", true)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.F", true)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.H", true)));
	}

	@SuppressWarnings("unchecked")
	public void testTransitivelyImplements() {
		Set lRange = this.aDB.getRange(this.getElement("java.lang.Object", true), Relation.TRANS_IMPLEMENTS);
		Assert.assertEquals(0, lRange.size());

		lRange = this.aDB.getRange(this.getElement("a.b.A", true), Relation.TRANS_IMPLEMENTS);
		Assert.assertEquals(0, lRange.size());

		lRange = this.aDB.getRange(this.getElement("a.b.B", true), Relation.TRANS_IMPLEMENTS);
		Assert.assertEquals(0, lRange.size());

		lRange = this.aDB.getRange(this.getElement("a.b.A$AA", true), Relation.TRANS_IMPLEMENTS);
		Assert.assertEquals(0, lRange.size());

		lRange = this.aDB.getRange(this.getElement("a.b.A$AA$AAA", true), Relation.TRANS_IMPLEMENTS);
		Assert.assertEquals(0, lRange.size());

		lRange = this.aDB.getRange(this.getElement("a.b.A$AA$1", true), Relation.TRANS_IMPLEMENTS);
		Assert.assertEquals(1, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.A$AB", true)));

		lRange = this.aDB.getRange(this.getElement("a.b.C", true), Relation.TRANS_IMPLEMENTS);
		Assert.assertEquals(2, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.I1", true)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.I2", true)));

		lRange = this.aDB.getRange(this.getElement("a.b.D", true), Relation.TRANS_IMPLEMENTS);
		Assert.assertEquals(1, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.I2", true)));

		lRange = this.aDB.getRange(this.getElement("a.b.E", true), Relation.TRANS_IMPLEMENTS);
		Assert.assertEquals(2, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.I2", true)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.I3", true)));

		lRange = this.aDB.getRange(this.getElement("a.b.F", true), Relation.TRANS_IMPLEMENTS);
		Assert.assertEquals(1, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.I2", true)));

		lRange = this.aDB.getRange(this.getElement("a.b.G", true), Relation.TRANS_IMPLEMENTS);
		Assert.assertEquals(1, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.I2", true)));

		lRange = this.aDB.getRange(this.getElement("a.b.H", true), Relation.TRANS_IMPLEMENTS);
		Assert.assertEquals(5, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.I2", true)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.I4", true)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.I5", true)));
		Assert.assertTrue(lRange.contains(this.getElement("java.lang.Runnable", true)));
		Assert.assertTrue(lRange.contains(this.getElement("java.io.Serializable", true)));

		lRange = this.aDB.getRange(this.getElement("a.b.K", true), Relation.TRANS_IMPLEMENTS);
		Assert.assertEquals(5, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.I2", true)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.I4", true)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.I5", true)));
		Assert.assertTrue(lRange.contains(this.getElement("java.lang.Runnable", true)));
		Assert.assertTrue(lRange.contains(this.getElement("java.io.Serializable", true)));

		lRange = this.aDB.getRange(this.getElement("a.b.J", true), Relation.TRANS_IMPLEMENTS);
		Assert.assertEquals(3, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.I1", true)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.I2", true)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.I6", true)));
	}

	@SuppressWarnings("unchecked")
	public void testTTransExtends() {
		Set lRange = this.aDB.getRange(this.getElement("a.b.A$AB", true), Relation.T_TRANS_EXTENDS);
		Assert.assertEquals(0, lRange.size());

		lRange = this.aDB.getRange(this.getElement("a.b.A$AA$1", true), Relation.T_TRANS_EXTENDS);
		Assert.assertEquals(0, lRange.size());

		lRange = this.aDB.getRange(this.getElement("a.b.I1", true), Relation.T_TRANS_EXTENDS);
		Assert.assertEquals(0, lRange.size());

		lRange = this.aDB.getRange(this.getElement("a.b.C", true), Relation.T_TRANS_EXTENDS);
		Assert.assertEquals(1, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.J", true)));

		lRange = this.aDB.getRange(this.getElement("a.b.D", true), Relation.T_TRANS_EXTENDS);
		Assert.assertEquals(5, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.E", true)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.F", true)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.G", true)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.H", true)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.K", true)));

		lRange = this.aDB.getRange(this.getElement("a.b.E", true), Relation.T_TRANS_EXTENDS);
		Assert.assertEquals(0, lRange.size());

		lRange = this.aDB.getRange(this.getElement("a.b.F", true), Relation.T_TRANS_EXTENDS);
		Assert.assertEquals(3, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.G", true)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.H", true)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.K", true)));

		lRange = this.aDB.getRange(this.getElement("a.b.G", true), Relation.T_TRANS_EXTENDS);
		Assert.assertEquals(0, lRange.size());

		lRange = this.aDB.getRange(this.getElement("a.b.H", true), Relation.T_TRANS_EXTENDS);
		Assert.assertEquals(1, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.K", true)));

		lRange = this.aDB.getRange(this.getElement("a.b.I", true), Relation.T_TRANS_EXTENDS);
		Assert.assertEquals(0, lRange.size());

		lRange = this.aDB.getRange(this.getElement("a.b.J", true), Relation.T_TRANS_EXTENDS);
		Assert.assertEquals(0, lRange.size());
	}

	@SuppressWarnings("unchecked")
	public void testTTransitivelyImplements() {
		Set lRange = this.aDB.getRange(this.getElement("java.lang.Object", true), Relation.T_TRANS_IMPLEMENTS);
		Assert.assertEquals(0, lRange.size());

		lRange = this.aDB.getRange(this.getElement("a.b.A", true), Relation.T_TRANS_IMPLEMENTS);
		Assert.assertEquals(0, lRange.size());

		lRange = this.aDB.getRange(this.getElement("a.b.A$AB", true), Relation.T_TRANS_IMPLEMENTS);
		Assert.assertEquals(1, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.A$AA$1", true)));

		lRange = this.aDB.getRange(this.getElement("a.b.I1", true), Relation.T_TRANS_IMPLEMENTS);
		Assert.assertEquals(2, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.C", true)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.J", true)));

		lRange = this.aDB.getRange(this.getElement("a.b.I2", true), Relation.T_TRANS_IMPLEMENTS);
		Assert.assertEquals(8, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.C", true)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.J", true)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.E", true)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.F", true)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.G", true)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.H", true)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.D", true)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.K", true)));

		lRange = this.aDB.getRange(this.getElement("a.b.I3", true), Relation.T_TRANS_IMPLEMENTS);
		Assert.assertEquals(1, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.E", true)));

		lRange = this.aDB.getRange(this.getElement("a.b.I4", true), Relation.T_TRANS_IMPLEMENTS);
		Assert.assertEquals(2, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.H", true)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.K", true)));

		lRange = this.aDB.getRange(this.getElement("a.b.I5", true), Relation.T_TRANS_IMPLEMENTS);
		Assert.assertEquals(2, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.H", true)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.K", true)));

		lRange = this.aDB.getRange(this.getElement("java.lang.Runnable", true), Relation.T_TRANS_IMPLEMENTS);
		Assert.assertEquals(2, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.H", true)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.K", true)));

		lRange = this.aDB.getRange(this.getElement("java.io.Serializable", true), Relation.T_TRANS_IMPLEMENTS);
		Assert.assertEquals(2, lRange.size());
		Assert.assertTrue(lRange.contains(this.getElement("a.b.H", true)));
		Assert.assertTrue(lRange.contains(this.getElement("a.b.K", true)));
	}

	private IElement getElement(final String pId, final boolean pIsClass) {
		if (pIsClass)
			return FlyweightElementFactory.getElement(Category.CLASS, pId);
		else if (pId.endsWith(")"))
			return FlyweightElementFactory.getElement(Category.METHOD, pId);
		else
			return FlyweightElementFactory.getElement(Category.FIELD, pId);
	}

	@Override
	protected void setUp() throws Exception {

		// try
		// {
		// aDB.initialize(
		// ResourcesPlugin.getWorkspace().getRoot().getProject("JayFXBenchmark"),
		// lMonitor, true );
		// }
		// catch( JayFXException pException )
		// {
		// fail( "Cannot load JavaDB " + pException.getMessage() );
		// }
		// aDB.dumpConverter();
	}

	@Override
	protected void tearDown() throws Exception {
	}

}
