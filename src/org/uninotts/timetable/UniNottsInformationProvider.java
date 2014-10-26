package org.uninotts.timetable;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.studentnow.Course;
import org.studentnow.CourseListing;
import org.studentnow.InformationProvider;
import org.studentnow.Module;
import org.studentnow.ModuleListing;
import org.studentnow.util.DocumentInput;
import org.studentnow.util.Time;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class UniNottsInformationProvider implements InformationProvider {

	private static final String a = "-a-", b = "-b-";

	private static final String url = "http://mobile.nottingham.ac.uk/timetabling/"
			+ a + "/" + b;

	@Override
	public int getCurrentWeek() {
		Calendar calNow, calTarg;

		Date now = new Date();

		int week = 0;

		Date date;
		try {
			date = Time.DF_DATE.parse("23/09/2013");
		} catch (ParseException e1) {
			// unless something changes with other classes this won't happen
			return 0;
		}

		if (now.compareTo(date) >= 0) {
			calNow = Calendar.getInstance();
			calTarg = Calendar.getInstance();
			calTarg.setTime(date);
			int tmpweek = 0;
			while (calTarg.before(calNow)) {
				calTarg.add(Calendar.DAY_OF_MONTH, 7);
				tmpweek++;
			}
			if (tmpweek > 0 && (tmpweek < week || week == 0)) {
				week = tmpweek;
			}
		}
		return week % 52;
	}

	@Override
	public List<CourseListing> searchCourse(String query) {
		List<CourseListing> courseListings = new ArrayList<CourseListing>();
		return courseListings;
	}

	@Override
	public List<ModuleListing> searchModule(String query) {
		List<ModuleListing> moduleListings = new ArrayList<ModuleListing>();
		return moduleListings;
	}

	@Override
	public Course queryCourse(String courseCode) {
		URL url;
		try {
			url = createRequestUrl("course", courseCode);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
		}

		Document doc;
		try {
			doc = queryDocument(url);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		NodeList nl = doc.getElementsByTagName("Course");
		if (nl.getLength() > 0) {
			Node courseNode = nl.item(0);
			return parseCourse(courseNode).setCode(courseCode);
		}
		return null;
	}

	@Override
	public Module queryModule(String moduleCode) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Course expandCourseListing(CourseListing courseListing) {
		String courseCode = courseListing.getCode();
		return queryCourse(courseCode);
	}

	@Override
	public Module expandModuleListing(ModuleListing moduleListing) {
		// TODO Auto-generated method stub
		return null;
	}

	private static URL createRequestUrl(String type, String param)
			throws MalformedURLException {
		String url2 = url.replace(a, type).replace(b, param);
		return new URL(url2);
	}

	private static Document queryDocument(URL url) throws Exception {
		InputStream is = DocumentInput.getInputStream(url);
		return is == null ? null : parseXML(is);
	}

	private static Document parseXML(InputStream stream) throws Exception {
		DocumentBuilder builder = DocumentBuilderFactory.newInstance()
				.newDocumentBuilder();
		// return builder.parse(new File("G601.xml"));
		return builder.parse(stream);
	}

	private static Course parseCourse(Node node) {

		String courseName = null;
		String courseCode = null;
		String courseDescription = null;
		String courseSchool = null;

		List<Module> courseModules = new ArrayList<Module>();

		NodeList nl = node.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			Node n2 = nl.item(i);

			if (n2.getNodeName().equals("Name")) {
				courseName = n2.getTextContent();
			} else if (n2.getNodeName().equals("Description")) {
				courseDescription = n2.getTextContent();
			} else if (n2.getNodeName().equals("School")) {
				courseSchool = parseName(n2);
			} else if (n2.getNodeName().equals("Module")) {
				Module module = parseModule(n2);
				if (module != null) {
					courseModules.add(module);
				}
			}
		}
		if (courseName != null) {
			Course course = new Course(courseName);
			course.setCode(courseCode);
			course.setDescription(courseDescription);
			course.setSchool(courseSchool);
			course.getModules().addAll(courseModules);
			return course;
		}
		return null;
	}

	private static Module parseModule(Node node) {
		String moduleCode = null;
		String moduleName = null;
		String moduleSchool = null;

		Boolean isCore = null;

		GuidIndexAgent guidIndexAgent = new GuidIndexAgent();

		NodeList nl = node.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			Node n2 = nl.item(i);
			if (n2.getNodeName().equals("Code")) {
				moduleCode = n2.getTextContent();
			} else if (n2.getNodeName().equals("Description")) {
				moduleName = n2.getTextContent();
			} else if (n2.getNodeName().equals("Type")) {
				isCore = n2.getTextContent() == "core";
			} else if (n2.getNodeName().equals("Activity")) {

				UniNottsSession session = parseSession(n2);
				if (session != null && session.isValid()) {
					guidIndexAgent.index(session);
				}
			} else if (n2.getNodeName().equals("School")) {
				moduleSchool = parseName(n2);
			}
		}

		Collection<UniNottsSession> moduleSessions = guidIndexAgent
				.getIndexedSessions();

		if (moduleName != null) {
			Module module = new Module(moduleName);
			module.setCode(moduleCode);
			module.setSchool(moduleSchool);
			module.setCore(isCore);
			module.getSessions().addAll(moduleSessions);
			return module;
		}
		return null;
	}

	private static UniNottsSession parseSession(Node node) {
		UniNottsSession session = new UniNottsSession();

		NodeList nl = node.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			Node n2 = nl.item(i);
			if (n2.getNodeName().equals("StartTime")) {
				Time startTime = parseTime(n2);
				if (startTime != null) {
					session.setStartTime(startTime);
				}
			} else if (n2.getNodeName().equals("EndTime")) {
				Time endTime = parseTime(n2);
				if (endTime != null) {
					session.setEndTime(endTime);
				}
			} else if (n2.getNodeName().equals("Staff")) {
				Node person = n2.getFirstChild();
				if (person != null) {
					String name = parseName(person);
					if (name != null) {
						session.set("staff", name);
					}
				}
			} else {
				session.set(n2.getNodeName(), n2.getTextContent());
			}
		}

		return session;
	}

	private static String parseName(Node node) {
		NodeList nl = node.getChildNodes();
		for (int j = 0; j < nl.getLength(); j++) {
			Node n3 = nl.item(j);
			if (n3.getNodeName().equals("Name")) {
				return n3.getTextContent();
			}
		}
		return null;
	}

	private static Time parseTime(Node node) {
		int hours = -1, minutes = -1;

		NodeList nl = node.getChildNodes();
		for (int j = 0; j < nl.getLength(); j++) {
			Node n3 = nl.item(j);
			if (n3.getNodeName().equals("Hours")) {
				try {
					hours = Integer.parseInt(n3.getTextContent());
				} catch (Exception e) {
					return null;
				}
			} else if (n3.getNodeName().equals("Minutes")) {
				try {
					minutes = Integer.parseInt(n3.getTextContent());
				} catch (Exception e) {
					return null;
				}
			}
		}
		if (hours > -1 && minutes > -1) {
			return new Time(hours, minutes);
		}
		return null;
	}

}
