package org.jsystemtest.plugin.svn;

import jsystem.extensions.sourcecontrol.SourceControlI.Status;

import org.tigris.subversion.javahl.StatusKind;

public class StatusUtils {
	
	private StatusUtils(){
		
	}
	
	/**
	 * 
	 * 
	 * @param xmlStatus
	 * @param propStatus
	 * @return
	 */
	static Status combinedStatus(final Status xmlStatus, final Status propStatus) {

		if (xmlStatus == Status.NONE || propStatus == Status.NONE) {
			return Status.NONE;
		} else if (xmlStatus == Status.UNVERSIONED || propStatus == Status.UNVERSIONED) {
			return Status.UNVERSIONED;
		} else if (xmlStatus == Status.IGNORED || propStatus == Status.IGNORED) {
			return Status.IGNORED;
		} else if (xmlStatus == Status.CONFLICTED || propStatus == Status.CONFLICTED) {
			return Status.CONFLICTED;
		} else if (xmlStatus == Status.DELETED || propStatus == Status.DELETED) {
			return Status.DELETED;
		} else if (xmlStatus == Status.ADDED || propStatus == Status.ADDED) {
			return Status.ADDED;
		} else if (xmlStatus == Status.MODIFIED || propStatus == Status.MODIFIED) {
			return Status.MODIFIED;
		} else if (xmlStatus == Status.NORMAL && propStatus == Status.NORMAL) {
			return Status.NORMAL;
		}
		return Status.NONE;
	}

	static Status mapStatus(int kind) {
		Status status = Status.NONE;
		switch (kind) {
		case StatusKind.none:
			status = Status.NONE;
			break;
		case StatusKind.normal:
			status = Status.NORMAL;
			break;
		case StatusKind.added:
			status = Status.ADDED;
			break;
		case StatusKind.missing:
			status = Status.NONE;
			break;
		case StatusKind.deleted:
			status = Status.DELETED;
			break;
		case StatusKind.replaced:
			status = Status.MODIFIED;
			break;
		case StatusKind.modified:
			status = Status.MODIFIED;
			break;
		case StatusKind.merged:
			status = Status.MODIFIED;
			break;
		case StatusKind.conflicted:
			status = Status.CONFLICTED;
			break;
		case StatusKind.ignored:
			status = Status.IGNORED;
			break;
		case StatusKind.incomplete:
			status = Status.NONE;
			break;
		case StatusKind.external:
			status = Status.UNVERSIONED;
			break;
		case StatusKind.unversioned:
			status = Status.UNVERSIONED;
			break;
		default:
			status = Status.UNVERSIONED;
		}
		return status;

	}

	
}
