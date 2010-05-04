/**
 * 
 */
package edu.ohio_state.cse.khatchad.fraglight.ui.views;

import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.PlatformUI;

import edu.ohio_state.cse.khatchad.fraglight.ui.PointcutChangePredictionProvider.Prediction;

class PointcutChangePredictionViewLabelProvider extends LabelProvider implements
		ITableLabelProvider {

	private DecoratingLabelProvider decoratingLabelProvider;

	public PointcutChangePredictionViewLabelProvider() {
		ILabelDecorator labelDecorator = PlatformUI.getWorkbench()
				.getDecoratorManager().getLabelDecorator();
		JavaElementLabelProvider javaElementLabelProvider = new JavaElementLabelProvider();
		decoratingLabelProvider = new DecoratingLabelProvider(
				javaElementLabelProvider, labelDecorator);
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
					throw new IllegalArgumentException(
							"Invalid column number: " + index);
			}
		}
		else
			return "";
	}

	public Image getColumnImage(Object obj, int index) {
		return getImage(obj);
	}

	public Image getImage(Object obj) {
		if (obj instanceof Prediction) {
			Prediction prediction = (Prediction) obj;
			return decoratingLabelProvider.getImage(prediction.getAdvice());
		}

		else
			return null;
	}
}