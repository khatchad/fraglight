/**
 * 
 */
package edu.ohio_state.cse.khatchad.fraglight.ui;

import org.eclipse.mylyn.context.core.AbstractContextListener;
import org.eclipse.mylyn.context.core.IInteractionContext;

/**
 * @author <a href="mailto:khatchad@cse.ohio-state.edu">Raffi Khatchadourian</a>
 *
 */
public class PointcutChangePredictionProvider extends AbstractContextListener {

	public PointcutChangePredictionProvider() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.mylyn.context.core.AbstractContextListener#contextActivated(org.eclipse.mylyn.context.core.IInteractionContext)
	 */
	@Override
	public void contextActivated(IInteractionContext context) {
		System.out.println("Hello from " + this.getClass().getName() + "!");
	}
}
