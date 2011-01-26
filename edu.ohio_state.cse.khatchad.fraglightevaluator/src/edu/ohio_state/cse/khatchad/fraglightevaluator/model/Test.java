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

	public class Project {
		private String name;
		private int version;

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
	}

	private static final String HEADER = "Benchmark#From version#To version#Number of pointcuts#Number of added shadows#Number of predictions#Analysis time (s)#Prediction time (s)";

	private Project projectI;

	private Project projectJ;

	private BiMap<String, String> oldPointcutKeyToNewPointcutKeyMap = HashBiMap
			.create();

	private double analysisTime;
	
	private double predictionTime;

	private int numberOfAddedShadows;

	private int numberOfPredictions;

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

	/**
	 * @param end
	 */
	public void setAnalysisTime(double analysisTime) {
		this.analysisTime = analysisTime;
	}

	/**
	 * @param size
	 */
	public void setNumberOfAddedShadows(int numberOfAddedShadows) {
		this.numberOfAddedShadows = numberOfAddedShadows;
	}

	public void write(CSVWriter writer) {
		String[] row = this.getRow();
		writer.writeNext(row);	
	}
	
	public static String[] getHeader() {
		return HEADER.split("#");
	}

	private String[] getRow() {
		List<Object> row = new ArrayList<Object>(HEADER.split("#").length);
		
		row.add(this.projectI.getBenchmarkName());
		row.add(this.projectI.getVersion());
		row.add(this.projectJ.getVersion());
		row.add(this.oldPointcutKeyToNewPointcutKeyMap.size());
		row.add(this.numberOfAddedShadows);
		row.add(this.numberOfPredictions);
		row.add(this.analysisTime);
		row.add(this.predictionTime);
		
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
	
				changePredictionProvider.processNewJoinPointShadow(
						addedShadow, predictionTimeCollector);
			} catch (JavaModelException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
	
		double predictionTimeEnd = Util.calculateTimeStatistics(
				predictionTimeStart, predictionTimeCollector);
		this.setPredictionTime(predictionTimeEnd);
		this.setNumberOfPredictions(changePredictionProvider
				.getPredictionSet().size());
		
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
}