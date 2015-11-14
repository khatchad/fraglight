package edu.ohio_state.cse.khatchad.fraglight.core.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.search.SearchMatch;

import ca.mcgill.cs.swevo.jayfx.ConversionException;
import ca.mcgill.cs.swevo.jayfx.model.FieldElement;
import ca.mcgill.cs.swevo.jayfx.model.MethodElement;
import edu.ohio_state.cse.khatchad.ajplugintools.ajayfx.Converter;
import edu.ohio_state.cse.khatchad.fraglight.core.analysis.util.TimeCollector;

/**
 * Various utility stuff.
 */
public class Util {

	public static void assertExpression(final boolean exp) {
		if (exp == false)
			throw new AssertionError("Failed assertion");
	}

	public static String stripQualifiedName(final String qualifiedName) {
		if (!qualifiedName.contains("."))
			return qualifiedName;

		final int pos = qualifiedName.lastIndexOf('.');
		return qualifiedName.substring(pos + 1);
	}

	@SuppressWarnings({ "unused" })
	private static boolean distinct(final Collection<Object> col) {
		@SuppressWarnings("rawtypes")
		final Comparable[] objs = new Comparable[col.size()];
		col.toArray(objs);
		try {
			Arrays.sort(objs);
		} catch (final ClassCastException E) {
			for (int i = 0; i < objs.length; i++)
				for (int j = i + 1; j < objs.length; j++)
					if (objs[i].equals(objs[j]))
						return false;
			return true;
		}
		for (int i = 1; i < objs.length; i++)
			if (objs[i].equals(objs[i - 1]))
				return false;
		return true;
	}

	private Util() {
	}

	/**
	 * @param values
	 * @return
	 */
	public static <E> Collection<E> flattenCollection(Collection<? extends Collection<E>> values) {
		Collection<E> ret = new LinkedHashSet<E>();
		for (Collection<E> col : values)
			for (E e : col)
				ret.add(e);
		return ret;
	}

	/**
	 * @param type
	 * @return
	 * @throws JavaModelException
	 */
	public static IMethod getDefaultConstructor(IType type) throws JavaModelException {
		for (final IMethod meth : type.getMethods())
			if (meth.isConstructor() && meth.getParameterNames().length == 0)
				return meth;
		return null;
	}

	/**
	 * @param resource
	 * @return
	 */
	public static IFile getFileFromResource(IResource resource) {
		if (resource == null)
			return null;
		else if (resource.getType() == IResource.FILE)
			return (IFile) resource;
		else
			return getFileFromResource(resource.getParent());
	}

	/**
	 * @param element
	 * @return
	 */
	public static ICompilationUnit getCompilationUnit(IJavaElement element) {
		if (element == null)
			return null;
		else if (element instanceof ICompilationUnit)
			return (ICompilationUnit) element;
		else
			return getCompilationUnit(element.getParent());
	}

	public static ASTNode getASTNode(IJavaElement elem, IProgressMonitor monitor) {
		final IMember mem = getIMember(elem);
		final ICompilationUnit icu = mem.getCompilationUnit();
		if (icu == null)
			throw new IllegalArgumentException("Source not present for " + elem);
		final ASTNode root = Util.getCompilationUnit(icu, monitor);
		return root;
	}

	public static IMember getIMember(IJavaElement elem) {

		if (elem == null)
			throw new IllegalArgumentException("Member not found.");

		switch (elem.getElementType()) {
		case IJavaElement.METHOD:
		case IJavaElement.FIELD:
		case IJavaElement.INITIALIZER:
		case IJavaElement.TYPE: {
			return (IMember) elem;
		}
		}

		return getIMember(elem.getParent());
	}

	public static CompilationUnit getCompilationUnit(ICompilationUnit icu, IProgressMonitor monitor) {
		final ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setSource(icu);
		parser.setResolveBindings(true);
		final CompilationUnit ret = (CompilationUnit) parser.createAST(monitor);
		return ret;
	}

	public static ASTNode getExactASTNode(CompilationUnit root, final SearchMatch match) {
		final ArrayList ret = new ArrayList(1);
		final ASTVisitor visitor = new ASTVisitor() {
			@Override
			public void preVisit(ASTNode node) {
				if (node.getStartPosition() == match.getOffset()) {
					ret.clear();
					ret.add(node);
				}
			}
		};
		root.accept(visitor);
		return (ASTNode) ret.get(0);
	}

	public static ASTNode getExactASTNode(IJavaElement elem, final SearchMatch match, IProgressMonitor monitor) {
		final IMember mem = getIMember(elem);
		final CompilationUnit root = Util.getCompilationUnit(mem.getCompilationUnit(), monitor);
		return getExactASTNode(root, match);
	}

	public static ASTNode getExactASTNode(SearchMatch match, IProgressMonitor monitor) {
		final IJavaElement elem = (IJavaElement) match.getElement();
		return Util.getExactASTNode(elem, match, monitor);
	}

