/* JayFX - A Fact Extractor Plug-in for Eclipse
 * Copyright (C) 2006  McGill University (http://www.cs.mcgill.ca/~swevo/jayfx)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * $Revision: 1.7 $
 */

package ca.mcgill.cs.swevo.jayfx;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.aspectj.ajdt.core.AspectJCore;
import org.eclipse.ajdt.core.javaelements.AJCodeElement;
import org.eclipse.ajdt.core.javaelements.AdviceElement;
import org.eclipse.ajdt.core.javaelements.AspectElement;
import org.eclipse.ajdt.core.javaelements.IAJCodeElement;
import org.eclipse.ajdt.core.model.AJRelationship;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;


import ca.mcgill.cs.swevo.jayfx.model.FlyweightElementFactory;
import ca.mcgill.cs.swevo.jayfx.model.Category;
import ca.mcgill.cs.swevo.jayfx.model.IElement;
import ca.mcgill.cs.swevo.jayfx.model.MethodElement;
import ca.mcgill.cs.swevo.jayfx.model.Relation;
import edu.ohio_state.cse.khatchad.fraglight.core.analysis.util.TimeCollector;
import edu.ohio_state.cse.khatchad.fraglight.core.util.Util;

/**
 * Facade for the JavaDB component. This component takes in a Java projects and
 * produces a database of the program relations between all the source element
 * in the input project and dependent projects.
 */
public class JayFX {

	/**
	 * Returns all the compilation units in this projects
	 * 
	 * @param pProject
	 *            The project to analyze. Should never be null.
	 * @return The compilation units to generate. Never null.
	 * @throws JayFXException
	 *             If the method cannot complete correctly
	 */
	private static List<ICompilationUnit> getCompilationUnits(
			final IJavaProject pProject) throws JayFXException {
		//		assert pProject != null;

		final List<ICompilationUnit> lReturn = new ArrayList<ICompilationUnit>();

		try {
			final IPackageFragment[] lFragments = pProject
					.getPackageFragments();
			for (final IPackageFragment element : lFragments) {
				final ICompilationUnit[] lCUs = element.getCompilationUnits();
				for (final ICompilationUnit element2 : lCUs) {
					if ( !element2.getResource().getFileExtension().equals("aj") )
						lReturn.add(element2);
				}
			}
		}
		catch (final JavaModelException pException) {
			throw new JayFXException(
					"Could not extract compilation units from project",
					pException);
		}
		return lReturn;
	}

	/**
	 * Returns all projects to analyze in IJavaProject form, including the
	 * dependent projects.
	 * 
	 * @param pProject
	 *            The project to analyze (with its dependencies. Should not be
	 *            null.
	 * @return A list of all the dependent projects (including pProject). Never
	 *         null.
	 * @throws JayFXException
	 *             If the method cannot complete correctly.
	 */
	private static List<IJavaProject> getJavaProjects(final IProject pProject)
			throws JayFXException {
		//		assert pProject != null;

		final List<IJavaProject> lReturn = new ArrayList<IJavaProject>();
		try {
			lReturn.add(JavaCore.create(pProject));
			final IProject[] lReferencedProjects = pProject
					.getReferencedProjects();
			for (final IProject element : lReferencedProjects)
				lReturn.add(JavaCore.create(element));
		}
		catch (final CoreException pException) {
			throw new JayFXException("Could not extract project information",
					pException);
		}
		return lReturn;
	}

	// The analyzer is a wrapper providing additional query functionalities
	// to the database.
	private final Analyzer aAnalyzer;

	// A flag that remembers whether the class-hierarchy analysis is enabled.
	private boolean aCHAEnabled;

	// A converter object that needs to be initialized with the database.
	private final FastConverter aConverter = new FastConverter();

	// The database object should be used for building the database
	private final ProgramDatabase aDB = new ProgramDatabase();

	// A Set of all the packages in the "project"
	private final Set<String> aPackages = new HashSet<String>();

	public JayFX() {
		this.aAnalyzer = new Analyzer(this.aDB);
	}

