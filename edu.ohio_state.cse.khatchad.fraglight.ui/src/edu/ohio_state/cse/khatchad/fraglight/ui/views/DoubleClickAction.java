package edu.ohio_state.cse.khatchad.fraglight.ui.views;

import org.eclipse.contribution.xref.internal.ui.utils.XRefUIUtils;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import edu.ohio_state.cse.khatchad.fraglight.ui.Prediction;

@SuppressWarnings("restriction")
public class DoubleClickAction extends Action {
	
	private ISelectionProvider selectionProvider;

	public DoubleClickAction(ISelectionProvider selectionProvider) {
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
