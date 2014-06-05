package org.uninotts.timetable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.parser.ParseException;
import org.studentnow.CardFactory;
import org.studentnow.ECard;
import org.studentnow.ICardsProvider;
import org.studentnow.ILocationProvider;
import org.studentnow.Institution;
import org.studentnow.Location;
import org.studentnow.Session;
import org.studentnow.Static.Fields;
import org.studentnow.StudentNowException;
import org.studentnow.Timetable;
import org.studentnow._;
import org.studentnow.gd.Directions;
import org.studentnow.gd.DirectionsQueryURL;
import org.studentnow.gd.DistanceHelper;
import org.studentnow.gd.GMapsLocation;
import org.studentnow.gd.Leg;
import org.studentnow.gd.QueryFailedException;
import org.studentnow.gd.Route;
import org.studentnow.util.DayHelper;
import org.studentnow.util.Q;
import org.studentnow.util.StringUtils;
import org.studentnow.util.Time;

public class UniNottsCardsProvider implements ICardsProvider {

	private Timetable timetable;
	private ILocationProvider locationProvider;

	public static final Institution uninotts_institution =

	new Institution("The University of Nottingham", "nottingham.ac.uk");

	@Override
	public void setTimetable(Timetable timetable) {
		this.timetable = timetable;
	}

	@Override
	public void setLocationProvider(ILocationProvider locationProvider) {
		this.locationProvider = locationProvider;
	}

