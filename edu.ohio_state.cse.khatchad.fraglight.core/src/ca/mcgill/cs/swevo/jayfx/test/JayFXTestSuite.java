/* JayFX - A Fact Extractor Plug-in for Eclipse
 * Copyright (C) 2006  McGill University (http://www.cs.mcgill.ca/~swevo/jayfx)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * $Revision: 1.2 $
 */

package ca.mcgill.cs.swevo.jayfx.test;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests the WordQuizz application
 */
public class JayFXTestSuite extends TestSuite {
	public static Test suite() {
		final TestSuite lSuite = new TestSuite("AnalyzePointcutPlugin suite for JayFX");
		lSuite.addTestSuite(TestProgramDatabase.class);
		lSuite.addTestSuite(TestRelation.class);
		return lSuite;
	}
}
