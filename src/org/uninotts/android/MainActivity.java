package org.uninotts.android;

import java.util.HashMap;
import java.util.Locale;

import org.uninotts.android.service.CardProviderModule;
import org.uninotts.android.service.LiveService;
import org.uninotts.android.service.UserSyncModule;
import org.uninotts.android.util.DepthPageTransformer;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.fima.cardsui.views.CardUI;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

public class MainActivity extends Activity implements Runnable {

	public final String TAG = MainActivity.class.getSimpleName();

	private String mTitle, mSubtitle, mDrawerTitle;

	private String[] mPlanetTitles;
	private DrawerLayout mDrawerLayout;
	private ActionBarDrawerToggle mDrawerToggle;
	private ListView mDrawerList;

	private SectionsPagerAdapter mSectionsPagerAdapter;
	private ViewPager mViewPager;

	private CardsFragment cardsFragment = null;

	private Thread thread = new Thread(this);

	private MyLiveServiceLink serviceLink = null;
	private LiveService mLiveService = null;

	private boolean updateCardsFlag = false;

	private boolean isForeground = false;

	private long lastBackground = 0, lastUpdate = System.currentTimeMillis();

	final HashMap<String, BroadcastReceiver> mReceivers = new HashMap<String, BroadcastReceiver>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mTitle = mDrawerTitle = getString(R.string.uni_nottingham);
		mSubtitle = getString(R.string.timetable);

		setContentView(R.layout.activity_cards);

		mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);
		mViewPager.setPageTransformer(true, new DepthPageTransformer());

		mPlanetTitles = getResources().getStringArray(R.array.navigationdrawer);

		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
				R.drawable.ic_drawer, R.string.drawer_open,
				R.string.drawer_close) {

			public void onDrawerClosed(View view) {
				super.onDrawerClosed(view);
				// getActionBar().setTitle(mTitle);
			}

			public void onDrawerOpened(View drawerView) {
				super.onDrawerOpened(drawerView);
				// getActionBar().setTitle(mDrawerTitle);
			}
		};
		mDrawerLayout.setDrawerListener(mDrawerToggle);

		mDrawerList = (ListView) findViewById(R.id.left_drawer);

		mDrawerList.setAdapter(new ArrayAdapter<String>(this,
				R.layout.drawer_list_item, mPlanetTitles));
		mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

		selectItem(0);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			ActionBar actionBar = getActionBar();
			actionBar.setDisplayHomeAsUpEnabled(true);
			actionBar.setHomeButtonEnabled(true);
			actionBar.setTitle(mTitle);
			// actionBar.setSubtitle(mSubtitle);
		}

		mReceivers.put(__.INTENT_CONNECT_SERVICE, connectServiceReceiver);
		mReceivers.put(__.INTENT_CLOSE_APP, closeAppReceiver);
		mReceivers.put(__.INTENT_CARD_UPDATE, cardUpdateReceiver);
		mReceivers.put(__.INTENT_ALERT, alertToastReciever);

		serviceLink = new MyLiveServiceLink(this);

	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		mDrawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		mDrawerToggle.onConfigurationChanged(newConfig);
	}

	private class DrawerItemClickListener implements
			ListView.OnItemClickListener {
		@Override
		public void onItemClick(
				@SuppressWarnings("rawtypes") AdapterView parent, View view,
				int pos, long id) {
			selectItem(pos);
		}
	}

	private void selectItem(int position) {
		// Fragment fragment = getNavigationFragment(position);

		// TODO: consider how best to do this - fragments for both nav drawer
		// AND pager? maybe view inflation good

		// FragmentManager fragmentManager = getFragmentManager();
		// fragmentManager.beginTransaction()
		// .replace(R.id.content_frame, fragment).commit();

		mDrawerList.setItemChecked(position, true);
		getActionBar().setSubtitle(mSubtitle = mPlanetTitles[position]);
		mDrawerLayout.closeDrawer(mDrawerList);
	}

	@Override
	public void onResume() {
		super.onResume();

		isForeground = true;
		serviceLink.start();

		for (String filter : mReceivers.keySet()) {
			registerReceiver(mReceivers.get(filter), new IntentFilter(filter));
		}

		updateCardsFlag = true;
		try {
			thread.start();
		} catch (RuntimeException re) {
		}
	}

	@Override
	protected void onPause() {
		isForeground = false;
		lastBackground = System.currentTimeMillis();
		serviceLink.stop();
		thread.interrupt();
		super.onPause();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		for (String filter : mReceivers.keySet()) {
			unregisterReceiver(mReceivers.get(filter));
		}
		super.onDestroy();
	}

	@Override
	public void onBackPressed() {
		moveTaskToBack(true);
		return;
		// super.onBackPressed();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_card_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (mDrawerToggle.onOptionsItemSelected(item)) {
			return true;
		}

		switch (item.getItemId()) {

		case R.id.action_setup:
			startActivity(new Intent(this, CourseSelectActivity.class));
			return true;

		case R.id.action_settings:
			startActivity(new Intent(this, SettingsActivity.class));
			return true;

		case R.id.action_refresh:
			Crouton.makeText(this, "Refreshing...", Style.INFO).show();

			UserSyncModule usm = ((UserSyncModule) mLiveService
					.getServiceModule(UserSyncModule.class));
			usm.requestUpdate();
			return true;

		case R.id.action_credits:
			startActivity(new Intent(this, CreditActivity.class));
			return true;

		default:
			return super.onOptionsItemSelected(item);

		}
	}

	// private void openSetup() {
	// startActivity(new Intent(this, SetupActivity.class));
	// }
	// TODO:

	private boolean isLoadingView = true;

	private boolean updateCardsView() {
		LiveService l = mLiveService;
		if (l == null) {
			return false;
		}

		if (cardsFragment == null) {
			// TODO: investigate if this is causing any issues when resuming
			return false;
		}

		CardProviderModule cardProviderModule = (CardProviderModule) l
				.getServiceModule(CardProviderModule.class);

		CardUI cardsView = cardsFragment.getCardsView();

		boolean cards = cardProviderModule.renderCardsView(this, cardsView);

		if (cards) {
			lastUpdate = System.currentTimeMillis();
		}
		if (cards && isLoadingView) {
			runOnUiThread(cardsFragment.showCards);
			isLoadingView = false;
		} else if (!cards && !isLoadingView) {
			runOnUiThread(cardsFragment.showProgress);
			isLoadingView = true;
		}
		return cards;

	}

	@Override
	public void run() {
		Log.i(TAG, "Started new background thread");
		do {
			long now = System.currentTimeMillis();
			if (!isForeground && (now - lastBackground) > (30 * 1000)) {
				Log.d(TAG, "Background timeout - killing foreground activity");
				finish();
				break;
			}
			if (isForeground && (now - lastUpdate) > (15 * 1000)) {
				updateCardsFlag = true;
			}
			if (updateCardsFlag) {
				updateCardsFlag = false;
				runOnUiThread(updateCardsRunnable);
			}
			try {
				Thread.sleep(250);
			} catch (Exception e) {
			}
		} while (!thread.isInterrupted());
		Log.i(TAG, "Exit thread");
	}

	private Runnable updateCardsRunnable = new Runnable() {
		@Override
		public void run() {
			LiveService l = mLiveService;
			if (l == null) {
				updateCardsFlag = true;
				return;
			}

			// TODO: USER SETUP
			// AccountModule am = (AccountModule) l
			// .getServiceModule(AccountModule.class);
			// if (isForeground && !am.hasAuthResponse()) {
			// Log.i(TAG, "Login a");
			// openSetup();
			// return;
			// }

			updateCardsFlag = !updateCardsView();
		}
	};

	private BroadcastReceiver cardUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			updateCardsFlag = true;
			if (isForeground) {
				/*
				 * Crouton.makeText(CardActivity.this, "Cards updated",
				 * Style.CONFIRM).show();
				 */
			}
		}
	};

	private BroadcastReceiver alertToastReciever = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (!isForeground) {
				return;
			}
			String alertString = intent.getStringExtra(__.EXTRA_ALERT);
			Crouton.makeText(MainActivity.this, alertString, Style.ALERT)
					.show();
		}
	};

	private BroadcastReceiver closeAppReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			MainActivity.this.finish();
		}
	};

	private BroadcastReceiver connectServiceReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			serviceLink.start();
		}
	};

	public static void showAlert(Context context, String alertString) {
		context.sendBroadcast(new Intent(__.INTENT_ALERT).putExtra(
				__.EXTRA_ALERT, alertString));
	}

	public static void finishAll(Context context) {
		context.sendBroadcast(new Intent(__.INTENT_CLOSE_APP));
	}

	private class MyLiveServiceLink extends LiveServiceLink {

		Context context = null;

		public MyLiveServiceLink(Context context) {
			super(context);
			this.context = context;
		}

		@Override
		public void onConnect() {
			mLiveService = getLiveService();
		}

		@Override
		public void onDisconnect() {
			MainActivity.finishAll(context);
		}

	}

	public class SectionsPagerAdapter extends FragmentPagerAdapter {

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			switch (position) {
			case 0:
				return new CardsFragment();
			case 1:
				return new CalendarFragment();
			}
			return null;
		}

		@Override
		public int getCount() {
			return 2;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			Locale l = Locale.getDefault();
			switch (position) {
			case 0:
				return getString(R.string.title_section1).toUpperCase(l);
			case 1:
				return getString(R.string.title_section2).toUpperCase(l);
			case 2:
				return getString(R.string.title_section3).toUpperCase(l);
			}
			return null;
		}
	}

	private Fragment getNavigationFragment(int position) {
		return new CardsFragment();
	}

	protected void attach(CardsFragment cardsFragment) {
		this.cardsFragment = cardsFragment;

		Log.d(TAG, "Attached to CardsFragment");
	}
}
