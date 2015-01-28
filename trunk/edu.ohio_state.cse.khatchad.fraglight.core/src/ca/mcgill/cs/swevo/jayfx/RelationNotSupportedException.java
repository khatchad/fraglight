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

public class RelationNotSupportedException extends RuntimeException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6904782827174823573L;

	public RelationNotSupportedException(final String pMessage) {
		super(pMessage);
	}

	public RelationNotSupportedException(final String pMessage,
			final Throwable pException) {
		super(pMessage, pException);
	}

	public RelationNotSupportedException(final Throwable pException) {
		super(pException);
	}
}
