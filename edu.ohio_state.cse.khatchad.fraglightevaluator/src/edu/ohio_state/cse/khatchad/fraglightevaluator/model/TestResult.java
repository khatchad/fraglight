/**
 * 
 */
package edu.ohio_state.cse.khatchad.fraglightevaluator.model;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.ajdt.core.javaelements.AdviceElement;
import org.eclipse.jdt.core.IJavaElement;

import edu.ohio_state.cse.khatchad.fraglight.ui.PointcutChangePredictionProvider.Prediction;
import edu.ohio_state.cse.khatchad.fraglight.ui.PointcutChangePredictionProvider.Prediction.ChangeDirection;
import edu.ohio_state.cse.khatchad.fraglightevaluator.model.Test.Project;
import edu.ohio_state.cse.khatchad.fraglightevaluator.util.DatabaseUtil;

/**
 * @author <a href="mailto:khatchad@cse.ohio-state.edu">Raffi Khatchadourian</a>
 * 
 */
public class TestResult {

	public static final String HEADER = "Benchmark#From version#To version#Added joint point shadow#Pointcut that is predicted to change as a direct result#Predicted direction#Change confidene#Time";

	private String benchmarkName;

	private int fromVersion;

	private int toVersion;

	private IJavaElement addedJoinPointShadow;

	private AdviceElement predictedPointcut;
	
	private ChangeDirection changeDirection;
	
	private double changeConfidence;
	
	private double time = -1;
	
	public String[] getRow() {
		List<String> ret = new ArrayList<String>(HEADER.split("#").length);
		
		ret.add(this.benchmarkName);
		ret.add(String.valueOf(this.fromVersion));
		ret.add(String.valueOf(this.toVersion));
		ret.add(DatabaseUtil.getKey(this.addedJoinPointShadow));
		ret.add(DatabaseUtil.getKey(this.predictedPointcut));
		ret.add(this.changeDirection.toString());
		ret.add(String.valueOf(this.changeConfidence));
		ret.add(String.valueOf(time));
		
		return ret.toArray(new String[0]);
	}

	public TestResult(Test test, Prediction prediction) {
		String benchmarkNameI = getBenchmarkName(test.getProjectI());
		String benchmarkNameJ = getBenchmarkName(test.getProjectJ());

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

	private static String getBenchmarkName(Project project) {
		String projectName = project.getName();
		String benchmarkName = projectName.substring(0,
				projectName.indexOf('_'));
		return benchmarkName;
	}

	public static String[] getHeader() {
		return HEADER.split("#");
	}
}
