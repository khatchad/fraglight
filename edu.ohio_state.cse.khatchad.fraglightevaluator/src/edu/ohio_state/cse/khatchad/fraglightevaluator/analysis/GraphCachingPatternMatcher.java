package edu.ohio_state.cse.khatchad.fraglightevaluator.analysis;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.ajdt.core.javaelements.AdviceElement;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;

import com.google.common.collect.Collections2;

import ca.mcgill.cs.swevo.jayfx.model.IElement;
import edu.ohio_state.cse.khatchad.fraglight.core.analysis.PatternMatcher;
import edu.ohio_state.cse.khatchad.fraglight.core.analysis.util.TimeCollector;
import edu.ohio_state.cse.khatchad.fraglight.core.graph.ConcernGraph;
import edu.ohio_state.cse.khatchad.fraglight.core.graph.IntentionArc;
import edu.ohio_state.cse.khatchad.fraglight.core.graph.Pattern;
import edu.ohio_state.cse.khatchad.fraglight.core.util.Util;

/**
 * @author <a href="mailto:khatchad@cse.ohio-state.edu">Raffi Khatchadourian</a>
 *
 */
public class GraphCachingPatternMatcher extends PatternMatcher {

	public GraphCachingPatternMatcher(
			Set<Pattern<IntentionArc<IElement>>> seed,
			short maximumAnalysisDepth) {
		super(seed, maximumAnalysisDepth);
	}

	public GraphCachingPatternMatcher(Set<Pattern<IntentionArc<IElement>>> seed) {
		super(seed);
	}
	
	private static Map<Collection<IProject>, ConcernGraph> projectCollectionToConcernGraphMap = new HashMap<Collection<IProject>, ConcernGraph>();
	
	private static Map<Collection<IProject>, Double> projectCollectionToConcernGraphConstructionTimeMap = new HashMap<Collection<IProject>, Double>();

	@Override
	protected ConcernGraph createConcernGraph(
			Collection<IProject> projectsToAnalyze, IProgressMonitor monitor,
			TimeCollector timeCollector) throws Exception {
		
		timeCollector.start();
		
		if ( !projectCollectionToConcernGraphMap.containsKey(projectsToAnalyze) ) {
			
			TimeCollector constructionTimeCollector = new TimeCollector();
			final long constructionTimeStart = System.currentTimeMillis();
			
			ConcernGraph concernGraph = super.createConcernGraph(projectsToAnalyze, monitor, constructionTimeCollector);
			
			double constructionTimeEnd = Util.calculateTimeStatistics(constructionTimeStart, constructionTimeCollector);
			projectCollectionToConcernGraphConstructionTimeMap.put(projectsToAnalyze, constructionTimeEnd);
			
			projectCollectionToConcernGraphMap.put(projectsToAnalyze, concernGraph);
		}
		
		ConcernGraph concernGraph = projectCollectionToConcernGraphMap.get(projectsToAnalyze);
		timeCollector.stop();
		return concernGraph;
	}
	
	public static void clearCache() {
		projectCollectionToConcernGraphMap.clear();
		projectCollectionToConcernGraphConstructionTimeMap.clear();
	}

	/**
	 * @return
	 */
	public static double getTotalGraphContructionTime() {
		Collection<Double> values = projectCollectionToConcernGraphConstructionTimeMap.values();
		return Util.sum(values);
	}
}
