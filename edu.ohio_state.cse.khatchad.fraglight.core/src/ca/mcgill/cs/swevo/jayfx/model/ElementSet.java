/* JayFX - A Fact Extractor Plug-in for Eclipse
 * Copyright (C) 2006  McGill University (http://www.cs.mcgill.ca/~swevo/jayfx)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * $Revision: 1.3 $
 */

package ca.mcgill.cs.swevo.jayfx.model;

import java.util.HashSet;
import java.util.Iterator;

/**
 * Contains a set of IElements. The comparison criteria for two elements are
 * their ids, so this set class will work even when the flyweight factory is not
 * used.
 */
public class ElementSet {
	private final HashSet<IElement> aElements;

	/**
	 * Default constructor. Creates an empty set.
	 */
	public ElementSet() {
		this.aElements = new HashSet<IElement>();
	}

	/**
	 * Adds a element to the set. As this is a set, duplicates are not allowed.
	 * 
	 * @param pElement
	 *            The element to add to this set.
	 */
	public void add(final IElement pElement) {
		this.aElements.add(pElement);
	}

	/**
	 * Adds all elements in pSet to this element set.
	 * 
	 * @param pSet
	 *            a set of elements to add to this set. All the elements in pSet
	 *            should be of type IElement.
	 */
	public void addAll(final ElementSet pSet) {
		this.aElements.addAll(pSet.aElements);
	}

	/**
	 * Returns true of pElement is in the set. Based on equality comparison
	 * based on method equals.
	 * 
	 * @param pElement
	 *            the element to search in this set.
	 * @return true if pElement is contained in this set.
	 */
	public boolean contains(final IElement pElement) {
		return this.aElements.contains(pElement);
	}

	/**
	 * @return An iterator to all the elements in this set.
	 */
	public Iterator<IElement> getAllElements() {
		return this.aElements.iterator();
	}

	/**
	 * Removes pElement from this set.
	 * 
	 * @param pElement
	 *            the element to remove from this set.
	 */
	public void remove(final IElement pElement) {
		this.aElements.remove(pElement);
	}

	/**
	 * @return The number of elements in the set.
	 */
	public int size() {
		return this.aElements.size();
	}

	/**
	 * Returns the set as an array ob objects.
	 * 
	 * @return All the elements in the set.
	 */
	public Object[] toArray() {
		return this.aElements.toArray();
	}
}
