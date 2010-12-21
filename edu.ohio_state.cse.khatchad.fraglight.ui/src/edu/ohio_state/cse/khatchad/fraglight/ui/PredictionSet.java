/**
 * 
 */
package edu.ohio_state.cse.khatchad.fraglight.ui;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Collection;
import java.util.LinkedHashSet;

import edu.ohio_state.cse.khatchad.fraglight.ui.PointcutChangePredictionProvider.Prediction;

/**
 * @author <a href="mailto:khatchad@cse.ohio-state.edu">Raffi Khatchadourian</a>
 * 
 */
public class PredictionSet extends LinkedHashSet<Prediction> {

	private static final long serialVersionUID = 6903522896863043250L;

	private enum Property {
		CONTENTS
	};

	private final PropertyChangeSupport changes = new PropertyChangeSupport(
			this);

	public void addPropertyChangeListener(final PropertyChangeListener l) {
		this.changes.addPropertyChangeListener(l);
	}

	@Override
	public boolean add(Prediction e) {
		Object clone = this.clone();
		boolean add = super.add(e);
		if (add)
			this.changes.firePropertyChange(Property.CONTENTS.toString(),
					clone, this);
		return add;
	}

	@Override
	public boolean remove(Object o) {
		Object clone = this.clone();
		boolean remove = super.remove(o);
		if (remove)
			this.changes.firePropertyChange(Property.CONTENTS.toString(),
					clone, this);
		return remove;
	}

	public void clear() {
		Object clone = this.clone();
		super.clear();
		this.changes.firePropertyChange(Property.CONTENTS.toString(), clone,
				this);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		Object clone = this.clone();
		boolean removeAll = super.removeAll(c);
		if (removeAll)
			changes.firePropertyChange(Property.CONTENTS.toString(), clone,
					this);
		return removeAll;
	}

	@Override
	public boolean addAll(Collection<? extends Prediction> c) {
		Object clone = this.clone();
		boolean addAll = super.addAll(c);
		if (addAll)
			changes.firePropertyChange(Property.CONTENTS.toString(), clone,
					this);
		return addAll;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		Object clone = this.clone();
		boolean retainAll = super.retainAll(c);
		if (retainAll)
			changes.firePropertyChange(Property.CONTENTS.toString(), clone,
					this);
		return retainAll;
	}
}