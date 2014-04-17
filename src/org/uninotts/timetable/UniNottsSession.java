package org.uninotts.timetable;

import org.studentnow.MissingFieldException;
import org.studentnow.Session;

public class UniNottsSession extends Session {

	private static final long serialVersionUID = -693903830879750196L;

	private static final String guid = "guid";

	@Override
	public String getType() throws MissingFieldException {
		return get(TYPE);
	}

	@Override
	public String getLocationName() throws MissingFieldException {
		return get(LOC_NAME);
	}

	@Override
	public String getModuleName() throws MissingFieldException {
		return get(DESC);
	}

	public String getGuid() throws MissingFieldException {
		return get(guid);
	}

}
