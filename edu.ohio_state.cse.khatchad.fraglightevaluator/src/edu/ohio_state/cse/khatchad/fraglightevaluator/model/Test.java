/**
 * 
 */
package edu.ohio_state.cse.khatchad.fraglightevaluator.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.eclipse.ajdt.core.javaelements.AdviceElement;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.jdom.Attribute;
import org.jdom.DataConversionException;
import org.jdom.Element;

import au.com.bytecode.opencsv.CSVWriter;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import edu.ohio_state.cse.khatchad.fraglight.core.analysis.util.TimeCollector;
import edu.ohio_state.cse.khatchad.fraglight.core.util.Util;
import edu.ohio_state.cse.khatchad.fraglight.ui.PointcutChangePredictionProvider;
import edu.ohio_state.cse.khatchad.fraglight.ui.PredictionSet;
import edu.ohio_state.cse.khatchad.fraglightevaluator.analysis.EvaluationPointcutChangePredictionProvider;

/**
 * @author <a href="mailto:khatchad@cse.ohio-state.edu">Raffi Khatchadourian</a>
 * 
 */
public class Test {

	public static class Project {

		private static final String SHADOWS_HEADER = "Benchmark#Version#Shadow";

		private static final String ADVICE_HEADER = "Benchmark#Version#Advice";

		private String name;

		private int version;

		private Set<IJavaElement> shadows;

		private Collection<AdviceElement> advice;

		public void setName(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public String getBenchmarkName() {
			String projectName = this.getName();
			String benchmarkName = projectName.substring(0,
					projectName.indexOf('_'));
			return benchmarkName;
		}

		public void setVersion(int version) {
			this.version = version;
		}

		public int getVersion() {
			return version;
		}

		@Override
		public String toString() {
			return this.getBenchmarkName() + ",v" + this.getVersion();
		}

		public void setAdvice(Collection<AdviceElement> oldPointcuts) {
			this.advice = oldPointcuts;
		}

		public Collection<AdviceElement> getAdvice() {
			return advice;
		}

		public void setShadows(Set<IJavaElement> shadows) {
			this.shadows = shadows;
		}

		public Set<IJavaElement> getShadows() {
			return shadows;
		}

		public static String[] getShadowsHeader() {
			return SHADOWS_HEADER.split("#");
		}

		public static String[] getAdviceHeader() {
			return ADVICE_HEADER.split("#");
		}

		private String[] getShadowRow(IJavaElement shadow) {
			List<Object> row = new ArrayList<Object>(
					SHADOWS_HEADER.split("#").length);

			row.add(this.getBenchmarkName());
			row.add(this.getVersion());
			row.add(Util.getKey(shadow));

			return Util.toStringArray(row);
		}

		private String[] getAdviceRow(IJavaElement advice) {
			List<Object> row = new ArrayList<Object>(
					ADVICE_HEADER.split("#").length);

			row.add(this.getBenchmarkName());
			row.add(this.getVersion());
			row.add(Util.getKey(advice));

			return Util.toStringArray(row);
		}
		
		public void write(CSVWriter shadowsWriter, CSVWriter adviceWriter) {
			for ( IJavaElement shadow : this.shadows ) {
				String[] shadowRow = this.getShadowRow(shadow);
				shadowsWriter.writeNext(shadowRow);
			}
			
			for ( IJavaElement shadow : this.advice) {
				String[] adviceRow = this.getAdviceRow(shadow);
				adviceWriter.writeNext(adviceRow);
			}
		}
	}

	private static final String TEST_HEADER = "Benchmark#From version#To version#Number of pointcuts#Number of added shadows#Number of predictions#Analysis time (s)#Prediction time (s)";

	private static final String ADDED_SHADOWS_HEADER = "Benchmark#From version#To version#Added shadow";

	private Project projectI;

	private Project projectJ;

	private BiMap<String, String> oldPointcutKeyToNewPointcutKeyMap = HashBiMap
			.create();

	private double analysisTime;

	private double predictionTime;

	private int numberOfPredictions;

	private Collection<IJavaElement> addedShadowCol;

