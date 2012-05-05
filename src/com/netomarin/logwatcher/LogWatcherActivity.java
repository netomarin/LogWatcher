package com.netomarin.logwatcher;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

public class LogWatcherActivity extends Activity {

	private ProgressDialog progressDialog;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		findViewById(R.id.gerarButton).setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View v) {
						gerarLog();
					}
				});

		findViewById(R.id.enviarButton).setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View v) {
						enviarLog();
					}
				});
	}

	private void enviarLog() {
		SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(LogWatcherActivity.this);
		String logFile = p.getString("last_saved", null);

		if (logFile != null) {
			File file = new File("sdcard/" + logFile);
			if (file.exists()) {
				Intent emailIntent = new Intent(
						android.content.Intent.ACTION_SEND);
				emailIntent.setType("text/plain");
				emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
						getString(R.string.email_subject));
				emailIntent.putExtra(android.content.Intent.EXTRA_TEXT,
						getString(R.string.email_text) + logFile);
				emailIntent.putExtra(Intent.EXTRA_STREAM,
						Uri.parse("file:///sdcard/" + logFile));
				startActivity(Intent.createChooser(emailIntent,
						getString(R.string.email_chooser)));

				return;
			}
		}

		Toast.makeText(LogWatcherActivity.this, getString(R.string.toast_no_file),
				Toast.LENGTH_SHORT).show();
	}

	private void gerarLog() {
		new AsyncTask<Void, Void, String>() {

			protected void onPreExecute() {
				progressDialog = ProgressDialog.show(LogWatcherActivity.this,
						getString(R.string.toast_title_wait), getString(R.string.toast_reading_device_log));
			}

			@Override
			protected String doInBackground(Void... params) {
				StringBuilder log = new StringBuilder();
				String fileSaved = null;
				try {
					Process process = Runtime.getRuntime().exec("logcat -d");
					BufferedReader bufferedReader = new BufferedReader(
							new InputStreamReader(process.getInputStream()));

					String line;
					while ((line = bufferedReader.readLine()) != null) {
						log.append(line+"\n");
					}
				} catch (IOException e) {
					return null;
				}

				if (log != null && log.length() > 0) {
					fileSaved = writeToFile(log.toString());
				}

				return fileSaved;
			}

			protected void onPostExecute(String result) {
				if (progressDialog != null)
					progressDialog.dismiss();

				if (result != null) {
					SharedPreferences.Editor editor = PreferenceManager
							.getDefaultSharedPreferences(LogWatcherActivity.this).edit();
					long logSaved = System.currentTimeMillis(); 
					editor.putLong("saved_timestamp", logSaved);
					editor.putString("last_saved", result);
					editor.commit();
					
					String dateFormatted = DateFormat.format("dd/MM/yyyy h:mm aa", logSaved).toString();
					((TextView)findViewById(R.id.geradoEmTextView)).setText(dateFormatted);

					Toast.makeText(LogWatcherActivity.this,
							getString(R.string.toast_saved_1) + " " + result + " " + getString(R.string.toast_saved_2),
							Toast.LENGTH_LONG).show();
				} else {
					Toast.makeText(LogWatcherActivity.this,
							getString(R.string.toast_error_saving),
							Toast.LENGTH_SHORT).show();
				}
			}
		}.execute();
	}

	private String writeToFile(String text) {
		File logFile = new File("sdcard/logcat_" + System.currentTimeMillis()
				+ ".txt");
		if (logFile.exists()) {
			logFile.delete();
		}

		try {
			logFile.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			BufferedWriter buf = new BufferedWriter(new FileWriter(logFile,
					true));
			buf.append(text);
			buf.newLine();
			buf.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return logFile.getName();
	}
}