	@SuppressWarnings("unused")
	// TODO
	@Override
	public List<ECard> renderCards() throws StudentNowException {

		System.out.println("Timetabe status: " + timetable.getState());

		int week = timetable.getWeek();

		long morningDate = Time.parseString("8:00").toDateToday().getTime();
		long eveningDate = Time.parseString("19:00").toDateToday().getTime();
		long midnightDate = DayHelper.getDateMidnight();
		String day = DayHelper.getTodayName();

		Session nextSession = timetable.getNextSession(), travelSession = nextSession;
		Session lastSession = timetable.getLastSession();

		long finishDate = lastSession == null ? 0 : lastSession.getDate(true);

		Location myLocation = null, destLocation = null;
		boolean atDestination = false;
		float destDistance;
		if (locationProvider != null) {
			myLocation = locationProvider.getLocation();
		}
		if (myLocation != null && destLocation != null) { //
			destDistance = DistanceHelper.distMeters(myLocation, destLocation);
			atDestination = destDistance < 200;

		}

		List<ECard> cards = new ArrayList<ECard>();

		if (week == 1) {
			cards.add(new ECard("Welcome to " + uninotts_institution.getName(),
					"It's the first week of your programme this year!"));
		}

		switch (timetable.getState()) {
		case ALL:
			break;

		case DAY_ENDED:
			String continues;
			if (nextSession == null) {
				continues = "soon";
			} else if (nextSession.isTomorrow()) {
				continues = "tomorrow";
			} else {
				continues = nextSession.get(Fields.DAY);
			}

			ECard dayWeekCard = new ECard("Week " + week,
					"You've finished for today!" + " "
							+ "Your sessions continue " + continues + ".");
			dayWeekCard.setExpiryTime(midnightDate);

			cards.add(dayWeekCard);

			int k = 0;
			String upcoming = "";
			for (Session session : timetable.getSessions()) {
				if (session.hasPassed())
					continue;
				if (k++ >= 5)
					break;
				upcoming += (session.getWeekdayName() + " "
						+ session.getStartTime().format2() + ": "
						+ StringUtils.shorten(session.getModuleName(), 28) + _.nl);
			}
			if (nextSession != null && upcoming.length() > 0) {
				String continuesTitle = StringUtils.capitalize(continues)
						+ " from " + nextSession.getStartTime().format2();

				ECard card = new ECard(continuesTitle, upcoming);
				card.setNotificationTime(eveningDate);
				card.setExpiryTime(midnightDate);
				cards.add(card);
			}
			return cards;

		case ERROR:
			break;

		case LIVE:
			ECard wkCard = CardFactory.mkWkCard(CardFactory.Week.LIVE,
					timetable.getWeek(), day);
			if (Q.nNull(wkCard)) {
				if (finishDate > 0) {
					wkCard.setExpiryTime(finishDate);
				}
				cards.add(wkCard);
			}

			// ECard dummyTravelCard = new
			// ECard("10 min walk to Jubilee Campus",
			// "If you leave now you will arrive at Jubilee Campus in 10 minutes. Tap for route information.");
			// dummyTravelCard.setMapCoords("52.951928,-1.185236");
			// cards.add(dummyTravelCard);

			for (Session s : timetable.getSessions()) {
				if (s.hasPassed()) {
					continue;
				}

				ECard sCard = CardFactory.mkSessionCard(s,
						uninotts_institution.getDomain());
				cards.add(sCard);
			}

			return cards;

		case PROGRAMME_ENDED:
			cards.add(new ECard("Week " + timetable.getWeek(),
					"Your sessions for this programme cycle are finished!"));
			ECard eCard2 = new ECard("Select next programme",
					"Tap this if you expect your programme to continue at "
							+ uninotts_institution.getName()
							+ " to select your new programme.");
			// TODO: fix action
			eCard2.setNotificationTime(morningDate);
			cards.add(eCard2);
			return cards;

		case UNAVAILABLE:
			break;

		case UPDATE:
			break;

		case WEEK_EMPTY:
			if (week == 1) {
				ECard freshersWk = CardFactory.mkWkCard(
						CardFactory.Week.FRESHERS, 1, day);
				if (Q.nNull(freshersWk)) {
					freshersWk.setExpiryTime(midnightDate);
					cards.add(freshersWk);
				}
			} else {
				ECard eCard1a = new ECard("Week " + timetable.getWeek(),
						"You don't have any sessions this week!");
				cards.add(eCard1a);
				ECard eCard2a = new ECard(
						"Events nearby",
						"Student Now is a developing project, soon your university will provide a listing of activities here.");
				cards.add(eCard2a);
			}
			return cards;

		case WEEK_ENDED:
			ECard wkCard3 = CardFactory.mkWkCard(CardFactory.Week.END_WK, week,
					null); // TODO: day
			if (Q.nNull(wkCard3)) {
				wkCard3.setExpiryTime(midnightDate);
				cards.add(wkCard3);
			}

			int k2 = 0;
			String upcoming2 = "";
			for (Session session : timetable.getSessions()) {
				if (!session.isWeek(week + 1)) {
					continue;
				}
				if (k2++ >= 5)
					break;
				upcoming2 += (session.getWeekdayName() + " "
						+ session.getStartTime().format2() + ": "
						+ StringUtils.shorten(session.getModuleName(), 28) + _.nl);
			}
			if (upcoming2.length() > 0) {
				ECard card = new ECard("Next week", upcoming2);
				card.setExpiryTime(midnightDate);
				cards.add(card);
			}
			return cards;

		default:
			ECard errorCard = new ECard(
					"Error processing timetable",
					"Something went wrong but your timetable will return as soon as the problem is resolved.");
			errorCard.setExpiryTime(Time.getNowTimeAdd(60000));
			return CardFactory.makeSingleCardList(errorCard);

		}
		return null;
	}

	private static Leg getLeg(GMapsLocation l1, GMapsLocation l2, long time,
			boolean arrivals) throws IOException, ParseException,
			QueryFailedException {
		DirectionsQueryURL directionURL = new DirectionsQueryURL(
				l1.getString(), l2.getString(), true);
		if (directionURL != null) {
			if (time > 0) {
				directionURL.addParam("mode", "transit");
				directionURL.addParam(arrivals ? "arrival_time"
						: "departure_time", String.valueOf(time));
			}
			Directions directions = Directions.fetch(directionURL);
			Route route = directions.getRoutes().get(0);
			for (Leg leg : route.getLegs()) {
				if (leg == null) {
					continue;
				}
				return leg;
			}
		}
		return null;
	}

}
