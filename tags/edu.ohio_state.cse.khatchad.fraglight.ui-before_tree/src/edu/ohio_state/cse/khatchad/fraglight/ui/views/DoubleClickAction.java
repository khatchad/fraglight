package edu.ohio_state.cse.khatchad.fraglight.ui.views;

import org.eclipse.contribution.xref.internal.ui.utils.XRefUIUtils;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.widgets.Shell;

import edu.ohio_state.cse.khatchad.fraglight.core.analysis.model.Suggestion;


public class DoubleClickAction extends Action {
	
	private Shell shell;
	
	private TableViewer viewer;

	public DoubleClickAction(Shell shell, TableViewer viewer) {
		this.shell = shell;
		this.viewer = viewer;
	}

	@SuppressWarnings({ "restriction", "unchecked" })
	@Override
	public void run() {
		ISelection selection = viewer.getSelection();
		if (selection instanceof IStructuredSelection) {
			Object sel =
				((IStructuredSelection) selection).getFirstElement();
			if ( sel instanceof Suggestion ) {
				Suggestion<IJavaElement> suggestion = (Suggestion<IJavaElement>)sel;
				XRefUIUtils.revealInEditor(suggestion.getSuggestion());
			}
		}
	}

}
