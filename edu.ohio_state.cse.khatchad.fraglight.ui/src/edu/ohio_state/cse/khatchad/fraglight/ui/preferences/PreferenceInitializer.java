package edu.ohio_state.cse.khatchad.fraglight.ui.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import edu.ohio_state.cse.khatchad.fraglight.core.analysis.PointcutAnalyzer;
import edu.ohio_state.cse.khatchad.fraglight.ui.FraglightUiPlugin;
import edu.ohio_state.cse.khatchad.fraglight.ui.PointcutChangePredictionProvider;

/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
	 */
	public void initializeDefaultPreferences() {
		IPreferenceStore store = FraglightUiPlugin.getDefault()
				.getPreferenceStore();
		PointcutChangePredictionProvider changePredictionProvider = FraglightUiPlugin
				.getDefault().getChangePredictionProvider();
		PointcutAnalyzer analyzer = changePredictionProvider.getAnalyzer();
		int maximumAnalysisDepth = analyzer.getMaximumAnalysisDepth();
		store.setDefault(PreferenceConstants.P_ANALYSIS_DEPTH,
				maximumAnalysisDepth);
		store.setDefault(PreferenceConstants.P_THRESHOLD,
				changePredictionProvider.getChangeConfidenceThreshold());
	}

}
