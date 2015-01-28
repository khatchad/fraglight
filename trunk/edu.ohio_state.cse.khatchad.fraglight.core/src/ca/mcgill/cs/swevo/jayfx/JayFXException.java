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
 */
public class JayFXException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = -3835440373464293435L;

	/**
	 * 
	 */
	public JayFXException() {
	}

	/**
	 * @param arg0
	 */
	public JayFXException(final String arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param arg0
	 * @param arg1
	 */
	public JayFXException(final String arg0, final Throwable arg1) {
		super(arg0, arg1);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param arg0
	 */
	public JayFXException(final Throwable arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}

}
