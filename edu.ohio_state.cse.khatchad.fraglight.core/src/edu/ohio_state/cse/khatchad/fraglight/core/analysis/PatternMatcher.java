/**
 * 
 */
package edu.ohio_state.cse.khatchad.fraglight.core.analysis;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.drools.QueryResult;
import org.drools.QueryResults;
import org.drools.RuleBase;
import org.drools.WorkingMemory;
import org.eclipse.ajdt.core.javaelements.AdviceElement;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.jdom.JDOMException;

import ca.mcgill.cs.swevo.jayfx.ConversionException;
import ca.mcgill.cs.swevo.jayfx.JayFX;
import ca.mcgill.cs.swevo.jayfx.JayFXException;
import ca.mcgill.cs.swevo.jayfx.model.IElement;
import ca.mcgill.cs.swevo.jayfx.model.Relation;
import edu.ohio_state.cse.khatchad.fraglight.core.analysis.util.FileUtil;
import edu.ohio_state.cse.khatchad.fraglight.core.analysis.util.TimeCollector;
import edu.ohio_state.cse.khatchad.fraglight.core.graph.IntentionArc;
import edu.ohio_state.cse.khatchad.fraglight.core.graph.GraphElement;
import edu.ohio_state.cse.khatchad.fraglight.core.graph.ConcernGraph;
import edu.ohio_state.cse.khatchad.fraglight.core.graph.IntentionNode;
import edu.ohio_state.cse.khatchad.fraglight.core.graph.Path;
import edu.ohio_state.cse.khatchad.fraglight.core.graph.Pattern;
import edu.ohio_state.cse.khatchad.fraglight.core.util.AJUtil;

/**
 * @author raffi
 * 
 */
public class PatternMatcher extends PointcutProcessor {

	private Map<Pattern<IntentionArc<IElement>>, Set<IJavaElement>> patternToMatchingJavaElementSetMap = new LinkedHashMap<Pattern<IntentionArc<IElement>>, Set<IJavaElement>>();
	private final Set<Pattern<IntentionArc<IElement>>> seed;

	/**
	 * @param seed
	 */
	public PatternMatcher(Set<Pattern<IntentionArc<IElement>>> seed,
			short maximumAnalysisDepth) {
		super(maximumAnalysisDepth);
		this.seed = seed;
	}

	public PatternMatcher(Set<Pattern<IntentionArc<IElement>>> seed) {
		this.seed = seed;
	}

	@Override
	protected void analyzeAdviceCollection(
			Collection<? extends AdviceElement> adviceCol, ConcernGraph graph,
			IProgressMonitor monitor,
			TimeCollector timeCollector) throws ConversionException,
			CoreException, IOException, JDOMException {

		final Map<Pattern<IntentionArc<IElement>>, Set<GraphElement<IElement>>> derivedPatternToResultMap = new LinkedHashMap<Pattern<IntentionArc<IElement>>, Set<GraphElement<IElement>>>();
		final Map<Pattern<IntentionArc<IElement>>, Set<GraphElement<IElement>>> derivedPatternToEnabledElementMap = new LinkedHashMap<Pattern<IntentionArc<IElement>>, Set<GraphElement<IElement>>>();

		/*
		 * OK, so let's suppose that this graph I get above is built
		 * incrementally and that is more efficient. Maybe I'll need to do that,
		 * who knows. But let's assume it is. It seems pretty clear that
		 * enabling all advisable elements and deriving both patterns and
		 * results from them is pretty dumb. What I really should do is enable
		 * elements that still exist. But then what? Derive patterns and results
		 * from them? That doesn't seem very incremental.
		 */

		graph.enableElementsAccordingTo(adviceCol, new SubProgressMonitor(
				monitor, -1));

		executeQueries(graph.getWorkingMemory(), derivedPatternToResultMap,
				derivedPatternToEnabledElementMap, monitor);

		for (Pattern<IntentionArc<IElement>> pattern : this.seed) {
			if (derivedPatternToResultMap.containsKey(pattern)) {
				for (GraphElement<IElement> intentionElement : derivedPatternToResultMap
						.get(pattern)) {

					IJavaElement matchedJavaElement = intentionElement
							.toJavaElement(graph.getDatabase());

					if (matchedJavaElement != null) {
						Set<IJavaElement> matchingPatternSet = this.patternToMatchingJavaElementSetMap
								.get(pattern);
						if (matchingPatternSet == null) {
							matchingPatternSet = new LinkedHashSet<IJavaElement>();
							this.patternToMatchingJavaElementSetMap.put(
									pattern, matchingPatternSet);
						}
						matchingPatternSet.add(matchedJavaElement);
					}
				}
			}
		}
	}

	/**
	 * @param pattern
	 * @return
	 */
	public Set<IJavaElement> getMatchingJavaElements(
			Pattern<IntentionArc<IElement>> pattern) {
		final Set<IJavaElement> matchingJavaElements = this.patternToMatchingJavaElementSetMap
				.get(pattern);
		if (matchingJavaElements == null)
			return Collections.emptySet();
		else
			return matchingJavaElements;
	}
}
