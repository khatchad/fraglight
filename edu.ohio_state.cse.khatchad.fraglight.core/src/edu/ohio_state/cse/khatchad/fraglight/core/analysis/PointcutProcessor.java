package edu.ohio_state.cse.khatchad.fraglight.core.analysis;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.drools.QueryResult;
import org.drools.QueryResults;
import org.drools.WorkingMemory;
import org.eclipse.ajdt.core.javaelements.AdviceElement;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.jdom2.Element;
import org.jdom2.JDOMException;

import ca.mcgill.cs.swevo.jayfx.ConversionException;
import ca.mcgill.cs.swevo.jayfx.model.IElement;
import ca.mcgill.cs.swevo.jayfx.model.Relation;
import edu.ohio_state.cse.khatchad.fraglight.core.analysis.util.TimeCollector;
import edu.ohio_state.cse.khatchad.fraglight.core.analysis.util.XMLUtil;
import edu.ohio_state.cse.khatchad.fraglight.core.graph.ConcernGraph;
import edu.ohio_state.cse.khatchad.fraglight.core.graph.GraphElement;
import edu.ohio_state.cse.khatchad.fraglight.core.graph.IntentionArc;
import edu.ohio_state.cse.khatchad.fraglight.core.graph.IntentionNode;
import edu.ohio_state.cse.khatchad.fraglight.core.graph.Path;
import edu.ohio_state.cse.khatchad.fraglight.core.graph.Pattern;
import edu.ohio_state.cse.khatchad.fraglight.core.util.AJUtil;
import edu.ohio_state.cse.khatchad.fraglight.core.util.Util;

public abstract class PointcutProcessor {

	private static Logger logger = Logger.getLogger(PointcutProcessor.class.getName());

	/**
	 * @param maximumAnalysisDepth
	 */
	public PointcutProcessor(short maximumAnalysisDepth) {
		this.maximumAnalysisDepth = maximumAnalysisDepth;
	}

	public PointcutProcessor() {
	}

	/**
	 * 
	 */
	private static final String SUGGESTED_ELEMENTS = "suggestedlements";
	/**
	 * 
	 */
	private static final String ENABLED_ELEMENTS = "enabledElements";

	private static final String ADVISED_ELEMENTS = "advisedElements";
	/**
	 * 
	 */
	private static final String SIMULARITY = "simularity";

	public static final int DEFAULT_MAXIMUM_ANALYSIS_DEPTH = 2;

	private short maximumAnalysisDepth = DEFAULT_MAXIMUM_ANALYSIS_DEPTH;

	/**
	 * @param relation
	 * @param string
	 * @param workingMemory
	 * @param patternToResultMap
	 * @param patternToEnabledElementMap
	 * @param lMonitor
	 */
	@SuppressWarnings("unchecked")
	private static void executeArcQuery(final String queryString, final Relation relation,
			final WorkingMemory workingMemory,
			final Map<Pattern<IntentionArc<IElement>>, Set<GraphElement<IElement>>> patternToResultMap,
			final Map<Pattern<IntentionArc<IElement>>, Set<GraphElement<IElement>>> patternToEnabledElementMap,
			final IProgressMonitor lMonitor) {

		final QueryResults suggestedArcs = workingMemory.getQueryResults(queryString, new Object[] { relation });

		lMonitor.beginTask("Executing query: " + queryString.replace("X", relation.toString()) + ".",
				suggestedArcs.size());
		for (final Iterator it = suggestedArcs.iterator(); it.hasNext();) {
			final QueryResult result = (QueryResult) it.next();
			final IntentionArc suggestedArc = (IntentionArc) result.get("$suggestedArc");

			final IntentionArc enabledArc = (IntentionArc) result.get("$enabledArc");

			final Path enabledPath = (Path) result.get("$enabledPath");

			final IntentionNode commonNode = (IntentionNode) result.get("$commonNode");
			final Pattern pattern = enabledPath.extractPattern(commonNode, enabledArc);

			if (!patternToResultMap.containsKey(pattern))
				patternToResultMap.put(pattern, new LinkedHashSet<GraphElement<IElement>>());
			patternToResultMap.get(pattern).add(suggestedArc);

			if (!patternToEnabledElementMap.containsKey(pattern))
				patternToEnabledElementMap.put(pattern, new LinkedHashSet<GraphElement<IElement>>());
			patternToEnabledElementMap.get(pattern).add(enabledArc);

			lMonitor.worked(1);
		}
		lMonitor.done();
	}

