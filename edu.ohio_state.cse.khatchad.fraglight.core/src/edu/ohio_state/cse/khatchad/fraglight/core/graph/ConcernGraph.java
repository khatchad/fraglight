/**
 * 
 */
package edu.ohio_state.cse.khatchad.fraglight.core.graph;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.drools.FactHandle;
import org.drools.QueryResult;
import org.drools.QueryResults;
import org.drools.RuleBase;
import org.drools.RuleBaseFactory;
import org.drools.WorkingMemory;
import org.drools.compiler.DroolsParserException;
import org.drools.compiler.PackageBuilder;
import org.drools.rule.Package;
import org.eclipse.ajdt.core.javaelements.AJCodeElement;
import org.eclipse.ajdt.core.javaelements.AdviceElement;
import org.eclipse.ajdt.core.javaelements.AspectElement;
import org.eclipse.ajdt.core.javaelements.IAJCodeElement;
import org.eclipse.ajdt.core.model.AJRelationship;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchPattern;

import ca.mcgill.cs.swevo.jayfx.ConversionException;
import ca.mcgill.cs.swevo.jayfx.JayFX;
import ca.mcgill.cs.swevo.jayfx.JayFXException;
import ca.mcgill.cs.swevo.jayfx.model.IElement;
import ca.mcgill.cs.swevo.jayfx.model.MethodElement;
import ca.mcgill.cs.swevo.jayfx.model.Relation;
import edu.ohio_state.cse.khatchad.fraglight.core.analysis.Constants;
import edu.ohio_state.cse.khatchad.fraglight.core.analysis.PointcutProcessor;
import edu.ohio_state.cse.khatchad.fraglight.core.analysis.model.JoinPointType;
import edu.ohio_state.cse.khatchad.fraglight.core.analysis.util.TimeCollector;
import edu.ohio_state.cse.khatchad.fraglight.core.util.AJUtil;
import edu.ohio_state.cse.khatchad.fraglight.core.util.SearchEngineUtil;

/**
 * @author raffi
 * 
 */
public class ConcernGraph {

	private Set<IntentionNode<IElement>> nodeSet = new LinkedHashSet<IntentionNode<IElement>>();

	private Map<IElement, IntentionNode<IElement>> elementToNodeMap = new LinkedHashMap<IElement, IntentionNode<IElement>>();

	private void buildNodes(IProgressMonitor monitor) {
		monitor.beginTask("Building Intention Nodes", database.getAllElements()
				.size());

		for (final IElement elem : database.getAllElements()) {
			IntentionNode<IElement> node = new IntentionNode<IElement>(elem);
			this.nodeSet.add(node);
			this.elementToNodeMap.put(elem, node);
			monitor.worked(1);
		}
		monitor.done();
	}

	public Set<GraphElement<IElement>> flatten() {
		final Set<GraphElement<IElement>> ret = new LinkedHashSet<GraphElement<IElement>>();
		for (final IntentionNode<IElement> node : this.getNodes()) {
			ret.add(node);
			for (final IntentionArc<IElement> edge : node.getArcs())
				ret.add(edge);
		}
		return ret;
	}

	private void buildArcs(IProgressMonitor monitor) {

		monitor.beginTask("Building Intention Arcs", this.nodeSet.size());
		Set<IntentionNode<IElement>> nodesToAdd = new LinkedHashSet<IntentionNode<IElement>>();
		for (IntentionNode<IElement> node : this.nodeSet) {
			// now make the edges.
			nodesToAdd.addAll(this.makeArcs(node, database, Relation.GETS));
			nodesToAdd.addAll(this.makeArcs(node, database, Relation.SETS));
			nodesToAdd.addAll(this.makeArcs(node, database, Relation.CALLS));
			nodesToAdd
					.addAll(this.makeArcs(node, database, Relation.OVERRIDES));
			nodesToAdd.addAll(this.makeArcs(node, database,
					Relation.IMPLEMENTS_METHOD));
			nodesToAdd.addAll(this.makeArcs(node, database,
					Relation.DECLARES_METHOD));
			nodesToAdd.addAll(this.makeArcs(node, database,
					Relation.DECLARES_FIELD));
			nodesToAdd.addAll(this.makeArcs(node, database,
					Relation.DECLARES_TYPE));
			nodesToAdd.addAll(this.makeArcs(node, database,
					Relation.EXTENDS_CLASS));
			nodesToAdd.addAll(this.makeArcs(node, database,
					Relation.EXTENDS_INTERFACES));
			nodesToAdd.addAll(this.makeArcs(node, database,
					Relation.IMPLEMENTS_INTERFACE));
			nodesToAdd.addAll(this.makeArcs(node, database, Relation.CONTAINS));
			nodesToAdd
					.addAll(this.makeArcs(node, database, Relation.ANNOTATES));
			nodesToAdd.addAll(this.makeArcs(node, database, Relation.ADVISES));

			monitor.worked(1);
		}
		this.nodeSet.addAll(nodesToAdd);
		monitor.done();
	}

