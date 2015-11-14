/* JayFX - A Fact Extractor Plug-in for Eclipse
 * Copyright (C) 2006  McGill University (http://www.cs.mcgill.ca/~swevo/jayfx)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * $Revision: 1.14 $
 */

package ca.mcgill.cs.swevo.jayfx;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IPackageBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import ca.mcgill.cs.swevo.jayfx.model.Category;
import ca.mcgill.cs.swevo.jayfx.model.ClassElement;
import ca.mcgill.cs.swevo.jayfx.model.FlyweightElementFactory;
import ca.mcgill.cs.swevo.jayfx.model.IElement;
import ca.mcgill.cs.swevo.jayfx.model.MethodElement;
import ca.mcgill.cs.swevo.jayfx.model.Relation;
import edu.ohio_state.cse.khatchad.fraglight.core.analysis.util.TimeCollector;

/**
 * Traversing the AST of a compilation unit and inserting all the information
 * gathered to the database.
 */
/*
 * <ul> <li> Class Initializer and Constructor The class initializer method <
 * clinit >() is a virtual method. There is no such a Node for this method.
 * Therefore the creation of such method is inside type declaration node. An
 * empty clinit method is created when the visitor enounter a top level class.
 * During the time before the endVisit method is called, if any static field is
 * initialized, some relations will be added to this clinit class. When endVisit
 * is called, the visitor checks to see if any relation is recorded in the
 * database under this method. If there are some, A relation of DECLARES will be
 * drawn between the current type and clinit method. The < temp >() method has
 * similar use. The difference is that temp method serves the purpose of keeping
 * track of non-static initializations. Any such kind of relations that are
 * recored under temp method will be added to all the constructors of the
 * current type. At the end of visiting this type declaration, this temp method
 * WILL be completely removed from database.
 * 
 * <li> Reminder mechanism: When the visitor visit a type declaration node, it
 * saves this type into field aCurrType. Before the endVisit for this type is
 * called, the visitor might enounter another type declaration node. This could
 * be the case of any kind of nested type. When this happens, the current type
 * will be saved, or pushed, into a backup stack -- aCurrTypeReminder. The
 * nested type will become the current type. This helps to reduce the complexity
 * of all the other visit methods, because they do not have to know whether the
 * current method is a nested method or not. After all the nodes of the outer
 * layer type declaration are visited, the endVisit method will be called. Then,
 * the back-uped type will be restored from the stack. Other stacks are
 * aCurrMethodReminder and aTempMethodReminder. They have the save function as
 * the reminder for type declaration. Because the use of stack as reminder, the
 * reminder mechanism support infinit number of nestings, although in most cases
 * we only see two layers of nesting.
 * 
 * <li> Error Compatible Traversing Whenever we call resolveBinding(), we should
 * check to see if this binding is null. Because we try to make this class
 * compatible with erroneous java files, there could be cases where bindings
 * cannot be resolved. This class works with unresolved classes, methods, or
 * field, but it is might not be able to pass a java file with structural
 * errors. </ul>
 */
public class ASTCrawler extends ASTVisitor {
	// This is an optimization flag tagged onto the modifiers for IElements. It
	// represents
	// the declaration of an abstract definition (as opposed to an
	// implementation.
	// With an type, it designates an interface. With a method, it defines
	// either an interface
	// method or an abstract method.
	private static final int ABSTRACT_FLAG = 16384;
	private static final String aTEMP_METHOD_NAME = "<temp>()";
	private static final String aINIT_METHOD_NAME = "<init>()";
	private static final String aCLINIT_METHOD_NAME = "<clinit>()";
	private static final int aINIT_CONSTRUCTOR_LIST_SIZE = 5;

	private static final int aINIT_METHOD_MODIFIERS = 8; // 8 is the modifier
															// integer of an
															// initializer block

	/**
	 * Encapsulates behavior to report null objects.
	 * 
	 * @param pObject
	 *            Object to check.
	 */
	private static boolean checkForNull(final Object pObject) {
		boolean lReturn = false;
		if (pObject == null)
			// Thread.dumpStack();
			lReturn = true;
		return lReturn;
	}

	/**
	 * Converts a method binding to a method element.
	 * 
	 * @param pBinding
	 *            The binding to covert. Cannot be null.
	 * @return A method element corresponding to pBinding. Never null.
	 */
	private static IElement convertBinding(final IMethodBinding pBinding) {
		ASTCrawler.checkForNull(pBinding);
		String lReturn = null;
		try {
			lReturn = ASTCrawler.convertBinding(pBinding.getDeclaringClass()).getId() + ".";
		} catch (final NullPointerException E) {
			E.printStackTrace();
			throw E;
		}
		if (pBinding.isConstructor())
			lReturn += "<init>";
		else
			lReturn += pBinding.getName();
		lReturn += "(";
		final ITypeBinding lParamBindings[] = pBinding.getParameterTypes();
		for (int i = 0; i < lParamBindings.length - 1; i++) {
			lReturn += ASTCrawler.convertParameterTypeBinding(lParamBindings[i]).getId();
			lReturn += ",";
		}
		if (lParamBindings.length > 0)
			lReturn += ASTCrawler.convertParameterTypeBinding(lParamBindings[lParamBindings.length - 1]).getId();
		lReturn += ")";

		return FlyweightElementFactory.getElement(Category.METHOD, lReturn);
	}

	/**
	 * Converts a type binding to a class element or an enum element.
	 * 
	 * @param pBinding
	 *            The binding to convert. Cannot be null.
	 * @return A class or enum element representing this binding. Cannot be
	 *         null.
	 */
	private static IElement convertBinding(final ITypeBinding pBinding) {
		ASTCrawler.checkForNull(pBinding);
		IJavaElement elem = null;
		try {
			elem = pBinding.getJavaElement();
		} catch (final NullPointerException E) {
			System.out.println("Bug in eclipse encountered for: " + pBinding.getName());
			return null;
		}
		return FlyweightElementFactory.getElement(Category.CLASS, pBinding.getBinaryName());
	}

	/**
	 * Converts a variable binding to a field element.
	 * 
	 * @param pBinding
	 *            The binding to convert. Cannot be null.
	 * @return A field element representing this binding. Cannot be null.
	 */
	private static IElement convertBinding(final IVariableBinding pBinding) {
		ASTCrawler.checkForNull(pBinding);
		final String lFieldID = ASTCrawler.convertBinding(pBinding.getDeclaringClass()).getId() + "."
				+ pBinding.getName();
		return FlyweightElementFactory.getElement(Category.FIELD, lFieldID);
	}

