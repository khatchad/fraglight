/**
 * 
 */
package edu.ohio_state.cse.khatchad.fraglight.core.graph;

import org.jdom2.Attribute;
import org.jdom2.Element;

import ca.mcgill.cs.swevo.jayfx.model.Category;
import ca.mcgill.cs.swevo.jayfx.model.ClassElement;
import ca.mcgill.cs.swevo.jayfx.model.IElement;
import ca.mcgill.cs.swevo.jayfx.model.Relation;

/**
 * @author raffi
 * 
 */
public class WildcardElement implements IElement {

	/**
	 * 
	 */
	private static final String QUESTION_MARK = "?";

	private static final long serialVersionUID = -4175380054692252185L;

	public WildcardElement() {
	}

	@Override
	public Category getCategory() {
		return Category.WILDCARD;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ca.mcgill.cs.swevo.jayfx.model.IElement#getDeclaringClass()
	 */
	@Override
	public ClassElement getDeclaringClass() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ca.mcgill.cs.swevo.jayfx.model.IElement#getId()
	 */
	@Override
	public String getId() {
		return QUESTION_MARK;
	}

	public static boolean isWildcardIdentifier(String identifier) {
		return identifier.equals(QUESTION_MARK);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ca.mcgill.cs.swevo.jayfx.model.IElement#getPackageName()
	 */
	@Override
	public String getPackageName() {
		return "";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ca.mcgill.cs.swevo.jayfx.model.IElement#getShortName()
	 */
	@Override
	public String getShortName() {
		return QUESTION_MARK;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ca.mcgill.cs.swevo.jayfx.model.IElement#hasEnabledRelationFor(ca.mcgill.
	 * cs.swevo.jayfx.model.Relation)
	 */
	public boolean hasEnabledRelationFor(final Relation relation) {
		return false;
	}

	@Override
	public Element getXML() {
		Element ret = new Element(IElement.class.getSimpleName());
		ret.setAttribute(new Attribute(ID, this.getId()));
		ret.addContent(this.getCategory().getXML());
		return ret;
	}

	/**
	 * @param elementXML
	 * @return
	 */
	public static boolean isWildcardElement(Element elementXML) {
		return elementXML.getAttribute(ID).getValue().equals(QUESTION_MARK);
	}
}