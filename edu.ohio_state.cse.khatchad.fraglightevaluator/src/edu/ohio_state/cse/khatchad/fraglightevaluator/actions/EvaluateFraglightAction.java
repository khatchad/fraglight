package edu.ohio_state.cse.khatchad.fraglightevaluator.actions;

import static edu.ohio_state.cse.khatchad.fraglight.core.util.AJUtil.extractAdviceElement;
import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.ajdt.core.javaelements.AJCodeElement;
import org.eclipse.ajdt.core.javaelements.AdviceElement;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.jface.dialogs.MessageDialog;
import org.jdom.DataConversionException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import au.com.bytecode.opencsv.CSVWriter;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import edu.ohio_state.cse.khatchad.fraglight.core.analysis.model.JoinPointType;
import edu.ohio_state.cse.khatchad.fraglight.core.analysis.util.FileUtil;
import edu.ohio_state.cse.khatchad.fraglight.core.util.AJUtil;
import edu.ohio_state.cse.khatchad.fraglight.core.util.Util;
import edu.ohio_state.cse.khatchad.fraglight.ui.FraglightUiPlugin;
import edu.ohio_state.cse.khatchad.fraglight.ui.PointcutChangePredictionProvider;
import edu.ohio_state.cse.khatchad.fraglight.ui.PointcutChangePredictionProvider.Prediction;
import edu.ohio_state.cse.khatchad.fraglight.ui.PredictionSet;
import edu.ohio_state.cse.khatchad.fraglightevaluator.model.Test;
import edu.ohio_state.cse.khatchad.fraglightevaluator.model.PredictionTestResult;

/**
 * Our sample action implements workbench action delegate. The action proxy will
 * be created by the workbench and shown in the UI. When the user tries to use
 * the action, this delegate will be created and execution will be delegated to
 * it.
 * 
 * @see IWorkbenchWindowActionDelegate
 */
public class EvaluateFraglightAction implements IWorkbenchWindowActionDelegate {

	/**
	 * 
	 */
	private static final String PREDICTION_FILENAME = "predictions.csv";

	/**
	 * 
	 */
	private static final String TEST_FILENAME = "tests.csv";

	protected static final String RESULT_PATH = new File(ResourcesPlugin
			.getWorkspace().getRoot().getLocation().toOSString()
			+ File.separator + "results").getPath()
			+ File.separator;

	private IWorkbenchWindow window;

	private CSVWriter predictionWriter;

	private CSVWriter testWriter;

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
		
		initReporters();

		Document xmlTestFile = null;
		try {
			xmlTestFile = getXMLTestFile();
		} catch (JDOMException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}

		Collection<Test> testCol = null;
		try {
			testCol = getTestCollection(xmlTestFile);
		} catch (DataConversionException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}

		for (Test test : testCol) {

			BiMap<String, String> oldPointcutKeyToNewPointcutKeyMap = test
					.getOldPointcutKeyToNewPointcutKeyMap();

			IWorkspace workspace = getWorkspace();
			
			buildWorkspace(workspace);

			IJavaProject jProjectI = getProject(test.getProjectI().getName(),
					workspace);
			
			IJavaProject jProjectJ = getProject(test.getProjectJ().getName(),
					workspace);

			Collection<AdviceElement> oldPointcuts = getPointcuts(
					oldPointcutKeyToNewPointcutKeyMap.keySet(), jProjectI);

			// A map from old pointcuts to new pointcuts (not just keys).
			BiMap<AdviceElement, AdviceElement> oldPointcutToNewPointcutMap = createOldPointcutToNewPointcutMap(
					oldPointcutKeyToNewPointcutKeyMap, jProjectI, jProjectJ);

			PointcutChangePredictionProvider changePredictionProvider = test.createPointcutChangePredictionProvider(
					oldPointcuts, oldPointcutToNewPointcutMap);

			// A set of join points that exist in project_j but not in project_i.
			Set<IJavaElement> addedShadowCol = getAddedShadowsBetween(
					jProjectJ, jProjectI);
			test.setNumberOfAddedShadows(addedShadowCol.size());

			PredictionSet predictionSet = test.run(changePredictionProvider, addedShadowCol);

			reportResults(test, predictionSet);
			
			test.write(this.testWriter);
		}
		
		try {
			this.predictionWriter.close();
			this.testWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		
		MessageDialog.openInformation(window.getShell(),
				"FraglightEvaluator", "Fraglight evaluated");
	}

