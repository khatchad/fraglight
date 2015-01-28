package edu.ohio_state.cse.khatchad.fraglight.core.analysis;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.drools.WorkingMemory;
import org.eclipse.ajdt.core.javaelements.AdviceElement;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.jdom.DocType;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import ca.mcgill.cs.swevo.jayfx.ConversionException;
import ca.mcgill.cs.swevo.jayfx.model.IElement;
import edu.ohio_state.cse.khatchad.fraglight.core.analysis.util.TimeCollector;
import edu.ohio_state.cse.khatchad.fraglight.core.analysis.util.XMLUtil;
import edu.ohio_state.cse.khatchad.fraglight.core.graph.IntentionArc;
import edu.ohio_state.cse.khatchad.fraglight.core.graph.GraphElement;
import edu.ohio_state.cse.khatchad.fraglight.core.graph.ConcernGraph;
import edu.ohio_state.cse.khatchad.fraglight.core.graph.Pattern;

public class PointcutAnalyzer extends PointcutProcessor {
	
	private static final String ENABLING_GRAPH_ELEMENTS_FOR_EACH_ADVICE = "Enabling graph elements for each advice.";

	private Map<AdviceElement, Element> pointcutToXMLMap = new LinkedHashMap<AdviceElement, Element>();
	
	private Map<AdviceElement, Set<Pattern<IntentionArc<IElement>>>> pointcutToPatternSetMap = new LinkedHashMap<AdviceElement, Set<Pattern<IntentionArc<IElement>>>>();
	
	private static Logger logger = Logger.getLogger(PointcutAnalyzer.class.getName());

	public PointcutAnalyzer(short maximumAnalysisDepth) {
		super(maximumAnalysisDepth);
	}

	protected void analyzeAdviceCollection(
			final Collection<? extends AdviceElement> adviceCol,
			final ConcernGraph graph,
			final IProgressMonitor monitor, TimeCollector timeCollector) throws ConversionException,
			CoreException, IOException {

		monitor.beginTask(ENABLING_GRAPH_ELEMENTS_FOR_EACH_ADVICE,
				adviceCol.size());
		
		timeCollector.start();
		logger.log(Level.INFO, ENABLING_GRAPH_ELEMENTS_FOR_EACH_ADVICE,
				adviceCol.size());
		timeCollector.stop();

		int pointcutNumber = 0;
		for (final AdviceElement advElem : adviceCol) {

			final Map<Pattern<IntentionArc<IElement>>, Set<GraphElement<IElement>>> patternToResultMap = new LinkedHashMap<Pattern<IntentionArc<IElement>>, Set<GraphElement<IElement>>>();
			final Map<Pattern<IntentionArc<IElement>>, Set<GraphElement<IElement>>> patternToEnabledElementMap = new LinkedHashMap<Pattern<IntentionArc<IElement>>, Set<GraphElement<IElement>>>();

			graph.enableElementsAccordingTo(advElem, monitor);

			executeQueries(graph.getWorkingMemory(), patternToResultMap,
					patternToEnabledElementMap, monitor);

			for (final Pattern<IntentionArc<IElement>> pattern : patternToResultMap
					.keySet()) {
				pattern.setAdvice(advElem);
				pattern.calculateSimularityToAdviceBasedOnResults(patternToResultMap.get(pattern),
						patternToEnabledElementMap.get(pattern), graph);
			}

			pointcutNumber++;
			monitor.worked(1);
			this.pointcutToPatternSetMap.put(advElem,
					patternToResultMap.keySet());
		}
	}

	public void writeXMLFile() throws IOException, CoreException {
		Set<AdviceElement> keySet = this.pointcutToXMLMap.keySet();
		for (AdviceElement advElem : keySet)
			writeXMLFile(advElem, this.pointcutToXMLMap.get(advElem));
	}

	protected void writeXMLFile(final AdviceElement advElem,
			Element adviceXMLElement) throws IOException, CoreException {
		DocType type = new DocType(this.getClass().getSimpleName());
		Document doc = new Document(adviceXMLElement, type);
		XMLOutputter serializer = new XMLOutputter(Format.getPrettyFormat());
		PrintWriter xmlOut = XMLUtil.getXMLFileWriter(advElem);
		serializer.output(doc, xmlOut);
		xmlOut.close();

		IJavaElement ancestor = advElem.getAncestor(IJavaElement.JAVA_PROJECT);
		IJavaProject jProject = (IJavaProject) ancestor;
		IProject project = jProject.getProject();
		project.refreshLocal(IResource.DEPTH_INFINITE,
				new NullProgressMonitor());
	}

	public Map<AdviceElement, Element> getPointcutToXMLMap() {
		return this.pointcutToXMLMap;
	}

	public Map<AdviceElement, Set<Pattern<IntentionArc<IElement>>>> getPointcutToPatternSetMap() {
		return this.pointcutToPatternSetMap;
	}
}