	private JayFX database;

	private WorkingMemory workingMemory;

	private Object maximumAnalysisDepth;

	/**
	 * 
	 */
	private static final String RULES_FILE = "/rules/NodeRules.drl";

	public ConcernGraph(final JayFX database, final IProgressMonitor monitor) {
		this.database = database;
		buildNodes(new SubProgressMonitor(monitor, -1));
		buildArcs(new SubProgressMonitor(monitor, -1));
	}
	
	public void enableElementsAccordingTo(final AdviceElement advisingElement,
			final IProgressMonitor monitor) throws JavaModelException,
			ConversionException {

		this.resetAllElements(new SubProgressMonitor(monitor, -1));
		
		Collection<IJavaElement> advisedJavaElements = AJUtil.getAdvisedJavaElements(advisingElement);

		monitor.beginTask("Enabling elements according to advice pointcut.",
				advisedJavaElements.size());

		for (final IJavaElement advisedElement: advisedJavaElements) {
			enableElementsAccordingTo(advisedElement, new SubProgressMonitor(
					monitor, -1));
			monitor.worked(1);
		}
		monitor.done();
	}
	
	public void enableElementsAccordingTo(final Collection<? extends AdviceElement> advisingElementCol,
			final IProgressMonitor monitor) throws JavaModelException,
			ConversionException {

		this.resetAllElements(new SubProgressMonitor(monitor, -1));
		
		Collection<IJavaElement> advisedJavaElements = new LinkedHashSet<IJavaElement>();
		for (AdviceElement advisingElement : advisingElementCol )
			advisedJavaElements.addAll(AJUtil.getAdvisedJavaElements(advisingElement));

		monitor.beginTask("Enabling elements according to advice pointcut.",
				advisedJavaElements.size());

		for (final IJavaElement advisedElement: advisedJavaElements) {
			enableElementsAccordingTo(advisedElement, new SubProgressMonitor(
					monitor, -1));
			monitor.worked(1);
		}
		monitor.done();
	}

	/**
	 * @param target
	 * @param monitor
	 * @throws ConversionException
	 * @throws JavaModelException
	 */
	private void enableElementsAccordingTo(IJavaElement advisedElement,
			IProgressMonitor monitor) throws ConversionException,
			JavaModelException {

		switch (advisedElement.getElementType()) {

			case IJavaElement.METHOD: {
				final IMethod meth = (IMethod) advisedElement;

				//No advice for now.
				if (meth.getParent() instanceof AspectElement)
					break;

				final IElement toEnableElement = this.database
						.convertToElement(meth);

				if (toEnableElement == null)
					throw new IllegalStateException("In trouble!");

				if (!this.elementToNodeMap.containsKey(toEnableElement)) {
					IntentionNode<IElement> newNode = new IntentionNode<IElement>(
							toEnableElement);
					this.elementToNodeMap.put(toEnableElement, newNode);
					this.nodeSet.add(newNode);
					//					throw new IllegalStateException(
					//							"No target node found for element "
					//									+ toEnableElement);
				}

				IntentionNode<IElement> toEnableNode = this.elementToNodeMap
						.get(toEnableElement);

				toEnableNode.enable();

				break;
			}

			case IJavaElement.TYPE: {

				// its a default ctor.
				final IType type = (IType) advisedElement;

				for (final IMethod meth : type.getMethods())

					if (meth.isConstructor()
							&& meth.getParameterNames().length == 0) {

						final IElement toEnableElement = this.database
								.convertToElement(meth);

						if (toEnableElement == null)
							throw new IllegalStateException("In trouble!");

						IntentionNode<IElement> toEnableNode = this.elementToNodeMap
								.get(toEnableElement);
						toEnableNode.enable();

					}

				break;

			}

			case IJavaElement.LOCAL_VARIABLE: {

				// its an aspect element.
				if (!(advisedElement instanceof IAJCodeElement))
					throw new IllegalStateException("Something is screwy here.");

				final IAJCodeElement ajElem = (IAJCodeElement) advisedElement;
				JoinPointType joinPointType = AJUtil.getJoinPointType(ajElem);
				IJavaElement source = advisedElement.getParent();
				String targetString = ajElem.getElementName();

				switch (joinPointType) {
					case FIELD_GET: {
						this.enableElementsAccordingToFieldGet(source,
								targetString, monitor);
						break;
					}

					case FIELD_SET: {
						this.enableElementsAccordingToFieldSet(source,
								targetString, monitor);
						break;
					}

					case METHOD_CALL: {
						this.enableElementsAccordingToMethodCall(source,
								targetString, monitor);
						break;
					}

					case CONSTRUCTOR_CALL: {
						this.enableElementsAccordingToConstructorCall(source,
								targetString, monitor);
						break;
					}

					case EXCEPTION_HANDLER: {
						System.out
								.println("Encountered handler-based advice, not sure how to deal with this yet. Nothing enabled.");
						break;
					}
				}

				break;
			}
			default:
				throw new IllegalStateException(
						"Unexpected relationship target type: "
								+ advisedElement.getElementType());
		}
	}

