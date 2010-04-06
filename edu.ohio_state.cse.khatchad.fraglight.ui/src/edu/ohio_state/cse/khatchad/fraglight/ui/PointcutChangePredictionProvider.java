/**
 * 
 */
package edu.ohio_state.cse.khatchad.fraglight.ui;

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.ajdt.core.javaelements.AdviceElement;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.mylyn.context.core.ContextChangeEvent;
import org.eclipse.mylyn.context.core.IInteractionElement;
import org.eclipse.mylyn.internal.context.core.IActiveSearchOperation;
import org.eclipse.mylyn.internal.java.ui.JavaStructureBridge;
import org.eclipse.mylyn.internal.java.ui.search.AbstractJavaRelationProvider;

import ca.mcgill.cs.swevo.jayfx.model.IElement;
import edu.ohio_state.cse.khatchad.fraglight.core.analysis.PointcutAnalyzer;
import edu.ohio_state.cse.khatchad.fraglight.core.analysis.PointcutRejuvenator;
import edu.ohio_state.cse.khatchad.fraglight.core.graph.IntentionArc;
import edu.ohio_state.cse.khatchad.fraglight.core.graph.Pattern;
import edu.ohio_state.cse.khatchad.fraglight.core.util.AJUtil;

/**
 * @author <a href="mailto:khatchad@cse.ohio-state.edu">Raffi Khatchadourian</a>
 * 
 */
@SuppressWarnings("restriction")
public class PointcutChangePredictionProvider extends
		AbstractJavaRelationProvider implements IElementChangedListener {

	private static final String ID = ID_GENERIC + ".pointcutchangeprediction";

	public static final String NAME = "may break"; //$NON-NLS-1$

	private PointcutAnalyzer analyzer = new PointcutAnalyzer();
	
	private PointcutRejuvenator rejuvenator = new PointcutRejuvenator();

	public PointcutChangePredictionProvider() {
		super(JavaStructureBridge.CONTENT_TYPE, ID);
	}

	@Override
	public void contextChanged(ContextChangeEvent event) {
		switch (event.getEventKind()) {
			case ACTIVATED: {
				//register as a Java editor change listener.
				JavaCore.addElementChangedListener(this);

				//analyze pointcuts.
				IWorkspace workspace = getWorkspace();
				Collection<? extends AdviceElement> toAnalyze = AJUtil.extractValidAdviceElements(workspace);
				
				if (!toAnalyze.isEmpty()) {
					try {
						//TODO: Get a progress monitor from somewhere.
						this.analyzer.analyze(toAnalyze,
								new NullProgressMonitor());
					}
					catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						throw new RuntimeException(e);
					}
				}	
				break;
			}

			case DEACTIVATED: {
				//TODO: Only write the XML file when an XML file already exists implies that the patterns have been rebuilt since last load.
				try {
					this.analyzer.writeXMLFile();
				}
				catch (IOException e) {
					throw new RuntimeException("Error writing XML file.", e);
				}
				catch (CoreException e) {
					throw new RuntimeException("Error writing XML file.", e);
				}
			}
		}
	}

	@Override
	protected void findRelated(IInteractionElement node, int degreeOfSeparation) {
		// TODO This method may very well be the one that eventually does the work.
		//Given an interaction, will find pointcuts that may have broken.
		System.out.println("Find realtd was called.");
	}

	/* (non-Javadoc)
	 * @see org.eclipse.mylyn.internal.context.core.AbstractRelationProvider#getName()
	 */
	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public IActiveSearchOperation getSearchOperation(IInteractionElement node,
			int limitTo, int degreeOfSeparation) {
		return null;
	}

	@Override
	protected String getSourceId() {
		return ID;
	}

	public void elementChanged(ElementChangedEvent event) {
		IJavaElementDelta delta = event.getDelta();
		try {
			handleDelta(delta.getAffectedChildren());
		}
		catch (JavaModelException e) {
		}
	}

	/**
	 * @param delta
	 * @throws JavaModelException
	 */
	private void handleDelta(IJavaElementDelta[] delta)
			throws JavaModelException {
		for (IJavaElementDelta child : delta) {
			if (child.getKind() == IJavaElementDelta.ADDED) {
				final IJavaElement element = child.getElement();
				//this represents the case where a new method execution join point is added.
				if (element.getElementType() == IJavaElement.METHOD
						&& element.isStructureKnown()) {
					
					//calculate the change confidence for every PCE.
					//TODO: May just want to call the rejuvenator here.
					
					List<AdviceElement> empty = Collections.emptyList();
					for (AdviceElement advElem : empty ) {

						//calculate the change confidence.
						double changeConfidence = calculateChangeConfidence(
								element, advElem);

					}
				}
			}
			handleDelta(child.getAffectedChildren());
		}
	}

	/**
	 * @param element
	 * @param advElem
	 * @return
	 * @throws JavaModelException 
	 */
	private double calculateChangeConfidence(IJavaElement joinPointShadow,
			AdviceElement advElem) throws JavaModelException {

		//decide if the new join point is captured by the pointcut.
		boolean captured = isCapturedBy(joinPointShadow, advElem);

		Set<Pattern<IntentionArc<IElement>>> patternSet = getValidPatterns(
				joinPointShadow, advElem);

		double numerator = 0;
		for (Pattern<IntentionArc<IElement>> pattern : patternSet) {
			double quantityToFindTheAbsoluteValueOf = (captured ? 1 : 0)
					- pattern.getSimularity();
			double absoluteValue = Math.abs(quantityToFindTheAbsoluteValueOf);
			numerator += absoluteValue;
		}

		int denominator = patternSet.size();
		double changeConfidence = ((double) numerator) / denominator;
		return changeConfidence;
	}

	/**
	 * @param advElem
	 * @param joinPointShadow
	 * @return
	 */
	private Set<Pattern<IntentionArc<IElement>>> getValidPatterns(
			IJavaElement joinPointShadow, AdviceElement advElem) {
		Map<AdviceElement, Set<Pattern<IntentionArc<IElement>>>> pointcutToPatternSetMap = this.analyzer
				.getPointcutToPatternSetMap();
		Set<Pattern<IntentionArc<IElement>>> patternSet = pointcutToPatternSetMap
				.get(advElem);
		//TODO: I need to know whether or not these patterns produce a "suggestion" that matches the given joinPointShadow.
		//      I guess that the next step from here is taking a look at the rejuvenation process.
		return null;
	}

	/**
	 * @param joinPointShadow
	 * @param advice
	 * @return
	 * @throws JavaModelException 
	 */
	private boolean isCapturedBy(IJavaElement joinPointShadow, AdviceElement advice) throws JavaModelException {
		Set<IJavaElement> advisedJavaElements = AJUtil.getAdvisedJavaElements(advice);
		return advisedJavaElements.contains(joinPointShadow);
	}
}