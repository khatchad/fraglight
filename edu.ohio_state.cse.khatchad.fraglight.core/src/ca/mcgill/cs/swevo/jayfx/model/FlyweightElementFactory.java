/* JayFX - A Fact Extractor Plug-in for Eclipse
 * Copyright (C) 2006  McGill University (http://www.cs.mcgill.ca/~swevo/jayfx)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * $Revision: 1.8 $
 */

package ca.mcgill.cs.swevo.jayfx.model;

import java.util.Hashtable;

import org.eclipse.jdt.core.IJavaElement;
import org.jdom.Element;

import edu.ohio_state.cse.khatchad.ajplugintools.ajayfx.model.AJCodeElement;
import edu.ohio_state.cse.khatchad.ajplugintools.ajayfx.model.AdviceElement;
import edu.ohio_state.cse.khatchad.ajplugintools.ajayfx.model.AspectElement;

/**
 * Factory participant in the Flyweight design pattern. Produces unique IElement
 * objects representing the various elements in a Java program.
 */
public class FlyweightElementFactory {

	private static final String KEY_SEPARATOR = ":";

	private static Hashtable<String, IElement> aElements = new Hashtable<String, IElement>();

	/**
	 * Returns a flyweight object representing a program element.
	 * 
	 * @param pCategory
	 *            The category of element. Must be a value declared in
	 *            Category.
	 * @param pId
	 *            The id for the element. For example, a field Id for
	 *            Category.FIELD.
	 * @see <a
	 *      href="http://java.sun.com/docs/books/jls/third_edition/html/binaryComp.html#13.1">
	 *      Java Specification, Third Section, 13.1 Section for the binary name
	 *      convention</a>
	 * @return A flyweight IElement.
	 * @exception InternalProblemException
	 *                if an invalid category is passed as parameter.
	 */
	public static IElement getElement(final Category pCategory,
			final String pId) {
		IElement lReturn = FlyweightElementFactory.aElements.get(pCategory
				+ FlyweightElementFactory.KEY_SEPARATOR + pId);
		if (lReturn == null) {
			if (pCategory == Category.CLASS)
				lReturn = new ClassElement(pId);
			else if (pCategory == Category.FIELD)
				lReturn = new FieldElement(pId);
			else if (pCategory == Category.METHOD)
				lReturn = new MethodElement(pId);
			else if (pCategory == Category.PACKAGE)
				lReturn = new PackageElement(pId);
			else if (pCategory == Category.ASPECT)
				lReturn = new AspectElement(pId);
			else if (pCategory == Category.ADVICE)
				lReturn = new AdviceElement(pId);
			else if (pCategory == Category.AJCODE)
				lReturn = new AJCodeElement(pId);
			else
				throw new InternalProblemException("Invalid element category: "
						+ pCategory);
			FlyweightElementFactory.aElements.put(pCategory
					+ FlyweightElementFactory.KEY_SEPARATOR + pId, lReturn);
		}
		return lReturn;
	}

	private FlyweightElementFactory() {
	}

	/**
	 * @param elementXML
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <E> E getElement(Element elementXML) {
		//extract the category and the ID and delegate the behavior.
		String identifierString = elementXML.getAttribute(IElement.ID).getValue();
		Category category = Category.valueOf(elementXML.getChild(Category.class.getSimpleName()));
		return (E) getElement(category, identifierString);
	}
}