package edu.ohio_state.cse.khatchad.fraglight.ui.views;

import org.eclipse.contribution.xref.internal.ui.utils.XRefUIUtils;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.widgets.Shell;

import edu.ohio_state.cse.khatchad.fraglight.core.analysis.model.Suggestion;
import edu.ohio_state.cse.khatchad.fraglight.ui.PointcutChangePredictionProvider.Prediction;

@SuppressWarnings("restriction")
public class DoubleClickAction extends Action {
	
	private Shell shell;
	
	private ISelectionProvider selectionProvider;

	public DoubleClickAction(Shell shell, ISelectionProvider selectionProvider) {
		this.shell = shell;
		this.selectionProvider = selectionProvider;
	}

	@SuppressWarnings({ "restriction", "unchecked" })
	@Override
	public void run() {
		ISelection selection = selectionProvider.getSelection();
		if (selection instanceof IStructuredSelection) {
			Object sel =
				((IStructuredSelection) selection).getFirstElement();
			if ( sel instanceof Prediction) {
				Prediction prediction = (Prediction)sel;
				XRefUIUtils.revealInEditor(prediction.getAdvice());
			}
		}
	}

}
