/**
 * 
 */
package edu.ohio_state.cse.khatchad.fraglight.core.analysis.model;

import ca.mcgill.cs.swevo.jayfx.model.IElement;
import edu.ohio_state.cse.khatchad.fraglight.core.graph.IntentionArc;
import edu.ohio_state.cse.khatchad.fraglight.core.graph.Pattern;

/**
 * @author raffi
 * 
 */
public class Suggestion<E> {
	private E suggestion;
	private Pattern<IntentionArc<IElement>> pattern;
	private double confidence;

	/**
	 * @param suggestion
	 * @param pattern
	 * @param confidence
	 */
	public Suggestion(E suggestion, Pattern<IntentionArc<IElement>> pattern, double confidence) {
		this.suggestion = suggestion;
		this.pattern = pattern;
		this.confidence = confidence;
	}

	/**
	 * @return the suggestion
	 */
	public E getSuggestion() {
		return this.suggestion;
	}

	/**
	 * @return the pattern
	 */
	public Pattern<IntentionArc<IElement>> getPattern() {
		return this.pattern;
	}

	/**
	 * @return the confidence
	 */
	public double getConfidence() {
		return this.confidence;
	}
}
