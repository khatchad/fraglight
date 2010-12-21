package edu.ohio_state.cse.khatchad.fraglight.ui.views;

import static edu.ohio_state.cse.khatchad.fraglight.ui.views.PointcutChangePredictionViewComparator.SortBy.CHANGE_CONFIDENCE;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.part.ViewPart;

import edu.ohio_state.cse.khatchad.fraglight.ui.FraglightUiPlugin;
import edu.ohio_state.cse.khatchad.fraglight.ui.views.PointcutChangePredictionViewComparator.SortBy;

public class PointcutChangePredictionView extends ViewPart implements
		PropertyChangeListener {

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "edu.ohio_state.cse.khatchad.fraglight.ui.views.PointcutChangePredictionView";

	/**
	 * Constant representing the advice column.
	 */
	private final String ADVICE_COLUMN_NAME = "Advice";

	private ClearAction clearAction;

	/**
	 * Constant representing the confidence column.
	 */
	private final String CONFIDENCE_COLUMN_NAME = "Confidence";

	private transient PointcutChangePredictionViewTreeContentProvider contentProvider;

	private Action doubleClickAction;

	private TreeViewer viewer;

	/**
	 * The constructor.
	 */
	public PointcutChangePredictionView() {
		FraglightUiPlugin.getDefault().setChangePredictionView(this);
		FraglightUiPlugin.getDefault().addPropertyChangeListener(this);
	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

	@Override
	public void createPartControl(Composite parent) {
		this.setViewer(new TreeViewer(parent));
		Tree tree = this.getViewer().getTree();
		final GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		this.getViewer().getControl().setLayoutData(gridData);
		this.getViewer().setUseHashlookup(true);

		tree.setHeaderVisible(true);
		tree.setLinesVisible(true);

		TreeColumn treeColumn = new TreeColumn(tree, SWT.LEFT);
		treeColumn.setText(this.ADVICE_COLUMN_NAME);
		treeColumn
				.setToolTipText("Advice whose pointcut is recommended to change.");

		treeColumn = new TreeColumn(tree, SWT.LEFT);
		treeColumn.setText(this.CONFIDENCE_COLUMN_NAME);
		treeColumn.setToolTipText("The confidence in this pointcut changing.");
		treeColumn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				PointcutChangePredictionView.this.getViewer()
						.setComparator(new PointcutChangePredictionViewComparator(
								SortBy.CHANGE_CONFIDENCE));
			}
		});

		TableLayout layout = new TableLayout();
		layout.addColumnData(new ColumnWeightData(80));
		layout.addColumnData(new ColumnWeightData(20));

		tree.setLayout(layout);

		this.contentProvider = new PointcutChangePredictionViewTreeContentProvider();
		this.getViewer().setContentProvider(this.contentProvider);

		this.getViewer()
				.setLabelProvider(new PointcutChangePredictionViewTableLabelProvider());
		this.getViewer().setComparator(new PointcutChangePredictionViewComparator(
				CHANGE_CONFIDENCE));
		this.getViewer().setInput(getViewSite());
		makeActions();
		hookContextMenu();
		hookDoubleClickAction();
		contributeToActionBars();
	}

	private void fillContextMenu(IMenuManager manager) {
		manager.add(this.clearAction);
		// Other plug-ins can contribute there actions here
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	private void fillLocalPullDown(IMenuManager manager) {
		manager.add(this.clearAction);
	}

	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(this.clearAction);
	}

	public TreeViewer getViewer() {
		return this.viewer;
	}

	public void setViewer(TreeViewer viewer) {
		this.viewer = viewer;
	}

	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				PointcutChangePredictionView.this.fillContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(this.getViewer().getControl());
		this.getViewer().getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, this.getViewer());
	}

	private void hookDoubleClickAction() {
		this.getViewer().addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				PointcutChangePredictionView.this.doubleClickAction.run();
			}
		});
	}

	private void makeActions() {
		this.doubleClickAction = new DoubleClickAction(this.getViewer());
		this.clearAction = new ClearAction();
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	@Override
	public void setFocus() {
		this.getViewer().getControl().setFocus();
	}

	public void propertyChange(PropertyChangeEvent evt) {
		Object source = evt.getSource();

		if (source == FraglightUiPlugin.getDefault())
			FraglightUiPlugin.getDefault().getChangePredictionProvider()
					.getPredictionSet().addPropertyChangeListener(this);

		else if (FraglightUiPlugin.getDefault().getChangePredictionProvider() != null
				&& source == FraglightUiPlugin.getDefault()
						.getChangePredictionProvider().getPredictionSet()) {
			Display display = Display.getDefault();
			display.asyncExec(new Runnable() {				
				public void run() {
					getViewer().refresh();
				}
			});
		}
	}
}