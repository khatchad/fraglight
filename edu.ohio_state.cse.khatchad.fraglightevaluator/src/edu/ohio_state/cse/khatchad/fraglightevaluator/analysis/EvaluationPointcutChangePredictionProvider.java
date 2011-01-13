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

import org.eclipse.ajdt.core.javaelements.AdviceElement;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import com.google.common.collect.BiMap;

import ca.mcgill.cs.swevo.jayfx.model.IElement;
import edu.ohio_state.cse.khatchad.fraglight.core.analysis.PatternMatcher;
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

	private BiMap<AdviceElement, AdviceElement> oldPointcutToNewPointcutMap;

	/**
	 * @param oldPointcutToNewPointcutMap
	 */
	public EvaluationPointcutChangePredictionProvider(
			BiMap<AdviceElement, AdviceElement> oldPointcutToNewPointcutMap) {
		this.oldPointcutToNewPointcutMap = oldPointcutToNewPointcutMap;
	}

	@Override
	protected Set<AdviceElement> retreivePreviouslyAnalyzedPointcuts() {
		Set<AdviceElement> ret = new LinkedHashSet<AdviceElement>();
		Collection<AdviceElement> previouslyAnalyzedPointcuts = super.retreivePreviouslyAnalyzedPointcuts();
		for ( AdviceElement oldPointcut : previouslyAnalyzedPointcuts ) 
			ret.add(this.oldPointcutToNewPointcutMap.get(oldPointcut));
			
		return ret;
	}

	@Override
	public void processNewJoinPointShadow(IJavaElement newJoinPointShadow)
			throws JavaModelException {
		clearPreviousPredictions();
		calculateChangeConfidence(newJoinPointShadow);
	}

	@Override
	protected Set<Pattern<IntentionArc<IElement>>> getPatternsDerivedFromPointcut(
			AdviceElement advElem) {
		return super.getPatternsDerivedFromPointcut(this.oldPointcutToNewPointcutMap.inverse().get(advElem));
	}

	@Override
	protected Map<AdviceElement, Set<Pattern<IntentionArc<IElement>>>> findPatternsMatchingJoinPoint(
			IJavaElement affectingJoinPoint,
			Collection<AdviceElement> newPointcuts, PatternMatcher matcher) {
		
		ArrayList<AdviceElement> oldPointcuts = new ArrayList<AdviceElement>(newPointcuts);
		
		ListIterator<AdviceElement> iterator = oldPointcuts.listIterator();
		while ( iterator.hasNext() ) {
			AdviceElement newAdvice = iterator.next();
			AdviceElement oldAdvice = this.oldPointcutToNewPointcutMap.inverse().get(newAdvice);
			iterator.set(oldAdvice);
		}
		
		return super.findPatternsMatchingJoinPoint(affectingJoinPoint, oldPointcuts,
				matcher);
	}
}