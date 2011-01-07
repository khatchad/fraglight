/**
 * 
 */
package edu.ohio_state.cse.khatchad.fraglightevaluator.analysis;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.ajdt.core.javaelements.AdviceElement;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import com.google.common.collect.BiMap;

import ca.mcgill.cs.swevo.jayfx.model.IElement;
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

	private BiMap<String, String> oldPointcutToNewPointcutMap;

	/**
	 * @param oldPointcutToNewPointcutMap
	 */
	public EvaluationPointcutChangePredictionProvider(
			BiMap<String, String> oldPointcutToNewPointcutMap) {
		this.oldPointcutToNewPointcutMap = oldPointcutToNewPointcutMap;
	}

	@Override
	protected Set<AdviceElement> retreivePreviouslyAnalyzedPointcuts() {
		Set<AdviceElement> ret = new LinkedHashSet<AdviceElement>();
		Collection<AdviceElement> previouslyAnalyzedPointcuts = super.retreivePreviouslyAnalyzedPointcuts();
		for ( AdviceElement oldPointcut : previouslyAnalyzedPointcuts ) {
			String oldPointcutKey = Util.getKey(oldPointcut);
			String newPointcutKey = this.oldPointcutToNewPointcutMap.get(oldPointcutKey);
			
			
		}
			
		return null;
	}
}