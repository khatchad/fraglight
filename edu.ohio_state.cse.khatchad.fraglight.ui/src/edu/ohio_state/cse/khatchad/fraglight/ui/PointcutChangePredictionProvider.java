/**
 * 
 */
package edu.ohio_state.cse.khatchad.fraglight.ui;

import static java.lang.System.out;
import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;

import java.util.Collection;

import org.eclipse.ajdt.core.javaelements.AdviceElement;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.mylyn.context.core.ContextChangeEvent;
import org.eclipse.mylyn.context.core.ContextCore;
import org.eclipse.mylyn.context.core.IInteractionElement;
import org.eclipse.mylyn.internal.context.core.ContextCorePlugin;
import org.eclipse.mylyn.internal.context.core.IActiveSearchOperation;
import org.eclipse.mylyn.internal.java.ui.JavaStructureBridge;
import org.eclipse.mylyn.internal.java.ui.search.AbstractJavaRelationProvider;

import uk.ac.lancs.comp.khatchad.rejuvenatepc.core.PointcutAnalyzer;

import edu.ohio_state.cse.khatchad.ajplugintools.util.AJUtil;

/**
 * @author <a href="mailto:khatchad@cse.ohio-state.edu">Raffi Khatchadourian</a>
 *
 */
@SuppressWarnings("restriction")
public class PointcutChangePredictionProvider extends AbstractJavaRelationProvider {

	private static final String ID = ID_GENERIC + ".pointcutchangeprediction";
	
	public static final String NAME = "may break"; //$NON-NLS-1$
	
	private PointcutAnalyzer analyzer = new PointcutAnalyzer();

	public PointcutChangePredictionProvider() {
		super(JavaStructureBridge.CONTENT_TYPE, ID);
	}

	@Override
	public void contextChanged(ContextChangeEvent event) {
		switch(event.getEventKind()) {
			case ACTIVATED: {
				IWorkspace workspace = getWorkspace();
				IWorkspaceRoot root = workspace.getRoot();
				IProject[] projects = root.getProjects();
				for ( IProject proj : projects ) {
					if ( proj.isAccessible() ) {
						IJavaProject javaProject = JavaCore.create(proj);
						Collection<? extends AdviceElement> toAnalyze = null;
						try {
							toAnalyze = AJUtil.extractValidAdviceElements(javaProject);
						}
						catch (JavaModelException e) {
							//next project.
							continue;
						}
						if (!toAnalyze.isEmpty()) {
							this.analyzer.analyze(toAnalyze, lMonitor);
						}
					}
				}
				
				break;
			}
		}
	}

	@Override
	protected void findRelated(IInteractionElement node, int degreeOfSeparation) {
		// TODO This method may very well be the one that eventually does the work.
		//Given an interaction, will find pointcuts that may have broken.
	}

	/* (non-Javadoc)
	 * @see org.eclipse.mylyn.internal.context.core.AbstractRelationProvider#getName()
	 */
	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public IActiveSearchOperation getSearchOperation(IInteractionElement node,
			int limitTo, int degreeOfSeparation) {
		return null;
	}

	@Override
	protected String getSourceId() {
		return ID;
	}
}