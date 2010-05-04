package edu.ohio_state.cse.khatchad.fraglight.ui;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import edu.ohio_state.cse.khatchad.fraglight.ui.views.PointcutChangePredictionView;

/**
 * The activator class controls the plug-in life cycle
 */
public class FraglightUiPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "edu.ohio_state.cse.khatchad.fraglight.ui";

	// The shared instance
	private static FraglightUiPlugin plugin;
	
	/**
	 * A reference to the change prediction provider, initially null.
	 */
	private PointcutChangePredictionProvider changePredictionProvider;
	
	/**
	 * A reference to the change prediction view, initially null.
	 */
	private PointcutChangePredictionView changePredictionView;
	
	/**
	 * The constructor
	 */
	public FraglightUiPlugin() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static FraglightUiPlugin getDefault() {
		return plugin;
	}

	/**
	 * @return the changePredictionProvider
	 */
	public PointcutChangePredictionProvider getChangePredictionProvider() {
		return this.changePredictionProvider;
	}

	/**
	 * @param changePredictionProvider the changePredictionProvider to set
	 */
	public void setChangePredictionProvider(
			PointcutChangePredictionProvider changePredictionProvider) {
		this.changePredictionProvider = changePredictionProvider;
	}

	/**
	 * @return the changePredictionView
	 */
	public PointcutChangePredictionView getChangePredictionView() {
		return this.changePredictionView;
	}

	/**
	 * @param changePredictionView the changePredictionView to set
	 */
	public void setChangePredictionView(
			PointcutChangePredictionView changePredictionView) {
		this.changePredictionView = changePredictionView;
	}

}
