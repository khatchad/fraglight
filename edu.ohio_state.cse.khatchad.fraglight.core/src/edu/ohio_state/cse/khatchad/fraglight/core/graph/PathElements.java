/**
 * 
 */
package edu.ohio_state.cse.khatchad.fraglight.core.graph;

import java.util.Vector;

/**
 * @author raffi
 * 
 */
public class PathElements<E extends IntentionNode<?>> extends Vector<E> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6538525649921978064L;
	private Path<?> path;

	public PathElements() {
	}

	public PathElements(final Path<?> path) {
		super();
		this.path = path;
	}

	/**
	 * @return the path
	 */
	public Path<?> getPath() {
		return this.path;
	}
}
