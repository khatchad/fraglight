package edu.ohio_state.cse.khatchad.fraglight.ui.views;

import static edu.ohio_state.cse.khatchad.fraglight.ui.views.PointcutChangePredictionViewComparator.SortBy.CHANGE_CONFIDENCE;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import edu.ohio_state.cse.khatchad.fraglight.ui.FraglightUiPlugin;
import edu.ohio_state.cse.khatchad.fraglight.ui.PointcutChangePredictionProvider;
import edu.ohio_state.cse.khatchad.fraglight.ui.views.PointcutChangePredictionViewComparator.SortBy;

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

//	private TableViewer viewer;
	private TreeViewer viewer;
	
	public TreeViewer getViewer() {
		return viewer;
	}

	private PointcutChangePredictionViewTreeContentProvider contentProvider;
	
	private Action doubleClickAction;
	
	private void makeActions() {
		doubleClickAction = new DoubleClickAction(getViewSite().getShell(),
				viewer);
	}

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
	/*
	public void createPartControl(Composite parent) {
		Table table = getTable(parent);
		
		this.viewer = new TableViewer(table);
		this.viewer.setUseHashlookup(true);
		this.viewer.setColumnProperties(COLUMN_NAMES);
		
		this.contentProvider = new PointcutChangePredictionViewContentProvider();
		viewer.setContentProvider(contentProvider);
		
		viewer.setLabelProvider(new PointcutChangePredictionViewTableLabelProvider());
		viewer.setComparator(new PointcutChangePredictionViewComparator(CHANGE_CONFIDENCE));
		viewer.setInput(getViewSite());

		// Create the help context id for the viewer's control
//		PlatformUI.getWorkbench().getHelpSystem().setHelp(viewer.getControl(),
//				"edu.ohio_state.cse.khatchad.fraglight.ui.viewer");
//		makeActions();
//		hookContextMenu();
		hookDoubleClickAction();
		contributeToActionBars();
	}
	*/
	
	public void createPartControl(Composite parent) {
		this.viewer = new TreeViewer(parent);
		Tree tree = this.viewer.getTree();
		final GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		this.viewer.getControl().setLayoutData(gridData);
		this.viewer.setUseHashlookup(true);
		
		tree.setHeaderVisible(true);
		tree.setLinesVisible(true);
		
		TreeColumn treeColumn = new TreeColumn(tree, SWT.LEFT);
		treeColumn.setText("Advice");
		treeColumn.setToolTipText("Advice whose pointcut is recommended to change.");
		
		treeColumn = new TreeColumn(tree, SWT.LEFT);
		treeColumn.setText("Confidence");
		treeColumn.setToolTipText("The confidence in this pointcut changing.");
		treeColumn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				viewer.setComparator(new PointcutChangePredictionViewComparator(SortBy.CHANGE_CONFIDENCE));
			}
		});

		TableLayout layout = new TableLayout();
		layout.addColumnData(new ColumnWeightData(80)); 
		layout.addColumnData(new ColumnWeightData(20)); 
		
		tree.setLayout(layout);
		
		this.contentProvider = new PointcutChangePredictionViewTreeContentProvider();
		viewer.setContentProvider(contentProvider);
		
		viewer.setLabelProvider(new PointcutChangePredictionViewTableLabelProvider());
		viewer.setComparator(new PointcutChangePredictionViewComparator(CHANGE_CONFIDENCE));
		viewer.setInput(getViewSite());
		makeActions();
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

//	private void makeActions() {
//		doubleClickAction = new DoubleClickAction(getViewSite().getShell(),
//				viewer);
//	}

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
		suggestionColumn.setResizable(true);
		suggestionColumn.setToolTipText("Advice whose pointcut is recommended to change.");
		suggestionColumn.setWidth(400);

		TableColumn confidenceColumn = new TableColumn(table, SWT.LEFT, 1);
		confidenceColumn.setText("Confidence");
		confidenceColumn.setResizable(true);
		confidenceColumn.setToolTipText("The confidence in this pointcut changing.");
		confidenceColumn.setWidth(100);
		
		confidenceColumn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				viewer.setComparator(new PointcutChangePredictionViewComparator(SortBy.CHANGE_CONFIDENCE));
			}
		});
		return table;
	}
}