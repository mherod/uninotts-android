package org.uninotts.android.service;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;

public class SignatureCheckModule extends ServiceModule {

	final static String TAG = SignatureCheckModule.class.getSimpleName();

	private LiveService mLiveService = null;

	public SignatureCheckModule(LiveService pLiveService) {
		this.mLiveService = pLiveService;
	}

	@Override
	public void load() {
		String signature = getSignature(mLiveService);
	}

	public static String getSignature(Context c) {
		PackageManager pManager = c.getPackageManager();
		String packageName = c.getPackageName();
		Signature[] sigs;
		try {
			sigs = pManager.getPackageInfo(packageName,
					PackageManager.GET_SIGNATURES).signatures;
		} catch (NameNotFoundException e) {
			return null;
		}
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			return null;
		}
		for (Signature sig : sigs) {
			md.reset();
			md.update((sig.hashCode() + "").getBytes());
			byte[] digest = md.digest();
			return new BigInteger(1, digest).toString(16);
		}
		return null;
	}

}
