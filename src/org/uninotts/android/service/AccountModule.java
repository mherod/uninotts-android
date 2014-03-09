package org.uninotts.android.service;

import java.io.File;

import org.uninotts.android.util.OFiles;

import android.util.Log;

public class AccountModule extends ServiceModule {

	final String TAG = AccountModule.class.getSimpleName();

	final String authKeyFile = "authkey.dat";

	private boolean requestSave = false;
	private boolean requestSyncUpdate = false;
	private boolean requestAccountInvalidate = false;

	private LiveService mLiveService = null;
	private UserSyncModule mUserSyncModule = null;

	public AccountModule(LiveService liveService) {
		this.mLiveService = liveService;
	}

	@Override
	public void link() {
		mUserSyncModule = ((UserSyncModule) mLiveService
				.getServiceModule(UserSyncModule.class));
	}

	@Override
	public void load() {
		final String folder = OFiles.getFolder(mLiveService);
		try {
			// authResponse = (AuthResponse) OFiles.read(folder + authKeyFile);
			Log.i(TAG, "Recovered authResponse");
		} catch (Exception e) {
			Log.i(TAG, "Error recovering authResponse " + e.toString());
		}
	}

	@Override
	public boolean save() {
		final String folder = OFiles.getFolder(mLiveService);
		if (!hasAuthResponse()) {
			new File(folder + authKeyFile).delete();
			return true;
		}
		// try {
		// // OFiles.saveObject(authResponse, folder + authKeyFile);
		// Log.i(TAG, "Saved authResponse");
		// } catch (IOException e) {
		// Log.i(TAG, "Error saving authResponse: " + e.toString());
		// return false;
		// }
		return true;
	}

	@Override
	public void cycle() {
		if (requestSave) {
			Log.i(TAG, "[" + "requestSave" + "]");
			if (save()) {
				requestSave = false;
			}
		}
		if (requestAccountInvalidate) {
			Log.i(TAG, "[" + "requestAccountInvalidate" + "]");
			mUserSyncModule.clearLocalData();
			requestAccountInvalidate = false;
			requestSyncUpdate = true;
		}
		if (requestSyncUpdate) {
			Log.i(TAG, "[" + "requestSyncUpdate" + "]");
			mUserSyncModule.requestUpdate();
			requestSyncUpdate = false;
		}
	}

	public boolean hasAuthResponse() {

		return true;

		// return Validation.validAuthResponse2(authResponse);
	}

	public Object getAuthResponse() {
		return null;

		// return authResponse;
	}

	public void setAuthResponse(Object pAuthResponse) {
		// authResponse = pAuthResponse;
		requestSave = true;
		requestAccountInvalidate = true;
	}

}