	/**
	 * Converts a type binding into a parameter-style binding Please see JVM
	 * Specification 4.3.2
	 * {@link http://java.sun.com/docs/books/vmspec/2nd-edition/html/ClassFile.doc.html#84645}
	 * .
	 * 
	 * @param pBinding
	 *            The binding to convert. Cannot be null.
	 * @return A class element representing this binding. Cannot be null.
	 */
	private static IElement convertParameterTypeBinding(final ITypeBinding pBinding) {
		ASTCrawler.checkForNull(pBinding);
		if (pBinding.getDimensions() == 0 && !pBinding.isPrimitive())
			return FlyweightElementFactory.getElement(Category.CLASS,
					Signature.C_RESOLVED + pBinding.getBinaryName() + Signature.C_SEMICOLON);
		else
			return FlyweightElementFactory.getElement(Category.CLASS, pBinding.getBinaryName());
	}

	@SuppressWarnings({ "unchecked", "unused" })
	// private static List<AdviceElement> getApplicableAdvice(final IJavaElement
	// je) {
	// final List<AdviceElement> advisedBy = new ArrayList<AdviceElement>();
	// final List<AdviceElement> direct = AJModel.getInstance()
	// .getRelatedElements(AJRelationshipManager.ADVISED_BY, je);
	// if (direct != null)
	// advisedBy.addAll(direct);
	//
	// // check for advised code elements
	// final List extras = AJModel.getInstance().getExtraChildren(je);
	// if (extras != null)
	// for (final Iterator iter = extras.iterator(); iter.hasNext();) {
	// final IJavaElement element = (IJavaElement) iter.next();
	// final List<AdviceElement> indirect = AJModel.getInstance()
	// .getRelatedElements(AJRelationshipManager.ADVISED_BY,
	// element);
	// if (indirect != null)
	// advisedBy.addAll(indirect);
	// }
	// return advisedBy;
	// }

	private static Assignment getAssignment(final ASTNode node) {
		if (node == null)
			return null;

		if (node.getNodeType() == ASTNode.ASSIGNMENT)
			return (Assignment) node;

		else
			return ASTCrawler.getAssignment(node.getParent());
	}

	/**
	 * Standard logging behavior
	 */
	private static void log(final String pMessage) {
		System.out.println(pMessage);
	}

	private ClassElement aCurrType;

	private MethodElement aCurrMethod;

	private MethodElement aTempMethod; // for field initializations

	private Stack<MethodElement> aTempMethodReminder;

	private Stack<ClassElement> aCurrTypeReminder; // for anonymous/inner class
													// situations

	private Stack<MethodElement> aCurrMethodReminder; // for anonymous/inner
														// class situations and
														// <clinit> method
														// situations

	// private boolean aHasConstructor = false;

	// private BooleanStack aHaaConstructorReminder;

	private Stack<ArrayList<MethodElement>> aCurrConstructorListReminder; // for
																			// all
																			// constructor
																			// methods

	private ArrayList<MethodElement> aCurrConstructorList;

	private final ProgramDatabase aDB;

	private final FastConverter aConverter;

	/**
	 * Constructor of ASTSpider working on a given database. A new DBManager is
	 * created.
	 * 
	 * @param pDatabase
	 *            Can be empty or can contain data
	 */
	public ASTCrawler(final ProgramDatabase pDB, final FastConverter pConverter) {
		this.aDB = pDB;
		this.aConverter = pConverter;
	}

	/**
	 * Parsing a compilation unit. Starting point of the AST traversal.
	 * 
	 * @param pCU
	 */
	@SuppressWarnings({ "restriction", "unchecked" })
	public void analyze(final ICompilationUnit pCU, TimeCollector timeCollector) {
		timeCollector.start();
		this.resetSpider();

		this.extractTypes(pCU);
		final ASTParser lParser = ASTParser.newParser(AST.JLS3); // handles JLS3
																	// (J2SE
																	// 1.5)
		lParser.setSource(pCU);
		lParser.setResolveBindings(true);
		final CompilationUnit lResult = (CompilationUnit) lParser.createAST(null);
		lResult.accept(this);
		timeCollector.stop();
	}

	@Override
	public void endVisit(final AnonymousClassDeclaration pNode) {
		this.aCurrType = this.aCurrTypeReminder.pop();
	}

	@Override
	public void endVisit(final EnumConstantDeclaration pNode) {
		this.restoreMethodRelation();
	}

	@Override
	public void endVisit(final EnumDeclaration pNode) {
		this.restoreTypeRelation();
	}

	@Override
	public void endVisit(final FieldDeclaration pNode) {
		this.aCurrMethod = this.aCurrMethodReminder.pop();
	}

	@Override
	public void endVisit(final Initializer pNode) {
		if (!this.aCurrMethodReminder.isEmpty())
			this.aCurrMethod = this.aCurrMethodReminder.pop();
		else
			this.aCurrMethod = null;
	}

	@Override
	public void endVisit(final MethodDeclaration pNode) {
		this.restoreMethodRelation();
	}

	//
	@Override
	public void endVisit(final TypeDeclaration pNode) {
		this.restoreTypeRelation();
	}

	@Override
	public boolean visit(final AnonymousClassDeclaration pNode) {
		final ITypeBinding lBinding = pNode.resolveBinding();

		if (ASTCrawler.checkForNull(lBinding))
			return false;
		if (ASTCrawler.checkForNull(this.aCurrType))
			return false;

		final IElement lAnonymousClass = ASTCrawler.convertBinding(lBinding);
		this.aCurrTypeReminder.push(this.aCurrType);
		this.aCurrType = (ClassElement) lAnonymousClass;
		this.aDB.addElement(this.aCurrType, pNode.resolveBinding().getModifiers());
		this.aDB.addRelation(this.aCurrMethod, Relation.DECLARES_TYPE, this.aCurrType);

		final ITypeBinding lSuperBinding = lBinding.getSuperclass();
		if (lSuperBinding != null) {
			final IElement lSuperClass = ASTCrawler.convertBinding(lSuperBinding);
			this.aDB.addElement(lSuperClass, lSuperBinding.getModifiers());
			this.aDB.addRelationAndTranspose(this.aCurrType, Relation.EXTENDS_CLASS, lSuperClass);
		}

		final ITypeBinding lInterfaceBindings[] = lBinding.getInterfaces();
		for (final ITypeBinding element : lInterfaceBindings) {
			final IElement lInterface = ASTCrawler.convertBinding(element);
			this.aDB.addElement(lInterface, element.getModifiers() | ASTCrawler.ABSTRACT_FLAG);
			this.aDB.addRelationAndTranspose(this.aCurrType, Relation.IMPLEMENTS_INTERFACE, lInterface);
		}
		return true;
	}