	private void enableElementsAccordingToFieldGet(IJavaElement parent,
			String targetString, IProgressMonitor monitor)
			throws ConversionException {
		String fieldNameTargetString = transformTargetStringToFieldName(targetString);
		this.enableElementsAccordingToRelation(parent, fieldNameTargetString,
				IJavaSearchConstants.FIELD, Relation.GETS, monitor);
	}

	private void enableElementsAccordingToFieldSet(IJavaElement parent,
			String targetString, IProgressMonitor monitor)
			throws ConversionException {
		String fieldNameTargetString = transformTargetStringToFieldName(targetString);
		this.enableElementsAccordingToRelation(parent, fieldNameTargetString,
				IJavaSearchConstants.FIELD, Relation.SETS, monitor);
	}

	private void enableElementsAccordingToConstructorCall(IJavaElement parent,
			String targetString, IProgressMonitor monitor)
			throws ConversionException {
		String constructorNameTargetString = transformTargetStringToConstructorName(targetString);
		this.enableElementsAccordingToRelation(parent,
				constructorNameTargetString, IJavaSearchConstants.CONSTRUCTOR,
				Relation.CALLS, monitor);
	}

	private void enableElementsAccordingToMethodCall(IJavaElement parent,
			String targetString, IProgressMonitor monitor)
			throws ConversionException {
		String methodNameTargetString = transformTargetStringToMethodName(targetString);
		this.enableElementsAccordingToRelation(parent, methodNameTargetString,
				IJavaSearchConstants.METHOD, Relation.CALLS, monitor);
	}

	private void enableElementsAccordingToRelation(IJavaElement parent,
			String targetString, final int javaSearchConstant,
			Relation relation, IProgressMonitor monitor)
			throws ConversionException {

		final SearchPattern pattern = SearchPattern.createPattern(targetString,
				javaSearchConstant, IJavaSearchConstants.DECLARATIONS,
				SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE);

		final Collection<SearchMatch> results = SearchEngineUtil.search(pattern,
				SearchEngine.createWorkspaceScope(), monitor);

		IElement sourceElement = this.database.convertToElement(parent);
		if (!this.elementToNodeMap.containsKey(sourceElement)) {
			IntentionNode<IElement> newNode = new IntentionNode<IElement>(
					sourceElement);
			this.elementToNodeMap.put(sourceElement, newNode);
			this.nodeSet.add(newNode);
			//			throw new IllegalStateException("Can't find node for IElement "
			//					+ sourceElement);
		}
		IntentionNode<IElement> sourceNode = this.elementToNodeMap
				.get(sourceElement);

		for (final SearchMatch match : results) {

			final IElement targetElement = this.database
					.convertToElement((IJavaElement) match.getElement());

			//find the edge connecting the source to the target and enable it.
			if (!this.elementToNodeMap.containsKey(targetElement)) {
				IntentionNode<IElement> newNode = new IntentionNode<IElement>(
						targetElement);
				this.elementToNodeMap.put(targetElement, newNode);
				this.nodeSet.add(newNode);
				//				throw new IllegalStateException("Can't find node for IElement "
				//						+ targetElement);
			}
			IntentionNode<IElement> targetNode = this.elementToNodeMap
					.get(targetElement);

			IntentionArc<IElement> arcToEnable = sourceNode.getArc(targetNode,
					relation);

			if (arcToEnable == null) {
				//				throw new IllegalStateException(
				//						"Can't find arc for target node " + targetNode
				//								+ " and relation " + relation);
				arcToEnable = new IntentionArc<IElement>(sourceNode,
						targetNode, relation);
				sourceNode.addArc(arcToEnable);
			}
			arcToEnable.enable();
		}
	}

