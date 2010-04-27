/**
 * 
 */
package edu.ohio_state.cse.khatchad.fraglight.ui;

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.ajdt.core.javaelements.AdviceElement;
import org.eclipse.ajdt.core.javaelements.CompilationUnitTools;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
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
import edu.ohio_state.cse.khatchad.fraglight.core.analysis.PatternMatcher;
import edu.ohio_state.cse.khatchad.fraglight.core.analysis.PointcutAnalyzer;
import edu.ohio_state.cse.khatchad.fraglight.core.analysis.PointcutRejuvenator;
import edu.ohio_state.cse.khatchad.fraglight.core.graph.IntentionArc;
import edu.ohio_state.cse.khatchad.fraglight.core.graph.Pattern;
import edu.ohio_state.cse.khatchad.fraglight.core.util.AJUtil;
import edu.ohio_state.cse.khatchad.fraglight.core.util.Util;
import edu.ohio_state.cse.khatchad.fraglight.ui.preferences.PreferenceConstants;

/**
 * @author <a href="mailto:khatchad@cse.ohio-state.edu">Raffi Khatchadourian</a>
 * 
 */
@SuppressWarnings("restriction")
public class PointcutChangePredictionProvider extends
		AbstractJavaRelationProvider implements IElementChangedListener {

	private static final String ID = ID_GENERIC + ".pointcutchangeprediction";

	public static final String NAME = "may break"; //$NON-NLS-1$

	private static final double CHANGE_CONFIDENCE_DEFAULT_THRESHOLD = 0.50;

	private PointcutAnalyzer analyzer = new PointcutAnalyzer();
	
	IProgressMonitor monitor = new NullProgressMonitor();
	
	private double changeConfidenceThreshold = CHANGE_CONFIDENCE_DEFAULT_THRESHOLD;

	public PointcutChangePredictionProvider() {
		super(JavaStructureBridge.CONTENT_TYPE, ID);
		FraglightUiPlugin.getDefault().setChangePredictionProvider(this);
	}

	@Override
	public void contextChanged(ContextChangeEvent event) {
		switch (event.getEventKind()) {
			case ACTIVATED: {
				//register as a Java editor change listener.
				JavaCore.addElementChangedListener(this);

				//analyze pointcuts.
				IWorkspace workspace = getWorkspace();
				try {
					workspace.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
				}
				catch (CoreException e) {
					throw new RuntimeException(e);
				}
				Collection<? extends AdviceElement> toAnalyze = AJUtil
						.extractValidAdviceElements(workspace);

				if (!toAnalyze.isEmpty()) {
					try {
						this.analyzer.analyze(toAnalyze,
								this.monitor);
					}
					catch (Exception e) {
						e.printStackTrace();
						throw new RuntimeException(e);
					}
				}
				break;
			}

			case DEACTIVATED: {
				//deregister as a Java editor change listener.
				JavaCore.removeElementChangedListener(this);
				
				//TODO: Only write the XML file when an XML file already exists implies that the patterns have been rebuilt since last load.
//				try {
//					this.analyzer.writeXMLFile();
//				}
//				catch (IOException e) {
//					throw new RuntimeException("Error writing XML file.", e);
//				}
//				catch (CoreException e) {
//					throw new RuntimeException("Error writing XML file.", e);
//				}
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
				
					//save the file.
					ICompilationUnit icu = Util.getCompilationUnit(element);
					icu.getBuffer().save(this.monitor, false);
					
					// Ensure that the building process is triggered.
					try {
						element.getJavaProject().getProject().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
					}
					catch (CoreException e) {
						throw new RuntimeException(e);
					}

					//calculate the change confidence for every PCE.
					Collection<Set<Pattern<IntentionArc<IElement>>>> allPatternSets = this.analyzer
							.getPointcutToPatternSetMap().values();
					Set<Pattern<IntentionArc<IElement>>> allPatterns = new LinkedHashSet<Pattern<IntentionArc<IElement>>>();
					for (Set<Pattern<IntentionArc<IElement>>> patternSet : allPatternSets) {
						allPatterns.addAll(patternSet);
					}

					//initialize the pattern matcher, seeding it with all patterns.
					PatternMatcher matcher = new PatternMatcher(allPatterns);
					
					//let the matcher match patterns against code in the projects:
					try {
						matcher.analyze(this.analyzer.getPointcutToPatternSetMap().keySet(), this.monitor);
					}
					catch (Exception e) {
						throw new RuntimeException(e);
					}
					
					Set<Pattern<IntentionArc<IElement>>> patternsMatchingJoinPoint = new LinkedHashSet<Pattern<IntentionArc<IElement>>>();

					for (Pattern<IntentionArc<IElement>> pattern : allPatterns) {
						//get all java elements produced by the pattern.
						Set<IJavaElement> matchingJavaElementSet = matcher
								.getMatchingJavaElements(pattern);
						if (matchingJavaElementSet.contains(element))
							patternsMatchingJoinPoint.add(pattern);
					}

					for (AdviceElement advElem : this.analyzer
							.getPointcutToPatternSetMap().keySet()) {

						Set<Pattern<IntentionArc<IElement>>> patternsDerivedFromPointcut = this.analyzer
								.getPointcutToPatternSetMap().get(advElem);

						Set<Pattern<IntentionArc<IElement>>> patternsToConsider = new LinkedHashSet<Pattern<IntentionArc<IElement>>>(
								patternsDerivedFromPointcut);
						patternsToConsider.retainAll(patternsMatchingJoinPoint);
						
						//decide if the new join point is captured by the pointcut.
						boolean captured = AJUtil.isCapturedBy(element, advElem);

						//calculate the change confidence.
						double changeConfidence = calculateChangeConfidence(
								captured, patternsToConsider);
						
						//TODO: Only output if the change confidence is above the threshold.
						double thresholdValue = FraglightUiPlugin.getDefault().getPreferenceStore().getDouble(PreferenceConstants.P_THRESHOLD);

						//TODO: Do something with the change confidence.
						System.out.println("Change confidence for pointcut " + advElem + " is " + changeConfidence);
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
	private double calculateChangeConfidence(boolean captured,
			Set<Pattern<IntentionArc<IElement>>> patternSet)
			throws JavaModelException {

		double numerator = 0;
		for (Pattern<IntentionArc<IElement>> pattern : patternSet) {
			double quantityToFindTheAbsoluteValueOf = (captured ? 1 : 0)
					- pattern.getSimularity();
			double absoluteValue = Math.abs(quantityToFindTheAbsoluteValueOf);
			numerator += absoluteValue;
		}

		int denominator = patternSet.size();
		//TODO: Guard against the denominator being zero here.
		double changeConfidence = ((double) numerator) / denominator;
		return changeConfidence;
	}

	/**
	 * @return the analyzer
	 */
	public PointcutAnalyzer getAnalyzer() {
		return this.analyzer;
	}

	/**
	 * @param changeConfidenceThreshold the changeConfidenceThreshold to set
	 */
	public void setChangeConfidenceThreshold(double changeConfidenceThreshold) {
		this.changeConfidenceThreshold = changeConfidenceThreshold;
	}

	/**
	 * @return the changeConfidenceThreshold
	 */
	public double getChangeConfidenceThreshold() {
		return changeConfidenceThreshold;
	}
}