	/**
	 * @param lMonitor
	 * @param workingMemory
	 * @param patternToResultMap
	 * @param patternToEnabledElementMap
	 */
	@SuppressWarnings("unchecked")
	private static void executeNodeQuery(final IProgressMonitor lMonitor, final WorkingMemory workingMemory,
			final Map<Pattern<IntentionArc<IElement>>, Set<GraphElement<IElement>>> patternToResultMap,
			final Map<Pattern<IntentionArc<IElement>>, Set<GraphElement<IElement>>> patternToEnabledElementMap,
			final String queryString) {

		final QueryResults suggestedNodes = workingMemory.getQueryResults(queryString);
		lMonitor.beginTask("Executing node query: " + queryString + ".", suggestedNodes.size());
		for (final Iterator it = suggestedNodes.iterator(); it.hasNext();) {
			final QueryResult result = (QueryResult) it.next();
			final IntentionNode suggestedNode = (IntentionNode) result.get("$suggestedNode");

			final IntentionNode enabledNode = (IntentionNode) result.get("$enabledNode");

			final Path enabledPath = (Path) result.get("$enabledPath");

			final IntentionNode commonNode = (IntentionNode) result.get("$commonNode");
			final Pattern pattern = enabledPath.extractPattern(commonNode, enabledNode);

			if (!patternToResultMap.containsKey(pattern))
				patternToResultMap.put(pattern, new LinkedHashSet<GraphElement<IElement>>());
			patternToResultMap.get(pattern).add(suggestedNode);

			if (!patternToEnabledElementMap.containsKey(pattern))
				patternToEnabledElementMap.put(pattern, new LinkedHashSet<GraphElement<IElement>>());
			patternToEnabledElementMap.get(pattern).add(enabledNode);

			lMonitor.worked(1);
		}
		lMonitor.done();
	}

	public void analyze(final Collection<? extends AdviceElement> adviceCol, final IProgressMonitor monitor,
			TimeCollector timeCollector) throws Exception {

		final Collection<IProject> projectsToAnalyze = Util.getProjects(adviceCol);

		timeCollector.start();
		logger.log(Level.INFO, "Building graph from projects.", projectsToAnalyze);
		timeCollector.stop();

		final ConcernGraph graph = createConcernGraph(projectsToAnalyze, monitor, timeCollector);

		timeCollector.start();
		logger.info("Analyzing.");
		timeCollector.stop();

		analyzeAdviceCollection(adviceCol, graph, monitor, timeCollector);
	}

	protected ConcernGraph createConcernGraph(final Collection<IProject> projectsToAnalyze,
			final IProgressMonitor monitor, TimeCollector timeCollector) throws Exception {
		final ConcernGraph graph = new ConcernGraph(projectsToAnalyze, this.maximumAnalysisDepth, monitor,
				timeCollector);
		return graph;
	}

	protected abstract void analyzeAdviceCollection(final Collection<? extends AdviceElement> adviceCol,
			final ConcernGraph graph, final IProgressMonitor monitor, TimeCollector timeCollector)
					throws ConversionException, CoreException, IOException, JDOMException;

