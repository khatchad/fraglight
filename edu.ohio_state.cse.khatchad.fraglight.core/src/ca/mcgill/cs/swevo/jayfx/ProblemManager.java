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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.Bundle;

/**
 * A general facade for common behavior related to dealing with exceptions in
 * JayFX.
 */
class ProblemManager {
	/**
	 * Silently reports an exception by logging it.
	 * 
	 * @param pException
	 *            The exception to report.
	 */
	public static void reportException(final Exception pException) {
		final Bundle lJayFX = Platform.getBundle("ca.mcgill.cs.swevo.jayfx");
		// assert( lJayFX != null );
		Platform.getLog(lJayFX).log(new Status(IStatus.ERROR, "ca.mcgill.cs.swevo.jayfx", IStatus.OK,
				pException.getClass().getName(), pException));
	}

	private ProblemManager() {

	}
}
