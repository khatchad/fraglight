/**
 * 
 */
package edu.ohio_state.cse.khatchad.fraglightevaluator.actions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.eclipse.ajdt.core.javaelements.AdviceElement;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import edu.ohio_state.cse.khatchad.fraglight.core.util.Util;

/**
 * @author <a href="mailto:khatchad@cse.ohio-state.edu">Raffi Khatchadourian</a>
 * 
 */
public class CopyJavaElementKeyAction implements
		IWorkbenchWindowActionDelegate {

	private IStructuredSelection aSelection;

	@Override
	public void run(IAction action) {
		final Collection<AdviceElement> selectedAdvice = this
				.getSelectedAdvice();
		if (!selectedAdvice.isEmpty()) {

			AdviceElement advice = selectedAdvice.iterator().next();
			String key = Util.getKey(advice).replace("&", "&amp;");
			if (key.length() > 0) {
				Clipboard clipboard = new Clipboard(Display.getDefault());
				TextTransfer textTransfer = TextTransfer.getInstance();
				clipboard.setContents(new Object[] { key },
						new Transfer[] { textTransfer });
			}
		}
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		if (selection instanceof IStructuredSelection)
			this.aSelection = (IStructuredSelection) selection;
	}

	protected Collection<AdviceElement> getSelectedAdvice() {
		final Collection<AdviceElement> ret = new ArrayList<AdviceElement>();
		@SuppressWarnings("rawtypes")
		final Iterator i = this.aSelection.iterator();
		while (i.hasNext()) {
			final Object lNext = i.next();
			if (lNext instanceof AdviceElement)
				ret.add((AdviceElement) lNext);
		}

		return ret;
	}

	@Override
	public void dispose() {
	}

	@Override
	public void init(IWorkbenchWindow window) {
	}
}