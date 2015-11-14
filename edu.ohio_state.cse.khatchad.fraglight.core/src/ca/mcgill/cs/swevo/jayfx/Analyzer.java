/* JayFX - A Fact Extractor Plug-in for Eclipse
 * Copyright (C) 2006  McGill University (http://www.cs.mcgill.ca/~swevo/jayfx)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * $Revision: 1.9 $
 */

package ca.mcgill.cs.swevo.jayfx;

import java.util.HashSet;
import java.util.Set;

import ca.mcgill.cs.swevo.jayfx.model.IElement;
import ca.mcgill.cs.swevo.jayfx.model.Relation;
import edu.ohio_state.cse.khatchad.fraglight.core.util.Util;

/**
 * Query engine for a program database
 */
public class Analyzer {
	private final ProgramDatabase aDB;

	/**
	 * @param pDB
	 *            The program database
	 * @pre pDB != null
	 */
	public Analyzer(final ProgramDatabase pDB) {
		Util.assertExpression(pDB != null);
		this.aDB = pDB;
	}

	/**
	 * Returns all the elements matching a defined relations
	 * 
	 * @param pElement
	 * @param pRelation
	 * @return
	 */
	public Set<IElement> getRange(final IElement pElement, final Relation pRelation) {
		Set<IElement> lReturn = new HashSet<IElement>();
		if (pRelation == Relation.DECLARES_TYPE || pRelation == Relation.DECLARES_METHOD
				|| pRelation == Relation.DECLARES_FIELD || pRelation == Relation.ACCESSES
				|| pRelation == Relation.T_ACCESSES || pRelation == Relation.SETS || pRelation == Relation.GETS
				|| pRelation == Relation.EXTENDS_CLASS || pRelation == Relation.T_EXTENDS_CLASS
				|| pRelation == Relation.EXTENDS_INTERFACES || pRelation == Relation.T_EXTENDS_INTERFACES
				|| pRelation == Relation.OVERRIDES || pRelation == Relation.T_OVERRIDES
				|| pRelation == Relation.OVERLOADS || pRelation == Relation.ANNOTATES || pRelation == Relation.ADVISES
				|| pRelation == Relation.IMPLEMENTS_INTERFACE || pRelation == Relation.T_IMPLEMENTS_INTERFACE
				|| pRelation == Relation.CONTAINS || pRelation == Relation.IMPLEMENTS_METHOD)
			lReturn.addAll(this.aDB.getRange(pElement, pRelation));
		else if (pRelation == Relation.TRANS_EXTENDS)
			lReturn = this.getTransitivelyExtends(pElement);
		else if (pRelation == Relation.T_TRANS_EXTENDS)
			lReturn = this.getTTransitivelyExtends(pElement);
		else if (pRelation == Relation.TRANS_IMPLEMENTS)
			lReturn = this.getTransitivelyImplements(pElement);
		else if (pRelation == Relation.T_TRANS_IMPLEMENTS)
			lReturn = this.getTTransitivelyImplements(pElement);
		else if (pRelation == Relation.CALLS)
			lReturn = this.getCalls(pElement);
		else if (pRelation == Relation.T_CALLS)
			lReturn = this.getTCalls(pElement);
		else
			throw new RelationNotSupportedException(pRelation.getName());
		return lReturn;
	}

	/**
	 * Does not do class-hierarchy analysis
	 * 
	 * @param pElement
	 * @return
	 */
	private Set<IElement> getCalls(final IElement pElement) {
		// Util.assertExpression(pElement instanceof MethodElement);

		final Set<IElement> lReturn = new HashSet<IElement>();
		// static stuff
		lReturn.addAll(this.aDB.getRange(pElement, Relation.STATIC_CALLS));

		// dynamic stuff
		final Set<IElement> lVirtualCalls = this.aDB.getRange(pElement, Relation.CALLS);

		lReturn.addAll(lVirtualCalls);
		for (final IElement lVirtualCallMember : lVirtualCalls)
			// Note: a static method cannot be overriden
			lReturn.addAll(this.aDB.getRange(lVirtualCallMember, Relation.T_OVERRIDES));

		return lReturn;
	}

