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
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
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

public class LFXSDKSamplesActivity extends Activity implements LocalAlarmManager.OnAlarmListener {
	private AlarmListAdaptor alarmListAdapter = null;
	private Handler handler;
	private MediaPlayer player;	
	private MegaLog megaLog;
	private LocalAlarmManager localAlarmManager;
	private final Activity outerThis = this;
	private LifxAnimationManager animationManager;
	private WakeLock wakeLock;

	/*
	Runnable sleep = new Runnable() {
		@Override
		public void run() {
			LFXHSBKColor hsbkColor = LFXHSBKColor.getColor(305, 0.8f, 0.0f, 3500);
		    networkContext.getAllLightsCollection().setColorOverDuration(hsbkColor, 10000);
		    handler.postDelayed(off, 20000);
		}
	};
	
	Runnable off = new Runnable() {
		@Override
		public void run() {
		    TextView sleepText = (TextView) findViewById(R.id.sleepText);
		    sleepText.setText("Go to sleep");
		    
			LFXHSBKColor hsbkColor = LFXHSBKColor.getColor(305, 0.8f, 0.0f, 3500);
		    networkContext.getAllLightsCollection().setPowerState(LFXPowerState.OFF);
		}
	};
	*/
	
	/*
	private boolean isMyServiceRunning(Class<?> serviceClass) {
	    ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
	    for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
	        if (serviceClass.getName().equals(service.service.getClassName())) {
	            return true;
	        }
	    }
	    return false;
	}
	*/
	
	@Override
	protected void onCreate( Bundle savedInstanceState)
	{
		super.onCreate( savedInstanceState);
		Log.e("LIFX", "onCreate");
		
		setContentView(R.layout.lifx_main_activity_layout);
		
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "StayAwakeForAlarm");
		
		megaLog = new MegaLog(this);
		megaLog.Log("", "This is a test");
		
		// Set up and manage the alarm functionality.
		localAlarmManager = new LocalAlarmManager(megaLog, this);
		localAlarmManager.initialize(this);
		
		// Set up the animation manager.
		this.animationManager = new LifxAnimationManager(megaLog);
		
		// Hide the button used to turn off the alarm.
		TextView alarmText = (TextView) findViewById(R.id.awakeText);
		alarmText.setVisibility(View.GONE);
		
		// Is this the alarm?
		Intent intent = getIntent();
		if (intent != null) {
			Bundle extra = intent.getExtras();
			if (extra != null) {
				String instruction = extra.getString("Instruction");
				if (instruction != null && instruction.equalsIgnoreCase("Alarm")) {
					megaLog.Log("",  "Alarm intent triggered");
					
					// Keep screen on and bring to foreground.
					Window win = getWindow();
					win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
					win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
					
					beginAlarm();
				}
			}
		}
		
		// Ensure the service is running.
		/*
		if (!isMyServiceRunning(RegisterAlarmService.class)) {
			Intent startServiceIntent = new Intent(this, RegisterAlarmService.class);
			this.startService(startServiceIntent);
		}
		*/
				
		/*
		Window win = getWindow();
		win.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
		win.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
		*/
		
		// Show the list of alarms.
		ListView alarmListView = (ListView) findViewById(R.id.alarmListView);
		ArrayList<String> days = new ArrayList<String>(Arrays.asList("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"));
		ArrayList<Alarm> alarms = new ArrayList<Alarm>();
		for (int i = 0; i < 7; ++i) {
			Alarm alarm = new Alarm(days.get(i), 0L, 0L, false);
			
			SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
			if (sharedPref.contains(days.get(i))) {
				String encodedAlarm = sharedPref.getString(days.get(i), "");
				alarm.fromString(encodedAlarm);
			}
			alarms.add(alarm);
		}
		
		alarmListAdapter = new AlarmListAdaptor(this);
		alarmListAdapter.updateWithAlarms(alarms);
		alarmListView.setAdapter(alarmListAdapter);
		
		// Show logs when button clicked.
		Button viewLogsButton = (Button) findViewById(R.id.viewLogs);
		viewLogsButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(outerThis, LogActivity.class);
				startActivity(intent);
			}
		});
	}
	
	@Override
	public void onAlarm() {
		wakeLock.acquire();
		
		megaLog.Log("Activity", "Alarm listener called");
		animationManager.beginAnimation(this, new AnimationCompleteHandler() {
			@Override
			public void complete() {
				megaLog.Log("Activity", "Animation complete called");
				
				// Intent ourselves to bring a new activity to the front.
				Intent intent;
        		PackageManager manager = getPackageManager();
        		intent = manager.getLaunchIntentForPackage("com.example.lifx_sdk_samples");
				intent.addCategory(Intent.CATEGORY_LAUNCHER);
				intent.putExtra("Instruction", "Alarm");
				startActivity(intent);
				
				// TODO(jmcgill): Move this to after the button is pressed.
				wakeLock.release();
			}
		});
	}
	
	protected void beginAlarm() {
		try {
			Uri alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM); 
			player  = new MediaPlayer();
			player.setDataSource(outerThis, alert);
		
			final AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
			if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
				player.setAudioStreamType(AudioManager.STREAM_ALARM);
				player.setLooping(true);
				player.prepare();
				player.start();
			}
		} catch (Exception e) {
			Log.e("LIFX", "Exception when playing alarm sound.");
		}
		
		TextView alarmText = (TextView) findViewById(R.id.awakeText);
		alarmText.setVisibility(View.VISIBLE);
		
		Log.e("LIFX", "Awake text visible");
			
		TextView sleepText = (TextView) findViewById(R.id.sleepText);
		sleepText.setVisibility(View.GONE);
		
		alarmText.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				player.stop();
				
				TextView alarmText = (TextView) findViewById(R.id.awakeText);
	     		alarmText.setVisibility(View.GONE);
	     		
	     		TextView sleepText = (TextView) findViewById(R.id.sleepText);
	     		sleepText.setVisibility(View.VISIBLE);
	     		
	     		Window win = getWindow();
	    		win.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
	    		win.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
			}
		});
	}
	
	/*
		Intent intent = getIntent();
		if (intent != null) {
			Bundle extra = intent.getExtras();
			if (extra != null) {
				String instruction = extra.getString("Instruction");
				if (instruction != null && instruction.equalsIgnoreCase("Alarm")) {
					Log.e("LIFX", "Starting alarm");
					
					Window win2 = getWindow();
					win2.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
					win2.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
					
					
			}
		}
		
		TextView sleepText = (TextView) findViewById(R.id.sleepText);
		sleepText.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.e("LIFX", "Sleeping");
				
				LFXHSBKColor hsbkColor = LFXHSBKColor.getColor(229, 0.95f, 0.60f, 3500);
			    networkContext.getAllLightsCollection().setColorOverDuration(hsbkColor, 10000);
			    handler.postDelayed(sleep, 60 * 1000 * 5);
			    
			    TextView sleepText = (TextView) findViewById(R.id.sleepText);
			    sleepText.setText("Sleeping....");
			}
		});
		*/
				
	@Override
	protected void onResume() {
		super.onResume();
		Log.e("LIFX", "onResume");
	}
	
	@Override
	protected void onPause() {
		super.onPause();
	}
}
