/**
 * 
 */
package edu.ohio_state.cse.khatchad.fraglightevaluator.analysis;

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
	protected Set<Pattern<IntentionArc<IElement>>> getPatternsDerivedFromPointcut(
			AdviceElement newPointcut) {
		
		String newAdvicekey = Util.getKey(newPointcut);
		String oldAdviceKey = this.oldPointcutToNewPointcutMap.inverse().get(newAdvicekey);
		IProject project = AJUtil.getProject(newPointcut);
		IJavaProject jProject = JavaCore.create(project);
		
		AdviceElement oldPointcut;
		try {
			oldPointcut = AJUtil.extractAdviceElement(oldAdviceKey, jProject);
		} catch (JavaModelException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}

		return super.getPatternsDerivedFromPointcut(oldPointcut);
	}
}