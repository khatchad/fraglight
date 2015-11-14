/**
 * 
 */
package edu.ohio_state.cse.khatchad.fraglight.core.analysis.util;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import org.eclipse.ajdt.core.javaelements.AdviceElement;
import org.eclipse.jdt.core.IJavaElement;
import org.jdom2.Attribute;
import org.jdom2.Element;

/**
 * @author raffi
 *
 */
public class XMLUtil {
	private XMLUtil() {
	}

	/**
	 * @param advElem
	 * @return
	 */
	@SuppressWarnings("restriction")
	public static File getSavedXMLFile(AdviceElement advElem) {
		String relativeFileName = getRelativeXMLFileName(advElem);
		File aFile = new File(FileUtil.WORKSPACE_LOC, relativeFileName);
		if (!aFile.exists())
			throw new IllegalArgumentException("No XML file found for advice " + advElem.getElementName());
		return aFile;
	}

	/**
	 * @param advElem
	 * @return
	 */
	@SuppressWarnings("restriction")
	public static String getRelativeXMLFileName(AdviceElement advElem) {
		StringBuilder fileNameBuilder = new StringBuilder(advElem.getPath().toOSString());
		fileNameBuilder.append("#" + advElem.toDebugString());
		fileNameBuilder.append(".rejuv-pc.xml");
		return fileNameBuilder.toString();
	}

	public static PrintWriter getXMLFileWriter(AdviceElement advElem) throws IOException {
		String fileName = getRelativeXMLFileName(advElem);
		final File aFile = new File(FileUtil.WORKSPACE_LOC, fileName);
		PrintWriter ret = FileUtil.getPrintWriter(aFile, false);
		return ret;
	}

	/**
	 * @param elem
	 * @return
	 */
	public static Element getXML(IJavaElement elem) {
		Element ret = new Element(elem.getClass().getSimpleName());
		String handleIdentifier = null;
		try {
			handleIdentifier = elem.getHandleIdentifier();
		} catch (NullPointerException e) {
			System.err.println("Can't retrieve element handler for: " + elem);
			System.exit(-1);
		}
		ret.setAttribute(new Attribute("id", handleIdentifier));
		ret.setAttribute(new Attribute("name", elem.getElementName()));
		ret.setAttribute(new Attribute("type", String.valueOf(elem.getElementType())));
		return ret;
	}
}
