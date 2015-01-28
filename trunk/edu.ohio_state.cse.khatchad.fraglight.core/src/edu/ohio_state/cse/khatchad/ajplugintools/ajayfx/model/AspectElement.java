/**
 * 
 */
package edu.ohio_state.cse.khatchad.ajplugintools.ajayfx.model;

import java.util.HashSet;
import java.util.Set;

import org.jdom.Attribute;
import org.jdom.Element;

import ca.mcgill.cs.swevo.jayfx.model.AbstractElement;
import ca.mcgill.cs.swevo.jayfx.model.ClassElement;
import ca.mcgill.cs.swevo.jayfx.model.Category;
import ca.mcgill.cs.swevo.jayfx.model.IElement;
import ca.mcgill.cs.swevo.jayfx.model.Relation;

/**
 * @author raffi
 * 
 */
public class AspectElement extends AbstractElement {

	private static final long serialVersionUID = 6987988981321889202L;

	/**
	 * @param id
	 */
	public AspectElement(final String id) {
		super(id);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof AspectElement))
			return false;
		else
			return this.getId().equals(((AspectElement) obj).getId());
	}

	/* (non-Javadoc)
	 * @see ca.mcgill.cs.swevo.jayfx.model.AbstractElement#getCategory()
	 */
	@Override
	public Category getCategory() {
		return Category.ASPECT;
	}

	/* (non-Javadoc)
	 * @see ca.mcgill.cs.swevo.jayfx.model.IElement#getDeclaringClass()
	 */
	public ClassElement getDeclaringClass() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see ca.mcgill.cs.swevo.jayfx.model.IElement#getPackageName()
	 */
	public String getPackageName() {
		final int lIndex = this.getId().lastIndexOf(".");
		if (lIndex >= 0)
			return this.getId().substring(0, this.getId().lastIndexOf("."));
		else
			return "";
	}

	/* (non-Javadoc)
	 * @see ca.mcgill.cs.swevo.jayfx.model.AbstractElement#getShortName()
	 */
	@Override
	public String getShortName() {
		final String lPackageName = this.getPackageName();
		if (lPackageName.length() > 0)
			return this.getId().substring(lPackageName.length() + 1,
					this.getId().length());
		else
			return this.getId();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return this.getId().hashCode();
	}
}