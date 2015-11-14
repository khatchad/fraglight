/* JayFX - A Fact Extractor Plug-in for Eclipse
 * Copyright (C) 2006  McGill University (http://www.cs.mcgill.ca/~swevo/jayfx)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * $Revision: 1.5 $
 */

package ca.mcgill.cs.swevo.jayfx;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import ca.mcgill.cs.swevo.jayfx.model.IElement;
import ca.mcgill.cs.swevo.jayfx.model.Relation;

/**
 * A database storing all the relations between different program elements.
 */
public class ProgramDatabase {
	/**
	 * Data bundle associated with an element. Contains modifier flags and a map
	 * linking relations to their ranges. an IElement instance.
	 */
	class Bundle {
		private final Map<Relation, Set<IElement>> aRelations;
		private final int aModifier;

		/**
		 * Creates a new, empty information bundle.
		 * 
		 * @param pModifier
		 *            A modifier flag.
		 */
		public Bundle(final int pModifier) {
			this.aRelations = new HashMap<Relation, Set<IElement>>();
			this.aModifier = pModifier;
		}

		/**
		 * @return The Map of relations to range. never null.
		 */
		public Map<Relation, Set<IElement>> getRelationMap() {
			return this.aRelations;
		}
	}

	// Maps IElements (unique because of the Flyweight pattern
	// to bundles containing modifiers and relations
	private final Map<IElement, Bundle> aElements;

	/**
	 * Creates an empty program database.
	 */
	public ProgramDatabase() {
		this.aElements = new HashMap<IElement, Bundle>();
	}

	/**
	 * Adds an element in the database. The element is initialized with an empty
	 * relation set. If the element is already in the database, nothing happens.
	 * 
	 * @param pElement
	 *            The element to add. Should never be null.
	 * @param pModifier
	 *            The modifier flags for this element.
	 */
	public void addElement(final IElement pElement, final int pModifier) {
		// assert (pElement != null);
		if (!this.aElements.containsKey(pElement))
			this.aElements.put(pElement, new Bundle(pModifier));
	}

	/**
	 * Adds a relation pRelation between pElement1 and pElement2. If pElement1
	 * or pElement2 does not exist in the database, an exception is raised, so
	 * these should always be added first.
	 * 
	 * @param pElement1
	 *            The first element in the relation, never null.
	 * @param pRelation
	 *            The relation, never null.
	 * @param pElement2
	 *            The second element in the relation, never null.
	 * @throws ElementNotFoundException
	 *             If pElement1 or pElement2 is not found in the database.
	 */
	public void addRelation(final IElement pElement1, final Relation pRelation, final IElement pElement2)
			throws ElementNotFoundException {
		// assert (pElement1 != null);
		// assert (pElement2 != null);
		// assert (pRelation != null);

		if (!this.contains(pElement1))
			throw new ElementNotFoundException(pElement1.getId());
		if (!this.contains(pElement2))
			throw new ElementNotFoundException(pElement2.getId());

		final Map<Relation, Set<IElement>> lRelations = this.aElements.get(pElement1).getRelationMap();
		// assert (lRelations != null);

		Set<IElement> lElements = lRelations.get(pRelation);
		if (lElements == null) {
			lElements = new HashSet<IElement>();
			lRelations.put(pRelation, lElements);
		}
		lElements.add(pElement2);
	}

	/**
	 * Convenience method to add a relatio and its transpose at the same time.
	 * 
	 * @param pElement1
	 *            The domain of the relation. Should not be null.
	 * @param pRelation
	 *            The Relation relating the domain to the range. Should not be
	 *            null.
	 * @param pElement2
	 *            The range of the relation. Should not be null.
	 * @throws ElementNotFoundException
	 *             if either of pElement1 or pElement2 are not indexed in the
	 *             database.
	 */
	public void addRelationAndTranspose(final IElement pElement1, final Relation pRelation, final IElement pElement2)
			throws ElementNotFoundException {
		// assert (pElement1 != null);
		// assert (pElement2 != null);
		// assert (pRelation != null);

		if (!this.contains(pElement1))
			throw new ElementNotFoundException(pElement1.getId());
		if (!this.contains(pElement2))
			throw new ElementNotFoundException(pElement2.getId());

		this.addRelation(pElement1, pRelation, pElement2);
		this.addRelation(pElement2, pRelation.getInverseRelation(), pElement1);
	}

	/**
	 * Returns whether an element is indexed in the database.
	 * 
	 * @param pElement
	 *            An element to check for. Should not be null.
	 * @return Whether the database has information about pElement.
	 */
	public boolean contains(final IElement pElement) {
		// assert (pElement != null);
		return this.aElements.containsKey(pElement);
	}

	/**
	 * Copies all the relations associated with pFrom to pTo, including its
	 * transposes
	 * 
	 * @param pFrom
	 *            The source element. Must not be null and must exist in the
	 *            database.
	 * @param pTo
	 *            The target element. Must not be null and must exist in the
	 *            database.
	 * @throws ElementNotFoundException
	 *             If either pFrom or pTo is not indexed in the database.
	 */
	@SuppressWarnings("unchecked")
	public void copyRelations(final IElement pFrom, final IElement pTo) throws ElementNotFoundException {
		// assert (pFrom != null);
		// assert (pTo != null);

		if (!this.contains(pFrom))
			throw new ElementNotFoundException(pFrom.getId());
		if (!this.contains(pTo))
			throw new ElementNotFoundException(pTo.getId());

		final Map lRelations = this.aElements.get(pFrom).getRelationMap();
		for (final Iterator i = lRelations.keySet().iterator(); i.hasNext();) {
			final Relation lNext = (Relation) i.next();
			final Set lElements = (Set) lRelations.get(lNext);
			for (final Iterator j = lElements.iterator(); j.hasNext();)
				this.addRelationAndTranspose(pTo, lNext, (IElement) j.next());
		}
	}

