/* JayFX - A Fact Extractor Plug-in for Eclipse
 * Copyright (C) 2006  McGill University (http://www.cs.mcgill.ca/~swevo/jayfx)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * $Revision: 1.8 $
 */

package ca.mcgill.cs.swevo.jayfx;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.ajdt.core.javaelements.IAJCodeElement;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchPattern;

import ca.mcgill.cs.swevo.jayfx.model.Category;
import ca.mcgill.cs.swevo.jayfx.model.FieldElement;
import ca.mcgill.cs.swevo.jayfx.model.FlyweightElementFactory;
import ca.mcgill.cs.swevo.jayfx.model.IElement;
import ca.mcgill.cs.swevo.jayfx.model.MethodElement;
import edu.ohio_state.cse.khatchad.fraglight.core.util.SearchEngineUtil;

/**
 * Convert elements between different formats using a fast lookup supported by a
 * map that needs to be initialized. This converter does not support finding
 * anonymous and local types.
 */
public class FastConverter {
	private static int numberOfParams(final String pSignature) {
		if (pSignature.length() == 2)
			return 0;
		int lReturn = 1;
		for (int i = 0; i < pSignature.length(); i++)
			if (pSignature.charAt(i) == ',')
				lReturn++;
		return lReturn;
	}

	// Binary type names -> IType
	private final Map<String, IType> aTypeMap;

	/**
	 * Creates a new, empty converter.
	 */
	public FastConverter() {
		this.aTypeMap = new HashMap<String, IType>();
	}

	/**
	 * Adds type to the converter
	 * 
	 * @param pType
	 *            The actual type in a Java project
	 */
	public void addMapping(final IType pType) {
		this.aTypeMap.put(pType.getFullyQualifiedName('$'), pType);
	}

	/**
	 * for early testing Description of FastConverter
	 */
	public void dump() {
		for (final String lNext : this.aTypeMap.keySet()) {
			String lConverted = "Not found";
			final IJavaElement lElement = this.aTypeMap.get(lNext);
			if (lNext != null) {
				lConverted = lElement.toString();
				System.out.println(lNext);
				System.out.println(lNext + " -> " + lConverted);
			}
		}
	}

