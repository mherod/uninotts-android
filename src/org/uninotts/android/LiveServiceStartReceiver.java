package org.uninotts.android;

import org.uninotts.android.service.LiveService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class LiveServiceStartReceiver extends BroadcastReceiver {

	private final String TAG = getClass().getSimpleName();

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(TAG, "onReceive called");

		Intent myIntent = new Intent(context, LiveService.class);
		context.startService(myIntent);
	}

}
