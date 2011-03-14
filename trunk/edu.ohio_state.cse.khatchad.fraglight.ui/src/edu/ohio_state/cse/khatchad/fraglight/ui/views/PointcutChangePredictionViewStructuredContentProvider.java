package edu.ohio_state.cse.khatchad.fraglight.ui.views;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

import edu.ohio_state.cse.khatchad.fraglight.ui.FraglightUiPlugin;
import edu.ohio_state.cse.khatchad.fraglight.ui.PointcutChangePredictionProvider;

public class PointcutChangePredictionViewStructuredContentProvider implements IStructuredContentProvider{

	public Object[] getElements(Object inputElement) {
		FraglightUiPlugin plugin = FraglightUiPlugin.getDefault();
		if ( plugin == null )
			return new Object[] {};
		else {
			PointcutChangePredictionProvider provider = plugin.getChangePredictionProvider();
			if ( provider == null )
				return new Object[] {};
			else
				return provider.getPredictionSet().toArray();
		}
	}

	public void dispose() {
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}
}
