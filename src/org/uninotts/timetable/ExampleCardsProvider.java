package org.uninotts.timetable;

import java.util.ArrayList;
import java.util.List;

import org.studentnow.ECard;
import org.studentnow.ICardsProvider;
import org.studentnow.ILocationProvider;
import org.studentnow.StudentNowException;
import org.studentnow.TimeProgressAdapter;
import org.studentnow.Timetable;
import org.studentnow.util.Time;

public class ExampleCardsProvider implements ICardsProvider {

	ILocationProvider mLocationProvider;

	@Override
	public void setTimetable(Timetable timetable) {

	}

	@Override
	public void setLocationProvider(ILocationProvider locationProvider) {
		mLocationProvider = locationProvider;
	}

	@Override
	public List<ECard> renderCards() throws StudentNowException {
		List<ECard> cards = new ArrayList<ECard>();

		ECard dummyTravelCard = new ECard(
				"10 min walk to Jubilee Campus",
				"If you leave now you will arrive at Jubilee Campus in 10 minutes. Tap for route information.");
		dummyTravelCard.setMapCoords("52.951928,-1.185236");
		cards.add(dummyTravelCard);

		Time timeStart = Time.parseString("16:00"), timeEnd = Time
				.parseString("18:00");

		ECard card = new ECard("Lecture at " + timeStart.format2(),
				"Software Engineering Methodologies in A32, School of Computer Science");

		long timeStarts = timeStart.toDateToday().getTime();
		long timeEnds = timeEnd.toDateToday().getTime();
		card.setExpiryTime(timeEnds);
		card.setProgressAdapter(new TimeProgressAdapter(timeStarts, timeEnds));
		cards.add(card);

		return cards;
	}
}