	/**
	 * Returns an IElement describing the argument Java element. Not designed to
	 * be able to find initializer blocks or arrays.
	 * 
	 * @param pElement
	 *            Never null.
	 * @return Never null
	 * @throws ConversionException
	 *             if the element cannot be converted.
	 */
	public IElement convertToElement(final IJavaElement pElement)
			throws ConversionException {
		final IElement ret = this.aConverter.getElement(pElement);
		if (ret == null)
			throw new IllegalStateException("In trouble.");
		return ret;
	}

	/**
	 * Returns a Java element associated with pElement. This method cannot track
	 * local or anonymous types or any of their members, primitive types, or
	 * array types. It also cannot find initializers, default constructors that
	 * are not explicitly declared, or non-top-level types outside the project
	 * analyzed. If such types are passed as argument, a ConversionException is
	 * thrown.
	 * 
	 * @param pElement
	 *            The element to convert. Never null.
	 * @return Never null.
	 * @throws ConversionException
	 *             If the element cannot be
	 */
	public IJavaElement convertToJavaElement(final IElement pElement)
			throws ConversionException {
		return this.aConverter.getJavaElement(pElement);
	}

	/** for testing purposes */
	public void dumpConverter() {
		this.aConverter.dump();
	}

	/**
	 * Returns all the elements in the database in their lighweight form.
	 * 
	 * @return A Set of IElement objects representing all the elements in the
	 *         program database.
	 */
	public Set<IElement> getAllElements() {
		return this.aDB.getAllElements();
	}

	/**
	 * Returns the modifier flag for the element
	 * 
	 * @return An integer representing the modifier. 0 if the element cannot be
	 *         found.
	 */
	public int getModifiers(final IElement pElement) {
		return this.aDB.getModifiers(pElement);
	}

	/**
	 * Returns all the non-static methods that pMethod potentially implements.
	 * 
	 * @param pMethod
	 *            The method to check. Cannot be null.
	 * @return a Set of methods found, or the empty set (e.g., if pMethod is
	 *         abstract.
	 */
	public Set<IElement> getOverridenMethods(final IElement pMethod) {
		//		assert pMethod != null && pMethod instanceof MethodElement;
		final Set<IElement> lReturn = new HashSet<IElement>();

		if (!this.isAbstractMethod(pMethod)) {
			final Set<IElement> lToProcess = new HashSet<IElement>();
			lToProcess.addAll(this.getRange(pMethod.getDeclaringClass(),
					Relation.EXTENDS_CLASS));
			lToProcess.addAll(this.getRange(pMethod.getDeclaringClass(),
					Relation.IMPLEMENTS_INTERFACE));
			while (lToProcess.size() > 0) {
				final IElement lType = lToProcess.iterator().next();
				lReturn
						.addAll(this
								.matchMethod((MethodElement) pMethod, lType));
				lToProcess.addAll(this.getRange(lType, Relation.EXTENDS_CLASS));
				lToProcess.addAll(this.getRange(lType,
						Relation.IMPLEMENTS_INTERFACE));
				lToProcess.addAll(this.getRange(lType,
						Relation.EXTENDS_INTERFACES));
				lToProcess.remove(lType);
			}
		}
		return lReturn;
	}

	/**
	 * Returns the range of the relation pRelation for domain pElement.
	 * 
	 * @param pElement
	 *            The domain element
	 * @param pRelation
	 *            The relation to query
	 * @return A Set of IElement objects representing all the elements in the
	 *         range.
	 */
	public Set<IElement> getRange(final IElement pElement,
			final Relation pRelation) {
		if (pRelation == Relation.DECLARES_TYPE
				&& !this.isProjectElement(pElement))
			return this.getDeclaresTypeForNonProjectElement(pElement);
		if (pRelation == Relation.DECLARES_METHOD
				&& !this.isProjectElement(pElement))
			return this.getDeclaresMethodForNonProjectElement(pElement);
		if (pRelation == Relation.DECLARES_FIELD
				&& !this.isProjectElement(pElement))
			return this.getDeclaresFieldForNonProjectElement(pElement);
		if (pRelation == Relation.EXTENDS_CLASS
				&& !this.isProjectElement(pElement))
			return this.getExtendsClassForNonProjectElement(pElement);
		if (pRelation == Relation.IMPLEMENTS_INTERFACE
				&& !this.isProjectElement(pElement))
			return this.getInterfacesForNonProjectElement(pElement, true);
		if (pRelation == Relation.EXTENDS_INTERFACES
				&& !this.isProjectElement(pElement))
			return this.getInterfacesForNonProjectElement(pElement, false);
		if (pRelation == Relation.OVERRIDES
				|| pRelation == Relation.T_OVERRIDES)
			if (!this.isCHAEnabled())
				throw new RelationNotSupportedException(
						"CHA must be enabled to support this relation");
		return this.aAnalyzer.getRange(pElement, pRelation);
	}

