package org.uninotts.android;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class CopyOfCardsFragment extends Fragment {

	private MainActivity mCardActivity;

	private View rootView;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		return inflater.inflate(R.layout.drawer_list_item, container, false);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		mCardActivity = (MainActivity) activity;
	}

	public View getRootView() {
		return rootView;
	}

}