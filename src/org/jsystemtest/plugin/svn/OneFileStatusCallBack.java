package org.jsystemtest.plugin.svn;

import org.tigris.subversion.javahl.Status;
import org.tigris.subversion.javahl.StatusCallback;

public class OneFileStatusCallBack implements StatusCallback {

	private int statusKind;

	@Override
	public void doStatus(Status status) {
		statusKind = status.getTextStatus();
	}

	public int getStatusKind() {
		return statusKind;
	}

}
