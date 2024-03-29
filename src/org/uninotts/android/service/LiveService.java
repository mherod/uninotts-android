package org.uninotts.android.service;

import java.util.ArrayList;
import java.util.List;

import org.uninotts.android.MainActivity;
import org.uninotts.android.__;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class LiveService extends Service implements Runnable {

	private final String TAG = LiveService.class.getSimpleName();

	private ServiceNotificationModule serviceNotificationModule = null;

	private final Runnable mNetworkOpsRunnable = new Runnable() {
		@Override
		public void run() {
			while (!mNetworkOpsThread.isInterrupted()) {
				if (serviceCycle) {
					for (ServiceModule m : modules) {
						try {
							m.cycleNetwork();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
				}
			}
		}
	};

	private final Thread mServiceThread = new Thread(this);
	private final Thread mNetworkOpsThread = new Thread(mNetworkOpsRunnable);

	private boolean serviceCycle = false;

	private final List<ServiceModule> modules = new ArrayList<ServiceModule>();

	@Override
	public int onStartCommand(Intent intent, int flags, int startid) {
		mServiceThread.setName("ServiceThread");
		if (!mServiceThread.isAlive()) {
			mServiceThread.start();
		}
		mNetworkOpsThread.setName("NetworkOperationsThread");
		if (!mNetworkOpsThread.isAlive()) {
			mNetworkOpsThread.start();
		}

		if (serviceNotificationModule == null) {
			serviceNotificationModule = new ServiceNotificationModule(this);
		}
		startForeground(ServiceNotificationModule.NOTIFICATION_ID,
				serviceNotificationModule.create(this));

		return START_STICKY;
	}

	@Override
	public void run() {
		Log.i(TAG, mServiceThread.getName() + " started");
		sendBroadcast(new Intent(__.INTENT_CONNECT_SERVICE));

		// Data
		modules.add(new UserAccountModule(this));
		modules.add(new UserTimetableModule(this));

		// View
		modules.add(new CardViewWorkerModule(this));

		modules.add(new NotificationModule(this));

		modules.add(new LocationModule(this));

		modules.add(new SignatureCheckModule(this));
		modules.add(new BugSubmissionModule(this));

		modules.add(serviceNotificationModule);

		for (ServiceModule m : modules) {
			m.link();
		}
		for (ServiceModule m : modules) {
			m.load();
		}
		for (ServiceModule m : modules) {
			m.schedule();
		}

		List<ServiceModule> retrySaveModule = new ArrayList<ServiceModule>();

		long t = 0, tt = 0;
		int saveTicker = 0;

		serviceCycle = true;
		while (!mServiceThread.isInterrupted()) {
			for (ServiceModule m : modules) {
				String moduleName = m.getClass().getSimpleName();
				try {
					if (retrySaveModule.contains(m) && m.save()) {
						retrySaveModule.remove(m);
						Log.i(TAG, "Saved " + moduleName
								+ " successfully after previous failure");

					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				try {
					t = System.currentTimeMillis();
					m.cycle();
					tt = System.currentTimeMillis() - t;
				} catch (Exception e) {
					e.printStackTrace();
				}
				try {
					Thread.sleep(75);
				} catch (InterruptedException e) {
				}
				if (tt > 100) {
					Log.w(TAG, "Cycle for " + moduleName + " took " + tt + "ms");
				}
			}
			if (saveTicker++ > 30) {
				saveTicker = 0;
			}
			if (saveTicker == 0) {
				retrySaveModule.clear(); // Saving all next so clear up
				for (ServiceModule m : modules) {
					boolean saved = false;
					try {
						saved = m.save();
					} catch (Exception e) {
						e.printStackTrace();
					}
					if (!saved && retrySaveModule.add(m)) {
						String moduleName = m.getClass().getSimpleName();
						Log.e(TAG, "Failed to save " + moduleName
								+ "... scheduled priority retry");
					}
				}
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
				}
			}
		}
	}

	@Override
	public void onDestroy() {
		Log.i(TAG, "Closing activity...");
		MainActivity.finishAll(this);

		Log.i(TAG, "Closing service...");
		for (ServiceModule m : modules) {
			m.save();
		}
		for (ServiceModule m : modules) {
			m.cancel();
		}
		super.onDestroy();
	}

	public List<ServiceModule> getModules() {
		return modules;
	}

	public ServiceModule getServiceModule(@SuppressWarnings("rawtypes") Class c) {
		for (ServiceModule m : modules) {
			if (m.getClass().equals(c)) {
				return m;
			}
		}
		return null;
	}

	private final IBinder mBinder = new MyBinder();

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	public class MyBinder extends Binder {
		public LiveService getService() {
			return LiveService.this;
		}
	}

}
