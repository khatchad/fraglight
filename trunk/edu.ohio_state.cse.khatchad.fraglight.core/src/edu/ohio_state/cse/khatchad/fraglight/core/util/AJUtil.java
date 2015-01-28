/**
 * 
 */
package edu.ohio_state.cse.khatchad.fraglight.core.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.ajdt.core.AspectJPlugin;
import org.eclipse.ajdt.core.javaelements.AJCodeElement;
import org.eclipse.ajdt.core.javaelements.AJCompilationUnit;
import org.eclipse.ajdt.core.javaelements.AJCompilationUnitManager;
import org.eclipse.ajdt.core.javaelements.AdviceElement;
import org.eclipse.ajdt.core.javaelements.AspectElement;
import org.eclipse.ajdt.core.javaelements.IAJCodeElement;
import org.eclipse.ajdt.core.model.AJProjectModelFacade;
import org.eclipse.ajdt.core.model.AJProjectModelFactory;
import org.eclipse.ajdt.core.model.AJRelationshipManager;
import org.eclipse.ajdt.core.model.AJRelationshipType;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import edu.ohio_state.cse.khatchad.fraglight.core.analysis.model.JoinPointType;

/**
 * @author raffi
 * 
 */
public class AJUtil {
	
	private AJUtil() {
	}

	public static Set<IJavaElement> getAdvisedJavaElements(IJavaProject project) throws JavaModelException {
		Set<IJavaElement> ret = new LinkedHashSet<IJavaElement>();
		Collection<? extends AdviceElement> adviceElements = extractAdviceElements(project);
		for ( AdviceElement advElem : adviceElements )
			ret.addAll(getAdvisedJavaElements(advElem));
		return ret;
	}

	/**
	 * @param advElem
	 * @return
	 * @throws JavaModelException
	 */
	@SuppressWarnings("unchecked")
	public static Set<IJavaElement> getAdvisedJavaElements(AdviceElement advElem)
			throws JavaModelException {
		Set<IJavaElement> ret = new LinkedHashSet<IJavaElement>();
		AJProjectModelFacade model = AJProjectModelFactory.getInstance()
				.getModelForJavaElement(advElem);
		List relationshipsForElement = model.getRelationshipsForElement(
				advElem, AJRelationshipManager.ADVISES);
		for (Iterator it = relationshipsForElement.iterator(); it.hasNext();) {
			IJavaElement target = (IJavaElement) it.next();
			
			if ( isRelatedToAspect(target) )
				continue;//TODO: consider advice elements.
			
			switch (target.getElementType()) {
				case IJavaElement.METHOD: {
					final IMethod meth = (IMethod) target;
					ret.add(meth);
					break;
				}
				case IJavaElement.TYPE: {
					// its a default ctor.
					final IType type = (IType) target;
					for (final IMethod meth : type.getMethods())
						if (meth.isConstructor()
								&& meth.getParameterNames().length == 0) {
							ret.add(meth);
						}
					break;
				}
				case IJavaElement.LOCAL_VARIABLE: {
					// its an aspect element.
					if (!(target instanceof IAJCodeElement))
						throw new IllegalStateException(
								"Something is screwy here.");
					AJCodeElement codeElem = (AJCodeElement) target;
					codeElem.getNameRange();
					ret.add(target);
					break;
				}
				default:
					throw new IllegalStateException(
							"Unexpected relationship target type: "
									+ target.getElementType());
			}
		}
		return ret;
	}

	/**
	 * @param proj
	 * @return
	 * @throws JavaModelException
	 */
	public static Collection<? extends AdviceElement> extractAdviceElements(
			final IJavaProject proj) throws JavaModelException {
		final Collection<AdviceElement> ret = new LinkedHashSet<AdviceElement>();

		if (AspectJPlugin.isAJProject(proj.getProject()))
			for (final IPackageFragment frag : proj.getPackageFragments())
				for (final ICompilationUnit unit : frag.getCompilationUnits()) {
					final ICompilationUnit mappedUnit = AJCompilationUnitManager
							.mapToAJCompilationUnit(unit);
					if (mappedUnit instanceof AJCompilationUnit) {
						final AJCompilationUnit ajUnit = (AJCompilationUnit) mappedUnit;
						for (final IType type : ajUnit.getAllTypes())
							if (type instanceof AspectElement) {
								final AspectElement aspectElem = (AspectElement) type;
								ret.addAll(Arrays
										.asList(aspectElem.getAdvice()));
							}
					}
				}
		return ret;
	}
	