	/**
	 * Convenience method. Returns the subset of elements in the range of the
	 * relation pRelation for domain pElement that are in the analyzed project.
	 * 
	 * @param pElement
	 *            The domain element
	 * @param pRelation
	 *            The relation to query
	 * @return A Set of IElement objects representing all the elements in the
	 *         range.
	 */
	public Set<IElement> getRangeInProject(final IElement pElement,
			final Relation pRelation) {
		final Set<IElement> lRange = this.aAnalyzer.getRange(pElement,
				pRelation);
		final Set<IElement> lReturn = new HashSet<IElement>();

		for (final IElement lElement : lRange)
			if (this.isProjectElement(lElement))
				lReturn.add(lElement);
		/*
		 * for( Iterator i = lRange.iterator(); i.hasNext(); ) { IElement lNext =
		 * (IElement)i.next(); if( isProjectElement( lNext )) { lReturn.add(
		 * lNext ); } }
		 */
		return lReturn;
	}

	/**
	 * Initializes the program database with information about relations between
	 * all the source elements in pProject and all of its dependent projects.
	 * 
	 * @param pProject
	 *            The project to analyze. Should never be null.
	 * @param pProgress
	 *            A progress monitor. Can be null.
	 * @param pCHA
	 *            Whether to calculate overriding relationships between methods
	 *            and to use these in the calculation of CALLS and CALLS_BY
	 *            relations.
	 * @throws JayFXException
	 *             If the method cannot complete correctly
	 * @throws ConversionException
	 * @throws ElementNotFoundException
	 * @throws JavaModelException
	 */
	@SuppressWarnings( { "restriction", "unchecked" })
	public void initialize(final Collection<? extends IProject> pProjectCol,
			final IProgressMonitor pProgress, boolean pCHA, TimeCollector timeCollector)
			throws JayFXException, ElementNotFoundException,
			ConversionException, JavaModelException {

		this.aCHAEnabled = pCHA;

		// Collect all target classes
		final List<ICompilationUnit> lTargets = new ArrayList<ICompilationUnit>();

		for (final IProject pProject : pProjectCol)
			for (final IJavaProject lNext : JayFX.getJavaProjects(pProject))
				lTargets.addAll(JayFX.getCompilationUnits(lNext));

		// Process all the target classes
		final ASTCrawler lAnalyzer = new ASTCrawler(this.aDB, this.aConverter);
		if (pProgress != null)
			pProgress.beginTask("Building program database", lTargets.size());

		for (final ICompilationUnit lCU : lTargets) {
			try {
				final IPackageDeclaration[] lPDs = lCU.getPackageDeclarations();
				if (lPDs.length > 0)
					this.aPackages.add(lPDs[0].getElementName());
			}
			catch (final JavaModelException lException) {
				throw new JayFXException(lException);
			}
			lAnalyzer.analyze(lCU, timeCollector);
			if (pProgress != null)
				pProgress.worked(1);
		}

		/*
		 * int lSize = lTargets.size(); int k = 0; for( Iterator i =
		 * lTargets.iterator(); i.hasNext(); ) { k++; ICompilationUnit lCU =
		 * (ICompilationUnit)i.next(); try { IPackageDeclaration[] lPDs =
		 * lCU.getPackageDeclarations(); if( lPDs.length > 0 ) { aPackages.add(
		 * lPDs[0].getElementName() ); } } catch( JavaModelException pException ) {
		 * throw new JavaDBException( pException ); } lAnalyzer.analyze( lCU );
		 * if( pProgress != null ) pProgress.worked(1);
		 * 
		 * System.out.println( k + "/" + lSize ); if( k == 1414 ) {
		 * System.out.println( k + "/" + lSize ); } }
		 */

		if (!pCHA)
			//			if (pProgress != null)
			//				pProgress.done();
			return;

		// Process the class hierarchy analysis
		if (pProgress != null)
			pProgress.beginTask("Performing class hierarchy analysis", this.aDB
					.getAllElements().size());
		final Set<IElement> lToProcess = new HashSet<IElement>();
		lToProcess.addAll(this.aDB.getAllElements());
		// int lSize = aDB.getAllElements().size();
		// k = 0;
		while (lToProcess.size() > 0) {
			// k++;
			final IElement lNext = lToProcess.iterator().next();
			lToProcess.remove(lNext);
			if (lNext.getCategory() == Category.METHOD)
				if (!this.isAbstractMethod(lNext)) {
					final Set<IElement> lOverrides = this
							.getOverridenMethods(lNext);
					for (final IElement lMethod : lOverrides) {
						if (!this.isProjectElement(lMethod)) {
							int lModifiers = 0;
							try {
								final IJavaElement lElement = this
										.convertToJavaElement(lMethod);
								if (lElement instanceof IMember) {
									lModifiers = ((IMember) lElement)
											.getFlags();
									if (Modifier.isAbstract(lModifiers))
										lModifiers += 16384;
								}
							}
							catch (final ConversionException lException) {
								// Ignore, the modifiers used is 0
							}
							catch (final JavaModelException lException) {
								// Ignore, the modifierds used is 0
							}
							this.aDB.addElement(lMethod, lModifiers);
						}
						this.aDB.addRelationAndTranspose(lNext,
								Relation.OVERRIDES, lMethod);
					}
					/*
					 * for( Iterator j = lOverrides.iterator(); j.hasNext(); ) {
					 * IElement lMethod = (IElement)j.next(); if(
					 * !isProjectElement( lMethod )) { int lModifiers = 0; try {
					 * IJavaElement lElement = convertToJavaElement( lMethod );
					 * if( lElement instanceof IMember ) { lModifiers =
					 * ((IMember)lElement).getFlags(); if( Modifier.isAbstract(
					 * lModifiers )) { lModifiers += 16384; } } } catch(
					 * ConversionException pException ) { // Ignore, the
					 * modifiers used is 0 } catch( JavaModelException
					 * pException ) { // Ignore, the modifiers used is 0 }
					 * aDB.addElement( lMethod, lModifiers ); }
					 * aDB.addRelationAndTranspose( lNext, Relation.OVERRIDES,
					 * lMethod ); }
					 */
				}
			pProgress.worked(1);
			// System.out.println( k + "/" + lSize );
		}

		// process the aspects, if any.
		// if ( AspectJPlugin.isAJProject(pProject)) {
		// AjASTCrawler aspectAnalyzer = new AjASTCrawler( aDB, aConverter );
		// if( pProgress != null ) pProgress.subTask("Analyzing aspects.");
		//	        
		// AjBuildManager.setAsmHierarchyBuilder(aspectAnalyzer);
		// try {
		// pProject.open(pProgress);
		// } catch (CoreException e) {
		// throw new JayFXException( "Could not open project ", e);
		// }
		//			
		// try {
		// pProject.build(IncrementalProjectBuilder.FULL_BUILD, pProgress);
		// } catch (CoreException e) {
		// throw new JayFXException( "Could not build project ", e);
		// }
		// }
		//		pProgress.done();
	}

