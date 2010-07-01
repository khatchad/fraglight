package edu.ohio_state.cse.khatchad.fraglight.ui.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import edu.ohio_state.cse.khatchad.fraglight.core.analysis.PointcutAnalyzer;
import edu.ohio_state.cse.khatchad.fraglight.core.analysis.PointcutProcessor;
import edu.ohio_state.cse.khatchad.fraglight.ui.FraglightUiPlugin;
import edu.ohio_state.cse.khatchad.fraglight.ui.PointcutChangePredictionProvider;
import edu.ohio_state.cse.khatchad.fraglight.ui.views.PointcutChangePredictionView;

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
		
		int maximumAnalysisDepth;
		double changeConfidenceThreshold;
		
		if ( changePredictionProvider == null ) {
			maximumAnalysisDepth = PointcutProcessor.DEFAULT_MAXIMUM_ANALYSIS_DEPTH;
			changeConfidenceThreshold = PointcutChangePredictionProvider.DEFAULT_CHANGE_CONFIDENCE_THRESHOLD;
		}
		
		else {
			changeConfidenceThreshold = changePredictionProvider.getChangeConfidenceThreshold();
			
			PointcutAnalyzer analyzer = changePredictionProvider.getAnalyzer();
			
			if ( analyzer == null ) {
				maximumAnalysisDepth = PointcutProcessor.DEFAULT_MAXIMUM_ANALYSIS_DEPTH;
			}
			
			else {
				maximumAnalysisDepth = analyzer.getMaximumAnalysisDepth();	
			}
			
		}	
		
		store.setDefault(PreferenceConstants.P_ANALYSIS_DEPTH,
				maximumAnalysisDepth);
		store.setDefault(PreferenceConstants.P_THRESHOLD,
				changeConfidenceThreshold);
	}

}
