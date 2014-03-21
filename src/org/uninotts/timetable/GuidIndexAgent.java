package org.uninotts.timetable;

import java.util.Collection;
import java.util.HashMap;

import org.studentnow.MissingFieldException;
import org.studentnow.Session;
import org.studentnow.StudentNowException;

public class GuidIndexAgent {

	private int uniqueNumber = 0;

	private HashMap<String, UniNottsSession> indexedSessions = new HashMap<String, UniNottsSession>();

	// this class will be for indexing elements by GUID and using that
	// information to cluster data

	/**
	 * Sessions added to this instance of GuidIndexAgent will be merged
	 * 
	 * @param session
	 * @return
	 */
	public boolean index(UniNottsSession inputSession) {
		String guid;
		try {
			guid = inputSession.getGuid();
		} catch (StudentNowException sne) {
			indexedSessions.put("no-guid-" + uniqueNumber++, inputSession);
			return true;
		}

		boolean conflict = false;

		UniNottsSession mergeSession = indexedSessions.get(guid);
		if (mergeSession == null) {
			indexedSessions.put(guid, inputSession);
			return true;
		}
		if (inputSession.getFields().size() != mergeSession.getFields().size()) {
			conflict = true;
		} else {

			for (String key : mergeSession.getFields().keySet()) {
				String oField, nField;
				try {
					oField = mergeSession.get(key);
					nField = inputSession.get(key);
				} catch (MissingFieldException e) {
					indexedSessions.put("no-guid-" + uniqueNumber++,
							inputSession);
					return true;
				}
				if (oField.equals(nField)) {
					continue;
				}
				if (key.equals(Session.WEEK)) {
					mergeSession.set(key, nField);
					continue;
				}
				if (key.equals("date")) {
					continue;
				}
				conflict = true;
				System.out.println("Conflict on " + key);
			}

		}
		if (conflict) {

			indexedSessions.put(guid + "-" + uniqueNumber++, inputSession);
			return true;
		}
		return false;
	}

	public Collection<UniNottsSession> getIndexedSessions() {
		return indexedSessions.values();
	}

}
