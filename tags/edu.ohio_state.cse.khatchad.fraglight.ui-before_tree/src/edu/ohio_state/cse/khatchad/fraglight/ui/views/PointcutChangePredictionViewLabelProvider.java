/**
 * 
 */
package edu.ohio_state.cse.khatchad.fraglight.ui.views;

import org.aspectj.asm.AsmManager.ModelInfo;
import org.eclipse.ajdt.core.model.AJProjectModelFacade;
import org.eclipse.ajdt.core.model.AJProjectModelFactory;
import org.eclipse.ajdt.internal.builder.AJNode;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.PlatformUI;

import edu.ohio_state.cse.khatchad.fraglight.ui.PointcutChangePredictionProvider.Prediction;

class PointcutChangePredictionViewLabelProvider extends LabelProvider implements
		ITableLabelProvider {

	private ILabelProvider labelProvider;

	public PointcutChangePredictionViewLabelProvider() {
		ILabelDecorator labelDecorator = PlatformUI.getWorkbench()
				.getDecoratorManager().getLabelDecorator();
		JavaElementLabelProvider javaElementLabelProvider = new JavaElementLabelProvider();
		labelProvider = new DecoratingLabelProvider(javaElementLabelProvider,
				labelDecorator);
	}

	private boolean addedListener = false;

	private ListenerList fListeners;

	public void addListener(ILabelProviderListener listener) {
		super.addListener(listener);
		if (fListeners == null) {
			fListeners = new ListenerList();
		}
		fListeners.add(listener);
		if (!addedListener) {
			addedListener = true;
			// as we are only retrieving images from labelProvider not using it
			// directly, we need to update this label provider whenever that one
			// updates
			labelProvider.addListener(new ILabelProviderListener() {
				public void labelProviderChanged(LabelProviderChangedEvent event) {
					fireLabelChanged();
				}
			});
		}
	}

	private void fireLabelChanged() {
		if (fListeners != null && !fListeners.isEmpty()) {
			LabelProviderChangedEvent event = new LabelProviderChangedEvent(
					this);
			Object[] listeners = fListeners.getListeners();
			for (int i = 0; i < listeners.length; i++) {
				((ILabelProviderListener) listeners[i])
						.labelProviderChanged(event);
			}
		}
	}

	public String getColumnText(Object obj, int index) {
		if (obj instanceof Prediction) {
			Prediction prediction = (Prediction) obj;
			switch (index) {
			case 0:
				return this.getText(prediction.getAdvice());
			case 1:
				return this.getText(prediction.getChangeConfidence());
			default:
				throw new IllegalArgumentException("Invalid column number: "
						+ index);
			}
		} else
			return "";
	}

	@Override
	public String getText(Object element) {
		String ret = element.toString();
		if ( element instanceof IJavaElement ) {
			IJavaElement jElem = (IJavaElement) element;
			AJProjectModelFacade model = AJProjectModelFactory.getInstance().getModelForJavaElement(jElem);
			String linkName = model.getJavaElementLinkName(jElem);
			AJNode associate = new AJNode(jElem, model
					.getJavaElementLinkName(jElem));
			ret = this.labelProvider.getText(associate);
		}
		return ret;
	}

	public Image getColumnImage(Object obj, int index) {
		switch (index) {
		case 0:
			return getImage(obj);
		case 1:
			return null;
		default:
			throw new IllegalArgumentException("Invalid column number: "
					+ index);
		}
	}

	@Override
	public Image getImage(Object obj) {
		if (obj instanceof Prediction) {
			Prediction prediction = (Prediction) obj;
			return labelProvider.getImage(prediction.getAdvice());
		}

		else
			return null;
	}

	@Override
	public void dispose() {
		fListeners = null;
		if (labelProvider != null) {
			labelProvider.dispose();
			labelProvider = null;
		}
	}
}