	/**
	 * Returns whether pElement is an non-implemented method, either in an
	 * interface or as an abstract method in an abstract class. Description of
	 * JayFX TODO: Get rid of the magic number
	 */
	public boolean isAbstractMethod(final IElement pElement) {
		boolean lReturn = false;
		if (pElement.getCategory() == Category.METHOD)
			if (this.aDB.getModifiers(pElement) >= 16384)
				lReturn = true;
		return lReturn;
	}

	/**
	 * Check if class-hierarchy analysis is enabled.
	 * 
	 * @return <code>true</code> if Class-hierarchy analysis is enabled;
	 *         <code>false</code> otherwise.
	 */
	public boolean isCHAEnabled() {
		return this.aCHAEnabled;
	}

	/**
	 * Returns whether pElement is an element in the packages analyzed.
	 * 
	 * @param pElement
	 *            Not null
	 * @return true if pElement is a project element.
	 */
	public boolean isProjectElement(final IElement pElement) {
		//		assert pElement != null;
		return this.aPackages.contains(pElement.getPackageName());
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return this.aDB.toString();
	}

	/**
	 * Convenience method that returns the elements declared by an element that
	 * is not in the project.
	 * 
	 * @param pElement
	 *            The element to analyze. Cannot be null.
	 * @return A set of elements declared. Cannot be null. Emptyset if there is
	 *         any problem converting the element.
	 */
	private Set<IElement> getDeclaresFieldForNonProjectElement(
			final IElement pElement) {
		//		assert pElement != null;
		final Set<IElement> lReturn = new HashSet<IElement>();
		if (pElement.getCategory() == Category.CLASS)
			try {
				final IJavaElement lElement = this
						.convertToJavaElement(pElement);
				if (lElement instanceof IType) {
					final IField[] lFields = ((IType) lElement).getFields();
					for (final IField element : lFields)
						lReturn.add(this.convertToElement(element));
				}
			}
			catch (final ConversionException pException) {
				// Nothing, we return the empty set.
			}
			catch (final JavaModelException pException) {
				// Nothing, we return the empty set.
			}
		return lReturn;
	}