	@Override
	public boolean visit(final CastExpression pNode) {
		// assert (aCurrMethod != null);

		final ITypeBinding lBinding = pNode.resolveTypeBinding();

		if (lBinding != null) {
			final IElement lClass = ASTCrawler.convertBinding(lBinding);
			this.aDB.addElement(lClass,
					lBinding.getModifiers() | (lBinding.isInterface() ? ASTCrawler.ABSTRACT_FLAG : 0));
			this.aDB.addRelationAndTranspose(this.aCurrMethod, Relation.CHECKS, lClass);
		}

		return true;
	}

	@Override
	public boolean visit(final ClassInstanceCreation pNode) {
		if (ASTCrawler.checkForNull(this.aCurrMethod))
			return false;

		final IMethodBinding lCBinding = pNode.resolveConstructorBinding();
		final ITypeBinding lTBinding = pNode.resolveTypeBinding();

		if (ASTCrawler.checkForNull(lCBinding))
			return false;
		if (ASTCrawler.checkForNull(lTBinding))
			return false;

		MethodElement lConstructor = null;

		if (lTBinding.isAnonymous()) {
			final IElement lDeclaringClass = ASTCrawler.convertBinding(lTBinding);
			// TODO HACK A bug in Eclipse occasionally causes binary names to
			// crap out.
			if (lDeclaringClass == null || lDeclaringClass.getId() == null)
				return false;

			lConstructor = (MethodElement) FlyweightElementFactory.getElement(Category.METHOD,
					lDeclaringClass.getId() + "." + ASTCrawler.aINIT_METHOD_NAME);
			this.aDB.addElement(lConstructor, ASTCrawler.aINIT_METHOD_MODIFIERS);
		} else
			lConstructor = (MethodElement) ASTCrawler.convertBinding(lCBinding);

		final IElement lClass = lConstructor.getDeclaringClass();

		// Register CALLS relationship to constructor
		this.addCallRelation(pNode, lCBinding, true);

		try {
			this.aDB.contains(lClass);
		} catch (final RuntimeException pException) {
			System.out.println(lClass.getId());
			System.out.println(lConstructor.getId());
			throw pException;
		}

		if (!this.aDB.contains(lClass)) {
			final ITypeBinding lType = lCBinding.getDeclaringClass();
			this.aDB.addElement(lClass, lType.getModifiers());
		}

		// Register CREATES relationship
		this.aDB.addRelationAndTranspose(this.aCurrMethod, Relation.CREATES, lClass);

		return true;
	}

	@Override
	public boolean visit(final ConstructorInvocation pNode) {
		this.addCallRelation(pNode, pNode.resolveConstructorBinding(), true);
		return true;
	}

	@Override
	public boolean visit(final EnumConstantDeclaration pNode) {
		// JLS3(�8.9): It is impossible to define a local (�14.3) enum, or to
		// define an enum in an inner class (�8.1.3).
		// assert (aCurrMethodReminder.empty());

		final String lSimpleName = pNode.getName().getIdentifier();
		if (lSimpleName == null)
			return false;

		IElement lField;
		lField = FlyweightElementFactory.getElement(Category.FIELD, this.aCurrType.getId() + "." + lSimpleName);
		this.aDB.addElement(lField, pNode.getModifiers());
		this.aDB.addRelation(this.aCurrType, Relation.DECLARES_FIELD, lField);

		// Register CALLS relationship to constructor
		// IMethodBinding lCBinding = pNode.resolveConstructorBinding();
		// if( checkForNull( lCBinding )) return false;
		// saveMethodRelation( lCBinding );
		// return true;
		return false;
	}

	/**
	 * Generated the DECLARES relations between a enum and and nested types.
	 */
	@Override
	public boolean visit(final EnumDeclaration pNode) {
		final ITypeBinding lBinding = pNode.resolveBinding();
		// assert (lBinding.isEnum());

		if (ASTCrawler.checkForNull(lBinding))
			return false;

		try {
			this.saveTypeRelation(lBinding);

			// JLS3(�8.9): It is impossible to define a local (�14.3) enum, or
			// to define an enum in an inner class (�8.1.3).
			// TODO: check if enum type is always a member class of an closing
			// class
			if (lBinding.isMember())
				this.aDB.addRelation(this.aCurrTypeReminder.peek(), Relation.DECLARES_TYPE, this.aCurrType);

			// Find interfaces.
			final ITypeBinding lInterfaceBindings[] = lBinding.getInterfaces();
			for (final ITypeBinding element : lInterfaceBindings) {
				final IElement lInterface = ASTCrawler.convertBinding(element);
				this.aDB.addElement(lInterface, element.getModifiers() | ASTCrawler.ABSTRACT_FLAG);
				this.aDB.addRelationAndTranspose(this.aCurrType, Relation.IMPLEMENTS_INTERFACE, lInterface);
			}
		} catch (final Exception pException) {
			ProblemManager.reportException(pException);
		}

		return true;
	}

	@Override
	public boolean visit(final FieldAccess pNode) {
		final IVariableBinding lBinding = (IVariableBinding) pNode.getName().resolveBinding();

		if (lBinding == null) {
			ASTCrawler.log("Null binding 1 for " + pNode.toString());
			return false;
		}

		this.addAccessRelation(lBinding);
		final Assignment assignment = ASTCrawler.getAssignment(pNode);
		if (assignment != null) {
			this.addSetsRelation(lBinding);

			if (!(assignment.getOperator() == Assignment.Operator.ASSIGN))
				this.addGetsRelation(lBinding);
		} else
			this.addGetsRelation(lBinding);
		return true;
	}

