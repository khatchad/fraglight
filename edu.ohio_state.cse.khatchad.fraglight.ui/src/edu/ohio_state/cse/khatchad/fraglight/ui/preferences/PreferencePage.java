package edu.ohio_state.cse.khatchad.fraglight.ui.preferences;

import java.util.logging.Logger;

import org.eclipse.jface.preference.*;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbench;

import edu.ohio_state.cse.khatchad.fraglight.ui.FraglightUiPlugin;
import edu.ohio_state.cse.khatchad.fraglight.ui.PointcutChangePredictionProvider;
import edu.ohio_state.cse.khatchad.fraglight.ui.PointcutChangePredictionProvider.PointcutAnalysisScope;

/**
 * This class represents a preference page that is contributed to the
 * Preferences dialog. By subclassing <samp>FieldEditorPreferencePage</samp>, we
 * can use the field support built into JFace that allows us to create a page
 * that is small and knows how to save, restore and apply itself.
 * <p>
 * This page is used to modify preferences only. They are stored in the
 * preference store that belongs to the main plug-in class. That way,
 * preferences can be accessed directly via the preference store.
 */

public class PreferencePage extends FieldEditorPreferencePage implements
		IWorkbenchPreferencePage {

	private static Logger logger = Logger.getLogger(PreferencePage.class
			.getName());

	public PreferencePage() {
		super(GRID);
		setPreferenceStore(FraglightUiPlugin.getDefault().getPreferenceStore());
		setDescription("Set desired properties of the Fraglight pointcut change predictor here.");
	}

	/**
	 * Creates the field editors. Field editors are abstractions of the common
	 * GUI blocks needed to manipulate various types of preferences. Each field
	 * editor knows how to save and restore itself.
	 */
	public void createFieldEditors() {

		IntegerFieldEditor maximumAnalysisDepthEditor = new IntegerFieldEditor(
				PreferenceConstants.P_ANALYSIS_DEPTH,
				"&Maximum analysis depth:", getFieldEditorParent());

		addField(maximumAnalysisDepthEditor);

		addField(new StringFieldEditor(PreferenceConstants.P_HIGH_THRESHOLD,
				"&High change confidence threshold:", getFieldEditorParent()));

		addField(new StringFieldEditor(PreferenceConstants.P_LOW_THRESHOLD,
				"&Low change confidence threshold:", getFieldEditorParent()));

		addField(new RadioGroupFieldEditor(
				PreferenceConstants.P_POINTCUT_SCOPE,
				"Pointcut analysis scope:",
				1,
				new String[][] {
						{ "&Workspace",
								PointcutAnalysisScope.WORKSPACE.toString() },
						{ "&Project", PointcutAnalysisScope.PROJECT.toString() } },
				getFieldEditorParent()));

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}

}