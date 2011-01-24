/**
 * 
 */
package edu.ohio_state.cse.khatchad.fraglightevaluator.model;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.ajdt.core.javaelements.AdviceElement;
import org.eclipse.jdt.core.IJavaElement;

import au.com.bytecode.opencsv.CSVWriter;

import edu.ohio_state.cse.khatchad.fraglight.ui.PointcutChangePredictionProvider.Prediction;
import edu.ohio_state.cse.khatchad.fraglight.ui.PointcutChangePredictionProvider.Prediction.ChangeDirection;
import edu.ohio_state.cse.khatchad.fraglightevaluator.model.Test.Project;
import edu.ohio_state.cse.khatchad.fraglightevaluator.util.DatabaseUtil;

/**
 * @author <a href="mailto:khatchad@cse.ohio-state.edu">Raffi Khatchadourian</a>
 * 
 */
public class PredictionTestResult {

	public static final String HEADER = "Benchmark#From version#To version#Added joint point shadow#Pointcut that is predicted to change as a direct result#Predicted direction#Change confidene";

	private String benchmarkName;

	private int fromVersion;

	private int toVersion;

	private IJavaElement addedJoinPointShadow;

	private AdviceElement predictedPointcut;
	
	private ChangeDirection changeDirection;
	
	private double changeConfidence;
	
	private String[] getRow() {
		List<String> ret = new ArrayList<String>(HEADER.split("#").length);
		
		ret.add(this.benchmarkName);
		ret.add(String.valueOf(this.fromVersion));
		ret.add(String.valueOf(this.toVersion));
		ret.add(DatabaseUtil.getKey(this.addedJoinPointShadow));
		ret.add(DatabaseUtil.getKey(this.predictedPointcut));
		ret.add(this.changeDirection.toString());
		ret.add(String.valueOf(this.changeConfidence));
		
		return ret.toArray(new String[0]);
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
	}

	public static String[] getHeader() {
		return HEADER.split("#");
	}

	
	public void write(CSVWriter writer) {
		String[] row = this.getRow();
		writer.writeNext(row);
	}
}