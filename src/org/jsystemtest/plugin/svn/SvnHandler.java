package org.jsystemtest.plugin.svn;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.JOptionPane;

import jsystem.extensions.sourcecontrol.SourceControlException;
import jsystem.extensions.sourcecontrol.SourceControlI;
import jsystem.framework.FrameworkOptions;
import jsystem.framework.JSystemProperties;
import jsystem.framework.scenario.Scenario;
import jsystem.framework.scenario.ScenariosManager;
import jsystem.treeui.TestRunner;
import jsystem.utils.FileUtils;

import org.tigris.subversion.javahl.ClientException;
import org.tigris.subversion.javahl.Info2;
import org.tigris.subversion.javahl.InfoCallback;
import org.tigris.subversion.javahl.Notify2;
import org.tigris.subversion.javahl.NotifyInformation;
import org.tigris.subversion.javahl.SVNClientInterface;
import org.tigris.subversion.javahl.SVNClientSynchronized;
import org.tigris.subversion.javahl.SubversionException;

public class SvnHandler implements SourceControlI {
	private final static String SUT_FOLDER = "sut";
	private final static String SCNEARIOS_FOLDER = "scenarios";

	private SVNClientInterface client;

	private String repoPath;

	private String userName;

	private String password;

	private File testSrcFolder;

	private File testClassFolder;

	private static boolean wcInit = false;

	public SvnHandler() {
	}

	@Override
	public void connect(String repoPath, String userName, String password) throws SourceControlException {
		this.repoPath = repoPath;
		this.userName = userName;
		this.password = password;
		testSrcFolder = new File(JSystemProperties.getInstance().getPreference(FrameworkOptions.TESTS_SOURCE_FOLDER));
		if (!testSrcFolder.exists() || !testSrcFolder.isDirectory()) {
			throw new SourceControlException("Folder " + testSrcFolder + " is not exists");
		}
		testClassFolder = new File(JSystemProperties.getInstance().getPreference(FrameworkOptions.TESTS_CLASS_FOLDER));
		if (!testClassFolder.exists() || !testClassFolder.isDirectory()) {
			throw new SourceControlException("Folder " + testClassFolder + " is not exists");
		}
		try {
			initClient();
		} catch (SubversionException e) {
			throw new SourceControlException("Failed to init client");
		}

	}

	/**
	 * 
	 */
	@Override
	public void initWorkingCopy() throws SourceControlException {
		// TODO: Copy the folder to temp directory and copy back in case of
		// failure
		File sutSrcFolder = new File(testSrcFolder, SUT_FOLDER);
		delTree(sutSrcFolder);
		File scenariosSrcFolder = new File(testSrcFolder, SCNEARIOS_FOLDER);
		delTree(scenariosSrcFolder);
		File sutClassFolder = new File(testClassFolder, SUT_FOLDER);
		delTree(sutClassFolder);
		File scenarioClassFolder = new File(testClassFolder, SCNEARIOS_FOLDER);
		delTree(scenarioClassFolder);

		try {
			client.checkout(repoPath + "/tests/sut", sutClassFolder.getAbsolutePath(), null, null, 3, true, false);
			client.doExport(sutClassFolder.getAbsolutePath(), sutSrcFolder.getAbsolutePath(), null, null, true, true,
					1, System.getProperty("line.separator"));
			client.checkout(repoPath + "/tests/scenarios", scenarioClassFolder.getAbsolutePath(), null, null, 3, true,
					false);
			client.doExport(scenarioClassFolder.getAbsolutePath(), scenariosSrcFolder.getAbsolutePath(), null, null,
					true, true, 3, System.getProperty("line.separator"));

		} catch (ClientException e) {
			throw new SourceControlException("Failed to checkout folders", e);
		}

		wcInit = true;

	}

	public boolean isWorkingCopyInitialize() {
		wcInit = true;
		try {
			client.info2(testClassFolder.getAbsolutePath() + File.separator + SCNEARIOS_FOLDER, null, null, 0, null,
					new InfoCallback() {
						@Override
						public void singleInfo(Info2 info) {
							if (info.getKind() == 0) {
								wcInit = false;
							}
						}

					});
			client.info2(testClassFolder.getAbsolutePath() + File.separator + SUT_FOLDER, null, null, 0, null,
					new InfoCallback() {
						@Override
						public void singleInfo(Info2 info) {
							if (info.getKind() == 0) {
								wcInit = false;
							}
						}

					});

		} catch (ClientException e) {
			return false;
		}
		return wcInit;
	}

	// ********Scenario Handling***********

