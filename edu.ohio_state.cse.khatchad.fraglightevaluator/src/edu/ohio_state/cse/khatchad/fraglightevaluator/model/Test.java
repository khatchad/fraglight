/**
 * 
 */
package edu.ohio_state.cse.khatchad.fraglightevaluator.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.core.IJavaProject;
import org.jdom.Attribute;
import org.jdom.DataConversionException;
import org.jdom.Element;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

/**
 * @author <a href="mailto:khatchad@cse.ohio-state.edu">Raffi Khatchadourian</a>
 * 
 */
public class Test {

	public class Project {
		private String name;
		private int version;

		public void setName(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setVersion(int version) {
			this.version = version;
		}

		public int getVersion() {
			return version;
		}
	}

	private Project projectI;

	private Project projectJ;

	BiMap<String, String> oldPointcutKeyToNewPointcutKeyMap = HashBiMap
			.create();

	/**
	 * @param testElem
	 * @throws DataConversionException
	 */
	public Test(Element testElem) throws DataConversionException {
		List<Element> projectElemCol = testElem.getChildren("project");

		List<Project> projectList = new ArrayList<Project>(2);

		for (Element projectElem : projectElemCol) {

			Project project = new Project();

			Attribute nameAttribute = projectElem.getAttribute("name");
			String projectName = nameAttribute.getValue();
			project.setName(projectName);

			Attribute versionAttribute = projectElem.getAttribute("version");
			int projectVersion = versionAttribute.getIntValue();
			project.setVersion(projectVersion);

			projectList.add(project);
		}

		if (projectList.size() != 2)
			throw new IllegalStateException(
					"Can't have more than two projects in a single test.");

		if (projectList.get(0).getVersion() < projectList.get(1).getVersion()) {
			projectI = projectList.get(0);
			projectJ = projectList.get(1);
		} else {
			projectI = projectList.get(1);
			projectJ = projectList.get(0);
		}

		Element pointcutMapElem = (Element) testElem.getChild("pointcutmap");

		for (Object obj : pointcutMapElem.getChildren("mapentry")) {
			Element mapEntry = (Element) obj;

			Attribute oldAttribute = mapEntry.getAttribute("old");
			String oldKey = oldAttribute.getValue();

			Attribute newAttribute = mapEntry.getAttribute("new");
			String newKey = newAttribute.getValue();

			oldPointcutKeyToNewPointcutKeyMap.put(oldKey, newKey);
		}
	}

	public Project getProjectI() {
		return projectI;
	}

	public Project getProjectJ() {
		return projectJ;
	}

	public BiMap<String, String> getOldPointcutKeyToNewPointcutKeyMap() {
		return oldPointcutKeyToNewPointcutKeyMap;
	}
}