/**
 * 
 */
package edu.ohio_state.cse.khatchad.fraglight.core.graph;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.Serializable;

import org.eclipse.jdt.core.IJavaElement;
import org.jdom.Attribute;
import org.jdom.DataConversionException;
import org.jdom.Element;

import ca.mcgill.cs.swevo.jayfx.ConversionException;
import ca.mcgill.cs.swevo.jayfx.FastConverter;
import ca.mcgill.cs.swevo.jayfx.JayFX;

/**
 * @author raffi
 * 
 * @param <E>
 */
public abstract class GraphElement<E> implements Serializable {

	private static final long serialVersionUID = 1905353972018475367L;

	private static final String ENABLED = "enabled";

	private final PropertyChangeSupport changes = new PropertyChangeSupport(
			this);

	private boolean enabled;

	/**
	 * 
	 */
	public GraphElement() {
		super();
	}

	public void addPropertyChangeListener(final PropertyChangeListener l) {
		this.changes.addPropertyChangeListener(l);
	}

	public void disable() {
		final boolean oldState = this.enabled;
		this.enabled = false;
		if (oldState != this.enabled)
			this.changes.firePropertyChange(new PropertyChangeEvent(this,
					ENABLED, oldState, this.enabled));
	}

	/**
	 * @param enabled
	 *            the enabled to set
	 */
	public void enable() {
		final boolean oldState = this.enabled;
		this.enabled = true;
		if (oldState != this.enabled)
			this.changes.firePropertyChange(new PropertyChangeEvent(this,
					ENABLED, oldState, this.enabled));
	}

	/**
	 * @return the enabled
	 */
	public boolean isEnabled() {
		return this.enabled;
	}

	public void removePropertyChangeListener(final PropertyChangeListener l) {
		this.changes.removePropertyChangeListener(l);
	}

	@Override
	public String toString() {
		return this.enabled ? "*" : "";
	}

	/**
	 * @return
	 */
	public Element getXML() {
		Element ret = new Element(this.getClass().getSimpleName());
		ret.setAttribute(ENABLED, String.valueOf(this.isEnabled()));
		return ret;
	}

	public static boolean isEnabled(Element elem)
			throws DataConversionException {
		Attribute enabledAttribute = elem.getAttribute(ENABLED);
		return enabledAttribute.getBooleanValue();
	}

	public GraphElement(Element elem) throws DataConversionException {
		this.enabled = isEnabled(elem);
	}

	/**
	 * @return
	 */
	public abstract String getLongDescription();

	/**
	 * @return
	 */
	public abstract String toPrettyString();

	/**
	 * Converts this IntentionElement into its corresponding IJavaElement.
	 * @param fastConverter 
	 * @return The IJavaElement representing this IntentionElement.
	 * @throws ConversionException 
	 */
	public abstract IJavaElement toJavaElement(JayFX database) throws ConversionException; 
	
	public abstract boolean isAdvisable();
}