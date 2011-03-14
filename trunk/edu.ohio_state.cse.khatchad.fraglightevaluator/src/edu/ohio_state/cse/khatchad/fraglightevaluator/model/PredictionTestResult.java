/**
 * 
 */
package edu.ohio_state.cse.khatchad.fraglightevaluator.model;

import static edu.ohio_state.cse.khatchad.fraglightevaluator.util.DatabaseUtil.getKey;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.eclipse.ajdt.core.javaelements.AdviceElement;
import org.eclipse.jdt.core.IJavaElement;

import ca.mcgill.cs.swevo.jayfx.model.IElement;

import au.com.bytecode.opencsv.CSVWriter;

import edu.ohio_state.cse.khatchad.fraglight.core.graph.IntentionArc;
import edu.ohio_state.cse.khatchad.fraglight.core.graph.Pattern;
import edu.ohio_state.cse.khatchad.fraglight.core.util.Util;
import edu.ohio_state.cse.khatchad.fraglight.ui.Prediction;
import edu.ohio_state.cse.khatchad.fraglight.ui.Prediction.ChangeDirection;
import edu.ohio_state.cse.khatchad.fraglight.ui.Prediction.InterestDirection;
import edu.ohio_state.cse.khatchad.fraglightevaluator.model.Test.Project;
import edu.ohio_state.cse.khatchad.fraglightevaluator.util.DatabaseUtil;

/**
 * @author <a href="mailto:khatchad@cse.ohio-state.edu">Raffi Khatchadourian</a>
 * 
 */
public class PredictionTestResult {

	private static final String PREDICTION_HEADER = "Benchmark#From version#To version#Added joint point shadow#Pointcut that is predicted to change as a direct result#Predicted direction#DOI Manipulation#Original DOI value#New DOI value#Change confidene";
	
	private static final String PATTERN_HEADER = PREDICTION_HEADER + "#Contibuting pattern#Pattern simularity";

	private String benchmarkName;

	private int fromVersion;

	private int toVersion;

	private IJavaElement addedJoinPointShadow;

	private AdviceElement predictedPointcut;
	
	private ChangeDirection changeDirection;
	
	private InterestDirection interestDirection;
	
	private double originalInterestLevel;
	
	private double newInterestLevel;
	
	private double changeConfidence;

	private Set<Pattern<IntentionArc<IElement>>> contributingPatterns;
	
	private String[] getPredictionRow() {
		List<Object> row = new ArrayList<Object>(PREDICTION_HEADER.split("#").length);
		
		row.add(this.benchmarkName);
		row.add(this.fromVersion);
		row.add(this.toVersion);
		row.add(getKey(this.addedJoinPointShadow));
		row.add(getKey(this.predictedPointcut));
		row.add(this.changeDirection);
		row.add(this.interestDirection);
		row.add(this.originalInterestLevel);
		row.add(this.newInterestLevel);
		row.add(this.changeConfidence);
		
		return Util.toStringArray(row);
	}

	public PredictionTestResult(Test test, Prediction prediction) {
		String benchmarkNameI = test.getProjectI().getBenchmarkName();
		String benchmarkNameJ = test.getProjectJ().getBenchmarkName();

		// sanity check
		if (!benchmarkNameI.equals(benchmarkNameJ))
			throw new IllegalArgumentException("Test " + test
					+ " has inconsistent projects.");
		
		this.benchmarkName = benchmarkNameI;
		this.fromVersion = test.getProjectI().getVersion();
		this.toVersion = test.getProjectJ().getVersion();

		this.addedJoinPointShadow = prediction.getAffectingJoinPoint();
		this.predictedPointcut = prediction.getAdvice();
		this.changeDirection = prediction.getChangeDirection();
		this.changeConfidence = prediction.getChangeConfidence();
		this.interestDirection = prediction.getInterestDirection();
		this.originalInterestLevel = prediction.getOriginalInterestLevel();
		this.newInterestLevel = prediction.getNewInterestLevel();
		this.contributingPatterns = prediction.getContributingPatterns();
	}

	public static String[] getPredictionHeader() {
		return PREDICTION_HEADER.split("#");
	}
	
	public static String[] getPatternHeader() {
		return PATTERN_HEADER.split("#");
	}
	
	public void write(CSVWriter predictionWriter, CSVWriter contributingPatternWriter) {
		String[] predictionRow = this.getPredictionRow();
		predictionWriter.writeNext(predictionRow);
		
		List<String[]> contributingPatternRows = this.getContributingPatternRows();
		contributingPatternWriter.writeAll(contributingPatternRows);
	}

	private List<String[]> getContributingPatternRows() {
		List<String[]> ret = new ArrayList<String[]>();
		
		for ( Pattern<IntentionArc<IElement>> pattern : this.contributingPatterns) 
			ret.add(this.getContributingPatternRow(pattern));
		
		return ret;
	}

	/**
	 * @param pattern
	 * @return
	 */
	private String[] getContributingPatternRow(
			Pattern<IntentionArc<IElement>> pattern) {
			
		List<Object> row = new ArrayList<Object>(PATTERN_HEADER.split("#").length);
		
		row.addAll(Arrays.asList(this.getPredictionRow()));
		
		row.add(pattern);
		row.add(pattern.getSimularity());
		
		return Util.toStringArray(row);
	}
}