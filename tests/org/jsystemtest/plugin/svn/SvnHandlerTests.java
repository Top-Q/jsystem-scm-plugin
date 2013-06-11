package org.jsystemtest.plugin.svn;

import java.io.IOException;

import jsystem.extensions.sourcecontrol.SourceControlException;
import jsystem.extensions.sourcecontrol.SourceControlI.Status;
import jsystem.framework.FrameworkOptions;
import jsystem.framework.JSystemProperties;
import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

public class SvnHandlerTests {

	private SvnHandler handler;

	private String scenarioName = "itai";

	private String sutName = "Itai";

	String repoPath = "file:///C://Users//Agmon//Dropbox//Projects//JSystem Source Control Toolbar//JavaHL//repositories//basic_test1";

	@Before
	public void setUp() throws IOException, SourceControlException {
		JSystemProperties.getInstance().setPreference(FrameworkOptions.TESTS_SOURCE_FOLDER,
				"C:\\jsystem\\runner\\projects\\jsystemServices\\tests");
		handler = new SvnHandler();
		handler.connect(repoPath, "", "");
	}

	@Test
	public void initWorkingCopy() throws SourceControlException {
		handler.initWorkingCopy();
	}

	@Test
	public void getScenarioStatus() {
//		Status status = handler.getScenarioStatus(scenarioName);
//		System.out.println(scenarioName + " : " + status.name());
//		Assert.assertEquals(Status.NORMAL, status);
	}
	
	@Test
	public void getWcStatus(){
		boolean response = handler.isWorkingCopyInitialize();
		System.out.println(response);
		
	}
	
	@Test
	public void getSutStatus() {
		Status status = handler.getSutStatus(sutName);
		System.out.println(sutName + " : " + status.name());
		Assert.assertEquals(Status.NORMAL, status);
	}

}
