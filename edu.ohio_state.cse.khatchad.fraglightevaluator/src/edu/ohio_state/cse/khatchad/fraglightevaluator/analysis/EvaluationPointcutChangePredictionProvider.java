/**
 * 
 */
package edu.ohio_state.cse.khatchad.fraglightevaluator.analysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.prefs.Preferences;

import org.eclipse.ajdt.core.javaelements.AdviceElement;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import com.google.common.collect.BiMap;

import ca.mcgill.cs.swevo.jayfx.model.IElement;
import edu.ohio_state.cse.khatchad.fraglight.core.analysis.PatternMatcher;
import edu.ohio_state.cse.khatchad.fraglight.core.analysis.util.TimeCollector;
import edu.ohio_state.cse.khatchad.fraglight.core.graph.IntentionArc;
import edu.ohio_state.cse.khatchad.fraglight.core.graph.Pattern;
import edu.ohio_state.cse.khatchad.fraglight.core.util.AJUtil;
import edu.ohio_state.cse.khatchad.fraglight.core.util.Util;
import edu.ohio_state.cse.khatchad.fraglight.ui.PointcutChangePredictionProvider;

/**
 * @author <a href="mailto:khatchad@cse.ohio-state.edu">Raffi Khatchadourian</a>
 * 
 */
public class EvaluationPointcutChangePredictionProvider extends
		PointcutChangePredictionProvider {

	private static final String USE_GRAPH_CACHING = "USE_GRAPH_CACHING";

	private Preferences prefs;

	private BiMap<AdviceElement, AdviceElement> oldPointcutToNewPointcutMap;

	/**
	 * @param oldPointcutToNewPointcutMap
	 */
	public EvaluationPointcutChangePredictionProvider(
			BiMap<AdviceElement, AdviceElement> oldPointcutToNewPointcutMap) {
		this.oldPointcutToNewPointcutMap = oldPointcutToNewPointcutMap;
	}

	@Override
	protected Set<AdviceElement> retreivePreviouslyAnalyzedPointcuts(
			TimeCollector timeCollector) {
		Set<AdviceElement> ret = new LinkedHashSet<AdviceElement>();
		Collection<AdviceElement> previouslyAnalyzedPointcuts = super
				.retreivePreviouslyAnalyzedPointcuts(timeCollector);
		for (AdviceElement oldPointcut : previouslyAnalyzedPointcuts)
			ret.add(this.oldPointcutToNewPointcutMap.get(oldPointcut));

		return ret;
	}

	@Override
	public void processNewJoinPointShadow(IJavaElement newJoinPointShadow,
			TimeCollector timeCollector) throws JavaModelException {
		calculateChangeConfidence(newJoinPointShadow, timeCollector);
	}

	@Override
	protected Map<AdviceElement, Set<Pattern<IntentionArc<IElement>>>> findPatternsMatchingJoinPoint(
			IJavaElement affectingJoinPoint,
			Collection<AdviceElement> newPointcuts, PatternMatcher matcher,
			TimeCollector timeCollector) {

		timeCollector.start();
		ArrayList<AdviceElement> oldPointcuts = new ArrayList<AdviceElement>(
				newPointcuts);

		ListIterator<AdviceElement> iterator = oldPointcuts.listIterator();
		while (iterator.hasNext()) {
			AdviceElement newAdvice = iterator.next();
			AdviceElement oldAdvice = this.oldPointcutToNewPointcutMap
					.inverse().get(newAdvice);
			iterator.set(oldAdvice);
		}
		timeCollector.stop();

		return super.findPatternsMatchingJoinPoint(affectingJoinPoint,
				oldPointcuts, matcher, timeCollector);
	}

	@Override
	protected boolean isCapturedBy(IJavaElement affectingJoinPoint,
			AdviceElement oldAdvElem) throws JavaModelException {
		AdviceElement newAdvElem = this.oldPointcutToNewPointcutMap
				.get(oldAdvElem);
		return super.isCapturedBy(affectingJoinPoint, newAdvElem);
	}

	@Override
	protected PatternMatcher createPatternMatcher(
			Set<Pattern<IntentionArc<IElement>>> allPatterns,
			short maximumAnalysisDepth, TimeCollector timeCollector) {

		timeCollector.start();
		prefs = Preferences.userNodeForPackage(this.getClass());
		boolean useGraphCaching = prefs.getBoolean(USE_GRAPH_CACHING, false);
		timeCollector.stop();

		PatternMatcher matcher;

		if (useGraphCaching)
			matcher = new GraphCachingPatternMatcher(allPatterns,
					maximumAnalysisDepth);
		else
			matcher = new PatternMatcher(allPatterns, maximumAnalysisDepth);

		return matcher;
	}
}