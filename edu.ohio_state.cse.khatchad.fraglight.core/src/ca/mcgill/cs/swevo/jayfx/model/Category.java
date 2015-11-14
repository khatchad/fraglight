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

import org.jdom.Attribute;
import org.jdom.Element;

/**
 * The various categories of elements supported by the Concern Graph model for
 * the Java language.
 */
public enum Category {
	CLASS, FIELD, METHOD, PACKAGE, ASPECT, ADVICE, AJCODE, WILDCARD;

	private static final String TYPE = "type";

	public Element getXML() {
		Element ret = new Element(this.getClass().getSimpleName());
		ret.setAttribute(TYPE, this.toString());
		return ret;
	}

	public static Category valueOf(Element elem) {
		Attribute typeAttribute = elem.getAttribute(TYPE);
		return valueOf(typeAttribute.getValue());
	}
}
