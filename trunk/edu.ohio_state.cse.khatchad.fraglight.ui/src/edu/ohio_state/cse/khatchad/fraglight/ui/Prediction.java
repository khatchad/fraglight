package edu.ohio_state.cse.khatchad.fraglight.ui;

import java.util.Set;

import org.eclipse.ajdt.core.javaelements.AdviceElement;
import org.eclipse.jdt.core.IJavaElement;

import ca.mcgill.cs.swevo.jayfx.model.IElement;
import edu.ohio_state.cse.khatchad.fraglight.core.graph.IntentionArc;
import edu.ohio_state.cse.khatchad.fraglight.core.graph.Pattern;

/**
 * A prediction consists of the advice predicted to change, the degree of
 * confidence that we have in that advice changing, and join point addition that
 * invoked the prediction, and the set of patterns that contributed to the
 * prediction.
 * 
 * @author <a href="mailto:khatchad@cse.ohio-state.edu>Raffi Khatchadourian</a>
 * 
 */
public class Prediction {

	public enum ChangeDirection {
		NEGATIVE('-'), POSITIVE('+');
		public static Prediction.ChangeDirection valueOf(boolean captured) {
			return captured ? NEGATIVE : POSITIVE;
		}

		private char symbol;

		ChangeDirection(char symbol) {
			this.symbol = symbol;
		}

		@Override
		public String toString() {
			return String.valueOf(this.symbol);
		}
	};

	public enum InterestDirection {
		LESS_INTERESTING('-'), MORE_INTERESTING('+'), NO_CHANGE('#');
		private char symbol;

		InterestDirection(char symbol) {
			this.symbol = symbol;
		}

		@Override
		public String toString() {
			return String.valueOf(this.symbol);
		}
	}

	private AdviceElement advice;

	private IJavaElement affectingJoinPoint;

	private double changeConfidence;

	private Prediction.ChangeDirection changeDirection;

	private Set<Pattern<IntentionArc<IElement>>> contributingPatterns;

	private Prediction.InterestDirection interestDirection = InterestDirection.NO_CHANGE;

	private double newInterestLevel;

	private double originalInterestLevel;

	public Prediction(AdviceElement advice, double changeConfidence,
			Prediction.ChangeDirection changeDirection,
			IJavaElement affectingJoinPoint,
			Set<Pattern<IntentionArc<IElement>>> contributingPatterns) {
		this.advice = advice;
		this.changeConfidence = changeConfidence;
		this.affectingJoinPoint = affectingJoinPoint;
		this.contributingPatterns = contributingPatterns;
		this.changeDirection = changeDirection;
	}

	/**
	 * @return the advice
	 */
	public AdviceElement getAdvice() {
		return this.advice;
	}

	public IJavaElement getAffectingJoinPoint() {
		return this.affectingJoinPoint;
	}

	/**
	 * @return the changeConfidence
	 */
	public double getChangeConfidence() {
		return this.changeConfidence;
	}

	public Prediction.ChangeDirection getChangeDirection() {
		return this.changeDirection;
	}

	public Set<Pattern<IntentionArc<IElement>>> getContributingPatterns() {
		return this.contributingPatterns;
	}

	public Prediction.InterestDirection getInterestDirection() {
		return this.interestDirection;
	}

	public double getNewInterestLevel() {
		return this.newInterestLevel;
	}

	public double getOriginalInterestLevel() {
		return this.originalInterestLevel;
	}

	public void setInterestDirection(
			Prediction.InterestDirection interestDirection) {
		this.interestDirection = interestDirection;
	}

	public void setNewInterestLevel(double newInterestLevel) {
		this.newInterestLevel = newInterestLevel;
	}

	/**
	 * @param originalInterestLevel
	 */
	public void setOriginalInterestLevel(double originalInterestLevel) {
		this.originalInterestLevel = originalInterestLevel;
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