/* JayFX - A Fact Extractor Plug-in for Eclipse
 * Copyright (C) 2006  McGill University (http://www.cs.mcgill.ca/~swevo/jayfx)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * $Revision: 1.6 $
 */

package ca.mcgill.cs.swevo.jayfx.model;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Vector;

import org.aspectj.lang.JoinPoint;
import org.jdom.Attribute;
import org.jdom.Element;

/**
 * Encapsulate various services related to relations.
 */
public enum Relation {

	EXPLICITLY_CALLS(Type.ID_EXPLICITLY_CALLS, true), ANNOTATES(
			Type.ID_ANNOTATES, true), CONTAINS(Type.ID_CONTAINS, true), CHECKS(
			Type.ID_CHECKS, true), CREATES(Type.ID_CREATES, true), DECLARES_METHOD(
			Type.ID_DECLARES_METHOD, true), DECLARES_FIELD(
			Type.ID_DECLARES_FIELD, true), DECLARES_TYPE(Type.ID_DECLARES_TYPE,
			true), EXTENDS_CLASS(Type.ID_EXTENDS_CLASS, true), EXTENDS_INTERFACES(
			Type.ID_EXTENDS_INTERFACES, true), HAS_PARAMETER_TYPES(
			Type.ID_HAS_PARAMETER_TYPES, true), HAS_RETURN_TYPE(
			Type.ID_HAS_RETURN_TYPE, true), IMPLEMENTS_INTERFACE(
			Type.ID_IMPLEMENTS_INTERFACE, true), OF_TYPE(Type.ID_OF_TYPE, true), TRANS_EXTENDS(
			Type.ID_TRANS_EXTENDS, true), TRANS_IMPLEMENTS(
			Type.ID_TRANS_IMPLEMENTS, true),

	ACCESSES(Type.ID_ACCESSES, true), SETS(Type.ID_SETS, true), GETS(
			Type.ID_GETS, true), ADVISES(Type.ID_ADVISES, true), CALLS(
			Type.ID_CALLS, true), IMPLEMENTS_METHOD(Type.ID_IMPLEMENTS_METHOD,
			true), INHERITS(Type.ID_INHERITS, true), OVERRIDES(
			Type.ID_OVERRIDES, true), OVERLOADS(Type.ID_OVERLOADS, true), USES(
			Type.ID_USES, true),

	IDENTITY(Type.ID_IDENTITY, true), STATIC_CALLS(Type.ID_STATIC_CALLS, true), REFERENCES(
			Type.ID_REFERENCES, true),

	T_EXPLICITLY_CALLS(Type.ID_EXPLICITLY_CALLS, false), T_CHECKS(
			Type.ID_CHECKS, false), T_CREATES(Type.ID_CREATES, false), T_DECLARES(
			Type.ID_DECLARES, false), T_EXTENDS_CLASS(Type.ID_EXTENDS_CLASS,
			false), T_EXTENDS_INTERFACES(Type.ID_EXTENDS_INTERFACES, false), T_HAS_PARAMETER_TYPES(
			Type.ID_HAS_PARAMETER_TYPES, false), T_HAS_RETURN_TYPE(
			Type.ID_HAS_RETURN_TYPE, false), T_IMPLEMENTS_INTERFACE(
			Type.ID_IMPLEMENTS_INTERFACE, false), T_OF_TYPE(Type.ID_OF_TYPE,
			false), T_TRANS_EXTENDS(Type.ID_TRANS_EXTENDS, false), T_TRANS_IMPLEMENTS(
			Type.ID_TRANS_IMPLEMENTS, false),

	T_ACCESSES(Type.ID_ACCESSES, false), T_CALLS(Type.ID_CALLS, false), T_IMPLEMENTS_METHOD(
			Type.ID_IMPLEMENTS_METHOD, false), T_INHERITS(Type.ID_INHERITS,
			false), T_OVERRIDES(Type.ID_OVERRIDES, false), T_USES(Type.ID_USES,
			false),

	T_IDENTITY(Type.ID_IDENTITY, false), T_STATIC_CALLS(Type.ID_STATIC_CALLS,
			false), T_REFERENCES(Type.ID_REFERENCES, false);

	/**
	 * 
	 */
	private static final String TYPE = "type";

