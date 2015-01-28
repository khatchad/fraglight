/**
 * 
 */
package edu.ohio_state.cse.khatchad.fraglight.core.graph;

import org.eclipse.jdt.core.IJavaElement;
import org.jdom.Attribute;
import org.jdom.Element;

import ca.mcgill.cs.swevo.jayfx.model.ClassElement;
import ca.mcgill.cs.swevo.jayfx.model.Category;
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

	public Category getCategory() {
		return Category.WILDCARD;
	}

	/* (non-Javadoc)
	 * @see ca.mcgill.cs.swevo.jayfx.model.IElement#getDeclaringClass()
	 */
	public ClassElement getDeclaringClass() {
		return null;
	}

	/* (non-Javadoc)
	 * @see ca.mcgill.cs.swevo.jayfx.model.IElement#getId()
	 */
	public String getId() {
		return QUESTION_MARK;
	}

	public static boolean isWildcardIdentifier(String identifier) {
		return identifier.equals(QUESTION_MARK);
	}

	/* (non-Javadoc)
	 * @see ca.mcgill.cs.swevo.jayfx.model.IElement#getPackageName()
	 */
	public String getPackageName() {
		return "";
	}

	/* (non-Javadoc)
	 * @see ca.mcgill.cs.swevo.jayfx.model.IElement#getShortName()
	 */
	public String getShortName() {
		return QUESTION_MARK;
	}

	/* (non-Javadoc)
	 * @see ca.mcgill.cs.swevo.jayfx.model.IElement#hasEnabledRelationFor(ca.mcgill.cs.swevo.jayfx.model.Relation)
	 */
	public boolean hasEnabledRelationFor(final Relation relation) {
		return false;
	}

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