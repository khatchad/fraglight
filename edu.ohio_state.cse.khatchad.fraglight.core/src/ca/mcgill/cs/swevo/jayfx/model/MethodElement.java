/* JayFX - A Fact Extractor Plug-in for Eclipse
 * Copyright (C) 2006  McGill University (http://www.cs.mcgill.ca/~swevo/jayfx)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * $Revision: 1.5 $
 */

package ca.mcgill.cs.swevo.jayfx.model;

import java.util.StringTokenizer;

import edu.ohio_state.cse.khatchad.fraglight.core.analysis.Constants;

/**
 * Represents a method element in the model.
 */
public class MethodElement extends AbstractElement {

	private static final long serialVersionUID = -6347203303128369543L;

	/**
	 * Creates a method objects. Such objects should not be created directly but
	 * should be obtained through a FlyweightElementFactory.
	 * 
	 * @param pId
	 *            The unique descriptor of this method. Comprises the fully
	 *            qualified name of the declaring class, followed by the name of
	 *            the method (or init for constructors), and the parameter list.
	 */
	protected MethodElement(final String pId) {
		super(pId);
	}

	/**
	 * Equality for method elements is based on the equality of their
	 * corresponding ids.
	 * 
	 * @param pObject
	 *            the object to compare to.
	 * @return true if this object has the same id as pObject.
	 * @see java.lang.Object#equals(Object)
	 */
	@Override
	public boolean equals(final Object pObject) {
		if (!(pObject instanceof MethodElement))
			return false;
		else
			return this.getId().equals(((MethodElement) pObject).getId());
	}

	/**
	 * Returns the category of this element type, i.e., a method.
	 * 
	 * @return Category.METHOD
	 */
	@Override
	public Category getCategory() {
		return Category.METHOD;
	}

	/**
	 * @return The name of the class declaring this method.
	 */
	@Override
	public ClassElement getDeclaringClass() {
		final String lName = this.getFirstParticle();
		final int lIndex = lName.lastIndexOf(".");
		ClassElement lReturn = null;
		lReturn = (ClassElement) FlyweightElementFactory.getElement(Category.CLASS, lName.substring(0, lIndex));
		return lReturn;
	}

	/**
	 * @return The simple name of the method.
	 */
	public String getName() {
		final String lName = this.getFirstParticle();
		final int lIndex = lName.lastIndexOf(".");
		return lName.substring(lIndex + 1, lName.length());
	}

	/**
	 * @return The name of the package in which the declaring class of this
	 *         method is defined in.
	 */
	@Override
	public String getPackageName() {
		return this.getDeclaringClass().getPackageName();
	}

	/**
	 * @return The String of parameter types for this method, including the
	 *         parentheses.
	 */
	public String getParameters() {
		final int lIndex = this.getId().indexOf("(");
		return this.getId().substring(lIndex, this.getId().length());
	}

	/**
	 * @return The id of this element without the package names for the name of
	 *         the method and the parameter types.
	 */
	@Override
	public String getShortName() {
		String lReturn = this.getDeclaringClass().getShortName() + "." + this.getName() + "(";
		final StringTokenizer lParser = new StringTokenizer(this.getParameters(), ",()");
		final int lNbTokens = lParser.countTokens();
		for (int i = 0; i < lNbTokens - 1; i++) {
			final String lToken = lParser.nextToken();
			final int lIndex = lToken.lastIndexOf('.');
			if (lIndex >= 0)
				lReturn += lToken.substring(lIndex + 1, lToken.length()) + ",";
			else
				lReturn += lToken + ",";
		}

		if (lNbTokens > 0) {
			final String lToken = lParser.nextToken();
			final int lIndex = lToken.lastIndexOf('.');
			if (lIndex >= 0)
				lReturn += lToken.substring(lIndex + 1, lToken.length());
			else
				lReturn += lToken;
		}
		return lReturn + ")";
	}

	/**
	 * The hashcode is determined based on the id of the method.
	 * 
	 * @return The hashcode of the id String for this method.
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return this.getId().hashCode();
	}

	/**
	 * @return Fully qualified name of the method.
	 */
	private String getFirstParticle() {
		final int lIndex = this.getId().indexOf("(");
		return this.getId().substring(0, lIndex);
	}

	public boolean isConstructor() {
		return this.getId().contains(Constants.INIT_STRING);
	}
}