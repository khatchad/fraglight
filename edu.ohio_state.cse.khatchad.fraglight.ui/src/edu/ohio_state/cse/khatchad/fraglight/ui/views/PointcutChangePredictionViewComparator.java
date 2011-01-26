package edu.ohio_state.cse.khatchad.fraglight.ui.views;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;

import ca.mcgill.cs.swevo.jayfx.model.IElement;

import edu.ohio_state.cse.khatchad.fraglight.core.graph.IntentionArc;
import edu.ohio_state.cse.khatchad.fraglight.core.graph.Pattern;
import edu.ohio_state.cse.khatchad.fraglight.ui.Prediction;

public class PointcutChangePredictionViewComparator extends ViewerComparator {

	private SortBy type;

	public PointcutChangePredictionViewComparator(SortBy type) {
		super();
		this.type = type;
	}

	public enum SortBy {
		CHANGE_CONFIDENCE
	}

	@SuppressWarnings("unchecked")
	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
		if (e1 instanceof Prediction && e2 instanceof Prediction) {
			Prediction p1 = (Prediction) e1;
			Prediction p2 = (Prediction) e2;

			switch (type) {
			case CHANGE_CONFIDENCE:
				return Double.compare(p1.getChangeConfidence(),
						p2.getChangeConfidence())
						* -1;

			default:
				return 0;
			}
		}

		else if (e1 instanceof Pattern<?> && e2 instanceof Pattern<?>) {
			Pattern<IntentionArc<IElement>> p1 = (Pattern<IntentionArc<IElement>>) e1;
			Pattern<IntentionArc<IElement>> p2 = (Pattern<IntentionArc<IElement>>) e2;

			switch (type) {
			case CHANGE_CONFIDENCE:
				return Double.compare(p1.getSimularity(), p2.getSimularity())
						* -1;

			default:
				return 0;
			}

		}

		else
			// we'll say that they're equal.
			return 0;
	}
}