package edu.ohio_state.cse.khatchad.fraglight.ui.views;

import java.util.logging.Logger;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import edu.ohio_state.cse.khatchad.fraglight.ui.FraglightUiPlugin;

public class ClearAction extends Action {

	private static Logger logger = Logger
			.getLogger(ClearAction.class.getName());

	private PointcutChangePredictionView view;

	public ClearAction(PointcutChangePredictionView view) {
		// TODO: Remove parameter.
		this.view = view;
		setText("Clear");
		setToolTipText("Clear the suggested pointcut list.");
		setImageDescriptor(PlatformUI.getWorkbench().getSharedImages()
				.getImageDescriptor(ISharedImages.IMG_ETOOL_CLEAR));
	}

	@Override
	public void run() {
		FraglightUiPlugin.getDefault().getChangePredictionProvider()
				.getPredictionSet().clear();
		logger.info("Cleared prediction list.");
		// TODO: Remove refresh.
		this.view.getViewer().refresh();
	}

}
