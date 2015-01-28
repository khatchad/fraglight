/**
 * 
 */
package edu.ohio_state.cse.khatchad.fraglight.core.util;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;

/**
 * @author raffi
 *
 */
public class SearchEngineUtil {
	private SearchEngineUtil() {}

	/**
	 * @param pattern
	 * @return
	 */
	public static Collection<SearchMatch> search(final SearchPattern pattern,
			IJavaSearchScope scope, IProgressMonitor monitor) {
		final SearchEngine engine = new SearchEngine();
		final Collection<SearchMatch> results = new ArrayList<SearchMatch>();
		try {
			engine.search(pattern, new SearchParticipant[] { SearchEngine
					.getDefaultSearchParticipant() }, scope,
					new SearchRequestor() {
	
						@Override
						public void acceptSearchMatch(final SearchMatch match)
								throws CoreException {
							if (match.getAccuracy() == SearchMatch.A_ACCURATE
									&& !match.isInsideDocComment())
								results.add(match);
						}
					}, monitor);
		}
		catch (final NullPointerException e) {
			System.err.println("Caught " + e
					+ " from search engine. Rethrowing.");
			throw e;
		}
		catch (final CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return results;
	}

	public static Collection<SearchMatch> search(final SearchPattern pattern, IProgressMonitor monitor) {
		return search(pattern, SearchEngine.createWorkspaceScope(), monitor);
	}

	public static Collection<SearchMatch> search(final SearchPattern pattern,
			IJavaSearchScope scope, final ISourceRange range, IProgressMonitor monitor) {
		final SearchEngine engine = new SearchEngine();
		final Collection<SearchMatch> results = new ArrayList<SearchMatch>();
		try {
			engine.search(pattern, new SearchParticipant[] { SearchEngine
					.getDefaultSearchParticipant() }, scope,
					new SearchRequestor() {
	
						@Override
						public void acceptSearchMatch(final SearchMatch match)
								throws CoreException {
							if (match.getAccuracy() == SearchMatch.A_ACCURATE
									&& !match.isInsideDocComment()
									&& match.getOffset() == range.getOffset()
									&& match.getLength() == range.getLength())
								results.add(match);
						}
					}, monitor);
		}
		catch (final NullPointerException e) {
			System.err.println("Caught " + e
					+ " from search engine. Rethrowing.");
			throw e;
		}
		catch (final CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return results;
	}

}
