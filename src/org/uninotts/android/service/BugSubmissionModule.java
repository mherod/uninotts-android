package org.uninotts.android.service;

import java.util.ArrayList;
import java.util.List;

public class BugSubmissionModule extends ServiceModule {

	private List<String> pendingUploadExceptions = new ArrayList<String>();

	public BugSubmissionModule(LiveService liveService) {

	}

	@Override
	public void cycleNetwork() {
		// upload bug reports
		// TODO
	}

	public void recordException(Exception exception) {
		pendingUploadExceptions.add(exception.toString());
		// TODO
	}

}
