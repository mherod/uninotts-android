package org.uninottstt.android;

import org.studentnow.ECard;
import org.studentnow.Static.Fields;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.fima.cardsui.objects.Card;

public class VECard extends Card {

	private ECard ecard = null;

	private Bitmap mainBitmap = null;
	private Bitmap bottomBitmap = null;

	public VECard(String title, String desc) {
		super(title, desc);
	}

	public VECard(ECard ecard) {
		this.ecard = ecard;
		if (ecard != null) {
			title = ecard.getTitle();
			desc = ecard.getDesc();
		}
	}

	@Override
	public View getCardContent(Context context) {
		long now = System.currentTimeMillis();

		View view = LayoutInflater.from(context).inflate(R.layout.card_content,
				null);

		TextView titleTextView = (TextView) view.findViewById(R.id.title);
		if (titleTextView != null) {
			if (ecard != null) {
				title = ecard.getTitle();
			}
			titleTextView.setText(title);
		}

		TextView descTextView = (TextView) view.findViewById(R.id.description);
		if (descTextView != null) {
			if (ecard != null) {
				desc = ecard.getDesc();
			}
			descTextView.setText(desc);
		}

		if (mainBitmap != null) {
			ImageView v = (ImageView) view.findViewById(R.id.imageView_ct);
			if (v != null) {
				v.setVisibility(View.VISIBLE);
			}
			v.setImageBitmap(mainBitmap);
		}
		if (bottomBitmap != null) {
			View v = view.findViewById(R.id.card_part_map);
			if (v != null) {
				v.setVisibility(View.VISIBLE);
			}
			ImageView iv = (ImageView) view.findViewById(R.id.imageView_cb);
			if (iv != null) {
				iv.setImageBitmap(bottomBitmap);
			}
		}
		if (ecard != null) {
			if (ecard.hasProgressInfo()) {
				View v = view.findViewById(R.id.card_part_progress);
				if (v != null) {
					v.setVisibility(View.VISIBLE);
				}
				int progress = ecard.getProgressPercentage(now);
				ProgressBar mProgressBar = ((ProgressBar) view
						.findViewById(R.id.progressBar00));
				if (mProgressBar != null) {
					mProgressBar.setMax(100);
					mProgressBar.setProgress(progress);
				}

				if (ecard.has(Fields.PROGRESS_CANCELLED)) {
					mProgressBar.getProgressDrawable().setColorFilter(
							Color.RED, Mode.SRC_IN);
				}

				String progressStartLabel = ecard
						.getString(Fields.PROGRESS_LABEL_START);
				String progressEndLabel = ecard
						.getString(Fields.PROGRESS_LABEL_END);
				TextView progressStartLabelView = (TextView) view
						.findViewById(R.id.progressStartLabel);
				TextView progressEndLabelView = (TextView) view
						.findViewById(R.id.progressEndLabel);
				if (nNull(progressStartLabel, progressStartLabelView)) {
					progressStartLabelView.setText(progressStartLabel);
				}
				if (nNull(progressEndLabel, progressEndLabelView)) {
					progressEndLabelView.setText(progressEndLabel);
				}
			}
		}
		return view;
	}

	public void setMainBitmap(Bitmap b) {
		mainBitmap = b;
	}

	public void setBottomBitmap(Bitmap b) {
		bottomBitmap = b;
	}

	private boolean nNull(Object... os) {
		for (Object o : os) {
			if (o == null) {
				return false;
			}
		}
		return true;
	}

}