	/**
	 * Type enum encapsulates the following information of a relation: i. code
	 * name ii. direct name iii. transpose name of the relation iv. detailed
	 * description
	 * 
	 * @author iyuen
	 * @see ca.mcgill.cs.swevo.jayfx.model.Relation
	 */
	public enum Type {
		/*
		 * Primitive: 0-9
		 */
		ID_EXPLICITLY_CALLS("explicitly_calls", "e-calling", "e-called by",
				"a method calls a method explicitly"), ID_ANNOTATES(
				"annotates", "annotating", "annotated by",
				"an annotation annotates something"), ID_CONTAINS("contains",
				"contains", "contains", "a package contains a type"), ID_CHECKS(
				"checks", "checking", "checked by",
				"a method checks the run_time type of an object for a specific type"), ID_CREATES(
				"creates", "creating", "created by",
				"a method creates an object of a type"), ID_DECLARES(
				"declares", "declaring", "declared by",
				"a class declares a member"), ID_DECLARES_FIELD(
				"declares_field", "declaring field", "field declared by",
				"a class declares a field"), ID_DECLARES_METHOD(
				"declares_method", "declaring method", "method declared by",
				"a class declares a method"), ID_DECLARES_TYPE("declares_type",
				"declaring type", "type declared by", "a class declares a type"), ID_EXTENDS_CLASS(
				"extends_class", "extending", "extended by",
				"a class directly extends a class"), ID_EXTENDS_INTERFACES(
				"extends_interface", "i-extending", "i-extended by",
				"an interface directly extends an interface"), ID_HAS_PARAMETER_TYPES(
				"as-parameter-types", "having p-types", "p-type of",
				"a method has parameters of types"), ID_HAS_RETURN_TYPE(
				"has-return-type", "having r-type", "r-type of",
				"a method has return type"), ID_IMPLEMENTS_INTERFACE(
				"implements_interface", "implementing", "implemented by",
				"a class directly implements an interface"), ID_OF_TYPE(
				"of-type", "being of type", "type of", "a field is of type"),

		ID_TRANS_EXTENDS("transitively_extends", "transitively extending",
				"transitively extended by",
				"a class transitively extends a class"), ID_TRANS_IMPLEMENTS(
				"transitively_implements", "transitively implementing",
				"transitively implemented by",
				"a class transitively implements an interface"),

		ID_ACCESSES("accesses_field", "accessing", "accessed by",
				"a method accesses a field, either to read or write"), ID_SETS(
				"sets_field", "setting", "set by", "a method sets a field"), ID_GETS(
				"gets_field", "getting", "got by", "a method gets a field"),

		ID_ADVISES("advises", "advising", "advised by",
				"an advise advises something"), ID_CALLS("calls", "calling",
				"called by", "a method calls a method"), ID_IMPLEMENTS_METHOD(
				"implements_method", "m-implementing", "m-implemented by",
				"a method implements an interface method"), ID_INHERITS(
				"inherits", "inheriting", "inherited by",
				"a class inherits fields and methods"), ID_OVERRIDES(
				"overrides", "overriding", "overridden by",
				"a method overrides a method"), ID_OVERLOADS("overloads",
				"overloading", "overloaded by", "a method overloads a method"), ID_USES(
				"uses", "using", "used by", "a method uses a program element"),

		ID_IDENTITY("identity", "is", "is", "an element is itself"), ID_STATIC_CALLS(
				"statically_calls", "statically calls", "statically called by",
				"a method statically calls another method"), ID_REFERENCES(
				"references", "references", "referenced by",
				"a class/method references a class/method/field"),

		ID_ERROR("error", "error", "error", "error");

		private final String aCode;
		private final String aDirectName;
		private final String aTransposeName;
		private final String aDescription;

		Type(final String pCode, final String pDirectName,
				final String pTransposeName, final String pDescription) {
			this.aCode = pCode;
			this.aDirectName = pDirectName;
			this.aTransposeName = pTransposeName;
			this.aDescription = pDescription;
		}

		public String getCode() {
			return this.aCode;
		}

		public String getDescription() {
			return this.aDescription;
		}

		public String getDirectName() {
			return this.aDirectName;
		}

		public String getTransposeName() {
			return this.aTransposeName;
		}
	}

	private static EnumSet<Relation> aRelationSet;
	private static EnumSet<Relation> tRelationSet; // Transpose
	private static EnumMap<Type, Relation> aRelationMap;
	private static EnumMap<Type, Relation> tRelationMap; // Transpose
	private static final String TRANSPOSE_CODE = "*";
	static {
		Relation.aRelationSet = EnumSet.range(Relation.EXPLICITLY_CALLS,
				Relation.REFERENCES);
		Relation.tRelationSet = EnumSet.complementOf(Relation.aRelationSet);
		//cRelationSet = EnumSet.range(Relation.T_EXPLICITLY_CALLS, Relation.T_REFERENCES);

		Relation.aRelationMap = new EnumMap<Type, Relation>(Type.class);
		Relation.tRelationMap = new EnumMap<Type, Relation>(Type.class);

		for (final Relation lRelation : Relation.aRelationSet)
			Relation.aRelationMap.put(lRelation.getType(), lRelation);
		for (final Relation lTransRelation : Relation.tRelationSet)
			Relation.tRelationMap.put(lTransRelation.getType(), lTransRelation);
	}

