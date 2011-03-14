package edu.ohio_state.cse.khatchad.fraglight.ui.views;

import java.util.logging.Logger;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import edu.ohio_state.cse.khatchad.fraglight.ui.FraglightUiPlugin;

public class ClearAction extends Action {

	/**
	 * 
	 */
	private static final String CLEAR_THE_SUGGESTED_POINTCUT_LIST = "Clear the suggested pointcut list.";

	private static final String CLEAR = "Clear";
	
	private static Logger logger = Logger
			.getLogger(ClearAction.class.getName());

	public ClearAction() {
		setText(CLEAR);
		setToolTipText(CLEAR_THE_SUGGESTED_POINTCUT_LIST);
		setImageDescriptor(PlatformUI.getWorkbench().getSharedImages()
				.getImageDescriptor(ISharedImages.IMG_ETOOL_CLEAR));
	}

	@Override
	public void run() {
		FraglightUiPlugin.getDefault().getChangePredictionProvider()
				.getPredictionSet().clear();
		logger.info("Cleared prediction list.");
	}
}