	public static FieldDeclaration getFieldDeclaration(ASTNode node) {
		if (node == null)
			return null;
		else if (node instanceof FieldDeclaration)
			return (FieldDeclaration) node;
		else
			return getFieldDeclaration(node.getParent());
	}

	public static InfixExpression getInfixExpression(ASTNode node) {
		if (node == null)
			return null;
		else if (node instanceof InfixExpression)
			return (InfixExpression) node;
		else
			return getInfixExpression(node.getParent());
	}

	public static MethodDeclaration getMethodDeclaration(ASTNode node) {
		ASTNode trav = node;
		while (trav.getNodeType() != ASTNode.METHOD_DECLARATION)
			trav = trav.getParent();
		return (MethodDeclaration) trav;
	}

	public static SingleVariableDeclaration getSingleVariableDeclaration(ASTNode node) {
		if (node == null)
			return null;
		else if (node instanceof SingleVariableDeclaration)
			return (SingleVariableDeclaration) node;
		else
			return getSingleVariableDeclaration(node.getParent());
	}

	public static Name getTopmostName(ASTNode node) {
		if (node == null)
			return null;
		else if (node.getParent() == null || node.getParent().getNodeType() != ASTNode.QUALIFIED_NAME)
			return (Name) node;
		else
			return getTopmostName(node.getParent());
	}

	public static VariableDeclarationStatement getVariableDeclarationStatement(ASTNode node) {
		if (node == null)
			return null;
		else if (node instanceof VariableDeclarationStatement)
			return (VariableDeclarationStatement) node;
		else
			return getVariableDeclarationStatement(node.getParent());
	}

	public static String getSimpleName(String qualifiedName) {
		if (qualifiedName.indexOf('.') == -1)
			return qualifiedName;

		final int pos = qualifiedName.lastIndexOf('.');
		return qualifiedName.substring(pos + 1);
	}

	public static String getPackageName(String qualifiedName) {
		if (qualifiedName.indexOf('.') == -1)
			return ""; // there is no package name, so return empty
						// string.

		final int pos = qualifiedName.lastIndexOf('.');
		return qualifiedName.substring(0, pos);
	}

	/**
	 * @param target
	 * @return
	 * @throws JavaModelException
	 * @throws ConversionException
	 */
	public static String getTargetString(IJavaElement target) throws JavaModelException, ConversionException {
		switch (target.getElementType()) {
		case IJavaElement.METHOD: {
			IMethod methodTarget = (IMethod) target;
			MethodElement methodElement = Converter.getMethodElement(methodTarget);
			return methodElement.getId();
		}

		case IJavaElement.FIELD: {
			IField fieldTarget = (IField) target;
			FieldElement fieldElement = Converter.getFieldElement(fieldTarget);
			return fieldElement.getId();
		}
			// TODO: Add other types? Exception handles, etc.?

		default: {
			throw new IllegalArgumentException("Can't construct target string for " + target);
		}
		}
	}

	public static String getKey(IJavaElement javaElem) {
		StringBuilder key = new StringBuilder(javaElem.getHandleIdentifier());
		key.delete(0, key.indexOf("<") + 1);
		int pos = key.indexOf("!");
		if (pos != -1)
			key.delete(pos, key.length() - 1);
		return key.toString();
	}

	public static double calculateTimeStatistics(final long start, TimeCollector timeCollector) {
		long end = System.currentTimeMillis();

		long collectedTime = timeCollector.getCollectedTime();
		long newStart = start + collectedTime;
		final long elapsed = end - newStart;

		timeCollector.clear();
		final double secs = (double) elapsed / 1000;
		return secs;
	}

	public static String[] toStringArray(List<Object> objList) {
		String ret[] = new String[objList.size()];
		for (int i = 0; i < objList.size(); i++)
			ret[i] = objList.get(i).toString();
		return ret;
	}

	/**
	 * @param values
	 * @return
	 */
	public static double sum(Collection<Double> values) {
		double ret = 0;
		for (Double d : values)
			ret += d;
		return ret;
	}

	public static IProject getProject(final IJavaElement jElem) {
		IJavaProject jProject = jElem.getJavaProject();
		IProject project = jProject.getJavaProject().getProject();
		return project;
	}

	/**
	 * @param adviceCol
	 * @return
	 */
	public static Set<IProject> getProjects(final Collection<? extends IJavaElement> jElemCol) {
		final Set<IProject> ret = new LinkedHashSet<IProject>();
		for (final IJavaElement elem : jElemCol) {
			IProject project = getProject(elem);
			ret.add(project);
		}
		return ret;
	}

	public static String getBenchmarkName(IJavaProject project) {
		IJavaProject jProj = project;
		String projectName = jProj.getElementName();
		String benchmarkName = projectName.substring(0, projectName.indexOf('_'));
		benchmarkName = benchmarkName.replace("AO", "");
		return benchmarkName;
	}
}