	@Override
	public Status getScenarioStatus(final Scenario scenario) throws SourceControlException {
		Status rootScenarioStatus = getSingleScnearioStatus(scenario);
		if (Status.NORMAL != rootScenarioStatus) {
			return rootScenarioStatus;
		}
		List<Scenario> subScnearios = getSubScenarios(scenario);
		for (Scenario subScenario : subScnearios) {
			Status subScnearioStatus = getSingleScnearioStatus(subScenario);
			if (Status.NORMAL != subScnearioStatus) {
				if (Status.CONFLICTED == subScnearioStatus) {
					return Status.CONFLICTED;
				}
				return Status.MODIFIED;
			}
		}
		return rootScenarioStatus;

	}

	private Status getSingleScnearioStatus(final Scenario scenario) {
		String[] filesNames = getScenarioFileNames(scenario);
		Status xmlStatus = getFileStatus(filesNames[0]);
		Status propStatus = getFileStatus(filesNames[1]);
		return StatusUtils.combinedStatus(xmlStatus, propStatus);
	}

	@Override
	public void addScenario(Scenario scenario) throws SourceControlException {
		Status scenarioStatus = getScenarioStatus(scenario);
		if (Status.UNVERSIONED != scenarioStatus) {
			return;
		}
		addSingleScenario(scenario);
		List<Scenario> subScenarios = getSubScenarios(scenario);
		for (Scenario subScenario : subScenarios) {
			addSingleScenario(subScenario);
		}

	}

	private void addSingleScenario(Scenario scenario) throws SourceControlException {
		Status scenarioStatus = getScenarioStatus(scenario);
		if (Status.UNVERSIONED != scenarioStatus) {
			return;
		}

		String[] filesNames = getScenarioFileNames(scenario);
		try {
			client.add(filesNames[0], 1, true, true, false);
			client.add(filesNames[1], 1, true, true, false);
		} catch (ClientException e) {
			e.printStackTrace();
			throw new SourceControlException("Falied to add scenario", e);
		}

	}

	@Override
	public void commitScenario(final Scenario scenario) throws SourceControlException {
		if (!isStatusSuitForCommit(scenario.getName(), getSingleScnearioStatus(scenario))) {
			return;
		}

		List<Scenario> subScnearios = getSubScenarios(scenario);
		for (Scenario subScenario : subScnearios) {
			if (!isStatusSuitForCommit(subScenario.getName(), getSingleScnearioStatus(subScenario))) {
				return;
			}
		}
		String comment = JOptionPane.showInputDialog(TestRunner.treeView, "Please type commit comment",
				"Commit comment", JOptionPane.QUESTION_MESSAGE);
		commitSingleScenario(scenario, comment);
		for (Scenario subScenario : subScnearios) {
			if (getSingleScnearioStatus(subScenario) != Status.NORMAL) {
				commitSingleScenario(subScenario, comment);
			}
		}

	}

	private void commitSingleScenario(final Scenario scenario, final String comment) throws SourceControlException {
		String[] filesNames = getScenarioFileNames(scenario);
		try {
			client.commit(filesNames, comment, 1, false, false, null, null);
		} catch (ClientException e) {
			e.printStackTrace();
			throw new SourceControlException("Falied to commit scenario " + scenario.getName(), e);
		}
	}

	@Override
	public void updateScenario(Scenario scenario) throws SourceControlException {
		Status scenarioStatus = getScenarioStatus(scenario);
		if (Status.CONFLICTED != scenarioStatus && Status.MODIFIED != scenarioStatus && Status.NORMAL != scenarioStatus) {
			return;
		}
		updateSingleScenario(scenario);
		List<Scenario> subScenarios = getSubScenarios(scenario);
		for (Scenario subScenario : subScenarios) {
			updateSingleScenario(subScenario);
		}

	}

	private void updateSingleScenario(Scenario scenario) throws SourceControlException {
		String[] filesNames = getScenarioFileNames(scenario);
		try {
			client.update(filesNames, null, 1, false, true, false);
		} catch (ClientException e) {
			e.printStackTrace();
			throw new SourceControlException("Falied to update scenario", e);
		}

	}

	@Override
	public void revertScenario(Scenario scenario) throws SourceControlException {
		revertSingleScenario(scenario);
		List<Scenario> subScenarios = getSubScenarios(scenario);
		for (Scenario subScenario : subScenarios) {
			revertSingleScenario(subScenario);
		}
	}

	private void revertSingleScenario(Scenario scenario) throws SourceControlException {
		Status scenarioStatus = getScenarioStatus(scenario);
		if (Status.CONFLICTED != scenarioStatus && Status.MODIFIED != scenarioStatus) {
			return;
		}
		String[] filesNames = getScenarioFileNames(scenario);
		try {
			client.revert(filesNames[0], 1, null);
			client.revert(filesNames[1], 1, null);
		} catch (ClientException e) {
			e.printStackTrace();
			throw new SourceControlException("Falied to revert scenario", e);
		}

	}