	/**
	 * @param patternToEnabledElementMap
	 * @param pattern
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	private Element getXML(final Set<GraphElement<IElement>> set, String elementName) {
		Element ret = new Element(elementName);
		for (GraphElement<IElement> enabledElement : set) {
			if (enabledElement instanceof IntentionArc)
				ret.addContent(((IntentionArc) enabledElement).getXML());
			else
				ret.addContent(enabledElement.getXML());
		}
		return ret;
	}

	/**
	 * @param adviceXMLElement
	 * @param pattern
	 * @param simularity
	 */
	@SuppressWarnings("unused")
	private static Element getPatternXMLElement(final Pattern pattern, double simularity) {
		Element patternXMLElement = pattern.getXML();
		patternXMLElement.setAttribute(SIMULARITY, String.valueOf(simularity));
		return patternXMLElement;
	}

	/**
	 * @param advElem
	 * @return
	 * @throws JavaModelException
	 */
	protected static Element createAdviceXMLElement(final AdviceElement advElem) throws JavaModelException {
		Element adviceXMLElement = new Element(AdviceElement.class.getSimpleName());
		Element ret = XMLUtil.getXML(advElem);
		Element advisedElementXML = getAdvisedJavaElementsXMLElement(adviceXMLElement,
				AJUtil.getAdvisedJavaElements(advElem));
		ret.addContent(advisedElementXML);
		return ret;
	}

	/**
	 * @param workingMemory
	 * @param patternToResultMap
	 * @param patternToEnabledElementMap
	 * @param monitor
	 */
	protected void executeQueries(final WorkingMemory workingMemory,
			final Map<Pattern<IntentionArc<IElement>>, Set<GraphElement<IElement>>> patternToResultMap,
			final Map<Pattern<IntentionArc<IElement>>, Set<GraphElement<IElement>>> patternToEnabledElementMap,
			final IProgressMonitor monitor) {

		executeNodeQuery(new SubProgressMonitor(monitor, 1, SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK),
				workingMemory, patternToResultMap, patternToEnabledElementMap, "forward suggested execution nodes");

		executeNodeQuery(new SubProgressMonitor(monitor, 1, SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK),
				workingMemory, patternToResultMap, patternToEnabledElementMap, "backward suggested execution nodes");

		executeArcQuery("forward suggested X arcs", Relation.CALLS, workingMemory, patternToResultMap,
				patternToEnabledElementMap,
				new SubProgressMonitor(monitor, 1, SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK));

		executeArcQuery("backward suggested X arcs", Relation.CALLS, workingMemory, patternToResultMap,
				patternToEnabledElementMap,
				new SubProgressMonitor(monitor, 1, SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK));

		executeArcQuery("forward suggested X arcs", Relation.GETS, workingMemory, patternToResultMap,
				patternToEnabledElementMap,
				new SubProgressMonitor(monitor, 1, SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK));

		executeArcQuery("backward suggested X arcs", Relation.GETS, workingMemory, patternToResultMap,
				patternToEnabledElementMap,
				new SubProgressMonitor(monitor, 1, SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK));

		executeArcQuery("forward suggested X arcs", Relation.SETS, workingMemory, patternToResultMap,
				patternToEnabledElementMap,
				new SubProgressMonitor(monitor, 1, SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK));

		executeArcQuery("backward suggested X arcs", Relation.SETS, workingMemory, patternToResultMap,
				patternToEnabledElementMap,
				new SubProgressMonitor(monitor, 1, SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK));
	}

	/**
	 * @param adviceXMLElement
	 * @param advisedJavaElements
	 */
	private static Element getAdvisedJavaElementsXMLElement(Element adviceXMLElement,
			Set<IJavaElement> advisedJavaElements) {
		Element ret = new Element(ADVISED_ELEMENTS);
		for (IJavaElement jElem : advisedJavaElements) {
			Element xmlElem = XMLUtil.getXML(jElem);
			ret.addContent(xmlElem);
		}
		return ret;
	}

	/**
	 * @return the maximumAnalysisDepth
	 */
	public short getMaximumAnalysisDepth() {
		return this.maximumAnalysisDepth;
	}

	/**
	 * @param maximumAnalysisDepth
	 *            the maximumAnalysisDepth to set
	 */
	public void setMaximumAnalysisDepth(short maximumAnalysisDepth) {
		this.maximumAnalysisDepth = maximumAnalysisDepth;
	}
}