	/**
	 * Returns an IElement describing the argument Java element. Not designed to
	 * be able to find initializer blocks or arrays.
	 * 
	 * @param pElement
	 *            Never null.
	 * @return Never null
	 */
	public IElement getElement(final IJavaElement pElement) throws ConversionException {
		// assert( pElement != null );
		IElement lReturn = null;

		if (pElement instanceof IType)
			lReturn = this.getClassElement((IType) pElement);
		else if (pElement instanceof IField) {
			final IElement lClass = this.getClassElement(((IField) pElement).getDeclaringType());
			lReturn = FlyweightElementFactory.getElement(Category.FIELD,
					lClass.getId() + "." + ((IField) pElement).getElementName());
		} else if (pElement instanceof IMethod) {
			final IElement lClass = this.getClassElement(((IMethod) pElement).getDeclaringType());
			String lName = ((IMethod) pElement).getElementName();
			try {
				if (((IMethod) pElement).isConstructor())
					lName = "<init>";
			} catch (final JavaModelException pException) {
				throw new ConversionException(pException);
			}
			String lSignature = "(";
			final String[] lParams = ((IMethod) pElement).getParameterTypes();
			for (int i = 0; i < lParams.length - 1; i++) {
				String param = null;
				if (Signature.getTypeSignatureKind(lParams[i]) == Signature.TYPE_VARIABLE_SIGNATURE) // its
																										// a
																										// type
																										// variable,
																										// erase
																										// it.
					param = "Ljava.lang.Object;";
				else
					param = lParams[i];

				lSignature += this.resolveType(param, ((IMethod) pElement).getDeclaringType()) + ",";
			}
			if (lParams.length > 0) {
				String param = lParams[lParams.length - 1];
				if (Signature
						.getTypeSignatureKind(Signature.getElementType(param)) == Signature.TYPE_VARIABLE_SIGNATURE) // its
																														// a
																														// type
																														// variable,
																														// erase
																														// it.
					param = "Ljava.lang.Object;";
				else
					param = lParams[lParams.length - 1];

				if (param.charAt(0) == 'Q' && param.charAt(1) == 'T')
					param = "Ljava.lang.Object;";
				lSignature += this.resolveType(param, ((IMethod) pElement).getDeclaringType());
			}
			lSignature += ")";
			lReturn = FlyweightElementFactory.getElement(Category.METHOD, lClass.getId() + "." + lName + lSignature);
		}

		else if (pElement instanceof IAJCodeElement) {
			final String lName = ((IAJCodeElement) pElement).getElementName();
			System.out.println(lName);
		}

		if (lReturn == null) {
			System.err.println("Error with element: " + pElement);
			throw new IllegalStateException("In trouble.");
		}
		return lReturn;
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
	public IJavaElement getJavaElement(final IElement pElement) throws ConversionException {
		// assert( pElement!= null );
		IJavaElement lReturn = null;
		if (pElement.getCategory() == Category.CLASS) {
			lReturn = this.aTypeMap.get(pElement.getId());
			if (lReturn == null)
				// TODO Make this smarter in the case of multiple projects
				if (this.aTypeMap.size() > 0)
					try {
						lReturn = this.aTypeMap.values().iterator().next().getJavaProject().findType(pElement.getId());
					} catch (final JavaModelException pException) {
						// noting
					}
			if (lReturn == null)
				throw new ConversionException("Cannot find type " + pElement);
		} else if (pElement.getCategory() == Category.PACKAGE) {
			String id = pElement.getId();
			final SearchPattern pattern = SearchPattern.createPattern(id, IJavaSearchConstants.PACKAGE,
					IJavaSearchConstants.DECLARATIONS, SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE);
			Collection<SearchMatch> matches = SearchEngineUtil.search(pattern, null);
			if (matches.isEmpty())
				throw new ConversionException("Cannot find type " + pElement);
			else
				lReturn = (IJavaElement) matches.iterator().next().getElement();
		} else {
			final IJavaElement lDeclaringClass = this.getJavaElement(pElement.getDeclaringClass());
			if (pElement.getCategory() == Category.FIELD) {
				lReturn = ((IType) lDeclaringClass).getField(((FieldElement) pElement).getSimpleName());
				if (lReturn == null)
					throw new ConversionException("Cannot find field " + pElement);
			} else if (pElement.getCategory() == Category.METHOD)
				try {
					final IMethod[] lMethods = ((IType) lDeclaringClass).getMethods();
					lReturn = this.findMethod((MethodElement) pElement, lMethods);

					// if( lReturn == null &&
					// isDefaultConstructor((MethodElement)pElement)) {
					//
					// }

					if (lReturn == null)
						throw new ConversionException("Cannot find method " + pElement);
				} catch (final JavaModelException pException) {
					throw new ConversionException("Cannot convert method " + pElement);
				}
			else
				throw new ConversionException("Unsupported element type " + pElement.getClass().getName());
		}
		// assert( lReturn != null );
		return lReturn;
	}

	/**
	 * Resets the lookup information in this converter.
	 */
	public void reset() {
		this.aTypeMap.clear();
	}

	public String resolveType(String pType, final IType pEnclosingType) throws ConversionException {
		String lReturn = "";
		int lDepth = 0;
		int lIndex = 0;
		while (pType.charAt(lIndex) == Signature.C_ARRAY) {
			lDepth++;
			lIndex++;
		}

		if (pType.charAt(lIndex) == Signature.C_BYTE || pType.charAt(lIndex) == Signature.C_CHAR
				|| pType.charAt(lIndex) == Signature.C_DOUBLE || pType.charAt(lIndex) == Signature.C_FLOAT
				|| pType.charAt(lIndex) == Signature.C_INT || pType.charAt(lIndex) == Signature.C_LONG
				|| pType.charAt(lIndex) == Signature.C_SHORT || pType.charAt(lIndex) == Signature.C_VOID
				|| pType.charAt(lIndex) == Signature.C_BOOLEAN || pType.charAt(lIndex) == Signature.C_RESOLVED)
			lReturn = pType;
		else
			try {
				pType = Signature.getTypeErasure(pType);
				final int lIndex2 = pType.indexOf(Signature.C_NAME_END);
				final String lType = pType.substring(lIndex + 1, lIndex2);
				final String[][] lTypes = pEnclosingType.resolveType(lType);
				if (lTypes == null)
					throw new ConversionException("Cannot convert type " + lType + " in " + pEnclosingType);
				if (lTypes.length != 1)
					throw new ConversionException("Cannot convert type " + lType + " in " + pEnclosingType);
				for (int i = 0; i < lDepth; i++)
					lReturn += "[";
				lReturn += "L" + lTypes[0][0] + "." + lTypes[0][1].replace('.', '$') + ";";
			} catch (final JavaModelException pException) {
				throw new ConversionException(pException);
			}
		return lReturn;
	}

	/**
	 * Optimization to find an IMethod without having to resolve the parameters.
	 * 
	 * @return the IMethod corresponding to a candidate. Null if none are found.
	 */
	private IJavaElement findMethod(final MethodElement pMethod, final IMethod[] pCandidates) {
		IJavaElement lReturn = null;

		final List<IMethod> lSimilar = new ArrayList<IMethod>();
		for (final IMethod element : pCandidates) {
			String lName = element.getElementName();
			try {
				if (element.isConstructor())
					lName = "<init>";
			} catch (final JavaModelException pException) {
				return null;
			}
			if (lName.equals(pMethod.getName()))
				if (element.getNumberOfParameters() == FastConverter.numberOfParams(pMethod.getParameters()))
					lSimilar.add(element);
		}
		if (lSimilar.size() == 1)
			lReturn = lSimilar.get(0);
		else
			for (int i = 0; i < lSimilar.size(); i++)
				try {
					if (this.getElement(lSimilar.get(i)) == pMethod)
						lReturn = lSimilar.get(i);
				} catch (final ConversionException pException) {
					// nothing, the method will return null
				}

		return lReturn;
	}

	@SuppressWarnings("unused")
	private IElement getAspectElement(final IType pType) {
		return FlyweightElementFactory.getElement(Category.ASPECT, pType.getFullyQualifiedName('$'));
	}

	private IElement getClassElement(final IType pType) {
		return FlyweightElementFactory.getElement(Category.CLASS, pType.getFullyQualifiedName('$'));
	}

	/**
	 * @param element
	 * @return
	 */
	@SuppressWarnings("unused")
	private boolean isDefaultConstructor(final MethodElement element) {
		return element.getName().contains("<init>") && FastConverter.numberOfParams(element.getParameters()) == 0;
	}
}