	private static BiMap<AdviceElement, AdviceElement> createOldPointcutToNewPointcutMap(
			BiMap<String, String> oldPointcutKeyToNewPointcutKeyMap,
			IJavaProject jProjectI, IJavaProject jProjectJ) {
		BiMap<AdviceElement, AdviceElement> oldPointcutToNewPointcutMap = HashBiMap
				.create();
		for (String oldPointcutKey : oldPointcutKeyToNewPointcutKeyMap
				.keySet())
			try {
				oldPointcutToNewPointcutMap.put(
						extractAdviceElement(oldPointcutKey, jProjectI),
						extractAdviceElement(
								oldPointcutKeyToNewPointcutKeyMap
										.get(oldPointcutKey), jProjectJ));
			} catch (JavaModelException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		return oldPointcutToNewPointcutMap;
	}

	private void reportResults(Test test, PredictionSet predictionSet) {
		for (Prediction prediction : predictionSet) {

			PredictionTestResult result = new PredictionTestResult(test,
					prediction);
			result.write(this.predictionWriter);
		}
	}

	/**
	 * @param xmlTestFile
	 * @return
	 * @throws DataConversionException
	 */
	private Collection<Test> getTestCollection(Document document)
			throws DataConversionException {
		Collection<Test> ret = new ArrayList<Test>();
		Element evaluationElement = document.getRootElement();

		@SuppressWarnings("unchecked")
		List<Element> testElemCol = evaluationElement.getChildren("test");
		for (Element testElem : testElemCol) {
			Test test = new Test(testElem);
			ret.add(test);
		}
		return ret;
	}

	private static Document getXMLTestFile() throws JDOMException, IOException {
		String[] commandLineArgs = Platform.getCommandLineArgs();
		String fileName = null;
		for (int i = 0; i < commandLineArgs.length; i++)
			if (commandLineArgs[i].equals("-test")) {
				fileName = commandLineArgs[i + 1];
				break;
			}

		File file = new File(fileName);
		SAXBuilder builder = new SAXBuilder();
		return builder.build(file);
	}

	private Collection<AdviceElement> getPointcuts(
			Collection<String> oldPointcutKeys, IJavaProject jProjectI) {
		Collection<AdviceElement> oldPointcuts = new ArrayList<AdviceElement>();
		for (String adviceKey : oldPointcutKeys)
			try {
				oldPointcuts.add(AJUtil.extractAdviceElement(adviceKey,
						jProjectI));
			} catch (JavaModelException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		return oldPointcuts;
	}

	private IJavaProject getProject(final String version_i, IWorkspace workspace) {
		IProject projectI = workspace.getRoot().getProject(version_i);
		IJavaProject jProjectI = JavaCore.create(projectI);
		return jProjectI;
	}

	private void buildWorkspace(IWorkspace workspace) {
		try {
			workspace.build(IncrementalProjectBuilder.FULL_BUILD, null);
		} catch (CoreException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	private static Set<IJavaElement> getAddedShadowsBetween(
			IJavaProject jProjectJ, IJavaProject jProjectI) {
		Set<IJavaElement> shadowsInVersionI = null;
		Set<IJavaElement> shadowsInVersionJ = null;
		try {
			shadowsInVersionI = AJUtil.getAdvisedJavaElements(jProjectI);
			AJUtil.removeShadowsCorrespondingToAspects(shadowsInVersionI);

			shadowsInVersionJ = AJUtil.getAdvisedJavaElements(jProjectJ);
			AJUtil.removeShadowsCorrespondingToAspects(shadowsInVersionJ);

		} catch (JavaModelException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}

		Set<IJavaElement> addedShadowCol = getAddedShadowsBetween(
				shadowsInVersionJ, shadowsInVersionI);
		return addedShadowCol;
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
			String key = Util.getKey(elem);
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
			ret.add(Util.getKey(elem));
		return ret;
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

	private void initReporters() {
		final File resultFolder = new File(RESULT_PATH);
		if (!resultFolder.exists())
			resultFolder.mkdir();

		try {
			this.testWriter = getWriter(TEST_FILENAME);
			this.predictionWriter = getWriter(PREDICTION_FILENAME);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		testWriter.writeNext(Test.getHeader());
		predictionWriter.writeNext(PredictionTestResult.getHeader());
	}

	private static CSVWriter getWriter(String fileName) throws IOException {
		final File aFile = new File(RESULT_PATH + fileName);
		PrintWriter printWriter = FileUtil.getPrintWriter(aFile, false);
		return new CSVWriter(printWriter);
	}
}