	private Set<IElement> getDeclaresMethodForNonProjectElement(
			final IElement pElement) {
		//		assert pElement != null;
		final Set<IElement> lReturn = new HashSet<IElement>();
		if (pElement.getCategory() == Category.CLASS)
			try {
				final IJavaElement lElement = this
						.convertToJavaElement(pElement);
				if (lElement instanceof IType) {
					final IMethod[] lMethods = ((IType) lElement).getMethods();
					for (final IMethod element : lMethods)
						lReturn.add(this.convertToElement(element));
				}
			}
			catch (final ConversionException pException) {
				// Nothing, we return the empty set.
			}
			catch (final JavaModelException pException) {
				// Nothing, we return the empty set.
			}
		return lReturn;
	}

	// /**
	// * Returns whether pElement is an interface type that exists in the
	// * DB.
	// */
	// public boolean isInterface( IElement pElement )
	// {
	// return aAnalyzer.isInterface( pElement );
	// }

	private Set<IElement> getDeclaresTypeForNonProjectElement(
			final IElement pElement) {
		//		assert pElement != null;
		final Set<IElement> lReturn = new HashSet<IElement>();
		if (pElement.getCategory() == Category.CLASS)
			try {
				final IJavaElement lElement = this
						.convertToJavaElement(pElement);
				if (lElement instanceof IType) {
					final IType[] lTypes = ((IType) lElement).getTypes();
					for (final IType element : lTypes)
						lReturn.add(this.convertToElement(element));
				}
			}
			catch (final ConversionException pException) {
				// Nothing, we return the empty set.
			}
			catch (final JavaModelException pException) {
				// Nothing, we return the empty set.
			}
		return lReturn;
	}

	/**
	 * Convenience method that returns the superclass of a class that is not in
	 * the project.
	 * 
	 * @param pElement
	 *            The element to analyze. Cannot be null.
	 * @return A set of elements declared. Cannot be null. Emptyset if there is
	 *         any problem converting the element.
	 */
	private Set<IElement> getExtendsClassForNonProjectElement(
			final IElement pElement) {
		//		assert pElement != null;
		final Set<IElement> lReturn = new HashSet<IElement>();
		if (pElement.getCategory() == Category.CLASS)
			try {
				final IJavaElement lElement = this
						.convertToJavaElement(pElement);
				if (lElement instanceof IType) {
					final String lSignature = ((IType) lElement)
							.getSuperclassTypeSignature();
					if (lSignature != null) {
						final IElement lSuperclass = FlyweightElementFactory
								.getElement(Category.CLASS, this.aConverter
										.resolveType(lSignature,
												(IType) lElement).substring(1,
												lSignature.length() - 1));
						//						assert lSuperclass instanceof ClassElement;
						lReturn.add(lSuperclass);
					}
				}
			}
			catch (final ConversionException lException) {
				// Nothing, we return the empty set.
			}
			catch (final JavaModelException lException) {
				// Nothing, we return the empty set.
			}
		return lReturn;
	}

