/**
 * 
 */
package edu.ohio_state.cse.khatchad.fraglight.ui;

import org.eclipse.mylyn.context.core.ContextChangeEvent;
import org.eclipse.mylyn.context.core.IInteractionElement;
import org.eclipse.mylyn.internal.context.core.IActiveSearchOperation;
import org.eclipse.mylyn.internal.java.ui.JavaStructureBridge;
import org.eclipse.mylyn.internal.java.ui.search.AbstractJavaRelationProvider;

/**
 * @author <a href="mailto:khatchad@cse.ohio-state.edu">Raffi Khatchadourian</a>
 *
 */
@SuppressWarnings("restriction")
public class PointcutChangePredictionProvider extends AbstractJavaRelationProvider {

	private static final String ID = ID_GENERIC + ".pointcutchangeprediction";
	
	public static final String NAME = "may break"; //$NON-NLS-1$

	public PointcutChangePredictionProvider() {
		super(JavaStructureBridge.CONTENT_TYPE, ID);
	}

	@Override
	public void contextChanged(ContextChangeEvent event) {
		//TODO: This will have to be overrided.
		System.out.println("Hello from " + this.getClass().getName() + "contextChanged");
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