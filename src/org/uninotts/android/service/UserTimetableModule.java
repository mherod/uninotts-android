package org.uninotts.android.service;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.studentnow.Course;
import org.studentnow.ECard;
import org.studentnow.ICardsProvider;
import org.studentnow.Module;
import org.studentnow.Timetable;
import org.studentnow.util.DayHelper;
import org.studentnow.util.ExponentialBackoff;
import org.studentnow.util.Time;
import org.studentnow.util.UpdateThrottle;
import org.uninotts.android.MainActivity;
import org.uninotts.android.__;
import org.uninotts.android.util.ConnectionDetector;
import org.uninotts.android.util.OFiles;
import org.uninotts.timetable.ExampleCardsProvider;
import org.uninotts.timetable.UniNottsCardsProvider;
import org.uninotts.timetable.UniNottsInformationProvider;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

public class UserTimetableModule extends ServiceModule {

	public final static String TAG = UserTimetableModule.class.getSimpleName();

	private final static String FILE_PERS = "cardupdatethrottle.dat";
	private final static String FILE_CARDS = "cards.dat";

	private final static int SYNC_INTERVAL = 1000 * 60 * 60;

	private LiveService mLiveService;
	private CardViewWorkerModule mCVWModule;
	private UserAccountModule mUserAccountModule;
	private LocationModule mLocationModule;
	private BugSubmissionModule mBugSubmissionModule;

	private UniNottsInformationProvider mInformationProvider;
	private ICardsProvider mCardsProvider;

	private AlarmManager mAlarmManager;

	private PendingIntent partDailyIntent, fullDailyIntent;

	private UpdateThrottle mCardUpdateThrottle;

	private boolean requestUpdate = false;
	private boolean requestCardRefresh = false;
	private boolean requestSave = false;

	protected ExponentialBackoff mCardUpdateErrorBackoff = new ExponentialBackoff();

	public UserTimetableModule(LiveService liveService) {
		this.mLiveService = liveService;
		this.partDailyIntent = PendingIntent.getBroadcast(liveService, 0,
				new Intent(__.INTENT_UPDATE_CARDS), 0);
		this.fullDailyIntent = PendingIntent.getBroadcast(liveService, 1,
				new Intent(__.INTENT_UPDATE_CARDS), 0);
	}

