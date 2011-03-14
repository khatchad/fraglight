package edu.ohio_state.cse.khatchad.fraglight.ui;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import edu.ohio_state.cse.khatchad.fraglight.ui.views.PointcutChangePredictionView;

/**
 * The activator class controls the plug-in life cycle
 */
public class FraglightUiPlugin extends AbstractUIPlugin {

	private enum Property {POINTCUT_CHANGE_PREDICTION_PROVIDER};

	/**
	 * The plug-in ID.
	 */
	public static final String PLUGIN_ID = "edu.ohio_state.cse.khatchad.fraglight.ui";

	/**
	 * The shared instance.
	 */
	private static FraglightUiPlugin plugin;
	
	/**
	 * A reference to the change prediction provider, initially null.
	 */
	private PointcutChangePredictionProvider changePredictionProvider;
	
	/**
	 * A reference to the change prediction view, initially null.
	 */
	private PointcutChangePredictionView changePredictionView;
	
	private final PropertyChangeSupport changes = new PropertyChangeSupport(this);
	
	public void addPropertyChangeListener(final PropertyChangeListener l) {
		this.changes.addPropertyChangeListener(l);
	}
	
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
		PointcutChangePredictionProvider oldValue = this.changePredictionProvider;
		this.changePredictionProvider = changePredictionProvider;
		this.changes.firePropertyChange(Property.POINTCUT_CHANGE_PREDICTION_PROVIDER.toString(), oldValue, this.changePredictionProvider);
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
