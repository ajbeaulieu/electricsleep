package com.androsz.electricsleepbeta.app;

import java.io.IOException;
import java.io.StreamCorruptedException;
import java.util.Calendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.androsz.electricsleepbeta.R;
import com.androsz.electricsleepbeta.alarmclock.AlarmClock;
import com.androsz.electricsleepbeta.content.StartSleepReceiver;
import com.androsz.electricsleepbeta.db.SleepContentProvider;
import com.androsz.electricsleepbeta.db.SleepRecord;
import com.androsz.electricsleepbeta.widget.SleepChart;
import com.androsz.electricsleepbeta.widget.calendar.MonthActivity;

/**
 * Front-door {@link Activity} that displays high-level features the application
 * offers to users.
 */
public class HomeActivity extends CustomTitlebarActivity {

	private class LoadLastSleepChartTask extends
			AsyncTask<String, Void, Cursor> {

		@Override
		protected Cursor doInBackground(String... params) {
			return managedQuery(SleepContentProvider.CONTENT_URI, null, null,
					new String[] { params[0] }, SleepRecord.KEY_TITLE);
		}

		@Override
		protected void onPostExecute(final Cursor cursor) {
			final TextView lastSleepTitleText = (TextView) findViewById(R.id.home_last_sleep_title_text);
			final TextView reviewTitleText = (TextView) findViewById(R.id.home_review_title_text);
			final RelativeLayout container = (RelativeLayout)findViewById(R.id.home_stats_container);
			if (cursor == null) {
				container.setVisibility(View.GONE);
				sleepChart.setVisibility(View.GONE);
				reviewTitleText
						.setText(getString(R.string.home_review_title_text_empty));
				lastSleepTitleText
						.setText(getString(R.string.home_last_sleep_title_text_empty));
			} else {

				final TextView avgScoreText = (TextView) findViewById(R.id.value_score_text);
				final TextView avgDurationText = (TextView) findViewById(R.id.value_duration_text);
				final TextView avgSpikesText = (TextView) findViewById(R.id.value_spikes_text);
				final TextView avgFellAsleepText = (TextView) findViewById(R.id.value_fell_asleep_text);
				cursor.moveToLast();
				sleepChart.setVisibility(View.VISIBLE);
				container.setVisibility(View.VISIBLE);
				try {
					sleepChart.sync(cursor);
				} catch (final StreamCorruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (final IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (final IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (final ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				lastSleepTitleText
						.setText(getString(R.string.home_last_sleep_title_text));

				cursor.moveToFirst();
				int avgSleepScore = 0;
				int avgDuration = 0;
				int avgSpikes = 0;
				int avgFellAsleep = 0;
				int count = 0;
				do {
					count++;
					final SleepRecord sleepRecord = new SleepRecord(cursor);
					avgSleepScore += sleepRecord.getSleepScore();
					avgDuration += sleepRecord.duration;
					avgSpikes += sleepRecord.spikes;
					avgFellAsleep += sleepRecord.getTimeToFallAsleep();
				} while (cursor.moveToNext());

				avgSleepScore /= count;
				avgDuration /= count;
				avgSpikes /= count;
				avgFellAsleep /= count;

				avgScoreText.setText(avgSleepScore + "%");
				avgDurationText.setText(SleepRecord.getTimespanText(
						avgDuration, getResources()));
				avgSpikesText.setText(avgSpikes + "");
				avgFellAsleepText.setText(SleepRecord.getTimespanText(
						avgFellAsleep, getResources()));

				reviewTitleText
						.setText(getString(R.string.home_review_title_text));
			}
		}

		@Override
		protected void onPreExecute() {
			sleepChart = (SleepChart) findViewById(R.id.home_sleep_chart);
		}

	}

	private class LoadSleepStatisticsTask extends AsyncTask<Void, Void, Cursor> {

		@Override
		protected Cursor doInBackground(Void... params) {
			return managedQuery(SleepContentProvider.CONTENT_URI, null, null,
					null, null);
		}

		@Override
		protected void onPostExecute(final Cursor cursor) {
			if (cursor == null) {
				finish();
				return;
			}
			cursor.moveToFirst();

			final SleepRecord sleepRecord = new SleepRecord(cursor);

			((TextView) findViewById(R.id.value_score_text))
					.setText(sleepRecord.getSleepScore() + "%");
			((TextView) findViewById(R.id.value_duration_text))
					.setText(sleepRecord.getDurationText(getResources()));
			((TextView) findViewById(R.id.value_spikes_text))
					.setText(sleepRecord.spikes + "");
			((TextView) findViewById(R.id.value_fell_asleep_text))
					.setText(sleepRecord.getFellAsleepText(getResources()));
			((TextView) findViewById(R.id.value_note_text))
					.setText(sleepRecord.note);

			((RatingBar) findViewById(R.id.value_rating_bar))
					.setRating(sleepRecord.rating);

			sleepChart.sync(sleepRecord);
		}

		@Override
		protected void onPreExecute() {
			sleepChart = (SleepChart) findViewById(R.id.home_sleep_chart);
		}

	}

	private SleepChart sleepChart;

	LoadLastSleepChartTask loadLastSleepChartTask;

	@Override
	protected int getContentAreaLayoutId() {
		return R.layout.activity_home;
	}

	public void onAlarmsClick(final View v) {
		startActivity(new Intent(this, AlarmClock.class));
	}

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		// do this before the google analytics tracker can send any data
		PreferenceManager.setDefaultValues(this, R.xml.settings, false);

		super.onCreate(savedInstanceState);

		// showTitleButton1(R.drawable.ic_title_share);
		// showTitleButton2(R.drawable.ic_title_refresh);
		setHomeButtonAsLogo();

		final SharedPreferences userPrefs = getSharedPreferences(
				SettingsActivity.PREFERENCES_ENVIRONMENT, Context.MODE_PRIVATE);
		final int prefsVersion = userPrefs.getInt(
				SettingsActivity.PREFERENCES_ENVIRONMENT, 0);
		if (prefsVersion == 0) {
			startActivity(new Intent(this, WelcomeTutorialWizardActivity.class)
					.putExtra("required", true));
		} else {

			if (!WelcomeTutorialWizardActivity
					.enforceCalibrationBeforeStartingSleep(this)) {
				// we've already calibrated... now show the beta-ending-donate
				// message

				final AlertDialog.Builder dialog = new AlertDialog.Builder(this)
						.setMessage(getString(R.string.delete_sleep_record))
						.setPositiveButton(getString(R.string.ok),
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(
											final DialogInterface dialog,
											final int id) {
										final NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
										notificationManager
												.cancel(getIntent()
														.getExtras()
														.getInt(SleepMonitoringService.EXTRA_ID));
										finish();
									}
								})
						.setNegativeButton(getString(R.string.cancel),
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(
											final DialogInterface dialog,
											final int id) {
										dialog.cancel();
									}
								});
				dialog.show();
			}

		}
	}

	public void onHistoryClick(final View v) {
		startActivity(new Intent(this, MonthActivity.class));
	}

	@Override
	public void onHomeClick(final View v) {
		// do nothing b/c home is home!
	}

	@Override
	protected void onPause() {
		super.onPause();

		if (loadLastSleepChartTask != null) {
			loadLastSleepChartTask.cancel(true);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (loadLastSleepChartTask != null) {
			loadLastSleepChartTask.cancel(true);
		}
		loadLastSleepChartTask = new LoadLastSleepChartTask();
		loadLastSleepChartTask.execute(getString(R.string.to));
	}

	public void onSleepClick(final View v) throws Exception {
		sendBroadcast(new Intent(StartSleepReceiver.START_SLEEP));
	}
}
