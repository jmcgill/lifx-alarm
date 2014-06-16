//
//  LFXSDKSamplesActivity.java
//  LIFX
//
//  Created by Jarrod Boyes on 24/03/14.
//  Copyright (c) 2014 LIFX Labs. All rights reserved.
//

package com.example.lifx_sdk_samples;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import lifx.java.android.client.LFXClient;
import lifx.java.android.entities.LFXHSBKColor;
import lifx.java.android.entities.LFXTypes.LFXPowerState;
import lifx.java.android.network_context.LFXNetworkContext;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class LogActivity extends Activity {
	private static String LogPreference = "logs";
	private SharedPreferences prefs;
	private TextView logTextView;
	@Override
	protected void onCreate( Bundle savedInstanceState) {
		super.onCreate( savedInstanceState);
		Log.e("LIFX-Log", "onCreate");
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		
		setContentView(R.layout.log_viewer);
		
		logTextView = (TextView) findViewById(R.id.logText);
		
		// Display logs.
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		String logs = prefs.getString(LogPreference, "");
		logTextView.setText(logs);
		
		// Clear logs when button is clicked.
		Button clearLogsButton = (Button) findViewById(R.id.clearLog);
		clearLogsButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				SharedPreferences.Editor editor = prefs.edit();
				editor.remove(LogPreference);
				editor.commit();
				logTextView.setText("");
			}
		});
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		Log.e("LIFX-Log", "onResume");
	}
	
	@Override
	protected void onPause()
	{
		super.onPause();
		Log.e("LIFX-Log", "onPause");
	}
}