	private static String transformTargetStringToFieldName(
			final String targetString) {
		StringBuilder ret = new StringBuilder(targetString);
		ret.delete(0, ret.indexOf(" ") + 1);
		ret.deleteCharAt(ret.length() - 1);
		return ret.toString();
	}

	private static String transformTargetStringToMethodName(
			final String targetString) {
		StringBuilder ret = new StringBuilder(targetString);
		ret.delete(0, ret.indexOf(" ") + 1);
		ret.deleteCharAt(ret.length() - 1);
		return ret.toString();
	}

	private static String transformTargetStringToConstructorName(
			final String targetString) {
		String methodTargetString = transformTargetStringToMethodName(targetString);
		StringBuilder ret = new StringBuilder(methodTargetString);
		final int initPos = ret.indexOf(Constants.INIT_STRING);
		ret.delete(initPos, initPos + Constants.INIT_STRING.length());
		return ret.toString();
	}

	private void resetAllElements(IProgressMonitor monitor) {
		// reset all elements.	
		Set<GraphElement<IElement>> allElements = this.flatten();
		monitor.beginTask("Disabling intention elements.", allElements.size());
		for (final GraphElement<IElement> elem : allElements) {
			elem.disable();
			monitor.worked(1);
		}
		monitor.done();
	}
	
	public Set<GraphElement<IElement>> getEnabledElements() {
		final Set<GraphElement<IElement>> ret = new LinkedHashSet<GraphElement<IElement>>();
		for (final IntentionNode<IElement> node : this.getNodes()) {
			if (node.isEnabled())
				ret.add(node);
			for (final IntentionArc<IElement> arc : node.getArcs())
				if (arc.isEnabled())
					ret.add(arc);
		}
		return ret;
	}

	public Collection<IntentionNode<IElement>> getNodes() {
		return this.nodeSet;
	}

	public String getProlog(final IProgressMonitor monitor) {
		final StringBuilder ret = new StringBuilder();
		monitor.beginTask("Writing nodes", this.nodeSet.size());
		for (final IntentionNode<IElement> node : this.nodeSet) {
			final SubProgressMonitor subMonitor = new SubProgressMonitor(
					monitor, -1);
			subMonitor.beginTask("Writing arcs", node.getArcs().size());
			for (final IntentionArc<IElement> edge : node.getArcs()) {
				ret.append(edge.getType().toString().toLowerCase());
				ret.append('(');
				ret.append("'" + edge.getFromNode().getElem() + "'");
				ret.append(',');
				ret.append("'" + edge.getToNode().getElem() + "'");
				ret.append(')');
				ret.append(". ");
				subMonitor.worked(1);
			}
			monitor.worked(1);
		}
		return ret.toString();
	}

	public String toDotFormat() {
		final StringBuilder ret = new StringBuilder();
		ret.append("digraph {");
		ret.append('\n');
		for (final IntentionNode<IElement> node : this.nodeSet) {
			ret.append('\t');
			ret.append(node.toDotFormat());
			ret.append('\n');
		}
		ret.append('}');
		return ret.toString();
	}

	private Set<IntentionNode<IElement>> makeArcs(
			final IntentionNode<IElement> fromNode, JayFX database,
			final Relation relation) {
		Set<IntentionNode<IElement>> ret = new LinkedHashSet<IntentionNode<IElement>>();

		for (final IElement toElement : database.getRange(fromNode.getElem(),
				relation)) {

			if (!this.elementToNodeMap.containsKey(toElement)) {
				IntentionNode<IElement> node = new IntentionNode<IElement>(
						toElement);
				this.elementToNodeMap.put(toElement, node);
				ret.add(node);
			}

			final IntentionNode<IElement> toNode = this.elementToNodeMap
					.get(toElement);
			final IntentionArc<IElement> arc = new IntentionArc<IElement>(
					fromNode, toNode, relation);

			fromNode.addArc(arc);
		}

		return ret;
	}

