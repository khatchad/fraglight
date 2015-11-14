/**
 * 
 */
package edu.ohio_state.cse.khatchad.fraglight.core.analysis;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.ASTMatcher;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Statement;

/**
 * @author <a href="mailto:khatchad@cse.ohio-state.edu">Raffi Khatchadourian</a>
 * 
 */
public class JoinPointShadowDifferenceAnalyzer extends ASTMatcher {

	private Set<IJavaElement> newJoinPointShadows = new LinkedHashSet<IJavaElement>();

	@Override
	public boolean match(Block node, Object other) {
		boolean match = super.match(node, other);
		if (match)
			return match;

		else if (!(other instanceof Block)) {
			return false;
		}

		else {
			// something is different about the statements.
			Block o = (Block) other;

			if (node.statements().size() < o.statements().size()) {

				List<Statement> newStatements = new ArrayList<Statement>(o.statements());
				for (Iterator it = newStatements.iterator(); it.hasNext();)
					if (contains(node.statements(), (ASTNode) it.next()))
						it.remove();

				for (Statement statement : newStatements) {
					// visit the statement to find all method invocations.
					JoinPointShadowExtractor extractor = new JoinPointShadowExtractor();
					statement.accept(extractor);
					this.newJoinPointShadows.addAll(extractor.getJoinPointShadows());
				}
			}

			else if (node.statements().size() == o.statements().size()) {
				// Extract new join points by comparing statements.
				for (int i = 0; i < node.statements().size(); i++) {
					Statement s1 = (Statement) node.statements().get(i);
					Statement s2 = (Statement) o.statements().get(i);

					if (!s1.subtreeMatch(this, s2)) {

						JoinPointShadowExtractor ex1 = new JoinPointShadowExtractor();
						s1.accept(ex1);

						JoinPointShadowExtractor ex2 = new JoinPointShadowExtractor();
						s2.accept(ex2);

						Set<IJavaElement> set2 = new LinkedHashSet<IJavaElement>(ex2.getJoinPointShadows());
						set2.removeAll(ex1.getJoinPointShadows());
						this.newJoinPointShadows.addAll(set2);
					}
				}
			}
		}
		return match;
	}

	/**
	 * @param statements
	 * @param next
	 * @return
	 */
	private boolean contains(List statements, ASTNode node) {
		for (Iterator it = statements.iterator(); it.hasNext();) {
			ASTNode currNode = (ASTNode) it.next();
			if (node.subtreeMatch(this, currNode))
				return true;
		}
		return false;
	}

	public Set<IJavaElement> getNewJoinPointShadows() {
		return newJoinPointShadows;
	}
}