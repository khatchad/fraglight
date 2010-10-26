/**
 * 
 */
package edu.ohio_state.cse.khatchad.fraglight.ui;

import static java.lang.Math.abs;
import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import org.eclipse.mylyn.monitor.core.InteractionEvent.Kind;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.ajdt.core.javaelements.AdviceElement;
import org.eclipse.ajdt.mylyn.ui.AspectJStructureBridge;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.mylyn.context.core.AbstractContextStructureBridge;
import org.eclipse.mylyn.context.core.ContextChangeEvent;
import org.eclipse.mylyn.context.core.ContextCore;
import org.eclipse.mylyn.context.core.IInteractionContext;
import org.eclipse.mylyn.context.core.IInteractionElement;
import org.eclipse.mylyn.internal.context.core.ContextCorePlugin;
import org.eclipse.mylyn.internal.context.core.IActiveSearchOperation;
import org.eclipse.mylyn.internal.java.ui.JavaStructureBridge;
import org.eclipse.mylyn.internal.java.ui.search.AbstractJavaRelationProvider;
import org.eclipse.mylyn.monitor.core.InteractionEvent;

import ca.mcgill.cs.swevo.jayfx.model.IElement;
import edu.ohio_state.cse.khatchad.fraglight.core.analysis.PatternMatcher;
import edu.ohio_state.cse.khatchad.fraglight.core.analysis.PointcutAnalyzer;
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

	public enum PointcutAnalysisScope {
		PROJECT, WORKSPACE
	}

	/**
	 * A prediction consists of the advice predicted to change, the degree of
	 * confidence that we have in that advice changing, and join point addition
	 * that invoked the prediction, and the set of patterns that contributed to
	 * the prediction.
	 * 
	 * @author <a href="mailto:khatchad@cse.ohio-state.edu>Raffi
	 *         Khatchadourian</a>
	 * 
	 */
	public class Prediction {

		private AdviceElement advice;

		private IJavaElement affectingJoinPoint;

		public IJavaElement getAffectingJoinPoint() {
			return affectingJoinPoint;
		}

		private double changeConfidence;

		private Set<Pattern<IntentionArc<IElement>>> contributingPatterns;

		/**
		 * @param advice
		 * @param changeConfidence
		 */
		public Prediction(AdviceElement advice, double changeConfidence,
				IJavaElement affectingJoinPoint,
				Set<Pattern<IntentionArc<IElement>>> contributingPatterns) {
			this.advice = advice;
			this.changeConfidence = changeConfidence;
			this.affectingJoinPoint = affectingJoinPoint;
			this.contributingPatterns = contributingPatterns;
		}

		/**
		 * @return the advice
		 */
		public AdviceElement getAdvice() {
			return this.advice;
		}

		/**
		 * @return the changeConfidence
		 */
		public double getChangeConfidence() {
			return this.changeConfidence;
		}

		public Set<Pattern<IntentionArc<IElement>>> getContributingPatterns() {
			return this.contributingPatterns;
		}

		@Override
		public String toString() {
			return this.changeConfidence
					* 100
					+ "% confident that "
					+ this.advice
					+ " will change it's pointcut due to the affected join point shadow "
					+ this.affectingJoinPoint
					+ " based on the contributing patterns "
					+ this.contributingPatterns + ".";
		}
	}

	public static final double DEFAULT_HIGH_CHANGE_CONFIDENCE_THRESHOLD = 0.75;

	public static final double DEFAULT_LOW_CHANGE_CONFIDENCE_THRESHOLD = 0.25;

	public static final PointcutAnalysisScope DEFAULT_POINTCUT_ANALYSIS_SCOPE = PointcutAnalysisScope.WORKSPACE;

	private static final String ID = ID_GENERIC + ".pointcutchangeprediction";

	private static Logger logger = Logger
			.getLogger(PointcutChangePredictionProvider.class.getName());

	public static final String NAME = "may break"; //$NON-NLS-1$

	private PointcutAnalyzer analyzer = new PointcutAnalyzer(
			(short) FraglightUiPlugin.getDefault().getPreferenceStore()
					.getInt(PreferenceConstants.P_ANALYSIS_DEPTH));

	private double highChangeConfidenceThreshold = DEFAULT_HIGH_CHANGE_CONFIDENCE_THRESHOLD;

	private double lowChangeConfidenceThreshold = DEFAULT_LOW_CHANGE_CONFIDENCE_THRESHOLD;

	IProgressMonitor monitor = new NullProgressMonitor();

	private PointcutAnalysisScope pointcutAnalysisScope = DEFAULT_POINTCUT_ANALYSIS_SCOPE;

	private Set<Prediction> predictionSet = new LinkedHashSet<Prediction>();

	public PointcutAnalysisScope getPointcutAnalysisScope() {
		return pointcutAnalysisScope;
	}

	public PointcutChangePredictionProvider() {
		super(AspectJStructureBridge.CONTENT_TYPE, ID);
		FraglightUiPlugin.getDefault().setChangePredictionProvider(this);
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
			double absoluteValue = abs(quantityToFindTheAbsoluteValueOf);
			numerator += absoluteValue;
		}

		int denominator = patternSet.size();
		// TODO: Guard against the denominator being zero here.
		double changeConfidence = numerator / denominator;
		return changeConfidence;
	}

	private void calculateChangeConfidence(
			final IJavaElement affectingJoinPoint,
			Set<Pattern<IntentionArc<IElement>>> patternsMatchingJoinPoint,
			AdviceElement advElem) throws JavaModelException {
		logger.log(
				Level.INFO,
				"Finding the change confidence value for a particular pointcut.",
				advElem);

		Set<Pattern<IntentionArc<IElement>>> patternsDerivedFromPointcut = this.analyzer
				.getPointcutToPatternSetMap().get(advElem);

		logger.log(
				Level.INFO,
				"Found patterns derived from the pointcut. This is \\delta(PCE) in the paper.",
				patternsDerivedFromPointcut);

		logger.info("Calculating \\mu(jps) \\intersect \\delta(PCE).");
		Set<Pattern<IntentionArc<IElement>>> patternsToConsider = getPatternsToConsider(
				patternsMatchingJoinPoint, patternsDerivedFromPointcut);

		// decide if the new join point is captured by the
		// pointcut.
		boolean captured = AJUtil.isCapturedBy(affectingJoinPoint, advElem);
		logger.log(
				Level.INFO,
				"Decided if the new join point shadow is captured by the current pointcut.",
				captured);

		// calculate the change confidence.
		double changeConfidence = calculateChangeConfidence(captured,
				patternsToConsider);
		logger.log(Level.INFO, "Found the change confidence.", changeConfidence);

		refreshThresholds();

		// Only make a prediction if the change confidence is
		// within the threshold interval.
		if (changeConfidence >= this.highChangeConfidenceThreshold
				|| changeConfidence <= this.lowChangeConfidenceThreshold) {
			logger.info("The change confidence for this pointcut is in the threshold interval.");
			Prediction prediction = new Prediction(advElem, changeConfidence,
					affectingJoinPoint, patternsToConsider);
			logger.log(Level.INFO, "Adding prediction to prediction set.",
					prediction);
			this.predictionSet.add(prediction);

			this.updateDOI(prediction);
		}
	}

	private void refreshThresholds() {
		this.setHighChangeConfidenceThreshold(FraglightUiPlugin.getDefault()
				.getPreferenceStore()
				.getDouble(PreferenceConstants.P_HIGH_THRESHOLD));
		logger.log(Level.INFO, "Using the high prediction threshold value.",
				this.getHighChangeConfidenceThreshold());

		this.setLowChangeConfidenceThreshold(FraglightUiPlugin.getDefault()
				.getPreferenceStore()
				.getDouble(PreferenceConstants.P_LOW_THRESHOLD));
		logger.log(Level.INFO, "Using the low prediction threshold value.",
				this.getLowChangeConfidenceThreshold());
	}

	protected IInteractionElement convertSelectionToInteractionElement(
			Object object) {
		IInteractionElement node = null;
		if (object instanceof IInteractionElement) {
			node = (IInteractionElement) object;
		} else {
			AbstractContextStructureBridge bridge = ContextCore
					.getStructureBridge(object);
			String handle = bridge.getHandleIdentifier(object);
			node = ContextCore.getContextManager().getElement(handle);
		}
		return node;
	}

	protected IInteractionContext getContext() {
		return ContextCore.getContextManager().getActiveContext();
	}

	private void updateDOI(Prediction prediction) {

		if (!ContextCore.getContextManager().isContextActive()) {
			return;
		}

		double changeConfidence = prediction.getChangeConfidence();

		if (changeConfidence <= this.getLowChangeConfidenceThreshold()) {

			AdviceElement advice = prediction.getAdvice();

			logger.log(
					Level.INFO,
					"Prediction is lower than threshold. Making element less interesting.",
					new Object[] { this.getLowChangeConfidenceThreshold(),
							advice });

			IInteractionElement node = convertSelectionToInteractionElement(advice);

			logger.info("Originally, the interest level was "
					+ node.getInterest().getValue());

			//let's preserve uninterestingness here.
			boolean manipulated = ContextCorePlugin.getContextManager()
					.manipulateInterestForElement(node, false, false, true,
							ID, true);

			logger.info("Context " + (manipulated ? "was" : "was not")
					+ " manipulated.");

			logger.info("Now, the interest level is "
					+ node.getInterest().getValue());

		}

		else if (changeConfidence >= this.getHighChangeConfidenceThreshold()) {

			AdviceElement advice = prediction.getAdvice();

			logger.log(
					Level.INFO,
					"Prediction is higher than threshold. Making element more interesting.",
					new Object[] { this.getHighChangeConfidenceThreshold(),
							advice });

			IInteractionContext activeContext = ContextCore.getContextManager()
					.getActiveContext();

			IInteractionElement interactionElement = ContextCorePlugin
					.getContextManager().processInteractionEvent(advice,
							Kind.PREDICTION, ID, activeContext);

			logger.info("The interest level is now "
					+ interactionElement.getInterest().getValue());

		}
	}

	private void calculateChangeConfidenceForPointcuts(
			final IJavaElement affectingJoinPoint) throws JavaModelException {

		logger.info("Retrieving all available patterns associated with pointcuts.");
		Set<Pattern<IntentionArc<IElement>>> allPatterns = getAllPatterns();

		short maximumAnalysisDepth = (short) FraglightUiPlugin.getDefault()
				.getPreferenceStore()
				.getInt(PreferenceConstants.P_ANALYSIS_DEPTH);

		/*
		 * TODO: Instead of seeding with the old patterns, maybe I can use the
		 * old graph and build it incrementally.
		 */
		PatternMatcher matcher = new PatternMatcher(allPatterns,
				maximumAnalysisDepth);

		// let the matcher match patterns against code in the
		// projects:
		logger.info("Retrieving all previously analyzed pointcuts.");
		Set<AdviceElement> pointcuts = this.analyzer
				.getPointcutToPatternSetMap().keySet();

		logger.log(Level.INFO, "Obtained previously analyzed pointcuts.",
				pointcuts);
		try {
			logger.info("Matching old patterns with new base-code, I think.");
			matcher.analyze(pointcuts, this.monitor);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		logger.info("Finding patterns that match the new join point shadow. This is the set \\mu(jps) in the paper.");
		Set<Pattern<IntentionArc<IElement>>> patternsMatchingJoinPoint = findPatternsMatchingJoinPoint(
				affectingJoinPoint, allPatterns, matcher);

		logger.log(Level.INFO,
				"Calculating the change confidence value for all pointcuts.",
				pointcuts);
		for (AdviceElement advElem : pointcuts) {
			calculateChangeConfidence(affectingJoinPoint,
					patternsMatchingJoinPoint, advElem);
		}

	}

	@Override
	public void contextChanged(ContextChangeEvent event) {
		switch (event.getEventKind()) {
		case ACTIVATED: {

			logger.info("Context has been activated.");

			// register as a Java editor change listener.
			logger.info("Registering as a java editor change listener.");
			JavaCore.addElementChangedListener(this);

			// analyze pointcuts.
			Collection<? extends AdviceElement> toAnalyze = null;

			this.pointcutAnalysisScope = PointcutAnalysisScope
					.valueOf(FraglightUiPlugin.getDefault()
							.getPreferenceStore()
							.getString(PreferenceConstants.P_POINTCUT_SCOPE));

			if (this.pointcutAnalysisScope == PointcutAnalysisScope.WORKSPACE) {

				IWorkspace workspace = getWorkspace();
				try {
					logger.info("Building workspace fully.");
					// TODO:Check if the workspace is on auto-build.
					workspace.build(IncrementalProjectBuilder.FULL_BUILD,
							this.monitor);
				} catch (CoreException e) {
					throw new RuntimeException(e);
				}

				logger.info("Obtaining all advice in the workspace.");
				toAnalyze = AJUtil.extractAdviceElements(workspace);
			}

			else if (this.pointcutAnalysisScope == PointcutAnalysisScope.PROJECT) {
				// TODO: Fix this.
				for (IInteractionElement interactionElement : event
						.getContext().getAllElements()) {
					IJavaElement javaElement = JavaCore
							.create(interactionElement.getHandleIdentifier());
					System.out.println(javaElement);
				}
			}

			if (!toAnalyze.isEmpty()) {
				try {
					short maximumAnalysisDepth = (short) FraglightUiPlugin
							.getDefault().getPreferenceStore()
							.getInt(PreferenceConstants.P_ANALYSIS_DEPTH);

					this.analyzer.setMaximumAnalysisDepth(maximumAnalysisDepth);

					logger.log(
							Level.INFO,
							"Analyzing all bound pointcuts with particular maximum analysis depth.",
							new Object[] { toAnalyze, maximumAnalysisDepth });

					this.analyzer.analyze(toAnalyze, this.monitor);
				} catch (Exception e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				}
			}
			break;
		}

		case DEACTIVATED: {
			// deregister as a Java editor change listener.
			logger.info("Context has been deactivated.");
			logger.info("Deregistering as a java editor change listener.");
			JavaCore.removeElementChangedListener(this);

			logger.info("Clearing previous prediction set.");
			this.predictionSet.clear();

			// TODO: This is not correct.
			logger.info("Refreshing the change prediction view.");
			FraglightUiPlugin.getDefault().getChangePredictionView()
					.getViewer().refresh();

			// TODO: Only write the XML file when an XML file already exists
			// implies that the patterns have been rebuilt since last load.
			// try {
			// this.analyzer.writeXMLFile();
			// }
			// catch (IOException e) {
			// throw new RuntimeException("Error writing XML file.", e);
			// }
			// catch (CoreException e) {
			// throw new RuntimeException("Error writing XML file.", e);
			// }
		}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
	 */
	public void dispose() {
	}

	public void elementChanged(ElementChangedEvent event) {

		IJavaElementDelta delta = event.getDelta();
		try {
			handleDelta(delta.getAffectedChildren());
		} catch (JavaModelException e) {
		}
	}

	private Set<Pattern<IntentionArc<IElement>>> findPatternsMatchingJoinPoint(
			final IJavaElement affectingJoinPoint,
			Set<Pattern<IntentionArc<IElement>>> allPatterns,
			PatternMatcher matcher) {
		Set<Pattern<IntentionArc<IElement>>> patternsMatchingJoinPoint = new LinkedHashSet<Pattern<IntentionArc<IElement>>>();

		logger.log(Level.INFO, "Cycling through all patterns.", allPatterns);
		for (Pattern<IntentionArc<IElement>> pattern : allPatterns) {

			// get all java elements produced by the pattern.
			logger.log(Level.INFO,
					"Retrieving join point shadows produced by the pattern.",
					pattern);
			Set<IJavaElement> matchingJavaElementSet = matcher
					.getMatchingJavaElements(pattern);

			logger.log(Level.INFO,
					"Found join point shadows matching the pattern.",
					matchingJavaElementSet);

			if (matchingJavaElementSet.contains(affectingJoinPoint)) {
				logger.info("Pattern matches new join point shadow.");
				patternsMatchingJoinPoint.add(pattern);
			}
		}
		return patternsMatchingJoinPoint;
	}

	@Override
	protected void findRelated(IInteractionElement node, int degreeOfSeparation) {
		// TODO This method may very well be the one that eventually does the
		// work.
		// Given an interaction, will find pointcuts that may have broken.
		System.out.println("Find related was called.");
	}

	private Set<Pattern<IntentionArc<IElement>>> getAllPatterns() {
		Collection<Set<Pattern<IntentionArc<IElement>>>> allPatternSets = this.analyzer
				.getPointcutToPatternSetMap().values();

		logger.info("Flattening all sets of patterns to a single set of patterns.");
		Set<Pattern<IntentionArc<IElement>>> allPatterns = new LinkedHashSet<Pattern<IntentionArc<IElement>>>();
		for (Set<Pattern<IntentionArc<IElement>>> patternSet : allPatternSets) {
			allPatterns.addAll(patternSet);
		}
		return allPatterns;
	}

	public double getHighChangeConfidenceThreshold() {
		return this.highChangeConfidenceThreshold;
	}

	public double getLowChangeConfidenceThreshold() {
		return this.lowChangeConfidenceThreshold;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.mylyn.internal.context.core.AbstractRelationProvider#getName
	 * ()
	 */
	@Override
	public String getName() {
		return NAME;
	}

	private Set<Pattern<IntentionArc<IElement>>> getPatternsToConsider(
			Set<Pattern<IntentionArc<IElement>>> patternsMatchingJoinPoint,
			Set<Pattern<IntentionArc<IElement>>> patternsDerivedFromPointcut) {
		Set<Pattern<IntentionArc<IElement>>> patternsToConsider = new LinkedHashSet<Pattern<IntentionArc<IElement>>>(
				patternsDerivedFromPointcut);
		patternsToConsider.retainAll(patternsMatchingJoinPoint);
		return patternsToConsider;
	}

	/**
	 * @return the analyzer
	 */
	public PointcutAnalyzer getPointcutAnalyzer() {
		return this.analyzer;
	}

	public Set<Prediction> getPredictionSet() {
		return this.predictionSet;
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

	/**
	 * @param delta
	 * @throws JavaModelException
	 */
	private void handleDelta(IJavaElementDelta[] delta)
			throws JavaModelException {
		for (IJavaElementDelta child : delta) {
			if (child.getKind() == IJavaElementDelta.ADDED) {

				logger.log(Level.INFO, "Found new element added to editor.",
						child);

				final IJavaElement newJoinPointShadow = child.getElement();
				logger.log(Level.INFO,
						"Obtained new method execution join point shadow.",
						newJoinPointShadow);

				// this represents the case where a new method execution join
				// point is added.
				if (newJoinPointShadow.getElementType() == IJavaElement.METHOD
						&& newJoinPointShadow.isStructureKnown()) {

					logger.info("Found new method execution join point shadow.");

					logger.info("Clearing previous prediction set.");
					this.predictionSet.clear();

					// TODO: This is not correct.
					logger.info("Refreshing the change prediction view.");
					FraglightUiPlugin.getDefault().getChangePredictionView()
							.getViewer().refresh();

					// save the file.
					logger.info("Saving the file.");
					ICompilationUnit icu = Util
							.getCompilationUnit(newJoinPointShadow);
					icu.getBuffer().save(this.monitor, false);

					// Ensure that the building process is triggered.
					try {
						logger.info("Forcing the project to build.");
						newJoinPointShadow
								.getJavaProject()
								.getProject()
								.build(IncrementalProjectBuilder.INCREMENTAL_BUILD,
										this.monitor);
					} catch (CoreException e) {
						throw new RuntimeException(e);
					}

					// calculate the change confidence for every PCE.
					logger.info("Calculating the change confidence for every available pointcut.");
					calculateChangeConfidenceForPointcuts(newJoinPointShadow);

					// TODO: This is not correct.
					logger.info("Refreshing the change prediction view.");
					FraglightUiPlugin.getDefault().getChangePredictionView()
							.getViewer().refresh();
				}
			}

			else {
				// TODO: Remove this.
				System.out.println("Not added.");
			}

			handleDelta(child.getAffectedChildren());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface
	 * .viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}

	public void setHighChangeConfidenceThreshold(
			double highChangeConfidenceThreshold) {
		this.highChangeConfidenceThreshold = highChangeConfidenceThreshold;
	}

	public void setLowChangeConfidenceThreshold(
			double lowChangeConfidenceThreshold) {
		this.lowChangeConfidenceThreshold = lowChangeConfidenceThreshold;
	}
}