	/**
	 * Returns all sub scenarios of the given scenario.
	 * 
	 * @param scenario
	 * @return List of all sub scenarios of the given scenario
	 * @throws SourceControlException
	 *             if failed to retrieve scenario
	 */
	private List<Scenario> getSubScenarios(final Scenario scenario) throws SourceControlException {
		Vector<String> subScenarioNamesVector = new Vector<String>();
		scenario.getSubScenariosNames(subScenarioNamesVector);
		ArrayList<Scenario> scenarioList = new ArrayList<Scenario>();
		for (String subScenarioName : subScenarioNamesVector) {
			Scenario subScenario = null;
			try {
				subScenario = ScenariosManager.getInstance().getScenario(subScenarioName);
			} catch (Exception e) {
				throw new SourceControlException("Failed to get scenario: " + subScenarioName, e);
			}
			scenarioList.add(subScenario);
		}
		return scenarioList;
	}

	private String[] getScenarioFileNames(Scenario scenario) {
		String[] filesNames = new String[2];
		filesNames[0] = scenario.getScenarioFiles()[2].getAbsolutePath();
		filesNames[1] = scenario.getScenarioFiles()[0].getAbsolutePath();
		return filesNames;
	}

	// ********SUT Handling***********

	@Override
	public Status getSutStatus(String sutName) {
		return getFileStatus(getSutFileName(sutName));
	}

	@Override
	public void addSut(String sutName) throws SourceControlException {
		Status sutStatus = getSutStatus(sutName);
		if (Status.UNVERSIONED != sutStatus) {
			return;
		}

		try {
			client.add(getSutFileName(sutName), 1, true, true, false);
		} catch (ClientException e) {
			e.printStackTrace();
			throw new SourceControlException("Falied to add sut", e);
		}
	}

	@Override
	public void commitSut(String sutName) throws SourceControlException {
		Status sutStatus = getSutStatus(sutName);
		if (!isStatusSuitForCommit(sutName, sutStatus)) {
			return;
		}
		String comment = JOptionPane.showInputDialog(TestRunner.treeView, "Please type commit comment",
				"Commit comment", JOptionPane.QUESTION_MESSAGE);
		try {
			client.commit(new String[] { getSutFileName(sutName) }, comment, 1, false, false, null, null);
		} catch (ClientException e) {
			e.printStackTrace();
			throw new SourceControlException("Falied to commit sut", e);
		}

	}

	@Override
	public void updateSut(String sutName) throws SourceControlException {
		Status sutStatus = getSutStatus(sutName);
		if (Status.CONFLICTED != sutStatus && Status.MODIFIED != sutStatus && Status.NORMAL != sutStatus) {
			return;
		}

		try {
			client.update(new String[] { getSutFileName(sutName) }, null, 1, false, true, false);
		} catch (ClientException e) {
			e.printStackTrace();
			throw new SourceControlException("Falied to update sut", e);
		}

	}

	@Override
	public void revertSut(String sutName) throws SourceControlException {
		Status sutStatus = getSutStatus(sutName);
		if (Status.CONFLICTED != sutStatus && Status.MODIFIED != sutStatus) {
			return;
		}
		try {
			client.revert(getSutFileName(sutName), 1, null);
		} catch (ClientException e) {
			e.printStackTrace();
			throw new SourceControlException("Falied to revert sut", e);
		}
	}

	private String getSutFileName(final String sutName) {
		return testClassFolder.getAbsolutePath() + File.separator + SUT_FOLDER + File.separator + sutName;
	}

	private boolean isStatusSuitForCommit(final String fileName, final Status status) {
		if (Status.UNVERSIONED == status) {
			JOptionPane.showConfirmDialog(TestRunner.treeView, "You must add the files of " + fileName
					+ " to the repository before commiting", "SVN Message", JOptionPane.DEFAULT_OPTION,
					JOptionPane.INFORMATION_MESSAGE);
			return false;
		}
		return true;
	}

	private Status getFileStatus(String fileName) {
		try {
			OneFileStatusCallBack statusCallBack = new OneFileStatusCallBack();
			client.status(fileName, 1, false, true, true, false, null, statusCallBack);
			return StatusUtils.mapStatus(statusCallBack.getStatusKind());
		} catch (Exception e) {
			return Status.NONE;
		}

	}

	private void delTree(final File folder) throws SourceControlException {
		FileUtils.deltree(folder);
		if (folder.exists()) {
			throw new SourceControlException("Failed to delete " + folder.getAbsolutePath() + "  folder");
		}
	}

	private void initClient() throws SubversionException {
		this.client = new SVNClientSynchronized();
		this.client.notification2(new Notify2() {

			@Override
			public void onNotify(NotifyInformation info) {

			}

		});
		this.client.username(userName);
		this.client.password(password);
	}

	@Override
	public boolean supportMakeWritable() throws SourceControlException {
		return false;
	}

	@Override
	public void makeWritable(List<File> files) throws SourceControlException {
		throw new SourceControlException("Make writeable is not supported by SVN");
	}

	@Override
	public void moveFile(File source, File destination) throws SourceControlException {
		// TODO Auto-generated method stub
		
	}

}
