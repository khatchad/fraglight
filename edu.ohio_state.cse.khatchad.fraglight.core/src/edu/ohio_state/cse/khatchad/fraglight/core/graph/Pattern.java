/**
 * 
 */
package edu.ohio_state.cse.khatchad.fraglight.core.graph;

import java.util.Collection;
import java.util.Set;

import org.eclipse.ajdt.core.javaelements.AdviceElement;
import org.jdom.DataConversionException;
import org.jdom.Element;

import ca.mcgill.cs.swevo.jayfx.model.IElement;

/**
 * @author raffi
 * 
 */
public class Pattern<E extends IntentionArc<IElement>> extends Path<E> {

	private static final long serialVersionUID = -8126850132892419370L;

	private double simularity;

	private AdviceElement advElem;

	/**
	 * @param pattern
	 * @return
	 */
	private double getConcreteness() {
		final Collection<IntentionNode<IElement>> allNodes = this.getNodes();
		final Collection<IntentionNode<IElement>> wildNodes = this.getWildcardNodes();
		return (double) (allNodes.size() - wildNodes.size()) / allNodes.size();
	}

	private static double calculatePrecision(final Set<GraphElement<IElement>> searchedFor,
			final Set<GraphElement<IElement>> found) {
		final int totalElements = found.size();
		final int lookingFor = searchedFor.size();
		return (double) lookingFor / totalElements;
	}

	private static double performSimularityCalculation(final double precision, final double coverage,
			final double concreteness) {
		return precision * concreteness + coverage * (1 - concreteness);
	}

	public Pattern() {
	}

	/**
	 * @param patternElem
	 * @throws DataConversionException
	 */
	public Pattern(final Element patternElem) throws DataConversionException {
		super(patternElem);
	}

	/**
	 * @return the advElem
	 */
	public AdviceElement getAdvElem() {
		return this.advElem;
	}

	/**
	 * @return the simularity
	 */
	public double getSimularity() {
		return this.simularity;
	}

	/**
	 * @param advElem
	 */
	public void setAdvice(final AdviceElement advElem) {
		this.advElem = advElem;
	}

	/**
	 * @param simularity
	 */
	protected void setSimularity(final double simularity) {
		this.simularity = simularity;
	}

	/**
	 * Calculates the similarity to the associated advice based on the given
	 * results from matching this pattern.
	 * 
	 * @param patternResults
	 *            Elements matching the pattern.
	 * @param patternEnabledResults
	 *            Elements matching the pattern that are enabled by the
	 *            associate advice.
	 * @param graph
	 *            The concern graph used to produce the results.
	 * @return The pattern simularity; also sets in the pattern.
	 */
	public double calculateSimularityToAdviceBasedOnResults(final Set<GraphElement<IElement>> patternResults,
			final Set<GraphElement<IElement>> patternEnabledResults, ConcernGraph graph) {

		double precision = calculatePrecision(patternEnabledResults, patternResults);

		double coverage = patternEnabledResults.size() / graph.getEnabledElements().size();

		double concreteness = this.getConcreteness();

		double simularity = performSimularityCalculation(precision, coverage, concreteness);

		this.setSimularity(simularity);
		return simularity;
	}
}