	/**
	 * @param testElem
	 * @throws DataConversionException
	 */
	public Test(Element testElem) throws DataConversionException {
		@SuppressWarnings("unchecked")
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

			if (oldAttribute == null)
				throw new IllegalStateException("Old element is missing for "
						+ mapEntry);

			String oldKey = oldAttribute.getValue();

			Attribute newAttribute = mapEntry.getAttribute("new");

			if (newAttribute == null)
				throw new IllegalStateException("New element is missing for "
						+ mapEntry);

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

	public void setAnalysisTime(double analysisTime) {
		this.analysisTime = analysisTime;
	}

	public void write(CSVWriter testWriter, CSVWriter addedShadowsWriter) {
		String[] testRow = this.getTestRow();
		testWriter.writeNext(testRow);

		for (IJavaElement addedShadow : this.addedShadowCol) {
			String[] addedShadowRow = this.getAddedShadowRow(addedShadow);
			addedShadowsWriter.writeNext(addedShadowRow);
		}
	}

	public static String[] getTestHeader() {
		return TEST_HEADER.split("#");
	}

	public static String[] getAddedShadowsHeader() {
		return ADDED_SHADOWS_HEADER.split("#");
	}

	private String[] getTestRow() {
		List<Object> row = new ArrayList<Object>(TEST_HEADER.split("#").length);

		row.add(this.projectI.getBenchmarkName());
		row.add(this.projectI.getVersion());
		row.add(this.projectJ.getVersion());
		row.add(this.oldPointcutKeyToNewPointcutKeyMap.size());
		row.add(this.addedShadowCol.size());
		row.add(this.numberOfPredictions);
		row.add(this.analysisTime);
		row.add(this.predictionTime);

		return Util.toStringArray(row);
	}

	private String[] getAddedShadowRow(IJavaElement addedShadow) {
		List<Object> row = new ArrayList<Object>(
				ADDED_SHADOWS_HEADER.split("#").length);

		row.add(this.projectI.getBenchmarkName());
		row.add(this.projectI.getVersion());
		row.add(this.projectJ.getVersion());
		row.add(Util.getKey(addedShadow));

		return Util.toStringArray(row);
	}

	public void setPredictionTime(double predictionTime) {
		this.predictionTime = predictionTime;
	}

	public void setNumberOfPredictions(int numberOfPredictions) {
		this.numberOfPredictions = numberOfPredictions;
	}

	public PredictionSet run(
			PointcutChangePredictionProvider changePredictionProvider,
			Set<IJavaElement> addedShadowCol) {
		TimeCollector predictionTimeCollector = new TimeCollector();
		final long predictionTimeStart = System.currentTimeMillis();

		for (IJavaElement addedShadow : addedShadowCol)
			try {
				changePredictionProvider.processNewJoinPointShadow(addedShadow,
						predictionTimeCollector);
			} catch (JavaModelException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}

		double predictionTimeEnd = Util.calculateTimeStatistics(
				predictionTimeStart, predictionTimeCollector);
		this.setPredictionTime(predictionTimeEnd);
		this.setNumberOfPredictions(changePredictionProvider.getPredictionSet()
				.size());

		return changePredictionProvider.getPredictionSet();
	}

	public PointcutChangePredictionProvider createPointcutChangePredictionProvider(
			Collection<AdviceElement> oldPointcuts,
			BiMap<AdviceElement, AdviceElement> oldPointcutToNewPointcutMap) {
		TimeCollector analysisTimeCollector = new TimeCollector();
		final long analysisTimeStart = System.currentTimeMillis();

		PointcutChangePredictionProvider changePredictionProvider = new EvaluationPointcutChangePredictionProvider(
				oldPointcutToNewPointcutMap);
		changePredictionProvider.analyzePointcuts(oldPointcuts,
				analysisTimeCollector);

		double analysisTimeEnd = Util.calculateTimeStatistics(
				analysisTimeStart, analysisTimeCollector);
		this.setAnalysisTime(analysisTimeEnd);
		return changePredictionProvider;
	}

	@Override
	public String toString() {
		return "[" + this.projectI + " -> " + this.projectJ + "]";
	}

	public void addToPredictionTime(double value) {
		this.predictionTime += value;
	}

	public void setAddedShadowCol(Collection<IJavaElement> addedShadowCol) {
		this.addedShadowCol = addedShadowCol;
	}

	public Collection<IJavaElement> getAddedShadowCol() {
		return addedShadowCol;
	}
}