	/**
	 * @return All relations.
	 */
	public static Relation[] getAllRelations() {

		return Relation.values();
	}

	public static Relation[] getAllRelations(final boolean pDirect) {
		final Relation[] rArray = new Relation[Relation.aRelationSet.size()];
		if (pDirect)
			return Relation.aRelationSet.toArray(rArray);
		else
			return Relation.tRelationSet.toArray(rArray);

	}

	/**
	 * Returns all the relations for which a domain category is valid.
	 */
	public static Relation[] getAllRelations(final Category pCategory,
			final boolean pDirect) {
		final Vector<Relation> lReturn = new Vector<Relation>();

		if (pDirect) {
			for (final Relation r : Relation.aRelationSet)
				if (r.hasDomainCategory(pCategory))
					lReturn.addElement(r);
		}
		else
			for (final Relation t : Relation.tRelationSet)
				if (t.hasDomainCategory(pCategory))
					lReturn.addElement(t);

		return lReturn.toArray(new Relation[lReturn.size()]);
	}

	/**
	 * Returns a flyweight relation.
	 * 
	 * @param pEncoding
	 *            The full encoding of the relation, prefixed in the case of a
	 *            transposed relation.
	 * @return The unique relation object corresponding to pEncoding.
	 * @exception UnsupportedRelationException
	 *                if the encoding does not resolve to a known exception
	 */
	public static Relation getRelation(final String pEncoding)
			throws UnsupportedRelationException {
		String lCode = pEncoding;
		// if 
		if (lCode.startsWith(Relation.TRANSPOSE_CODE)) // 
		{
			lCode = pEncoding.substring(Relation.TRANSPOSE_CODE.length(),
					pEncoding.length());
			for (final Type relationType : Type.values())
				if (lCode.equals(relationType.getCode()))
					return Relation.tRelationMap.get(relationType); // return the transpose relation
		}
		else
			for (final Type lRelationType : Type.values())
				if (lCode.equals(lRelationType.getCode()))
					return Relation.aRelationMap.get(lRelationType);

		throw new UnsupportedRelationException("Code: " + lCode);
	}

	private final boolean aDirect;

	private final Type aId;

	/**
	 * Construct a relation by specifying the relation Id and whether it is a
	 * direct or transposed relation.
	 * 
	 * @param pId
	 *            The id code for the relation.
	 * @param pDirect
	 *            true for a direct relation, false for a transpose relation.
	 * @exception UnsupportedRelationException
	 *                if the id does not correspond to a recognized relation.
	 */
	private Relation(final Type pId, final boolean pDirect) {
		this.aId = pId;
		this.aDirect = pDirect;
	}

	/**
	 * @return A Description of this relation, in English.
	 */
	public String getDescription() {
		return this.aId.getDescription();
	}

	/**
	 * @return The direct relation corresponding to this relation, whether or
	 *         not the relation is transpose.
	 */
	public Relation getDirectRelation() {
		if (this.isDirect())
			return this;
		else
			return Relation.aRelationMap.get(this.aId);
	}

	/**
	 * @return The complete encoding for this relation, that is, the simple
	 *         code, prefixed with the transpose code in the case of a transpose
	 *         relation.
	 */
	public String getFullCode() {
		if (this.isDirect())
			return this.aId.getCode();
		else
			return Relation.TRANSPOSE_CODE + this.aId.getCode();
	}

	/**
	 * @return If this is a direct relation, returns the corresponding
	 *         transpose. If this is a transpose relation, returns the
	 *         corresponding direct relation.
	 */
	public Relation getInverseRelation() {
		if (this.isDirect())
			return Relation.tRelationMap.get(this.aId);
		else
			return this.getDirectRelation();
	}

	/**
	 * @return The name of the relation.
	 */
	public String getName() {
		if (this.isDirect())
			return this.aId.getDirectName();
		else
			return this.aId.getTransposeName();
	}