	public static Collection<? extends AdviceElement> extractAdviceElements(
			final IWorkspace workspace) {
		
		Collection<AdviceElement> ret = new ArrayList<AdviceElement>();
		
		IWorkspaceRoot root = workspace.getRoot();
		IProject[] projects = root.getProjects();
		for (IProject proj : projects) {
			if (proj.isAccessible() && AspectJPlugin.isAJProject(proj)) {
				IJavaProject javaProject = JavaCore.create(proj);
				try {
					ret.addAll(AJUtil
							.extractAdviceElements(javaProject));
				}
				catch (JavaModelException e) {
					//next project.
					continue;
				}
			}
		}
		return ret;
	}

	/**
	 * @param joinPointShadow
	 * @param advice
	 * @return
	 * @throws JavaModelException
	 */
	public static boolean isCapturedBy(IJavaElement joinPointShadow,
			AdviceElement advice) throws JavaModelException {
		Set<IJavaElement> advisedJavaElements = getAdvisedJavaElements(advice);
		return advisedJavaElements.contains(joinPointShadow);
	}

	/**
	 * @param ajElem
	 */
	public static JoinPointType getJoinPointType(final IAJCodeElement ajElem) {
		final String type = ajElem.getElementName().substring(0,
				ajElem.getElementName().indexOf("("));
		final StringBuilder typeBuilder = new StringBuilder(type.toUpperCase());
		final int pos = typeBuilder.indexOf("-");
	
		final String joinPointTypeAsString = typeBuilder.replace(pos, pos + 1,
				"_").toString();
	
		final JoinPointType joinPointTypeAsEnum = JoinPointType
				.valueOf(joinPointTypeAsString);
	
		return joinPointTypeAsEnum;
	}

	/**
	 * @param elem
	 * @return
	 */
	public static boolean isRelatedToAspect(IJavaElement elem) {
		if ( elem == null )
			return false;
		else
			return (elem instanceof AdviceElement) || isRelatedToAspect(elem.getParent()); 
	}

	/**
	 * @param adviceKey
	 * @param jProjectI
	 * @return
	 * @throws JavaModelException 
	 */
	public static AdviceElement extractAdviceElement(String adviceKey,
			IJavaProject jProject) throws JavaModelException {
		Collection<? extends AdviceElement> adviceElementCol = extractAdviceElements(jProject);
		for ( AdviceElement adviceElement : adviceElementCol) {
			String key = Util.getKey(adviceElement);
			if ( key.equals(adviceKey) )
				return adviceElement;
		}
		
		return null;
	}

	/**
	 * @param addedShadowCol
	 * @param fieldSet
	 */
	public static void removeShadowsCorrespondingTo(Set<IJavaElement> addedShadowCol,
			JoinPointType... joinPointTypesToRemove) {
		for (Iterator<IJavaElement> it  = addedShadowCol.iterator(); it.hasNext(); ) {
			IJavaElement shadow = it.next(); 		
			if ( shadow instanceof IAJCodeElement ) {
				JoinPointType joinPointType = getJoinPointType((IAJCodeElement) shadow);
				if ( Arrays.asList(joinPointTypesToRemove).contains(joinPointType))
					it.remove();
			}
		}
	}

	public static void removeDummyAdvice(
			Collection<? extends IJavaElement> adviceElements) {
		for (Iterator<? extends IJavaElement> it = adviceElements.iterator(); it
				.hasNext();) {
			IJavaElement elem = it.next();
			if (elem.getHandleIdentifier().contains("Dummy.aj"))
				it.remove();
		}
	}

	/**
	 * @param shadowsInVersionI
	 */
	public static void removeShadowsCorrespondingToAspects(Set<IJavaElement> shadowCol) {
		for (Iterator<IJavaElement> it = shadowCol.iterator(); it.hasNext();) {
			IJavaElement elem = it.next();
			if (isRelatedToAspect(elem))
				it.remove();
		}
	}
}