	/**
	 * @return
	 */
	public Collection<GraphElement<IElement>> getAllElements() {
		final Set<GraphElement<IElement>> ret = new LinkedHashSet<GraphElement<IElement>>();
		for (final IntentionNode<IElement> node : this.getNodes()) {
			ret.add(node);
			for (final IntentionArc<IElement> arc : node.getArcs())
				ret.add(arc);
		}
		return ret;
	}
	
	public Collection<GraphElement<IElement>> getAllAdvisableElements() {
		final Set<GraphElement<IElement>> ret = new LinkedHashSet<GraphElement<IElement>>();
		for (final IntentionNode<IElement> node : this.getNodes()) {
			if ( node.isAdvisable() )
				ret.add(node);
			for (final IntentionArc<IElement> arc : node.getArcs())
				if ( arc.isAdvisable() )
					ret.add(arc);
		}
		return ret;
	}

	/**
	 * @param subProgressMonitor
	 */
	public void enableAllElements(IProgressMonitor monitor) {
		Collection<GraphElement<IElement>> allElements = this.getAllElements();
		monitor.beginTask("Enabling all graph elements.", allElements.size());
		for ( GraphElement elem : allElements )
			elem.enable();
		monitor.done();
	}
	
	public void enableAllAdvisableElements(IProgressMonitor monitor) {
		Collection<GraphElement<IElement>> allAdvisableElements = this.getAllAdvisableElements();
		monitor.beginTask("Enabling all advisable graph elements.", allAdvisableElements.size());
		for ( GraphElement elem : allAdvisableElements )
			elem.enable();
		monitor.done();
	}

	public JayFX getDatabase() {
		return this.database;
	}

	public ConcernGraph (
			final Collection<? extends IProject> projectCol,
			short maximumAnalysisDepth, final IProgressMonitor monitor, TimeCollector timeCollector) throws Exception {
		
		this.maximumAnalysisDepth = maximumAnalysisDepth;
		
		this.database = new JayFX();
		this.database.initialize(projectCol, monitor, true, timeCollector);
		
		buildNodes(new SubProgressMonitor(monitor, -1));
		buildArcs(new SubProgressMonitor(monitor, -1));
		
		setRulesBase(monitor);
		fireRules(monitor);
	}
	
	private void setRulesBase(
			final IProgressMonitor monitor) throws Exception {
		monitor.subTask("Loading up the rulebase.");
		final Reader source = new InputStreamReader(PointcutProcessor.class
				.getResourceAsStream(RULES_FILE));
		final RuleBase ruleBase = readRule(source);
		final WorkingMemory workingMemory = ruleBase.newStatefulSession();
		workingMemory.setGlobal("maximumAnalysisDepth", this.maximumAnalysisDepth);

		final Set<GraphElement<IElement>> elemCol = this.flatten();
		monitor.beginTask("Inserting facts.", elemCol.size());
		for (final GraphElement<IElement> elem : elemCol) {
			workingMemory.insert(elem, true);
			monitor.worked(1);
		}
		this.workingMemory = workingMemory;
	}
	
	private static RuleBase readRule(final Reader source) throws Exception {
		//Use package builder to build up a rule package.
		//An alternative lower level class called "DrlParser" can also be used...
	
		final PackageBuilder builder = new PackageBuilder();
	
		//this will parse and compile in one step
		//NOTE: There are 2 methods here, the one argument one is for normal DRL.
		builder.addPackageFromDrl(source);
	
		//get the compiled package (which is serializable)
		final Package pkg = builder.getPackage();
	
		//add the package to a rulebase (deploy the rule package).
		final RuleBase ruleBase = RuleBaseFactory.newRuleBase();
		ruleBase.addPackage(pkg);
		return ruleBase;
	}
	
	private void fireRules(final IProgressMonitor monitor) {
		monitor.subTask("Firing rules.");
		this.workingMemory.fireAllRules();

		final QueryResults pathsToReverse = this.workingMemory
				.getQueryResults("all paths");
		monitor.beginTask("Reversing all paths.", pathsToReverse.size());
		for (final Iterator it = pathsToReverse.iterator(); it.hasNext();) {
			final QueryResult result = (QueryResult) it.next();
			final Path path = (Path) result.get("$path");
			Collections.reverse(path);
			final FactHandle handle = this.workingMemory.getFactHandle(path);
			this.workingMemory.update(handle, path);
			monitor.worked(1);
		}
	}

	public WorkingMemory getWorkingMemory() {
		return workingMemory;
	}

	public Object getMaximumAnalysisDepth() {
		return maximumAnalysisDepth;
	}
}