	/**
	 * Does not do class-hierarchy analysis
	 * 
	 * @param pElement
	 *            IMethodElement
	 * @return Set of IMethodElement that calls the method
	 */
	private Set<IElement> getTCalls(final IElement pElement) {
		// Util.assertExpression(pElement instanceof MethodElement);

		final Set<IElement> lReturn = new HashSet<IElement>();
		// static stuff
		lReturn.addAll(this.aDB.getRange(pElement, Relation.T_STATIC_CALLS));

		// dynamic stuff
		lReturn.addAll(this.aDB.getRange(pElement, Relation.T_CALLS));
		final Set<IElement> lOverrides = this.aDB.getRange(pElement, Relation.OVERRIDES);
		for (final IElement lOverridsElement : lOverrides)
			// Note: a static method cannot be overriden
			lReturn.addAll(this.aDB.getRange(lOverridsElement, Relation.T_CALLS));
		return lReturn;
	}

	/**
	 * Returns all the classes extending class pElement directly or indirectly.
	 * 
	 * @param pElement
	 *            The domain class
	 * @return A set of IElement containing classes
	 */
	private Set<IElement> getTransitivelyExtends(final IElement pElement) {
		// Util.assertExpression(pElement instanceof ClassElement);

		Set<IElement> lRange = this.aDB.getRange(pElement, Relation.EXTENDS_CLASS);
		final Set<IElement> lReturn = new HashSet<IElement>();

		while (lRange.size() > 0) {
			// Util.assertExpression( lRange.size() == 1 );

			final IElement lSuperClass = lRange.iterator().next();
			lReturn.add(lSuperClass);
			lRange = this.aDB.getRange(lSuperClass, Relation.EXTENDS_CLASS);
		}
		return lReturn;
	}

	/**
	 * Returns all the interfaces that pElement implements, directly or not.
	 * 
	 * @param pElement
	 *            The domain class
	 * @return A set of IElement containing classes
	 */
	private Set<IElement> getTransitivelyImplements(final IElement pElement) {
		// Util.assertExpression(pElement instanceof ClassElement);

		final Set<IElement> lReturn = new HashSet<IElement>();

		// First get all directly implemented interfaces
		final Set<IElement> lInterfaces = new HashSet<IElement>();
		lInterfaces.addAll(this.aDB.getRange(pElement, Relation.IMPLEMENTS_INTERFACE));

		// Then find the interfaces that are extended by one or more of the
		// interfaces we found
		while (lInterfaces.size() > 0) {
			final IElement lNext = lInterfaces.iterator().next();
			lReturn.add(lNext);
			lInterfaces.remove(lNext);
			lInterfaces.addAll(this.aDB.getRange(lNext, Relation.EXTENDS_INTERFACES));
		}

		// Now find the class this class extends
		final Set<IElement> lSuperclass = this.aDB.getRange(pElement, Relation.EXTENDS_CLASS);

		// Obtain all it interfaces
		for (final IElement lSuperclassElement : lSuperclass)
			lReturn.addAll(this.getTransitivelyImplements(lSuperclassElement));

		return lReturn;
	}

	/**
	 * Returns all the classes extended by class pElement directly or
	 * indirectly.
	 * 
	 * @param pElement
	 *            The domain class
	 * @return A set of IElement containing classes
	 */
	private Set<IElement> getTTransitivelyExtends(final IElement pElement) {
		// Util.assertExpression(pElement instanceof ClassElement);

		Set<IElement> lToProcess = this.aDB.getRange(pElement, Relation.T_EXTENDS_CLASS);
		final Set<IElement> lReturn = new HashSet<IElement>();

		while (lToProcess.size() > 0) {
			final IElement lNext = lToProcess.iterator().next();
			lReturn.add(lNext);
			lToProcess.remove(lNext);
			lToProcess.addAll(this.aDB.getRange(lNext, Relation.T_EXTENDS_CLASS));
		}
		lToProcess = this.aDB.getRange(pElement, Relation.T_EXTENDS_CLASS);

		return lReturn;
	}

