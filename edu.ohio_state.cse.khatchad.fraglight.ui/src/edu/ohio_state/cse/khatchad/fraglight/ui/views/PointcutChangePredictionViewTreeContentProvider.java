package edu.ohio_state.cse.khatchad.fraglight.ui.views;

import java.util.Arrays;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import edu.ohio_state.cse.khatchad.fraglight.ui.FraglightUiPlugin;
import edu.ohio_state.cse.khatchad.fraglight.ui.PointcutChangePredictionProvider;
import edu.ohio_state.cse.khatchad.fraglight.ui.PointcutChangePredictionProvider.Prediction;

public class PointcutChangePredictionViewTreeContentProvider implements
		ITreeContentProvider {

	private final Object[] EMPTY = new Object[] {};

	public void dispose() {
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}

	public Object[] getElements(Object inputElement) {
		FraglightUiPlugin plugin = FraglightUiPlugin.getDefault();
		if (plugin == null)
			return EMPTY;
		else {
			PointcutChangePredictionProvider provider = plugin
					.getChangePredictionProvider();
			if (provider == null)
				return EMPTY;
			else
				return provider.getPredictionSet().toArray();
		}
	}

	public Object[] getChildren(Object parentElement) {
		if (!(parentElement instanceof Prediction))
			return EMPTY;
		else {
			Prediction prediction = (Prediction) parentElement;
			return prediction.getContributingPatterns().toArray();
		}
	}

	public Object getParent(Object element) {
		return null;
	}

	public boolean hasChildren(Object element) {
		if (!(element instanceof Prediction))
			return false;
		else {
			Prediction prediction = (Prediction) element;
			return !prediction.getContributingPatterns().isEmpty();
		}
	}
}