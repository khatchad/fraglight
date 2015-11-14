/**
 * 
 */
package edu.ohio_state.cse.khatchad.fraglight.core.analysis.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.eclipse.core.resources.ResourcesPlugin;

/**
 * @author raffi
 *
 */
public class FileUtil {
	/**
	 * Where to store benchmark results.
	 */
	public static final File WORKSPACE_LOC = ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile();

	private FileUtil() {
	}

	/**
	 * @return
	 * @throws IOException
	 */
	public static PrintWriter getPrintWriter(final File aFile, final boolean append) throws IOException {
		final FileWriter resFileOut = new FileWriter(aFile, append);
		PrintWriter ret = new PrintWriter(resFileOut);
		return ret;
	}
}
