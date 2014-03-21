package org.uninotts.timetable;

import java.util.ArrayList;
import java.util.List;

import org.studentnow.CardFactory;
import org.studentnow.CardsProvider;
import org.studentnow.ECard;
import org.studentnow.Institution;
import org.studentnow.Location;
import org.studentnow.Session;
import org.studentnow.Static.Fields;
import org.studentnow.StudentNowException;
import org.studentnow.Timetable;
import org.studentnow._;
import org.studentnow.util.DayHelper;
import org.studentnow.util.Q;
import org.studentnow.util.StringUtils;
import org.studentnow.util.Time;

public class UniNottsCardsProvider implements CardsProvider {

	public static final Institution uninotts_institution =

	new Institution("The University of Nottingham", "nottingham.ac.uk");

	@Override
	public List<ECard> renderCards(Timetable timetable)
			throws StudentNowException {

		System.out.println("Timetabe status: " + timetable.getState());

		int week = timetable.getWeek();

		long morningDate = Time.parseString("8:00").toLong();
		long eveningDate = Time.parseString("19:00").toLong();
		long midnightDate = DayHelper.getDateMidnight();
		String day = DayHelper.getTodayName();

		Session nextSession = timetable.getNextSession(), travelSession = nextSession;
		Session lastSession = timetable.getLastSession();

		long finishDate = lastSession == null ? 0 : lastSession.getDate(true);

		// Location myLocation = null, homeLocation = null, destLocation = null;
		// boolean atHome = false, atDest = false;
		// long travelArrival = 0, travelIdealArrival = 0;

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
			
//			ECard dummyTravelCard = new ECard("10 min walk to Jubilee Campus", "If you leave now you will arrive at Jubilee Campus in 10 minutes. Tap for route information.");
//			dummyTravelCard.setMapCoords("52.951928,-1.185236");
//			cards.add(dummyTravelCard);
			
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
			ECard eCard1 = new ECard("Week " + timetable.getWeek(),
					"Your sessions for this programme cycle are finished!");
			cards.add(eCard1);
			ECard eCard2 = new ECard("Select next programme",
					"Tap this if you expect your programme to continue at "
							+ uninotts_institution.getName()
							+ " to select your new programme.");
			eCard2.setType(ECard.SELECT_COURSE);
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

			// ECard eCard2z = new ECard("Events nearby",
			// "Soon we'll show you events happening at " + INST.getName()
			// + " and nearby.");
			// cards.add(eCard2z);

			return cards;

		default:
			ECard errorCard = CardFactory
					.getTemplateCard(CardFactory.Template.UNEXPECTED_ERROR);

			return CardFactory.makeSingleCardList(errorCard);

		}
		return null;
	}
}