	/*
	 * Add information about one or more fields in this field declaration
	 * statment The method also desides to put "access" relation to Class
	 * Initializer or Constructor depending on if the method is static or not,
	 * and if it's initalized with a value or not.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public boolean visit(final FieldDeclaration pNode) {
		final List fragments = pNode.fragments();
		IElement lField;

		this.aCurrMethodReminder.push(this.aCurrMethod);

		if (Modifier.isStatic(pNode.getModifiers())) {
			this.aCurrMethod = (MethodElement) FlyweightElementFactory.getElement(Category.METHOD,
					this.aCurrType.getId() + "." + ASTCrawler.aCLINIT_METHOD_NAME);
			if (!this.aDB.contains(this.aCurrMethod))
				// This <clinit>() method will be in any class that has a static
				// field with initialization
				// But the DECLARES relation will be linked only if this method
				// has at least one sub relations
				// The linkage is done at the end of traversing each
				// compilations unit while end visiting type Declaration
				this.aDB.addElement(this.aCurrMethod, pNode.getModifiers());
		} else
			this.aCurrMethod = this.aTempMethod;

		// Consider multiple declaration in one statment
		for (final Iterator itr = fragments.iterator(); itr.hasNext();) {
			final VariableDeclarationFragment fragment = (VariableDeclarationFragment) itr.next();
			final String lSimpleName = fragment.getName().getIdentifier();
			final Expression lInit = fragment.getInitializer();

			if (lSimpleName != null) {
				lField = FlyweightElementFactory.getElement(Category.FIELD, this.aCurrType.getId() + "." + lSimpleName);
				this.aDB.addElement(lField, pNode.getModifiers());

				this.aDB.addRelation(this.aCurrType, Relation.DECLARES_FIELD, lField);

				// If there is any initialization to this field then we write
				// them as an access by <init> or <clinit>
				if (lInit != null) {
					this.aDB.addRelationAndTranspose(this.aCurrMethod, Relation.ACCESSES, lField);
					this.aDB.addRelationAndTranspose(this.aCurrMethod, Relation.SETS, lField);

					// Want to go into the right side of assignment operator
					lInit.accept(this);
				}
			}
		}

		// Because we have covered everything we need about field declaration,
		// we dont' have to go deeper into this node.
		// return false;
		return true;
	}

	@Override
	public boolean visit(final ImportDeclaration node) {
		return false;
	}

	@Override
	public boolean visit(final Initializer pNode) {
		if (this.aCurrMethod != null)
			this.aCurrMethodReminder.push(this.aCurrMethod);

		if (Flags.isStatic(pNode.getModifiers()))
			this.aCurrMethod = (MethodElement) FlyweightElementFactory.getElement(Category.METHOD,
					this.aCurrType.getId() + "." + ASTCrawler.aCLINIT_METHOD_NAME);
		else {
			this.aCurrMethod = (MethodElement) FlyweightElementFactory.getElement(Category.METHOD,
					this.aCurrType.getId() + "." + ASTCrawler.aINIT_METHOD_NAME);
			this.aCurrConstructorList.add(this.aCurrMethod);
		}

		this.aDB.addElement(this.aCurrMethod, pNode.getModifiers());
		this.aDB.addRelation(this.aCurrType, Relation.DECLARES_METHOD, this.aCurrMethod);
		return true;
	}

	@Override
	public boolean visit(final InstanceofExpression pNode) {
		if (ASTCrawler.checkForNull(this.aCurrMethod))
			return false;

		final ITypeBinding lBinding = pNode.getRightOperand().resolveBinding();
		if (lBinding != null) {
			final IElement lClass = ASTCrawler.convertBinding(lBinding);
			this.aDB.addElement(lClass,
					lBinding.getModifiers() | (lBinding.isInterface() ? ASTCrawler.ABSTRACT_FLAG : 0));
			this.aDB.addRelationAndTranspose(this.aCurrMethod, Relation.CHECKS, lClass);
		}
		return true;
	}

	@Override
	public boolean visit(final MarkerAnnotation node) {
		final ITypeBinding binding = node.resolveTypeBinding();
		if (ASTCrawler.checkForNull(binding))
			return false;
		final IElement annoteElem = ASTCrawler.convertBinding(binding);
		this.aDB.addElement(annoteElem, binding.getModifiers());

		final ASTNode annotatedNode = node.getParent();
		switch (annotatedNode.getNodeType()) {
		case ASTNode.METHOD_DECLARATION: {
			final MethodDeclaration annotatedMethod = (MethodDeclaration) annotatedNode;
			final IMethodBinding mBinding = annotatedMethod.resolveBinding();
			return this.addAnnotationRelation(annoteElem, mBinding);

		}
		case ASTNode.ANNOTATION_TYPE_DECLARATION: {
			final AnnotationTypeDeclaration annotatedAnnotation = (AnnotationTypeDeclaration) annotatedNode;
			final ITypeBinding tBinding = annotatedAnnotation.resolveBinding();
			return this.addAnnotationRelation(annoteElem, tBinding);
		}

		case ASTNode.VARIABLE_DECLARATION_FRAGMENT: {
			final VariableDeclarationFragment varDeclFrag = (VariableDeclarationFragment) annotatedNode;
			final IVariableBinding vBinding = varDeclFrag.resolveBinding();
			return this.addAnnotationRelation(annoteElem, vBinding);
		}

		case ASTNode.PACKAGE_DECLARATION: {
			final PackageDeclaration packDecl = (PackageDeclaration) annotatedNode;
			final IPackageBinding pBinding = packDecl.resolveBinding();
			return this.addAnnotationRelation(annoteElem, pBinding);
		}

		case ASTNode.SINGLE_VARIABLE_DECLARATION: {
			final SingleVariableDeclaration svd = (SingleVariableDeclaration) annotatedNode;
			final IVariableBinding vBinding = svd.resolveBinding();
			return this.addAnnotationRelation(annoteElem, vBinding);
		}
		case ASTNode.TYPE_DECLARATION: {
			final TypeDeclaration tDecl = (TypeDeclaration) annotatedNode;
			final ITypeBinding tBinding = tDecl.resolveBinding();
			return this.addAnnotationRelation(annoteElem, tBinding);
		}
		case ASTNode.ENUM_DECLARATION: {
			final EnumDeclaration eDecl = (EnumDeclaration) annotatedNode;
			final ITypeBinding tBinding = eDecl.resolveBinding();
			return this.addAnnotationRelation(annoteElem, tBinding);
		}

		case ASTNode.FIELD_DECLARATION: {
			final FieldDeclaration fieldDecl = (FieldDeclaration) annotatedNode;
			for (final Object obj : fieldDecl.fragments()) {
				final VariableDeclarationFragment vdf = (VariableDeclarationFragment) obj;
				final IVariableBinding vBinding = vdf.resolveBinding();
				return this.addAnnotationRelation(annoteElem, vBinding);
			}
		}

		default: {
			throw new IllegalStateException("Illegal annotated node type: " + annotatedNode);
		}
		}
	}

	/*
	 * Add the method to database. Add "Declares" relation to Class. If it's a
	 * constructor, copy all relations in temp method
	 */
	@Override
	@SuppressWarnings("restriction")
	public boolean visit(final MethodDeclaration pNode) {
		final IMethodBinding lMBinding = pNode.resolveBinding();

		if (ASTCrawler.checkForNull(lMBinding))
			return false;
		this.saveMethodRelation(lMBinding);

		this.aDB.addRelation(this.aCurrType, Relation.DECLARES_METHOD, this.aCurrMethod);

		// If this is a constructor, we dump the class initilization relations
		// into the constructor
		if (lMBinding.isConstructor())
			this.aCurrConstructorList.add(this.aCurrMethod);
		// aDB.copyRelations( aTempMethod, aCurrMethod ); // TODO: But what if
		// there're fields that have not yet be parsed?

		// System.out.println(this.aCurrMethod);
		// IJavaElement elem = lMBinding.getJavaElement();
		// if ( AJModel.getInstance().isAdvised(elem) ) {
		// List<AdviceElement> applicableAdvice = getApplicableAdvice(elem);
		// for (AdviceElement advice: applicableAdvice ) {
		// if ( advice != null && advice.equals(this.selectedAdvice) ) {
		// IElement adviceElem = Utils.convertBinding(Category.ADVICE,
		// advice.getHandleIdentifier());
		// try {
		// this.aDB.addElement(adviceElem, advice.getFlags());
		// } catch (JavaModelException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		//
		// this.aDB.addRelation(adviceElem, Relation.ADVISES, aCurrMethod);
		// }
		// }
		// }
		return true;
	}

