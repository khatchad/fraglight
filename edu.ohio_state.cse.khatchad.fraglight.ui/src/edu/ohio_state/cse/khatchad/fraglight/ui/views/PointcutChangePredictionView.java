package edu.ohio_state.cse.khatchad.fraglight.ui.views;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import edu.ohio_state.cse.khatchad.fraglight.ui.FraglightUiPlugin;
import edu.ohio_state.cse.khatchad.fraglight.ui.PointcutChangePredictionProvider;

/**
 * This sample class demonstrates how to plug-in a new workbench view. The view
 * shows data obtained from the model. The sample creates a dummy model on the
 * fly, but a real implementation would connect to the model available either in
 * this or another plug-in (e.g. the workspace). The view is connected to the
 * model using a content provider.
 * <p>
 * The view uses a label provider to define how model objects should be
 * presented in the view. Each view can present the same model objects using
 * different labels and icons, if needed. Alternatively, a single label provider
 * can be shared between views in order to ensure that objects of the same type
 * are presented in the same way everywhere.
 * <p>
 */

public class PointcutChangePredictionView extends ViewPart {

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "edu.ohio_state.cse.khatchad.fraglight.ui.views.PointcutChangePredictionView";

	/**
	 * Constant representing the advice column.
	 */
	private final String ADVICE_COLUMN = "advice";

	/**
	 * Constant representing the confidence column.
	 */
	private final String CONFIDENCE_COLUMN = "confidence";

	/**
	 * Constant representing all column names.
	 */
	private final String[] COLUMN_NAMES = new String[] { ADVICE_COLUMN,
			CONFIDENCE_COLUMN };

	private TableViewer viewer;
	
	private Action doubleClickAction;

	/*
	 * The content provider class is responsible for
	 * providing objects to the view. It can wrap
	 * existing objects in adapters or simply return
	 * objects as-is. These objects may be sensitive
	 * to the current input of the view, or ignore
	 * it and always show the same content 
	 * (like Task List, for example).
	 */

	/**
	 * The constructor.
	 */
	public PointcutChangePredictionView() {
		FraglightUiPlugin.getDefault().setChangePredictionView(this);
	}

	/**
	 * This is a callback that will allow us to create the viewer and initialize
	 * it.
	 */
	public void createPartControl(Composite parent) {
		Table table = getTable(parent);
		
		this.viewer = new TableViewer(table);
		this.viewer.setUseHashlookup(true);
		this.viewer.setColumnProperties(COLUMN_NAMES);
		
		IContentProvider contentProvider;
		
		FraglightUiPlugin fraglightUiPlugin = FraglightUiPlugin.getDefault();
		if ( fraglightUiPlugin == null )
			contentProvider = null;
		else {
			PointcutChangePredictionProvider changePredictionProvider = fraglightUiPlugin.getChangePredictionProvider();
			contentProvider = changePredictionProvider;
		}
		
		viewer.setContentProvider(contentProvider);
		
		
		viewer.setLabelProvider(new PointcutChangePredictionViewLabelProvider());
		viewer.setSorter(new PointcutChangePredictionViewNameSorter());
		viewer.setInput(getViewSite());

		// Create the help context id for the viewer's control
		PlatformUI.getWorkbench().getHelpSystem().setHelp(viewer.getControl(),
				"edu.ohio_state.cse.khatchad.fraglight.ui.viewer");
		makeActions();
		hookContextMenu();
		hookDoubleClickAction();
		contributeToActionBars();
	}

	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				PointcutChangePredictionView.this.fillContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, viewer);
	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

	private void fillLocalPullDown(IMenuManager manager) {
	}

	private void fillContextMenu(IMenuManager manager) {
	}

	private void fillLocalToolBar(IToolBarManager manager) {
	}

	private void makeActions() {
		doubleClickAction = new DoubleClickAction(getViewSite().getShell(),
				viewer);
	}

	private void hookDoubleClickAction() {
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				doubleClickAction.run();
			}
		});
	}

	private void showMessage(String message) {
		MessageDialog.openInformation(viewer.getControl().getShell(),
				"Pointcut Change Prediction View", message);
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		viewer.getControl().setFocus();
	}
	
	private Table getTable(Composite parent) {
		int tableStyle = SWT.SINGLE | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL
				| SWT.FULL_SELECTION | SWT.HIDE_SELECTION;

		Table table = new Table(parent, tableStyle);

		GridData gridData = new GridData(GridData.FILL_BOTH);
		gridData.grabExcessVerticalSpace = true;
		gridData.horizontalSpan = 2;
		table.setLayoutData(gridData);

		table.setLinesVisible(true);
		table.setHeaderVisible(true);

		TableColumn suggestionColumn = new TableColumn(table, SWT.LEFT, 0);
		suggestionColumn.setText("Advice");
		suggestionColumn.setWidth(200);

		// Add listener to column so tasks are sorted by suggestion when clicked 
//		suggestionColumn.addSelectionListener(new SelectionAdapter() {
//			public void widgetSelected(SelectionEvent e) {
//				viewer.setSorter(new SuggestionViewSorter(SortBy.SUGGESTIONS));
//			}
//		});

		TableColumn confidenceColumn = new TableColumn(table, SWT.LEFT, 1);
		confidenceColumn.setText("Confidence");
		confidenceColumn.setWidth(300);
//		suggestionColumn.addSelectionListener(new SelectionAdapter() {
//			public void widgetSelected(SelectionEvent e) {
//				viewer.setSorter(new SuggestionViewSorter(SortBy.CONFIDENCE));
//			}
//		});
		return table;
	}
}