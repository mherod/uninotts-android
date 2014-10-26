package org.uninotts.android.service;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.studentnow.ECard;
import org.studentnow.Static.Fields;
import org.studentnow.util.QueryFactory;
import org.studentnow.util.Time;
import org.studentnow.util.UpdateThrottle;
import org.uninotts.android.MainActivity;
import org.uninotts.android.R;
import org.uninotts.android.VECard;
import org.uninotts.android.__;
import org.uninotts.android.service.LocationCache.CachedLocation;
import org.uninotts.android.util.ConnectionDetector;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

import com.fima.cardsui.objects.Card;
import com.fima.cardsui.views.CardUI;

public class CardViewWorkerModule extends ServiceModule {

	private final String TAG = CardViewWorkerModule.class.getSimpleName();

	private static final String IMG_TEXT_SRC = Fields.IMAGE_TEXT_SRC;
	private static final String IMG_MAIN_SRC = Fields.IMAGE_MAIN_SRC;
	private static final String IMG_TEXT_SRC_ = Fields.IMAGE_SRC;

	private static final String[] URL_FIELDS = { IMG_TEXT_SRC, IMG_MAIN_SRC,
			IMG_TEXT_SRC_ };

	private LiveService mLiveService;
	private UserTimetableModule mUserSyncModule;
	private LocationModule mLocationModule;

	private boolean requestCardViewUpdate = false;

	private List<ECard> cards = new ArrayList<ECard>();
	private final HashMap<String, Bitmap> bitmaps = new HashMap<String, Bitmap>();

	private UpdateThrottle maintenanceThrottle = new UpdateThrottle(60 * 1000);

	public CardViewWorkerModule(LiveService pLiveService) {
		mLiveService = pLiveService;
	}

	@Override
	public void link() {
		mUserSyncModule = (UserTimetableModule) mLiveService
				.getServiceModule(UserTimetableModule.class);
		mLocationModule = (LocationModule) mLiveService
				.getServiceModule(LocationModule.class);
	}

	@Override
	public void schedule() {
		requestUpdate();
	}

	@Override
	public void cycle() {
		if (maintenanceThrottle.isDue()) {
			maintenanceThrottle.update();
			maintainCards();
		}
		processRequests();
	}

	@Override
	public void cycleNetwork() {
		if (!isPrepared()) {
			loadBitmaps();
		}
	}

	public void requestUpdate() {
		requestCardViewUpdate = true;
	}

	public List<ECard> getCards() {
		return cards;
	}

	public Intent getCardIntent(ECard ecard) {
		if (ecard.hasLink()) {
			try {
				Uri uri = Uri.parse(ecard.getLink());
				return new Intent(Intent.ACTION_VIEW, uri);
			} catch (NullPointerException npe) {
				return null;
			}
		}
		// if (ecard.isType(ECard.LOGIN)) {
		// return new Intent(mLiveService, LoginActivity.class);
		// }
		// if (ecard.isType(ECard.SELECT_COURSE)) {
		// return new Intent(mLiveService, CourseSelectActivity.class);
		// }
		// if (ecard.isType(ECard.TRAVEL)) {
		//
		if (ecard.hasMapCoords()) {
			HashMap<String, String> params = new HashMap<String, String>();
			CachedLocation loc = getLastLocation();
			if (loc != null) {
				params.put("saddr", loc.getString());
			}
			params.put("daddr", ecard.getMapCoords());

			String query = QueryFactory.renderQuery(params);
			String url = null;
			try {
				url = new URI("https", "maps.google.com", "/maps", query, null)
						.toString();
			} catch (URISyntaxException e) {
				return null;
			}
			Uri gm = Uri.parse(url);
			return new Intent(Intent.ACTION_VIEW, gm).setClassName(
					"com.google.android.apps.maps",
					"com.google.android.maps.MapsActivity");
		}
		return new Intent(mLiveService, MainActivity.class);
	}

	private void maintainCards() {
		Iterator<ECard> cardsIterator = getCards().iterator();
		long time = Time.getNowLong();
		while (cardsIterator.hasNext()) {
			ECard c = cardsIterator.next();
			if (c.hasExpiryTime() && time > c.getExpiryTime()) {
				cardsIterator.remove();
				Log.i(TAG, "Removed card: " + c.getTitle());
			}
		}
	}

	private boolean isPrepared() {
		List<ECard> cards = getCards();
		if (cards == null || cards.size() == 0) {
			return true;
		}
		for (ECard ecard : cards) {
			if (ecard.hasMapCoords()) {
				String coords = ecard.getMapCoords();
				if (!bitmaps.containsKey(coords)) {
					Log.e(TAG, "No coords bitmap: " + coords);
					return false;
				}
			}
			String img_text_src = ecard.getFString(IMG_TEXT_SRC, IMG_TEXT_SRC_);
			if (img_text_src != null && !bitmaps.containsKey(img_text_src)) {
				Log.e(TAG, "No url bitmap: " + img_text_src);
				return false;
			}
		}
		return true;
	}