	@Override
	public boolean visit(final MethodInvocation pNode) {
		final SimpleName lName = pNode.getName();
		final IBinding lBinding = lName.resolveBinding();

		if (ASTCrawler.checkForNull(lBinding))
			return false;

		final IMethodBinding lMethod = (IMethodBinding) lBinding;
		this.addCallRelation(pNode, lMethod, Modifier.isStatic(lMethod.getModifiers()));
		return true;
	}

	@Override
	public boolean visit(final NormalAnnotation node) {
		final ITypeBinding binding = node.resolveTypeBinding();
		if (ASTCrawler.checkForNull(binding))
			return false;
		final IElement annoteElem = ASTCrawler.convertBinding(binding);
		this.aDB.addElement(annoteElem, binding.getModifiers());

		final ASTNode annotatedNode = node.getParent();
		switch (annotatedNode.getNodeType()) {
		case ASTNode.METHOD_DECLARATION: {
			final MethodDeclaration annotatedMethod = (MethodDeclaration) annotatedNode;
			final IMethodBinding mBinding = annotatedMethod.resolveBinding();
			return this.addAnnotationRelation(annoteElem, mBinding);

		}
		case ASTNode.ANNOTATION_TYPE_DECLARATION: {
			final AnnotationTypeDeclaration annotatedAnnotation = (AnnotationTypeDeclaration) annotatedNode;
			final ITypeBinding tBinding = annotatedAnnotation.resolveBinding();
			return this.addAnnotationRelation(annoteElem, tBinding);
		}

		case ASTNode.VARIABLE_DECLARATION_FRAGMENT: {
			final VariableDeclarationFragment varDeclFrag = (VariableDeclarationFragment) annotatedNode;
			final IVariableBinding vBinding = varDeclFrag.resolveBinding();
			return this.addAnnotationRelation(annoteElem, vBinding);
		}

		case ASTNode.PACKAGE_DECLARATION: {
			final PackageDeclaration packDecl = (PackageDeclaration) annotatedNode;
			final IPackageBinding pBinding = packDecl.resolveBinding();
			return this.addAnnotationRelation(annoteElem, pBinding);
		}

		case ASTNode.SINGLE_VARIABLE_DECLARATION: {
			final SingleVariableDeclaration svd = (SingleVariableDeclaration) annotatedNode;
			final IVariableBinding vBinding = svd.resolveBinding();
			return this.addAnnotationRelation(annoteElem, vBinding);
		}
		case ASTNode.TYPE_DECLARATION: {
			final TypeDeclaration tDecl = (TypeDeclaration) annotatedNode;
			final ITypeBinding tBinding = tDecl.resolveBinding();
			return this.addAnnotationRelation(annoteElem, tBinding);
		}
		case ASTNode.ENUM_DECLARATION: {
			final EnumDeclaration eDecl = (EnumDeclaration) annotatedNode;
			final ITypeBinding tBinding = eDecl.resolveBinding();
			return this.addAnnotationRelation(annoteElem, tBinding);
		}
		case ASTNode.FIELD_DECLARATION: {
			final FieldDeclaration fieldDecl = (FieldDeclaration) annotatedNode;
			for (final Object obj : fieldDecl.fragments()) {
				final VariableDeclarationFragment vdf = (VariableDeclarationFragment) obj;
				final IVariableBinding vBinding = vdf.resolveBinding();
				return this.addAnnotationRelation(annoteElem, vBinding);
			}
		}
		default: {
			throw new IllegalStateException("Illegal annotated node type: " + annotatedNode);
		}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean visit(final PackageDeclaration pNode) {
		final IPackageBinding binding = pNode.resolveBinding();
		if (ASTCrawler.checkForNull(binding))
			return false;
		final IElement packageElem = this.convertBinding(binding);

		this.aDB.addElement(packageElem, binding.getModifiers());

		final CompilationUnit parent = (CompilationUnit) pNode.getParent();
		final List containedTypes = parent.types();
		for (final Iterator it = containedTypes.iterator(); it.hasNext();) {
			final AbstractTypeDeclaration type = (AbstractTypeDeclaration) it.next();
			final ITypeBinding typeBinding = type.resolveBinding();
			final IElement typeElem = ASTCrawler.convertBinding(typeBinding);
			this.aDB.addElement(typeElem, typeBinding.getModifiers());
			this.aDB.addRelation(packageElem, Relation.CONTAINS, typeElem);
		}

		return true;
	}

	@Override
	public boolean visit(final QualifiedName pNode) {
		final IBinding lBinding = pNode.resolveBinding();

		if (lBinding == null) {
			ASTCrawler.log("Null binding 3 for " + pNode);
			return false;
		}

		if (lBinding.getKind() == IBinding.VARIABLE)
			if (((IVariableBinding) lBinding).isField() && this.aCurrMethod != null) {
				this.addAccessRelation((IVariableBinding) lBinding);

				final Assignment assignment = ASTCrawler.getAssignment(pNode);
				if (assignment != null) {
					this.addSetsRelation((IVariableBinding) lBinding);

					if (!(assignment.getOperator() == Assignment.Operator.ASSIGN))
						this.addGetsRelation((IVariableBinding) lBinding);
				} else
					this.addGetsRelation((IVariableBinding) lBinding);
			}
		return false;
	}

	@Override
	public boolean visit(final SimpleName pNode) {
		final IBinding lBinding = pNode.resolveBinding();

		if (lBinding == null)
			// Occurs for all labels (e.g., loop labels)
			// log( "Null binding 4 for " + pNode );
			return false;
		if (lBinding.getKind() == IBinding.VARIABLE)
			if (((IVariableBinding) lBinding).isField()) {
				this.addAccessRelation((IVariableBinding) lBinding);

				final Assignment assignment = ASTCrawler.getAssignment(pNode);
				if (assignment != null) {
					this.addSetsRelation((IVariableBinding) lBinding);

					if (!(assignment.getOperator() == Assignment.Operator.ASSIGN))
						this.addGetsRelation((IVariableBinding) lBinding);
				} else
					this.addGetsRelation((IVariableBinding) lBinding);
			}

		return false;
	}

	@Override
	public boolean visit(final SingleMemberAnnotation node) {
		final ITypeBinding binding = node.resolveTypeBinding();
		if (ASTCrawler.checkForNull(binding))
			return false;
		final IElement annoteElem = ASTCrawler.convertBinding(binding);
		this.aDB.addElement(annoteElem, binding.getModifiers());

		final ASTNode annotatedNode = node.getParent();
		switch (annotatedNode.getNodeType()) {
		case ASTNode.METHOD_DECLARATION: {
			final MethodDeclaration annotatedMethod = (MethodDeclaration) annotatedNode;
			final IMethodBinding mBinding = annotatedMethod.resolveBinding();
			return this.addAnnotationRelation(annoteElem, mBinding);

		}
		case ASTNode.ANNOTATION_TYPE_DECLARATION: {
			final AnnotationTypeDeclaration annotatedAnnotation = (AnnotationTypeDeclaration) annotatedNode;
			final ITypeBinding tBinding = annotatedAnnotation.resolveBinding();
			return this.addAnnotationRelation(annoteElem, tBinding);
		}

		case ASTNode.VARIABLE_DECLARATION_FRAGMENT: {
			final VariableDeclarationFragment varDeclFrag = (VariableDeclarationFragment) annotatedNode;
			final IVariableBinding vBinding = varDeclFrag.resolveBinding();
			return this.addAnnotationRelation(annoteElem, vBinding);
		}

		case ASTNode.PACKAGE_DECLARATION: {
			final PackageDeclaration packDecl = (PackageDeclaration) annotatedNode;
			final IPackageBinding pBinding = packDecl.resolveBinding();
			return this.addAnnotationRelation(annoteElem, pBinding);
		}

		case ASTNode.SINGLE_VARIABLE_DECLARATION: {
			final SingleVariableDeclaration svd = (SingleVariableDeclaration) annotatedNode;
			final IVariableBinding vBinding = svd.resolveBinding();
			return this.addAnnotationRelation(annoteElem, vBinding);
		}
		case ASTNode.TYPE_DECLARATION: {
			final TypeDeclaration tDecl = (TypeDeclaration) annotatedNode;
			final ITypeBinding tBinding = tDecl.resolveBinding();
			return this.addAnnotationRelation(annoteElem, tBinding);
		}
		case ASTNode.ENUM_DECLARATION: {
			final EnumDeclaration eDecl = (EnumDeclaration) annotatedNode;
			final ITypeBinding tBinding = eDecl.resolveBinding();
			return this.addAnnotationRelation(annoteElem, tBinding);
		}
		case ASTNode.FIELD_DECLARATION: {
			final FieldDeclaration fieldDecl = (FieldDeclaration) annotatedNode;
			for (final Object obj : fieldDecl.fragments()) {
				final VariableDeclarationFragment vdf = (VariableDeclarationFragment) obj;
				final IVariableBinding vBinding = vdf.resolveBinding();
				return this.addAnnotationRelation(annoteElem, vBinding);
			}
		}
		default: {
			throw new IllegalStateException("Illegal annotated node type: " + annotatedNode);
		}
		}
	}

	@Override
	public boolean visit(final SuperConstructorInvocation pNode) {
		final Object obj = pNode.resolveConstructorBinding();
		if (obj == null) {
			System.out.println("uh oh");
			return true;
		}
		this.addCallRelation(pNode, pNode.resolveConstructorBinding(), true);
		return true;
	}

	@Override
	public boolean visit(final SuperFieldAccess pNode) {
		final IVariableBinding lBinding = (IVariableBinding) pNode.getName().resolveBinding();

		if (lBinding == null) {
			ASTCrawler.log("Null binding 2 for" + pNode);
			return false;
		}
		this.addAccessRelation(lBinding);

		final Assignment assignment = ASTCrawler.getAssignment(pNode);
		if (assignment != null) {
			this.addSetsRelation(lBinding);

			if (!(assignment.getOperator() == Assignment.Operator.ASSIGN))
				this.addGetsRelation(lBinding);
		} else
			this.addGetsRelation(lBinding);
		return true;
	}

	@Override
	public boolean visit(final SuperMethodInvocation pNode) {
		this.addCallRelation(pNode, pNode.resolveMethodBinding(), true);
		return true;
	}

	/**
	 * Generated the DECLARES relations between a type and and nested types, the
	 * EXTENDS relations, and the IMPLEMENTS relation
	 */
	@Override
	public boolean visit(final TypeDeclaration pNode) {
		final ITypeBinding lBinding = pNode.resolveBinding();

		if (ASTCrawler.checkForNull(lBinding))
			return false;

		try {
			this.saveTypeRelation(lBinding);

			// Add Declaration relations if this is a local or nested class
			if (lBinding.isLocal() || lBinding.isAnonymous())
				this.aDB.addRelation(this.aCurrMethod, Relation.DECLARES_TYPE, this.aCurrType);
			else if (lBinding.isNested())
				this.aDB.addRelation(this.aCurrTypeReminder.peek(), Relation.DECLARES_TYPE, this.aCurrType);

			// Find superclass
			if (!pNode.isInterface()) {
				final ITypeBinding lSuperBinding = lBinding.getSuperclass();
				if (lSuperBinding != null) {
					final IElement lSuperClass = ASTCrawler.convertBinding(lSuperBinding);
					this.aDB.addElement(lSuperClass, lSuperBinding.getModifiers());
					this.aDB.addRelationAndTranspose(this.aCurrType, Relation.EXTENDS_CLASS, lSuperClass);
				}
			}

			// Find interfaces.
			final ITypeBinding lInterfaceBindings[] = lBinding.getInterfaces();
			for (final ITypeBinding element : lInterfaceBindings) {
				final IElement lInterface = ASTCrawler.convertBinding(element);
				this.aDB.addElement(lInterface, element.getModifiers() | ASTCrawler.ABSTRACT_FLAG);
				if (pNode.isInterface())
					this.aDB.addRelationAndTranspose(this.aCurrType, Relation.EXTENDS_INTERFACES, lInterface);
				else
					this.aDB.addRelationAndTranspose(this.aCurrType, Relation.IMPLEMENTS_INTERFACE, lInterface);
			}
		} catch (final Exception pException) {
			ProblemManager.reportException(pException);
		}

		return true;
	}

	/**
	 * Add an ACCESS relation and its transpose between aCurrentMethod and the
	 * field described by pBinding. If any problem is detected the relation is
	 * not added and the error is logged.
	 * 
	 * @param pBinding
	 *            The field to add a relation to.
	 * @pre aCurrMethod is not null
	 * @pre aCurrMethod is in the database
	 * @pre pBinding is not null
	 */
	private void addAccessRelation(final IVariableBinding pBinding) {
		// assert (pBinding != null);
		// assert (aDB.contains(aCurrMethod));
		// assert (aCurrMethod != null);

		if (pBinding.getDeclaringClass() == null)
			// This is most likely an access to the length
			// field of an array.
			return;
		final IElement lField = ASTCrawler.convertBinding(pBinding);
		this.aDB.addElement(lField, pBinding.getModifiers());
		this.aDB.addRelationAndTranspose(this.aCurrMethod, Relation.ACCESSES, lField);
	}

	/**
	 * @param annoteElem
	 * @param tBinding
	 * @param annotatedElement
	 */
	private void addAnnotationRelation(final IElement annoteElem, final IBinding binding,
			final IElement annotatedElement) {
		this.aDB.addElement(annotatedElement, binding.getModifiers());
		this.aDB.addRelation(annoteElem, Relation.ANNOTATES, annotatedElement);
	}

	/**
	 * @param annoteElem
	 * @param annotatedAnnotation
	 */
	private boolean addAnnotationRelation(final IElement annoteElem, final IMethodBinding mBinding) {
		if (ASTCrawler.checkForNull(mBinding))
			return false;
		final IElement annotatedElement = ASTCrawler.convertBinding(mBinding);
		this.addAnnotationRelation(annoteElem, mBinding, annotatedElement);
		return true;
	}

	/**
	 * @param annoteElem
	 * @param binding
	 * @return
	 */
	private boolean addAnnotationRelation(final IElement annoteElem, final IPackageBinding binding) {
		if (ASTCrawler.checkForNull(binding))
			return false;
		final IElement annotatedElement = this.convertBinding(binding);
		this.addAnnotationRelation(annoteElem, binding, annotatedElement);
		return true;
	}

	/**
	 * @param annoteElem
	 * @param annotatedAnnotation
	 */
	private boolean addAnnotationRelation(final IElement annoteElem, final ITypeBinding tBinding) {
		if (ASTCrawler.checkForNull(tBinding))
			return false;
		final IElement annotatedElement = ASTCrawler.convertBinding(tBinding);
		this.addAnnotationRelation(annoteElem, tBinding, annotatedElement);
		return true;
	}

	/**
	 * @param annoteElem
	 * @param binding
	 * @return
	 */
	private boolean addAnnotationRelation(final IElement annoteElem, final IVariableBinding binding) {
		if (ASTCrawler.checkForNull(binding))
			return false;
		final IElement annotatedElement = ASTCrawler.convertBinding(binding);
		this.addAnnotationRelation(annoteElem, binding, annotatedElement);
		return true;
	}

	private void addCallRelation(final ASTNode pNode, final IMethodBinding pBinding, final boolean pStatic) {
		// assert( pBinding != null ); TODO

		if (this.aCurrMethod == null)
			// constructors calling itself. Ignore it.
			return;

		// lAcceptor could be a Java.util method
		if (pBinding == null)
			return;
		final IElement lAcceptor = ASTCrawler.convertBinding(pBinding);
		final IElement lCaller = this.aCurrMethod;

		// lCaller instanceof IClassElement
		// lAcceptor instanceof IMethodElement
		int lModifiers = pBinding.getModifiers();
		if (pBinding.getDeclaringClass().isInterface() || Modifier.isAbstract(lModifiers))
			lModifiers = lModifiers | ASTCrawler.ABSTRACT_FLAG;

		this.aDB.addElement(lAcceptor, lModifiers);

		if (pStatic)
			this.aDB.addRelationAndTranspose(lCaller, Relation.STATIC_CALLS, lAcceptor);
		else
			this.aDB.addRelationAndTranspose(lCaller, Relation.CALLS, lAcceptor);
	}

	private void addGetsRelation(final IVariableBinding pBinding) {
		// assert (pBinding != null);
		// assert (aDB.contains(aCurrMethod));
		// assert (aCurrMethod != null);

		if (pBinding.getDeclaringClass() == null)
			// This is most likely an access to the length
			// field of an array.
			return;
		final IElement lField = ASTCrawler.convertBinding(pBinding);
		this.aDB.addElement(lField, pBinding.getModifiers());
		this.aDB.addRelationAndTranspose(this.aCurrMethod, Relation.GETS, lField);
	}

	private void addSetsRelation(final IVariableBinding pBinding) {
		// assert (pBinding != null);
		// assert (aDB.contains(aCurrMethod));
		// assert (aCurrMethod != null);

		if (pBinding.getDeclaringClass() == null)
			// This is most likely an access to the length
			// field of an array.
			return;
		final IElement lField = ASTCrawler.convertBinding(pBinding);
		this.aDB.addElement(lField, pBinding.getModifiers());
		this.aDB.addRelationAndTranspose(this.aCurrMethod, Relation.SETS, lField);
	}

	/**
	 * @param binding
	 * @return
	 */
	private IElement convertBinding(final IPackageBinding binding) {
		ASTCrawler.checkForNull(binding);
		return FlyweightElementFactory.getElement(Category.PACKAGE, binding.getName());
	}

	// Extracts types and loads them into the converter
	private void extractTypes(final ICompilationUnit pCU) {
		try {
			final IType[] lTypes = pCU.getAllTypes();

			for (final IType element : lTypes)
				this.aConverter.addMapping(element);
		} catch (final JavaModelException pException) {
			pException.printStackTrace();
		}
	}

	@SuppressWarnings("unused")
	private boolean isNonPrimitive(final ITypeBinding pBinding) {
		// As long as it's not of primitive type or primitive type array
		if (pBinding.isPrimitive())
			return false;
		else if (pBinding.isArray()) {
			final ITypeBinding lBinding = pBinding.getElementType();
			return !lBinding.isPrimitive();
		}

		return true;
	}

	/**
	 * Clear class fields and be ready to work on a different compilation unit.
	 */
	private void resetSpider() {
		// aHasConstructor = false;
		// aHasConstructorReminder = new BooleanStack();
		this.aCurrConstructorList = null;
		this.aCurrConstructorListReminder = new Stack<ArrayList<MethodElement>>();
		this.aCurrType = null;
		this.aCurrTypeReminder = new Stack<ClassElement>();
		this.aCurrMethod = null;
		this.aCurrMethodReminder = new Stack<MethodElement>();
		this.aTempMethod = null;
		this.aTempMethodReminder = new Stack<MethodElement>();
	}

	private void restoreMethodRelation() {
		if (!this.aCurrMethodReminder.isEmpty())
			this.aCurrMethod = this.aCurrMethodReminder.pop();
		else
			this.aCurrMethod = null;
	}

	/**
	 * Conclusion method for the visit on this Type Declaration and Enum
	 * Declaration. This type could be an inner class. This method checks to see
	 * if there is an actual (non-empty) <clinit>() method. If the method does
	 * exist and has relations in it, a DECLARES relations is added to the
	 * current type for this method. Then it removes current <temp> method
	 * completely from database And restore backup <temp>, if there is one.
	 * Should only be called by endVisit( EnumDeclaration ) and endVisit(
	 * TypeDeclaration ).
	 * 
	 */
	private void restoreTypeRelation() {
		// if this <clinit> method has relations in it, the relation DECLARES is
		// added to the current type
		if (this.aCurrType != null) {
			final MethodElement lMethod = (MethodElement) FlyweightElementFactory.getElement(Category.METHOD,
					this.aCurrType.getId() + "." + ASTCrawler.aCLINIT_METHOD_NAME);
			if (this.aDB.contains(lMethod) && this.aDB.hasRelations(lMethod))
				this.aDB.addRelation(this.aCurrType, Relation.DECLARES_METHOD, lMethod);
		}

		if (this.aTempMethod != null) {
			if (this.aDB.hasRelations(this.aTempMethod))
				if (this.aCurrConstructorList.size() == 0) {
					final IElement lDefaultConstructor = FlyweightElementFactory.getElement(Category.METHOD,
							this.aCurrType.getId() + "." + ASTCrawler.aINIT_METHOD_NAME);
					this.aDB.addElement(lDefaultConstructor, ASTCrawler.aINIT_METHOD_MODIFIERS);
					this.aDB.copyRelations(this.aTempMethod, lDefaultConstructor);
					this.aDB.addRelation(this.aCurrType, Relation.DECLARES_METHOD, lDefaultConstructor);
				} else
					for (final MethodElement lConstructor : this.aCurrConstructorList)
						this.aDB.copyRelations(this.aTempMethod, lConstructor);
			// Remove temp method, and any relations associated with it, from
			// memory database
			this.aDB.removeElement(this.aTempMethod);
		}

		// restore current type and temp method
		if (!this.aCurrTypeReminder.isEmpty())
			this.aCurrType = this.aCurrTypeReminder.pop();
		else
			this.aCurrType = null;

		if (!this.aTempMethodReminder.isEmpty())
			this.aTempMethod = this.aTempMethodReminder.pop();
		else
			this.aTempMethod = null;

		if (!this.aCurrConstructorListReminder.empty())
			this.aCurrConstructorList = this.aCurrConstructorListReminder.pop();
		else
			this.aCurrConstructorList = null;
	}

	private void saveMethodRelation(final IMethodBinding pMBinding) {

		if (this.aCurrMethod != null)
			this.aCurrMethodReminder.push(this.aCurrMethod);
		this.aCurrMethod = (MethodElement) ASTCrawler.convertBinding(pMBinding);

		int lModifiers = pMBinding.getModifiers();
		if (pMBinding.getDeclaringClass().isInterface() || Modifier.isAbstract(lModifiers))
			lModifiers = lModifiers | ASTCrawler.ABSTRACT_FLAG;
		this.aDB.addElement(this.aCurrMethod, lModifiers);
	}

	private void saveTypeRelation(final ITypeBinding pBinding) {
		if (pBinding.isTopLevel()) { // Assume both reminder stacks are empty

		} else // other wise, backup current type.
		{
			this.aCurrTypeReminder.push(this.aCurrType);
			this.aTempMethodReminder.push(this.aTempMethod);
			this.aCurrConstructorListReminder.push(this.aCurrConstructorList);
		}

		// Initialize the list for storing constructor declaration
		this.aCurrConstructorList = new ArrayList<MethodElement>(ASTCrawler.aINIT_CONSTRUCTOR_LIST_SIZE); //

		// Insert this type
		this.aCurrType = (ClassElement) ASTCrawler.convertBinding(pBinding);
		this.aDB.addElement(this.aCurrType,
				pBinding.getModifiers() | (pBinding.isInterface() ? ASTCrawler.ABSTRACT_FLAG : 0));

		// Insert temp method for field initializers
		this.aTempMethod = (MethodElement) FlyweightElementFactory.getElement(Category.METHOD,
				this.aCurrType.getId() + "." + ASTCrawler.aTEMP_METHOD_NAME);
		this.aDB.addElement(this.aTempMethod, pBinding.getModifiers());
	}

}
