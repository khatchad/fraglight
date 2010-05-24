package edu.ohio_state.cse.khatchad.fraglight.ui.views;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;

import edu.ohio_state.cse.khatchad.fraglight.ui.PointcutChangePredictionProvider.Prediction;

public class PointcutChangePredictionViewComparator extends ViewerComparator {

	private SortBy type;

	public PointcutChangePredictionViewComparator(SortBy type) {
		super();
		this.type = type;
	}

	public enum SortBy {
		CHANGE_CONFIDENCE
	}

	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
		if (e1 instanceof Prediction && e2 instanceof Prediction) {
			Prediction p1 = (Prediction) e1;
			Prediction p2 = (Prediction) e2;

			switch (type) {
			case CHANGE_CONFIDENCE:
				return Double.compare(p1.getChangeConfidence(), p2
						.getChangeConfidence())
						* -1;
			default:
				return 0;
			}
		}

		else
			throw new IllegalArgumentException(
					"Items to sort must be of a Prediction type.");
	}
}