	/**
	 * Returns all the classes that implement, directly or not, pElement.
	 * 
	 * @param pElement
	 *            The domain interface class
	 * @return A set of IElement containing classes
	 */
	private Set<IElement> getTTransitivelyImplements(final IElement pElement) {
		// Util.assertExpression(pElement instanceof ClassElement);

		final Set<IElement> lReturn = new HashSet<IElement>();

		// First get all transitively extending interfaces
		final Set<IElement> lInterfaces = new HashSet<IElement>();
		Set<IElement> lToProcess = new HashSet<IElement>();
		lToProcess.add(pElement);
		lInterfaces.add(pElement);

		// Retrieve all the interfaces that extend pElement
		while (lToProcess.size() > 0) {
			final IElement lNext = lToProcess.iterator().next();
			lToProcess.remove(lNext);
			lInterfaces.addAll(this.aDB.getRange(lNext, Relation.T_EXTENDS_INTERFACES));
			lToProcess.addAll(this.aDB.getRange(lNext, Relation.T_EXTENDS_INTERFACES));
		}

		// Then for each interface find all implementing classes and their
		// subclasses
		lToProcess = new HashSet<IElement>();
		for (final IElement lNext : lInterfaces) {
			lToProcess.addAll(this.aDB.getRange(lNext, Relation.T_IMPLEMENTS_INTERFACE));
			lReturn.addAll(this.aDB.getRange(lNext, Relation.T_IMPLEMENTS_INTERFACE));
		}

		for (final IElement lNext : lToProcess) {
			lReturn.addAll(this.getTTransitivelyExtends(lNext));

		}
		return lReturn;
	}
	/**
	 * Returns whether pElement is an interface type that exists in the DB.
	 */
	// public boolean isInterface( IElement pElement )
	// {
	// boolean lReturn = false;
	// if( pElement.getCategory() == Category.CLASS )
	// {
	// if( aDB.getModifiers( pElement ) >= 16384 )
	// {
	// lReturn = true;
	// }
	// }
	// return lReturn;
	// }
	/**
	 * Returns whether pElement is an non-implemented method, either in an
	 * interface or as an abstract method in an abstract class. Description of
	 * JavaDB
	 */
	// public boolean isAbstractMethod( IElement pElement )
	// {
	// boolean lReturn = false;
	// if( pElement.getCategory() == Category.METHOD )
	// {
	// if( aDB.getModifiers( pElement ) >= 16384 )
	// {
	// lReturn = true;
	// }
	// }
	// return lReturn;
	// }
	/**
	 * returns pelement included in the set if it is a class. pelement can be an
	 * interface, in which case all the implementing types will be included.
	 * 
	 * @param pElement
	 * @return
	 */
	// private Set getNonAbstractSubtypes( IElement pElement )
	// {
	// Set lReturn = new HashSet();
	// if( isInterface( pElement ))
	// {
	// Set lImplementors = getTTransitivelyImplements( pElement );
	// for( Iterator i = lImplementors.iterator(); i.hasNext(); )
	// {
	// IElement lNext = (IElement)i.next();
	// if( !Modifier.isAbstract( aDB.getModifiers( lNext )))
	// {
	// lReturn.add( lNext );
	// }
	// }
	// }
	// else
	// {
	// if( !Modifier.isAbstract( aDB.getModifiers( pElement )))
	// {
	// lReturn.add( pElement );
	// }
	// Set lSubclasses = getTTransitivelyExtends( pElement );
	// for( Iterator i = lSubclasses.iterator(); i.hasNext(); )
	// {
	// IElement lNext = (IElement)i.next();
	// if( !Modifier.isAbstract( aDB.getModifiers( lNext )))
	// {
	// lReturn.add( lNext );
	// }
	// }
	// }
	// return lReturn;
	// }
	/**
	 * Returns the method implementation that is executed if pMethod is called
	 * on an object of dynamic type pTarget.
	 * 
	 * @param pMethod
	 *            the static type of the method called
	 * @param pTarget
	 *            the dynamic type of the object
	 * @return The lowest non-abstract method in the class hierarchy. null if
	 *         none are found. (should not happen)
	 */
	// private IElement getMethodImplementation( MethodElement pMethod, IElement
	// pTarget )
	// {
	// IElement lTarget = pTarget;
	// IElement lReturn = matchMethod( pMethod, lTarget );
	// while( lReturn == null )
	// {
	// Set lSuperclass = aDB.getRange( lTarget, Relation.EXTENDS_CLASS );
	// if( lSuperclass.size() != 1 )
	// {
	// break;
	// }
	// lTarget = (IElement)lSuperclass.iterator().next();
	// lReturn = matchMethod( pMethod, lTarget );
	// }
	// return lReturn;
	// }
	/**
	 * Returns a non-static, non-constructor method that matches pMethod but
	 * that is declared in pClass. null if none are found. Small concession to
	 * correctness here for sake of efficiency: methods are matched only if they
	 * parameter types match exactly. pAbstract whether to look for abstract or
	 * non-abstract methods
	 */
	// private IElement matchMethod( MethodElement pMethod, IElement pClass,
	// boolean pAbstract )
	// {
	// IElement lReturn = null;
	// String lThisName = pMethod.getName();
	//
	// Set lElements = aDB.getRange( pClass, Relation.DECLARES );
	// for( Iterator i = lElements.iterator(); i.hasNext(); )
	// {
	// IElement lNext = (IElement)i.next();
	// if( lNext.getCategory() == Category.METHOD )
	// {
	// if( !((MethodElement)lNext).getName().startsWith("<init>") &&
	// !((MethodElement)lNext).getName().startsWith("<clinit>"))
	// {
	// if( !Modifier.isStatic( aDB.getModifiers( lNext )))
	// {
	// if( lThisName.equals( ((MethodElement)lNext).getName() ))
	// {
	// pMethod.getParameters().equals( ((MethodElement)lNext).getParameters() );
	// if( isAbstractMethod( lNext ) == pAbstract )
	// {
	// lReturn = lNext;
	// break;
	// }
	// }
	// }
	// }
	// }
	// }
	//
	// return lReturn;
	// }
	/**
	 * Returns the method that this method directly overrides.
	 * 
	 * @return A non-null set.
	 */
	// private Set getOverrides( IElement pElement )
	// {
	// Set lReturn = new HashSet();
	//
	// if( !(pElement instanceof MethodElement ))
	// return lReturn;
	//
	// if( isAbstractMethod( pElement ))
	// return lReturn;
	//
	// // look in superclasses
	// Set lSuperclass = aDB.getRange( pElement.getDeclaringClass(),
	// Relation.EXTENDS_CLASS );
	// while( lSuperclass.size() != 0 )
	// {
	// IElement lNext = (IElement)lSuperclass.iterator().next();
	// IElement lMethod = matchMethod( (MethodElement)pElement, lNext, false );
	// if( lMethod != null )
	// {
	// lReturn.add( lMethod );
	// break;
	// }
	// lSuperclass = aDB.getRange( lNext, Relation.EXTENDS_CLASS );
	// }
	//
	// return lReturn;
	// }
	/**
	 * Returns the methods that this method is directly overriden by.
	 * 
	 * @return A non-null set.
	 */
	// private Set getTOverrides( IElement pElement )
	// {
	// Set lReturn = new HashSet();
	//
	// if( !(pElement instanceof MethodElement ))
	// return lReturn;
	//
	// if( isAbstractMethod( pElement ))
	// return lReturn;
	//
	// // look in subclasses
	// Set lSubclasses = new HashSet();
	// lSubclasses.addAll( aDB.getRange( pElement.getDeclaringClass(),
	// Relation.T_EXTENDS_CLASS ));
	// while( lSubclasses.size() != 0 )
	// {
	// IElement lNext = (IElement)lSubclasses.iterator().next();
	// lSubclasses.remove( lNext );
	// IElement lMethod = matchMethod( (MethodElement)pElement, lNext, false );
	// if( lMethod != null )
	// {
	// lReturn.add( lMethod );
	// }
	// else
	// {
	// lSubclasses.addAll( aDB.getRange( lNext, Relation.T_EXTENDS_CLASS ));
	// }
	// }
	//
	// return lReturn;
	//
	// }
	/**
	 * Returns the first abstract method found that matches this method. The
	 * methods are - are abstract, non-static - match pElement - and that are
	 * not implemented by any method that pElement overrides. Typically there
	 * should only be one method returned.
	 * 
	 * @param pElement
	 * @return
	 */
	// private Set getImplementsMethod( IElement pElement )
	// {
	// Set lReturn = new HashSet();
	//
	// if( !(pElement instanceof MethodElement ))
	// return lReturn;
	//
	// // First, search the superclasses for abstract methods
	// Set lSuperclass = aDB.getRange( pElement.getDeclaringClass(),
	// Relation.EXTENDS_CLASS );
	// while( lSuperclass.size() != 0 )
	// {
	// IElement lNext = (IElement)lSuperclass.iterator().next();
	// IElement lMethod = matchMethod( (MethodElement)pElement, lNext, true );
	// if( lMethod != null )
	// {
	// lReturn.add( lMethod );
	// break;
	// }
	// lSuperclass = aDB.getRange( lNext, Relation.EXTENDS_CLASS );
	// }
	//
	//
	// return lReturn;
	// }
}
