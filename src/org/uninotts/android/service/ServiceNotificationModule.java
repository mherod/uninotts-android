package org.uninotts.android.service;

import org.studentnow.ECard;
import org.studentnow.util.UpdateHold;
import org.uninotts.android.CardNotification;
import org.uninotts.android.MainActivity;
import org.uninotts.android.R;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class ServiceNotificationModule extends ServiceModule {

	public static final String TAG = ServiceNotificationModule.class
			.getSimpleName();

	public static final int NOTIFICATION_ID = 1;

	private static final int updateInterval = 20 * 60 * 1000;

	private LiveService mLiveService = null;

	private NotificationManager mNotificationManager;

	private CardProviderModule mCardModule;

	public ServiceNotificationModule(LiveService pLiveService) {
		this.mLiveService = pLiveService;
	}

	@Override
	public void link() {
		mNotificationManager = (NotificationManager) mLiveService
				.getSystemService(Context.NOTIFICATION_SERVICE);

		mCardModule = ((CardProviderModule) mLiveService
				.getServiceModule(CardProviderModule.class));
	}

	@Override
	public void load() {

	}

	private UpdateHold updateHold = new UpdateHold();

	private int notificationNumber = 0;
	
	private String notificationTitle = "University of Nottingham";	
	private String notificationText = "Starting up...";

	@Override
	public void cycle() {
		if (updateHold.isDue(updateInterval)) {
			updateHold.update();

			for (ECard c : mCardModule.getCards()) {
				notificationTitle = c.getTitle();
				notificationText = c.getDesc();
				break;
			}
			notificationNumber = mCardModule.getCards().size();

			mNotificationManager.notify(NOTIFICATION_ID, create(mLiveService));
		}
	}

	public Notification create(Context context) {
		final Resources res = context.getResources();

		final Bitmap picture = BitmapFactory.decodeResource(res,
				R.drawable.ic_stat_uninotts_white);

		final String title = res.getString(R.string.uni_nottingham);

		final NotificationCompat.Builder builder = new NotificationCompat.Builder(
				context)
				.setDefaults(Notification.DEFAULT_ALL)
				.setSmallIcon(R.drawable.ic_stat_uninotts_white)
				.setContentTitle(notificationTitle)
				.setContentText(notificationText)
				.setPriority(NotificationCompat.PRIORITY_MIN)
				.setLargeIcon(picture)
				.setTicker(null)
				.setNumber(notificationNumber)

				.setContentIntent(
						PendingIntent.getActivity(context, 0, new Intent(
								context, MainActivity.class),
								PendingIntent.FLAG_UPDATE_CURRENT))

				.addAction(
						R.drawable.ic_action_stat_share,
						res.getString(R.string.action_share),
						PendingIntent.getActivity(context, 0, Intent
								.createChooser(
										new Intent(Intent.ACTION_SEND).setType(
												"text/plain")
												.putExtra(Intent.EXTRA_TEXT,
														"Dummy text"),
										"Dummy title"),
								PendingIntent.FLAG_UPDATE_CURRENT))

				.addAction(R.drawable.ic_action_stat_reply,
						res.getString(R.string.action_reply), null)

				.setAutoCancel(false);

		return builder.build();
	}

	@TargetApi(Build.VERSION_CODES.ECLAIR)
	private static void notify(final Context context,
			final Notification notification) {

		final NotificationManager nm = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR) {
			nm.notify(TAG, NOTIFICATION_ID, notification);
		} else {
			nm.notify(TAG.hashCode(), notification);
		}
	}

	/**
	 * Cancels any notifications of this type previously shown using
	 * {@link #notify(Context, String, int)}.
	 */
	@TargetApi(Build.VERSION_CODES.ECLAIR)
	public static void cancel(final Context context) {
		final NotificationManager nm = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR) {
			nm.cancel(TAG, NOTIFICATION_ID);
		} else {
			nm.cancel(TAG.hashCode());
		}
	}

}