	@Override
	public void link() {
		mAlarmManager = (AlarmManager) mLiveService
				.getSystemService(Context.ALARM_SERVICE);
		mCVWModule = ((CardViewWorkerModule) mLiveService
				.getServiceModule(CardViewWorkerModule.class));
		mUserAccountModule = (UserAccountModule) mLiveService
				.getServiceModule(UserAccountModule.class);
		mLocationModule = (LocationModule) mLiveService
				.getServiceModule(LocationModule.class);
		mBugSubmissionModule = (BugSubmissionModule) mLiveService
				.getServiceModule(BugSubmissionModule.class);

		mInformationProvider = new UniNottsInformationProvider();
		mCardsProvider = new ExampleCardsProvider();
		mCardsProvider.setLocationProvider(mLocationModule);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void load() {
		String f = OFiles.getFolder(mLiveService);
		try {
			mCardUpdateThrottle = (UpdateThrottle) OFiles.read(f + FILE_PERS);
		} catch (Exception e) {
			mCardUpdateThrottle = new UpdateThrottle();
		}

		List<ECard> loadCards = null;
		try {
			loadCards = (List<ECard>) OFiles.read(f + FILE_CARDS);
			Log.i(TAG, "Recovered " + loadCards.size()
					+ " cards from previous service session");
		} catch (Exception e) {
			Log.e(TAG, e.toString() + " loading cards");
		}
		if (loadCards != null) {
			mCVWModule.getCards().clear();
			mCVWModule.getCards().addAll(loadCards);
		}
	}

	@Override
	public void schedule() {
		mLiveService.registerReceiver(updateReciever, new IntentFilter(
				__.INTENT_UPDATE_CARDS));

		requestUpdate();

		long midnightRefreshDate = DayHelper.getDateMidnight() + (30 * 1000);
		long sixHoursRefreshDate = Time
				.getNowTimeAdd(AlarmManager.INTERVAL_DAY / 4);

		scheduleRepeatIntentRTC(fullDailyIntent, 2, midnightRefreshDate);
		scheduleRepeatIntentRTC(partDailyIntent, 6, sixHoursRefreshDate);
	}

	public boolean scheduleRepeatIntentRTC(PendingIntent pi, int timesPerDay,
			long start) {
		if (pi == null) {
			return false;
		}
		if (timesPerDay <= 0) {
			return false;
		}
		long i = AlarmManager.INTERVAL_DAY / timesPerDay;
		if (start == 0) {
			start = System.currentTimeMillis() + 5000;
		}
		mAlarmManager.setInexactRepeating(AlarmManager.RTC, start, i, pi);
		return true;
	}

	@Override
	public void cancel() {
		mAlarmManager.cancel(partDailyIntent);
		mAlarmManager.cancel(fullDailyIntent);
		mLiveService.unregisterReceiver(updateReciever);
	}

	@Override
	public void cycle() {
		if (!requestUpdate && mCVWModule.getCards().size() == 0) {
			requestUpdate = true;
		} else if (mCardUpdateThrottle.isDue(SYNC_INTERVAL)) {
			requestUpdate = true;
		}
		if (requestCardRefresh && mCVWModule != null) {
			mCVWModule.requestUpdate();
			requestCardRefresh = false;
		}
		if (requestSave && save()) {
			requestSave = false;
		}

	}

	@Override
	public void cycleNetwork() {
		if (mUserAccountModule != null && mUserAccountModule.hasAuthResponse()) {
			if (requestUpdate && !mCardUpdateErrorBackoff.isSuppressed()) {

				// Block - No network connection available to device
				if (!ConnectionDetector.hasNetwork(mLiveService)) {
					long r = mCardUpdateErrorBackoff.suppress();
					String c = "No network connection - retry in " + r / 1000
							+ "s";
					Log.e(TAG, c);
					MainActivity.showAlert(mLiveService, c);
					return;
				}

				List<ECard> newCards = null;

				try {
					Course course = mInformationProvider.queryCourse("G601");

					if (course == null) {
						throw new NullPointerException();
					}
					try {
						List<Module> modules = course.getCoreModules();

						Timetable timetable2 = new Timetable(
								mInformationProvider);
						timetable2.addModules(modules);
						timetable2.sync();

						mCardsProvider.setTimetable(timetable2);

						newCards = mCardsProvider.renderCards();

					} catch (Exception e) {
						mBugSubmissionModule.recordException(e);

						Log.e(TAG,
								"Timetable update error processing data provided by API");
						e.printStackTrace();
					}
				} catch (Exception sne) {
					Log.e(TAG,
							"Timetable update error retrieving new course data");
					sne.printStackTrace();
				}

				if (newCards != null && newCards.size() > 0) {

					Log.d(TAG, "Updating cards with " + newCards.size());

					mCVWModule.getCards().clear();
					mCVWModule.getCards().addAll(newCards);

					requestUpdate = false;
					requestCardRefresh = requestSave = true;

					mCardUpdateErrorBackoff.reset();
					mCardUpdateThrottle.update();

				} else {
					long r = mCardUpdateErrorBackoff.suppress();
					String c = "Refresh error - retry in " + r / 1000 + "s";
					Log.e(TAG, c);
					MainActivity.showAlert(mLiveService, c);
				}
			}
		}
	}

	private BroadcastReceiver updateReciever = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			requestUpdate();
			mLocationModule.requestLocationUpdate(true);
		}
	};

	public void clearLocalData() {
		mCVWModule.getCards().clear();
		requestSave = true;
		Log.i(TAG, "Cleared local sync data");
	}

	public void requestUpdate() {
		this.requestUpdate = true;
	}

	@Override
	public boolean save() {
		String f = OFiles.getFolder(mLiveService);
		List<ECard> cards = mCVWModule.getCards();
		return save(f + FILE_PERS, mCardUpdateThrottle,
				mCardUpdateThrottle != null)
				&& save(f + FILE_CARDS, cards, cards.size() > 0);
	}

	private boolean save(String file, Object o, boolean toSave) {
		File f = new File(file);
		if (toSave) {
			try {
				OFiles.saveObject(o, f);
			} catch (IOException e) {
				Log.e(TAG, e.toString());
				return false;
			}
			Log.i(TAG, "Saved " + o.toString() + " to " + file);
			return true;
		}
		return !f.exists() || f.delete();
	}

}