	/**
	 * Returns whether a relation as a specified domain category. A domain
	 * category indicates the categories of elements which can be in a valid
	 * domain for a relation.
	 * 
	 * @param pCategory
	 *            The category to test for.
	 * @return True if this relation can have elements of category pCategory in
	 *         its domain.
	 */
	public boolean hasDomainCategory(final Category pCategory) {
		boolean lReturn = false;
		if (pCategory == Category.CLASS) {
			if (this == DECLARES_METHOD || this == EXTENDS_CLASS
					|| this == EXTENDS_INTERFACES
					|| this == IMPLEMENTS_INTERFACE || this == INHERITS
					|| this == REFERENCES || this == T_CHECKS
					|| this == T_CREATES || this == T_EXTENDS_CLASS
					|| this == T_EXTENDS_INTERFACES
					|| this == T_HAS_PARAMETER_TYPES
					|| this == T_HAS_RETURN_TYPE
					|| this == T_IMPLEMENTS_INTERFACE || this == T_OF_TYPE
					|| this == T_USES || this == IDENTITY || this == T_IDENTITY
					|| this == TRANS_EXTENDS || this == TRANS_IMPLEMENTS
					|| this == T_TRANS_EXTENDS || this == T_TRANS_IMPLEMENTS
					|| this == T_REFERENCES)
				lReturn = true;
		}
		else if (pCategory == Category.METHOD) {
			if (this == ACCESSES || this == SETS || this == GETS
					|| this == CALLS || this == EXPLICITLY_CALLS
					|| this == CHECKS || this == CREATES
					|| this == HAS_PARAMETER_TYPES || this == HAS_RETURN_TYPE
					|| this == IMPLEMENTS_METHOD || this == OVERRIDES
					|| this == OVERLOADS || this == USES || this == REFERENCES
					|| this == T_EXPLICITLY_CALLS || this == T_DECLARES
					|| this == T_CALLS || this == T_IMPLEMENTS_METHOD
					|| this == T_INHERITS || this == T_OVERRIDES
					|| this == T_USES || this == IDENTITY || this == T_IDENTITY
					|| this == STATIC_CALLS || this == T_STATIC_CALLS
					|| this == T_REFERENCES)
				lReturn = true;
		}
		else if (pCategory == Category.FIELD)
			if (this == OF_TYPE || this == T_DECLARES || this == T_ACCESSES
					|| this == T_INHERITS || this == T_USES || this == IDENTITY
					|| this == T_IDENTITY || this == T_REFERENCES)
				lReturn = true;
		return lReturn;
	}

	/**
	 * @return Whether this relation implies pRelation. For example, CALLS
	 *         implies USES.
	 */
	public boolean implies(final Relation pRelation) {
		if (this.getDirectRelation() == Relation.CALLS
				|| this.getDirectRelation() == Relation.CREATES
				|| this.getDirectRelation() == Relation.ACCESSES)
			if (pRelation.getDirectRelation() == Relation.USES)
				return true;
		if (this.getDirectRelation() == Relation.IMPLEMENTS_INTERFACE
				&& pRelation.getDirectRelation() == Relation.TRANS_IMPLEMENTS)
			return true;
		if (this.getDirectRelation() == Relation.EXTENDS_CLASS
				&& pRelation.getDirectRelation() == Relation.TRANS_EXTENDS)
			return true;
		if (this.getDirectRelation() == Relation.T_ACCESSES
				|| this.getDirectRelation() == Relation.T_CALLS
				|| this.getDirectRelation() == Relation.T_CHECKS
				|| this.getDirectRelation() == Relation.T_CREATES
				|| this.getDirectRelation() == Relation.T_EXTENDS_CLASS
				|| this.getDirectRelation() == Relation.T_EXTENDS_INTERFACES
				|| this.getDirectRelation() == Relation.T_HAS_PARAMETER_TYPES
				|| this.getDirectRelation() == Relation.T_HAS_RETURN_TYPE
				|| this.getDirectRelation() == Relation.T_IMPLEMENTS_INTERFACE
				|| this.getDirectRelation() == Relation.T_OF_TYPE
				|| this.getDirectRelation() == Relation.T_STATIC_CALLS)
			if (pRelation.getDirectRelation() == Relation.REFERENCES)
				return true;
		return false;
	}

	/**
	 * @return True if this is a direct relation.
	 */
	public boolean isDirect() {
		return this.aDirect;
	}

	/**
	 * @return Whether the relation is a primitive relation.
	 */
	public boolean isPrimitive() {
		return this.aId.ordinal() <= 9;
	}

	/**
	 * @return Whether the relation is a union of two or more relations, or not.
	 */
	public boolean isUnion() {
		boolean lReturn = false;
		if (this.aId == Type.ID_ACCESSES || this.aId == Type.ID_CALLS
				|| this.aId == Type.ID_USES || this.aId == Type.ID_REFERENCES)
			lReturn = true;
		return lReturn;
	}

	/**
	 * @return The full code for this relation.
	 */
	//	public String toString() {
	//		return this.getFullCode();
	//	}
	private Type getType() {
		return this.aId;
	}

	/**
	 * @return
	 */
	public Element getXML() {
		Element ret = new Element(this.getClass().getSimpleName());
		ret.setAttribute(TYPE, this.toString());
		return ret;
	}

	public static Relation valueOf(Element elem) {
		Attribute typeAttribute = elem.getAttribute(TYPE);
		String typeString = typeAttribute.getValue();
		return valueOf(typeString);
	}

	/**
	 * @return
	 */
	public boolean isAdvisable() {
		switch (this) {
			case CALLS:
			case EXPLICITLY_CALLS:
			case GETS:
			case SETS:
			case STATIC_CALLS:
				return true;
			default:
				return false;
		}
	}
}