	/**
	 * Convenience method that returns the interfaces class that is not in the
	 * project implements.
	 * 
	 * @param pElement
	 *            The element to analyze. Cannot be null.
	 * @param pImplements
	 *            if we want the implements interface relation. False for the
	 *            extends interface relation
	 * @return A set of elements declared. Cannot be null. Emptyset if there is
	 *         any problem converting the element.
	 */
	private Set<IElement> getInterfacesForNonProjectElement(
			final IElement pElement, boolean pImplements) {
		//		assert pElement != null;
		final Set<IElement> lReturn = new HashSet<IElement>();
		if (pElement.getCategory() == Category.CLASS)
			try {
				final IJavaElement lElement = this
						.convertToJavaElement(pElement);
				if (lElement instanceof IType)
					if (((IType) lElement).isInterface() == !pImplements) {
						final String[] lInterfaces = ((IType) lElement)
								.getSuperInterfaceTypeSignatures();
						if (lInterfaces != null)
							for (final String element : lInterfaces) {
								final IElement lInterface = FlyweightElementFactory
										.getElement(
												Category.CLASS,
												this.aConverter
														.resolveType(
																element,
																(IType) lElement)
														.substring(
																1,
																element
																		.length() - 1));
								lReturn.add(lInterface);
							}
					}
			}
			catch (final ConversionException lException) {
				// Nothing, we return the empty set.
			}
			catch (final JavaModelException lException) {
				// Nothing, we return the empty set.
			}
		return lReturn;
	}

	/**
	 * Returns a non-static, non-constructor method that matches pMethod but
	 * that is declared in pClass. null if none are found. Small concession to
	 * correctness here for sake of efficiency: methods are matched only if they
	 * parameter types match exactly.
	 * 
	 * @param pMethod
	 * @param pClass
	 * @return
	 */
	private Set<IElement> matchMethod(final MethodElement pMethod,
			final IElement pClass) {
		final Set<IElement> lReturn = new HashSet<IElement>();
		final String lThisName = pMethod.getName();

		final Set<IElement> lElements = this.getRange(pClass,
				Relation.DECLARES_METHOD);
		for (final IElement lMethodElement : lElements)
			if (lMethodElement.getCategory() == Category.METHOD)
				if (!((MethodElement) lMethodElement).getName().startsWith(
						"<init>")
						&& !((MethodElement) lMethodElement).getName()
								.startsWith("<clinit>"))
					if (!Modifier.isStatic(this.aDB
							.getModifiers(lMethodElement)))
						if (lThisName.equals(((MethodElement) lMethodElement)
								.getName())) {
							pMethod.getParameters().equals(
									((MethodElement) lMethodElement)
											.getParameters());
							lReturn.add(lMethodElement);
							break;
						}

		/*
		 * for( Iterator i = lElements.iterator(); i.hasNext(); ) { IElement
		 * lNext = (IElement)i.next(); if( lNext.getCategory() ==
		 * Category.METHOD ) { if(
		 * !((MethodElement)lNext).getName().startsWith("<init>") &&
		 * !((MethodElement)lNext).getName().startsWith("<clinit>")) { if(
		 * !Modifier.isStatic( aDB.getModifiers( lNext ))) { if(
		 * lThisName.equals( ((MethodElement)lNext).getName() )) {
		 * pMethod.getParameters().equals(
		 * ((MethodElement)lNext).getParameters() ); lReturn.add( lNext );
		 * break; } } } } }
		 */
		return lReturn;
	}

}