	private void loadBitmaps() {
		List<ECard> cards = getCards();
		if (cards == null || cards.size() == 0) {
			return;
		}
		for (ECard ecard : cards) {

			// Deprecated
			if (ecard.hasMapCoords()) {
				String coords = ecard.getMapCoords();
				if (!bitmaps.containsKey(coords)) {
					Bitmap b = getGoogleMapThumbnail(coords);
					if (b != null) {
						bitmaps.put(coords, b);
						Log.i(TAG, "Prepared coords bitmap: " + coords);
					}
				}
			}

			for (String url_field : URL_FIELDS) {
				String url = ecard.getString(url_field);
				if (url == null || bitmaps.containsKey(url)) {
					continue;
				}
				URL url2 = null;
				try {
					url2 = new URL(url);
				} catch (Exception e) {
					continue;
				}
				Bitmap b = bitmapFromURL(url2);
				if (b != null) {
					bitmaps.put(url, b);
					Log.i(TAG, "Prepared url bitmap: " + url);
				}
			}
		}
	}

	private void processRequests() {
		if (requestCardViewUpdate) {
			requestCardViewUpdate = false;
			updateActivityCards(mLiveService);
		}
	}

	/**
	 * 
	 * @param context
	 * @param cardsView
	 * @return true if we should render what cards are available - false causes
	 *         a block on views implementing display of card information
	 */
	public boolean renderCardsView(final Context context, CardUI cardsView) {
		List<ECard> cards = getCards();
		if (cards == null || cards.size() == 0) {
			if (!ConnectionDetector.hasNetwork(context)) {
				// No cards and no Internet - we need the user to get online
				cardsView.setSwipeable(false);
				Card myCard = new VECard(
						mLiveService.getString(R.string.card_offline_title),
						mLiveService.getString(R.string.card_offline_content));
				cardsView.addCard(myCard);

				Log.e(TAG, "No network - returning help card");
				return true;
			} else if (mUserSyncModule.mCardUpdateErrorBackoff.isSuppressed()) {
				// We don't currently have any cards, and if we are in a
				// suppressed state then we can safely assume we are getting
				// errors
				cardsView.setSwipeable(false);
				Card myCard = new VECard(
						mLiveService.getString(R.string.card_error_title),
						mLiveService.getString(R.string.card_error_content));
				cardsView.addCard(myCard);
				return true;
			}
			Log.e(TAG, "No cards to render");
			return false;
		}
		if (!isPrepared()) {
			Log.e(TAG, "Not prepared");
			return false;
		}

		cardsView.clearCards();
		cardsView.setSwipeable(false);

		long time = Time.getNowLong();

		int cardCount = 0;

		for (final ECard ecard : cards) {
			if (!ecard.isRelevantTime(time)) {
				continue;
			}
			VECard card = new VECard(ecard);

			String image_text_src = ecard.getFString(IMG_TEXT_SRC,
					IMG_TEXT_SRC_);
			if (image_text_src != null) {
				Bitmap b = bitmaps.get(image_text_src);
				if (b != null) {
					card.setMainBitmap(b);
				}
			}
			String img_main_src = ecard.getString(IMG_MAIN_SRC);
			if (img_main_src != null) {
				Bitmap b = bitmaps.get(img_main_src);
				if (b != null) {
					card.setBottomBitmap(b);
				}
			}
			// Deprecated TODO:
			String map_coords = ecard.getMapCoords();
			if (map_coords != null) {
				Bitmap b = bitmaps.get(map_coords);
				if (b != null) {
					card.setBottomBitmap(b);
				}
			}

			final Intent intent = getCardIntent(ecard);
			if (intent != null) {
				card.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						context.startActivity(intent);
					}
				});
			}
			card.setSwipeable(ecard.isSwipable());
			cardsView.addCard(card);
			cardCount++;
		}
		if (cardCount > 0) {
			cardsView.refresh();
			return true;
		}
		return false;
	}

	private CachedLocation getLastLocation() {
		try {
			LocationCache mLocationCache = mLocationModule.getLocationCache();
			return mLocationCache.getLastLocation();
		} catch (Exception e) {
			return null;
		}
	}

	public static void updateActivityCards(Context context) {
		context.sendBroadcast(new Intent(__.INTENT_CARD_UPDATE));
	}

	public static Bitmap getGoogleMapThumbnail(String... coords) {
		try {
			String urlString = "https://maps.googleapis.com/maps/api/staticmap?";
			for (String marker : coords) {
				urlString += "markers=color:red|" + marker + "&";
			}
			urlString += "size=600x350" + "&sensor=false"; // zoom=17&
			return bitmapFromURL(new URL(urlString));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static Bitmap bitmapFromURL(URL url) {
		Bitmap bmp = null;
		InputStream in = null;
		try {
			in = url.openStream();
			bmp = BitmapFactory.decodeStream(in);
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return bmp;
	}
}
