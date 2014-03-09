package org.uninotts.android;

import org.uninotts.android.util.ViewHelpers;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.fima.cardsui.views.CardUI;

public class CardsFragment extends Fragment {

	private MainActivity mCardActivity = null;

	private View rootView;

	private View mContentView;
	private CardUI mCardsView;
	private ProgressBar mLoadingView;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		rootView = inflater.inflate(R.layout.fragment_cards, container, false);

		mLoadingView = (ProgressBar) rootView
				.findViewById(R.id.loading_spinner);
		mContentView = rootView.findViewById(R.id.content);
		mCardsView = (CardUI) rootView.findViewById(R.id.cards);

		return rootView;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		mCardActivity = (MainActivity) activity;
		mCardActivity.attach(this);
	}

	public View getRootView() {
		return rootView;
	}

	public boolean isLoadingView() {
		return mLoadingView.getVisibility() == View.VISIBLE;
	}

	final Runnable showCards = new Runnable() {
		@Override
		public void run() {
			if (!isLoadingView()) {
				return;
			}
			ViewHelpers.crossfade(mLoadingView, mContentView);
		}
	};

	final Runnable showProgress = new Runnable() {
		@Override
		public void run() {
			if (isLoadingView()) {
				return;
			}
			ViewHelpers.crossfade(mContentView, mLoadingView);
		}
	};

	public CardUI getCardsView() {
		return mCardsView;
	}

	public ProgressBar getLoadingView() {
		return mLoadingView;
	}
}