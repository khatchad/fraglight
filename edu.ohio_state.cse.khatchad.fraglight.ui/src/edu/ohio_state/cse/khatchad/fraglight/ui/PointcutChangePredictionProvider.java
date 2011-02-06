/**
 * 
 */
package edu.ohio_state.cse.khatchad.fraglight.ui;

import static java.lang.Math.abs;
import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.ajdt.core.javaelements.AJCodeElement;
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
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.mylyn.context.core.AbstractContextStructureBridge;
import org.eclipse.mylyn.context.core.ContextChangeEvent;
import org.eclipse.mylyn.context.core.ContextCore;
import org.eclipse.mylyn.context.core.IInteractionContext;
import org.eclipse.mylyn.context.core.IInteractionElement;
import org.eclipse.mylyn.internal.context.core.ContextCorePlugin;
import org.eclipse.mylyn.internal.context.core.IActiveSearchOperation;
import org.eclipse.mylyn.internal.java.ui.search.AbstractJavaRelationProvider;
import org.eclipse.mylyn.monitor.core.InteractionEvent.Kind;

import ca.mcgill.cs.swevo.jayfx.model.IElement;

import com.google.common.collect.Sets;

import edu.ohio_state.cse.khatchad.fraglight.core.analysis.JoinPointShadowDifferenceAnalyzer;
import edu.ohio_state.cse.khatchad.fraglight.core.analysis.PatternMatcher;
import edu.ohio_state.cse.khatchad.fraglight.core.analysis.PointcutAnalyzer;
import edu.ohio_state.cse.khatchad.fraglight.core.analysis.util.TimeCollector;
import edu.ohio_state.cse.khatchad.fraglight.core.graph.IntentionArc;
import edu.ohio_state.cse.khatchad.fraglight.core.graph.Pattern;
import edu.ohio_state.cse.khatchad.fraglight.core.util.AJUtil;
import edu.ohio_state.cse.khatchad.fraglight.core.util.Util;
import edu.ohio_state.cse.khatchad.fraglight.ui.Prediction.ChangeDirection;
import edu.ohio_state.cse.khatchad.fraglight.ui.Prediction.InterestDirection;
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

	private PredictionSet predictionSet = new PredictionSet();

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
	private static double calculateChangeConfidence(boolean captured,
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
		if (denominator == 0)
			return captured ? 1 : 0;

		else {
			double changeConfidence = numerator / denominator;
			return changeConfidence;
		}
	}

	private void calculateChangeConfidence(
			final IJavaElement affectingJoinPoint,
			Set<Pattern<IntentionArc<IElement>>> patternsMatchingJoinPoint,
			AdviceElement advElem, TimeCollector timeCollector)
			throws JavaModelException {

		timeCollector.start();
		logger.log(
				Level.INFO,
				"Finding the change confidence value for a particular pointcut.",
				advElem);
		timeCollector.stop();

		Set<Pattern<IntentionArc<IElement>>> patternsDerivedFromPointcut = getPatternsDerivedFromPointcut(advElem);

		timeCollector.start();
		logger.log(
				Level.INFO,
				"Found patterns derived from the pointcut. This is \\delta(PCE) in the paper.",
				patternsDerivedFromPointcut);

		logger.info("Calculating \\mu(jps) \\intersect \\delta(PCE).");
		timeCollector.stop();

		Set<Pattern<IntentionArc<IElement>>> patternsToConsider = Sets
				.intersection(patternsMatchingJoinPoint,
						patternsDerivedFromPointcut);

		// decide if the new join point is captured by the
		// pointcut.
		boolean captured = isCapturedBy(affectingJoinPoint, advElem);

		timeCollector.start();
		logger.log(
				Level.INFO,
				"Decided if the new join point shadow is captured by the current pointcut.",
				captured);
		timeCollector.stop();

		// calculate the change confidence.
		double changeConfidence = calculateChangeConfidence(captured,
				patternsToConsider);

		timeCollector.start();
		logger.log(Level.INFO, "Found the change confidence.", changeConfidence);

		refreshThresholds();
		timeCollector.stop();

		// Only make a prediction if the change confidence is
		// within the threshold interval.
		if (changeConfidence >= this.highChangeConfidenceThreshold
				|| changeConfidence <= this.lowChangeConfidenceThreshold) {

			timeCollector.start();
			logger.info("The change confidence for this pointcut is within the threshold interval.");
			timeCollector.stop();

			Prediction prediction = new Prediction(advElem, changeConfidence,
					ChangeDirection.valueOf(captured), affectingJoinPoint,
					patternsToConsider);

			timeCollector.start();
			logger.log(Level.INFO, "Adding prediction to prediction set.",
					prediction);
			timeCollector.stop();

			this.predictionSet.add(prediction);

			this.updateDOI(prediction, timeCollector);
		}
	}

	protected boolean isCapturedBy(IJavaElement affectingJoinPoint,
			AdviceElement advElem) throws JavaModelException {
		return AJUtil.isCapturedBy(affectingJoinPoint, advElem);
	}

	protected Set<Pattern<IntentionArc<IElement>>> getPatternsDerivedFromPointcut(
			AdviceElement advElem) {
		Map<AdviceElement, Set<Pattern<IntentionArc<IElement>>>> pointcutToPatternSetMap = this.analyzer
				.getPointcutToPatternSetMap();
		Set<Pattern<IntentionArc<IElement>>> patternsDerivedFromPointcut = pointcutToPatternSetMap
				.get(advElem);
		return patternsDerivedFromPointcut;
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

	private static IInteractionElement convertSelectionToInteractionElement(
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

	private void updateDOI(Prediction prediction, TimeCollector timeCollector) {

		if (!ContextCore.getContextManager().isContextActive()) {
			return;
		}

		AdviceElement advice = prediction.getAdvice();
		IInteractionElement node = convertSelectionToInteractionElement(advice);

		final double originalInterestLevel = node == null ? 0 : node
				.getInterest().getValue();
		prediction.setOriginalInterestLevel(originalInterestLevel);

		timeCollector.start();
		logger.info("Originally, the interest level was "
				+ originalInterestLevel);
		timeCollector.stop();

		double changeConfidence = prediction.getChangeConfidence();

		if (changeConfidence <= this.getLowChangeConfidenceThreshold()) {

			timeCollector.start();
			logger.log(
					Level.INFO,
					"Prediction is lower than threshold. Making element less interesting.",
					new Object[] { this.getLowChangeConfidenceThreshold(),
							advice });
			timeCollector.stop();

			prediction.setInterestDirection(InterestDirection.LESS_INTERESTING);

			// let's preserve uninterestingness here.
			boolean manipulated = ContextCorePlugin.getContextManager()
					.manipulateInterestForElement(node, false, false, true, ID,
							true);

			final double newInterestLevel = node.getInterest().getValue();

			prediction.setNewInterestLevel(newInterestLevel);

			timeCollector.start();
			logger.info("Context " + (manipulated ? "was" : "was not")
					+ " manipulated.");
			logger.info("Now, the interest level is " + newInterestLevel);
			timeCollector.stop();
		}

		else if (changeConfidence >= this.getHighChangeConfidenceThreshold()) {

			timeCollector.start();
			logger.log(
					Level.INFO,
					"Prediction is higher than threshold. Making element more interesting.",
					new Object[] { this.getHighChangeConfidenceThreshold(),
							advice });
			timeCollector.stop();

			prediction.setInterestDirection(InterestDirection.MORE_INTERESTING);

			IInteractionContext activeContext = ContextCore.getContextManager()
					.getActiveContext();

			IInteractionElement interactionElement = ContextCorePlugin
					.getContextManager().processInteractionEvent(advice,
							Kind.PREDICTION, ID, activeContext);

			final float newInterestLevel = interactionElement.getInterest()
					.getValue();

			prediction.setNewInterestLevel(newInterestLevel);

			timeCollector.start();
			logger.info("The interest level is now " + newInterestLevel);
			timeCollector.stop();
		}
	}

	private void calculateChangeConfidenceForPointcuts(
			final IJavaElement affectingJoinPoint,
			Collection<AdviceElement> pointcuts, TimeCollector timeCollector)
			throws JavaModelException {

		timeCollector.start();
		logger.info("Retrieving all available patterns associated with pointcuts.");
		timeCollector.stop();

		Set<Pattern<IntentionArc<IElement>>> allPatterns = getAllPatterns(timeCollector);

		short maximumAnalysisDepth = (short) FraglightUiPlugin.getDefault()
				.getPreferenceStore()
				.getInt(PreferenceConstants.P_ANALYSIS_DEPTH);

		/*
		 * TODO: Instead of seeding with the old patterns, maybe I can use the
		 * old graph and build it incrementally.
		 */
		PatternMatcher matcher = createPatternMatcher(allPatterns,
				maximumAnalysisDepth, timeCollector);

		timeCollector.start();
		logger.info("Matching old patterns with new base-code, I think.");
		timeCollector.stop();

		try {
			// let the matcher match patterns against code in the projects:
			matcher.analyze(pointcuts, this.monitor, timeCollector);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		timeCollector.start();
		logger.info("Finding patterns that match the new join point shadow. This is the set \\mu(jps) in the paper.");
		timeCollector.stop();

		Map<AdviceElement, Set<Pattern<IntentionArc<IElement>>>> pointcutToPatternsMatchingJoinPointMap = findPatternsMatchingJoinPoint(
				affectingJoinPoint, pointcuts, matcher, timeCollector);

		timeCollector.start();
		logger.log(Level.INFO,
				"Calculating the change confidence value for all pointcuts.",
				pointcuts);
		timeCollector.stop();

		for (AdviceElement advElem : pointcutToPatternsMatchingJoinPointMap
				.keySet()) {
			calculateChangeConfidence(affectingJoinPoint,
					pointcutToPatternsMatchingJoinPointMap.get(advElem),
					advElem, timeCollector);
		}

	}

	protected PatternMatcher createPatternMatcher(
			Set<Pattern<IntentionArc<IElement>>> allPatterns,
			short maximumAnalysisDepth, TimeCollector timeCollector) {
		PatternMatcher matcher = new PatternMatcher(allPatterns,
				maximumAnalysisDepth);
		return matcher;
	}

	protected Set<AdviceElement> retreivePreviouslyAnalyzedPointcuts(
			TimeCollector timeCollector) {
		timeCollector.start();
		logger.info("Retrieving all previously analyzed pointcuts.");
		timeCollector.stop();

		Set<AdviceElement> pointcuts = this.analyzer
				.getPointcutToPatternSetMap().keySet();

		timeCollector.start();
		logger.log(Level.INFO, "Obtained previously analyzed pointcuts.",
				pointcuts);
		timeCollector.stop();

		return pointcuts;
	}

	@Override
	public void contextChanged(ContextChangeEvent event) {
		switch (event.getEventKind()) {
		case ACTIVATED: {

			logger.info("Context has been activated.");

			IJavaElement input = EditorUtility.getActiveEditorJavaInput();
			if (input != null) {
				ICompilationUnit icu = Util.getCompilationUnit(input);
				CompilationUnit ast = Util.getCompilationUnit(icu, monitor);
				this.typeToAST.put(icu, ast);
				logger.info("Stored initial AST for " + icu.getElementName()
						+ ".");
			}

			// register as a Java editor change listener.
			logger.info("Registering as a java editor change listener.");
			JavaCore.addElementChangedListener(this,
					ElementChangedEvent.POST_RECONCILE);

			analyzePointcuts(new TimeCollector());
			break;
		}

		case DEACTIVATED: {

			logger.info("Clearing type to AST map.");
			this.typeToAST.clear();

			// de-register as a Java editor change listener.
			logger.info("Context has been deactivated.");
			logger.info("De-registering as a java editor change listener.");
			JavaCore.removeElementChangedListener(this);

			clearPreviousPredictions(new TimeCollector());

			// TODO: This is not correct.
			// logger.info("Refreshing the change prediction view.");
			// FraglightUiPlugin.getDefault().getChangePredictionView()
			// .getViewer().refresh();

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

	private void analyzePointcuts(TimeCollector timeCollector) {
		// analyze pointcuts.
		Collection<? extends AdviceElement> toAnalyze = null;

		this.pointcutAnalysisScope = PointcutAnalysisScope
				.valueOf(FraglightUiPlugin.getDefault().getPreferenceStore()
						.getString(PreferenceConstants.P_POINTCUT_SCOPE));

		if (this.pointcutAnalysisScope == PointcutAnalysisScope.WORKSPACE) {

			IWorkspace workspace = getWorkspace();
			try {

				timeCollector.start();
				logger.info("Building workspace fully.");
				// TODO:Check if the workspace is on auto-build.
				workspace.build(IncrementalProjectBuilder.FULL_BUILD,
						this.monitor);
				timeCollector.stop();

			} catch (CoreException e) {
				throw new RuntimeException(e);
			}

			timeCollector.start();
			logger.info("Obtaining all advice in the workspace.");
			timeCollector.stop();

			toAnalyze = AJUtil.extractAdviceElements(workspace);
		}

		else if (this.pointcutAnalysisScope == PointcutAnalysisScope.PROJECT) {
			// TODO: Fix this.
			/*
			 * for (IInteractionElement interactionElement : event
			 * .getContext().getAllElements()) { IJavaElement javaElement =
			 * JavaCore .create(interactionElement.getHandleIdentifier());
			 * System.out.println(javaElement); }
			 */
			throw new IllegalStateException(PointcutAnalysisScope.PROJECT
					+ " analysis scope is not supported at this time.");
		}

		analyzePointcuts(toAnalyze, timeCollector);
	}

	public void analyzePointcuts(Collection<? extends AdviceElement> toAnalyze,
			TimeCollector timeCollector) {
		if (!toAnalyze.isEmpty()) {
			try {
				short maximumAnalysisDepth = (short) FraglightUiPlugin
						.getDefault().getPreferenceStore()
						.getInt(PreferenceConstants.P_ANALYSIS_DEPTH);

				this.analyzer.setMaximumAnalysisDepth(maximumAnalysisDepth);

				timeCollector.start();
				logger.log(
						Level.INFO,
						"Analyzing all bound pointcuts with particular maximum analysis depth.",
						new Object[] { toAnalyze, maximumAnalysisDepth });
				timeCollector.stop();

				this.analyzer.analyze(toAnalyze, this.monitor, timeCollector);
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException(e);
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
		IJavaElementDelta[] affectedChildren = delta.getAffectedChildren();

		if (affectedChildren.length == 0
				&& (delta.getFlags() & IJavaElementDelta.F_AST_AFFECTED) != 0
				&& delta.getCompilationUnitAST().getProblems().length == 0) {

			IJavaElement element = delta.getElement();
			ICompilationUnit icu = Util.getCompilationUnit(element);

			if (!typeToAST.containsKey(icu)) {
				CompilationUnit ast = Util
						.getCompilationUnit(icu, this.monitor);
				typeToAST.put(icu, ast);
				logger.info("Stored initial AST for " + icu.getElementName()
						+ ".");
			}

			else {

				CompilationUnit originalAST = typeToAST.get(icu);
				logger.log(Level.INFO,
						"Retrieved original AST for " + icu.getElementName()
								+ ".", originalAST);

				CompilationUnit newAST = Util.getCompilationUnit(icu,
						this.monitor);
				logger.log(Level.INFO,
						"Retrieved new AST for " + icu.getElementName() + ".",
						newAST);

				// update map with new AST.
				this.typeToAST.put(icu, newAST);
				logger.info("Updated stored AST for " + icu.getElementName()
						+ ".");

				// now we need to find out the difference. It should be a code
				// element.
				IJavaElement newJoinPointShadow = extractNewJoinPointShadow(
						originalAST, newAST);
				if (newJoinPointShadow != null) {
					assertIsAJCodeElement(newJoinPointShadow); // sanity check.
					logger.log(Level.INFO,
							"Found new code-level join point shadow.",
							newJoinPointShadow);
					try {
						processNewJoinPointShadow(newJoinPointShadow,
								new TimeCollector());
					} catch (JavaModelException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						throw new RuntimeException(e);
					}
				}
			}
		}

		try {
			handleDelta(affectedChildren);
		} catch (JavaModelException e) {
			// TODO: Handle properly.
			e.printStackTrace();
			throw new RuntimeException(e);
		}

	}

	private static void assertIsAJCodeElement(IJavaElement newJoinPointShadow) {
		if (!(newJoinPointShadow instanceof AJCodeElement))
			throw new IllegalStateException(
					"Found illegal type of join point element: "
							+ newJoinPointShadow.getClass());
	}

	private IJavaElement extractNewJoinPointShadow(CompilationUnit originalAST,
			CompilationUnit newAST) {
		JoinPointShadowDifferenceAnalyzer joinPointShadowDifferenceAnalyzer = new JoinPointShadowDifferenceAnalyzer();
		boolean match = joinPointShadowDifferenceAnalyzer.safeSubtreeMatch(
				originalAST, newAST);
		if (!match
				&& !joinPointShadowDifferenceAnalyzer.getNewJoinPointShadows()
						.isEmpty())
			if (joinPointShadowDifferenceAnalyzer.getNewJoinPointShadows()
					.size() > 1)
				// TODO: Deal with this later.
				throw new IllegalStateException(
						"Found multiple new join points.");
			else
				return joinPointShadowDifferenceAnalyzer
						.getNewJoinPointShadows().iterator().next();
		return null;
	}

	private Map<ICompilationUnit, CompilationUnit> typeToAST = new HashMap<ICompilationUnit, CompilationUnit>();

	protected Map<AdviceElement, Set<Pattern<IntentionArc<IElement>>>> findPatternsMatchingJoinPoint(
			final IJavaElement affectingJoinPoint,
			Collection<AdviceElement> pointcuts, PatternMatcher matcher,
			TimeCollector timeCollector) {

		Map<AdviceElement, Set<Pattern<IntentionArc<IElement>>>> ret = new LinkedHashMap<AdviceElement, Set<Pattern<IntentionArc<IElement>>>>();

		for (AdviceElement advElem : pointcuts) {

			Set<Pattern<IntentionArc<IElement>>> allPatterns = this.analyzer
					.getPointcutToPatternSetMap().get(advElem);

			timeCollector.start();
			logger.log(Level.INFO, "Cycling through all patterns.", allPatterns);
			timeCollector.stop();

			for (Pattern<IntentionArc<IElement>> pattern : allPatterns) {

				// get all java elements produced by the pattern.
				timeCollector.start();
				logger.log(
						Level.INFO,
						"Retrieving join point shadows produced by the pattern.",
						pattern);
				timeCollector.stop();

				Set<IJavaElement> matchingJavaElementSet = matcher
						.getMatchingJavaElements(pattern);

				timeCollector.start();
				logger.log(Level.INFO,
						"Found join point shadows matching the pattern.",
						matchingJavaElementSet);
				timeCollector.stop();

				if (matchingJavaElementSet.contains(affectingJoinPoint)) {

					timeCollector.start();
					logger.info("Pattern matches new join point shadow.");
					timeCollector.stop();

					if (!ret.containsKey(advElem))
						ret.put(advElem,
								new LinkedHashSet<Pattern<IntentionArc<IElement>>>());

					Set<Pattern<IntentionArc<IElement>>> patternsMatchingJoinPoint = ret
							.get(advElem);
					patternsMatchingJoinPoint.add(pattern);
				}
			}
		}

		return ret;
	}

	@Override
	protected void findRelated(IInteractionElement node, int degreeOfSeparation) {
		// TODO This method may very well be the one that eventually does the
		// work.
		// Given an interaction, will find pointcuts that may have broken.
		System.out.println("Find related was called.");
	}

	private Set<Pattern<IntentionArc<IElement>>> getAllPatterns(
			TimeCollector timeCollector) {
		Collection<Set<Pattern<IntentionArc<IElement>>>> allPatternSets = this.analyzer
				.getPointcutToPatternSetMap().values();

		timeCollector.start();
		logger.info("Flattening all sets of patterns to a single set of patterns.");
		timeCollector.stop();

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

	private static Set<Pattern<IntentionArc<IElement>>> getPatternsToConsider(
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

	public PredictionSet getPredictionSet() {
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
			switch (child.getKind()) {
			case IJavaElementDelta.ADDED: {

				logger.log(Level.INFO, "Found new element added to editor.",
						child);

				final IJavaElement newJoinPointShadow = child.getElement();
				logger.log(Level.INFO, "Obtained new source code element.",
						newJoinPointShadow);

				// this represents the case where a new method execution join
				// point is added.
				if (newJoinPointShadow.getElementType() == IJavaElement.METHOD
						&& newJoinPointShadow.isStructureKnown()) {

					logger.info("Found new method execution join point shadow.");
					processNewJoinPointShadow(newJoinPointShadow,
							new TimeCollector());
				}
				break;
			}
			}

			handleDelta(child.getAffectedChildren());
		}
	}

	public void processNewJoinPointShadow(
			final IJavaElement newJoinPointShadow, TimeCollector timeCollector)
			throws JavaModelException {
		clearPreviousPredictions(timeCollector);

		// save the file.
		saveAssociatedFile(newJoinPointShadow);

		// Ensure that the building process is triggered.
		buildAssociatedProject(newJoinPointShadow);

		// calculate the change confidence for every PCE.
		calculateChangeConfidence(newJoinPointShadow, timeCollector);
	}

	protected void calculateChangeConfidence(
			final IJavaElement newJoinPointShadow, TimeCollector timeCollector)
			throws JavaModelException {

		timeCollector.start();
		logger.info("Calculating the change confidence for every available pointcut.");
		timeCollector.stop();

		Set<AdviceElement> pointcuts = retreivePreviouslyAnalyzedPointcuts(timeCollector);
		calculateChangeConfidenceForPointcuts(newJoinPointShadow, pointcuts,
				timeCollector);
	}

	protected void buildAssociatedProject(final IJavaElement newJoinPointShadow) {
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
	}

	protected void saveAssociatedFile(final IJavaElement newJoinPointShadow)
			throws JavaModelException {
		logger.info("Saving the file.");
		ICompilationUnit icu = Util.getCompilationUnit(newJoinPointShadow);
		icu.getBuffer().save(this.monitor, false);
	}

	protected void clearPreviousPredictions(TimeCollector timeCollector) {
		timeCollector.start();
		logger.info("Clearing previous prediction set.");
		this.predictionSet.clear();
		timeCollector.stop();
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