	/**
	 * Dumps an image of the database to System.out. For testing purposes. Can
	 * be removed from stable releases.
	 */
	@SuppressWarnings("unchecked")
	public String dump() {
		final StringBuilder ret = new StringBuilder();
		for (final IElement lElement1 : this.aElements.keySet()) {
			ret.append(lElement1);
			final Bundle lRelations = this.aElements.get(lElement1);
			for (final Object element : lRelations.getRelationMap().keySet()) {
				final Relation lRelation = (Relation) element;
				System.out.println("    " + lRelation);
				for (final Iterator k = ((Set) lRelations.getRelationMap().get(lRelation)).iterator(); k.hasNext();)
					ret.append("        " + k.next());
			}
		}
		return ret.toString();
	}

	/**
	 * Returns all the elements indexed in the database.
	 * 
	 * @return A Set of IElement objects
	 */
	public Set<IElement> getAllElements() {
		return this.aElements.keySet();
	}

	/**
	 * Returns the modifier flag for the element
	 * 
	 * @return An integer representing the modifier. 0 if the element cannot be
	 *         found.
	 */
	public int getModifiers(final IElement pElement) {
		int lReturn = 0;
		if (this.aElements.containsKey(pElement)) {
			final Bundle lBundle = this.aElements.get(pElement);
			lReturn = lBundle.aModifier;
		}
		return lReturn;
	}

	/**
	 * Returns the set of elements related to the domain element through the
	 * specified relation.
	 * 
	 * @param pElement
	 *            The domain element. Cannot be null.
	 * @param pRelation
	 *            The target relation. Cannot be null.
	 * @return A Set of IElement representing the desired range. Never null.
	 * @throws ElementNotFoundException
	 *             If pElement is not indexed in the database
	 */
	public Set<IElement> getRange(final IElement pElement, final Relation pRelation) throws ElementNotFoundException {
		// assert (pElement != null);
		// assert (pRelation != null);
		if (!this.contains(pElement))
			// throw new ElementNotFoundException(pElement.getId());
			return new HashSet<IElement>();

		final Set<IElement> lReturn = new HashSet<IElement>();
		final Map<Relation, Set<IElement>> lRelations = this.aElements.get(pElement).getRelationMap();

		if (lRelations.containsKey(pRelation))
			lReturn.addAll(lRelations.get(pRelation));
		return lReturn;
	}

	/**
	 * Returns whether pElements has any associated relations.
	 * 
	 * @param pElement
	 *            The element to check. Must not be null and exist in the
	 *            database.
	 * @return True if pElement has any associated relations.
	 * @throws ElementNotFoundException
	 *             If either pFrom or pTo is not indexed in the database.
	 */
	@SuppressWarnings("unchecked")
	public boolean hasRelations(final IElement pElement) throws ElementNotFoundException {
		// assert (pElement != null);
		if (!this.contains(pElement))
			throw new ElementNotFoundException(pElement.getId());

		final Map lRelations = this.aElements.get(pElement).getRelationMap();
		return !lRelations.isEmpty();
	}

	/**
	 * Remove an element and all its direct and transpose relations.
	 * 
	 * @param pElement
	 *            The element to remove. Must not be null and must exist in the
	 *            database.
	 * @throws ElementNotFoundException
	 *             If pElement is not indexed in the database.
	 */
	@SuppressWarnings("unchecked")
	public void removeElement(final IElement pElement) throws ElementNotFoundException {
		// assert (pElement != null);
		if (!this.contains(pElement))
			throw new ElementNotFoundException(pElement.getId());

		final Map lRelations = this.aElements.get(pElement).getRelationMap();
		for (final Iterator i = lRelations.keySet().iterator(); i.hasNext();) {
			final Relation lNext = (Relation) i.next();
			final Set lElements = (Set) lRelations.get(lNext);
			for (final Iterator j = lElements.iterator(); j.hasNext();)
				this.removeRelation((IElement) j.next(), lNext.getInverseRelation(), pElement);
		}

		// Remove the element
		this.aElements.remove(pElement);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final StringBuilder ret = new StringBuilder();
		for (final IElement elem : this.getAllElements()) {
			ret.append(elem + "\n");
			for (final Relation relation : Relation.values()) {
				ret.append("\t" + relation + "\n");
				for (final IElement toElem : this.getRange(elem, relation))
					ret.append("\t" + toElem + "\n");
			}
		}
		return ret.toString();
	}

	/**
	 * Removes a relation from an element. Does not automatically remove the
	 * transpose relation.
	 * 
	 * @param pElement1
	 *            The element to remove the relation from. Must not be null and
	 *            must exist in the program database.
	 * @param pRelation
	 *            The relation linking pElement1 with pElement2. Must not be
	 *            null.
	 * @param pElement2
	 *            The range element of the relation. Must not be null and must
	 *            exist in the program database.
	 * @throws ElementNotFoundException
	 *             If either pFrom or pTo is not indexed in the database.
	 */
	@SuppressWarnings("unchecked")
	private void removeRelation(final IElement pElement1, final Relation pRelation, final IElement pElement2)
			throws ElementNotFoundException {
		// assert (pElement1 != null);
		// assert (pElement2 != null);
		// assert (pRelation != null);

		if (!this.contains(pElement1))
			throw new ElementNotFoundException(pElement1.getId());
		if (!this.contains(pElement2))
			throw new ElementNotFoundException(pElement2.getId());

		final Map lRelations = this.aElements.get(pElement1).getRelationMap();
		if (!lRelations.containsKey(pRelation))
			return;
		final Set lElements = (Set) lRelations.get(pRelation);
		lElements.remove(pElement2);
	}
}
