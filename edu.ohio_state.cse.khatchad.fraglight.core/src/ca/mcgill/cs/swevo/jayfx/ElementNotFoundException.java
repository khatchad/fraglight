/* JayFX - A Fact Extractor Plug-in for Eclipse
 * Copyright (C) 2006  McGill University (http://www.cs.mcgill.ca/~swevo/jayfx)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * $Revision: 1.3 $
 */

package ca.mcgill.cs.swevo.jayfx;

/**
 * Element never been added to the database.
 */
public class ElementNotFoundException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3846765061551720103L;

	/**
	 * 
	 */
	public ElementNotFoundException() {
		super();
	}

	/**
	 * @param arg0
	 */
	public ElementNotFoundException(final String arg0) {
		super(arg0);

	}

	/**
	 * @param arg0
	 * @param arg1
	 */
	public ElementNotFoundException(final String arg0, final Throwable arg1) {
		super(arg0, arg1);

	}

	/**
	 * @param arg0
	 */
	public ElementNotFoundException(final Throwable arg0) {
		super(arg0);

	}

}
