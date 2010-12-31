package edu.ohio_state.cse.khatchad.fraglightevaluator.actions;

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.aspectj.asm.internal.JDTLikeHandleProvider;
import org.eclipse.ajdt.core.AspectJCore;
import org.eclipse.ajdt.core.javaelements.AdviceElement;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.internal.UIPlugin;
import org.eclipse.jface.dialogs.MessageDialog;

import edu.ohio_state.cse.khatchad.fraglight.core.util.AJUtil;
import edu.ohio_state.cse.khatchad.fraglight.core.util.Util;

/**
 * Our sample action implements workbench action delegate. The action proxy will
 * be created by the workbench and shown in the UI. When the user tries to use
 * the action, this delegate will be created and execution will be delegated to
 * it.
 * 
 * @see IWorkbenchWindowActionDelegate
 */
public class EvaluateFraglightAction implements IWorkbenchWindowActionDelegate {
	private IWorkbenchWindow window;

	/**
	 * The constructor.
	 */
	public EvaluateFraglightAction() {
	}

	/**
	 * The action has been activated. The argument of the method represents the
	 * 'real' action sitting in the workbench UI.
	 * 
	 * @see IWorkbenchWindowActionDelegate#run
	 */
	public void run(IAction action) {

		final String version_i = "FraglightTest1_v1";
		final String version_j = "FraglightTest1_v2";

		IWorkspace workspace = getWorkspace();

		try {
			workspace.build(IncrementalProjectBuilder.FULL_BUILD, null);
		} catch (CoreException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}

		IProject projectI = workspace.getRoot().getProject(version_i);
		IJavaProject jProjectI = JavaCore.create(projectI);

		IProject projectJ = workspace.getRoot().getProject(version_j);
		IJavaProject jProjectJ = JavaCore.create(projectJ);

		// would like to have a set of join points that exist in project_j but
		// not in project_i.
		Set<IJavaElement> shadowsInVersionI = null;
		Set<IJavaElement> shadowsInVersionJ = null;
		try {
			shadowsInVersionI = AJUtil.getAdvisedJavaElements(jProjectI);
			removeShadowsCorrespondingToAspects(shadowsInVersionI);

			shadowsInVersionJ = AJUtil.getAdvisedJavaElements(jProjectJ);
			removeShadowsCorrespondingToAspects(shadowsInVersionJ);

		} catch (JavaModelException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}

		Set<IJavaElement> addedShadows = getAddedShadowsBetween(
				shadowsInVersionJ, shadowsInVersionI);

		Collection<? extends AdviceElement> adviceElements = null;
		try {
			adviceElements = AJUtil.extractAdviceElements(jProjectI);
		} catch (JavaModelException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}

		removeDummyAdvice(adviceElements);
		
		for ( IJavaElement elem : adviceElements )
			System.out.println(elem);

		MessageDialog.openInformation(window.getShell(), "FraglightEvaluator",
				"Fraglight evaluated");
	}

	private void removeDummyAdvice(
			Collection<? extends IJavaElement> adviceElements) {
		for (Iterator<? extends IJavaElement> it = adviceElements.iterator(); it
				.hasNext();) {
			IJavaElement elem = it.next();
			if (elem.getHandleIdentifier().contains("Dummy.aj"))
				it.remove();
		}
	}

	/**
	 * @param shadowsInVersionJ
	 * @param shadowsInVersionI
	 * @return
	 */
	private static Set<IJavaElement> getAddedShadowsBetween(
			Set<IJavaElement> shadowsInVersionJ,
			Set<IJavaElement> shadowsInVersionI) {

		Set<String> shadowKeysfromVersionJ = getShadowKeys(shadowsInVersionJ);
		Set<String> shadowKeysfromVersionI = getShadowKeys(shadowsInVersionI);

		Set<String> addedShadowKeys = new LinkedHashSet<String>(
				shadowKeysfromVersionJ);
		addedShadowKeys.removeAll(shadowKeysfromVersionI);

		Set<IJavaElement> ret = new LinkedHashSet<IJavaElement>(
				shadowsInVersionJ);

		for (Iterator<IJavaElement> it = ret.iterator(); it.hasNext();) {
			IJavaElement elem = it.next();
			String key = getKey(elem);
			if (!addedShadowKeys.contains(key))
				it.remove();
		}

		return ret;
	}

	/**
	 * @param shadowsInVersionJ
	 * @return
	 */
	private static Set<String> getShadowKeys(Set<IJavaElement> shadowCol) {
		Set<String> ret = new LinkedHashSet<String>();
		for (IJavaElement elem : shadowCol)
			ret.add(getKey(elem));
		return ret;
	}

	/**
	 * @param shadowsInVersionI
	 */
	private void removeShadowsCorrespondingToAspects(Set<IJavaElement> shadowCol) {
		for (Iterator<IJavaElement> it = shadowCol.iterator(); it.hasNext();) {
			IJavaElement elem = it.next();
			if (AJUtil.isRelatedToAspect(elem))
				it.remove();
		}
	}

	/**
	 * Selection in the workbench has been changed. We can change the state of
	 * the 'real' action here if we want, but this can only happen after the
	 * delegate has been created.
	 * 
	 * @see IWorkbenchWindowActionDelegate#selectionChanged
	 */
	public void selectionChanged(IAction action, ISelection selection) {
	}

	/**
	 * We can use this method to dispose of any system resources we previously
	 * allocated.
	 * 
	 * @see IWorkbenchWindowActionDelegate#dispose
	 */
	public void dispose() {
	}

	/**
	 * We will cache window object in order to be able to provide parent shell
	 * for the message dialog.
	 * 
	 * @see IWorkbenchWindowActionDelegate#init
	 */
	public void init(IWorkbenchWindow window) {
		this.window = window;
	}

	public static String getKey(IJavaElement javaElem) {
		StringBuilder key = new StringBuilder(javaElem.getHandleIdentifier());
		key.delete(0, key.indexOf("<") + 1);
		int pos = key.indexOf("!");
		if (pos != -1)
			key.delete(pos, key.length() - 1);
		return key.toString();
	}
}