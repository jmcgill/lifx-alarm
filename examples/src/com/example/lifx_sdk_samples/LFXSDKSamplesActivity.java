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
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class LFXSDKSamplesActivity extends Activity
{
	private boolean shouldStopLifxOnPause;
	private LFXNetworkContext networkContext;
	private MulticastLock ml = null;
	private AlarmListAdaptor alarmListAdapter = null;
	private Log log;
	// private BroadcastReceiver br;
	// private BroadcastReceiver br2;
	private Handler handler;
	private final ArrayList<String> wakeupColors = new ArrayList<String>(
			Arrays.asList(
					"240,80,1",
					"240,80,79",
					// Purple
					"305,80,44",
					// White
					"344,84,85",
					"247,8,98",
					"247,8,98"
					));
	
	private int wakeupCounter = 0;
	
	private double hIncrease = 0;
	private double sIncrease = 0;
	private double bIncrease = 0;
	
	private float hBase = 0;
	private float sBase = 0;
	private float bBase = 0;
	private final Activity outerThis = this;
	
	private Messenger serviceMessenger;

	MediaPlayer player;
	
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
	
	private boolean isMyServiceRunning(Class<?> serviceClass) {
	    ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
	    for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
	        if (serviceClass.getName().equals(service.service.getClassName())) {
	            return true;
	        }
	    }
	    return false;
	}
	
	private ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            serviceMessenger = new Messenger(service);
            Log.e("LIFX", "Attached to service");
//            try {
//                Message msg = Message.obtain(null, RegisterAlarmService.MSG_REGISTER_CLIENT);
//                msg.replyTo = mMessenger;
//                mService.send(msg);
//            } catch (RemoteException e) {
//                // In this case the service has crashed before we could even do anything with it
//            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been unexpectedly disconnected - process crashed.
            serviceMessenger = null;
            Log.e("LIFX", "Disconnected from service");
        }
    };
	
	@Override
	protected void onCreate( Bundle savedInstanceState)
	{
		super.onCreate( savedInstanceState);
		
		Log.e("LIFX", "onCreate");
		
		// Ensure the service is running.
		if (!isMyServiceRunning(RegisterAlarmService.class)) {
			Intent startServiceIntent = new Intent(this, RegisterAlarmService.class);
			this.startService(startServiceIntent);
		}
		
		// Bind to the background service.
		// bindService(new Intent(this, RegisterAlarmService.class), serviceConnection, Context.BIND_AUTO_CREATE);
		
		// A Multicast lock should be acquired, as some phones disable UDP broadcast / recieve
//		WifiManager wifi;
//      	wifi = (WifiManager) getSystemService( Context.WIFI_SERVICE);
//      	ml = wifi.createMulticastLock( "lifx_samples_tag");
//      	ml.acquire();
		
		networkContext = LFXClient.getSharedInstance( getApplicationContext()).getLocalNetworkContext();
// networkContext.connect();
		
		Window win = getWindow();
		win.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
		win.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
		
		setContentView(R.layout.lifx_main_activity_layout);
		TextView alarmText = (TextView) findViewById(R.id.awakeText);
		alarmText.setVisibility(View.GONE);
		
		handler = new Handler();
		
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
					
					Uri alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM); 
					player  = new MediaPlayer();
					try {
						player.setDataSource(outerThis, alert);
					} catch (IllegalArgumentException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (SecurityException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IllegalStateException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					Log.e("LIFX", "Getting audio manager");
					final AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);

					if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
						Log.e("LIFX", "Playing audio stream");
					    player.setAudioStreamType(AudioManager.STREAM_ALARM);
					    player.setLooping(true);
					    try {
							player.prepare();
						} catch (IllegalStateException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					    
					    Log.e("LIFX", "Finished playing");
					    player.start();
					    
					    Log.e("LIFX", "Ready for next thing after playing.");
					}
					
	     			Log.e("LIFX", "Showing alarm text");
	     			alarmText.setVisibility(View.VISIBLE);
	     			
	     			TextView sleepText = (TextView) findViewById(R.id.sleepText);
	     			sleepText.setVisibility(View.GONE);
	     			
	     			Log.e("LIFX", "Alarm visible");
	     			
	     			alarmText.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							player.stop();
							
							TextView alarmText = (TextView) findViewById(R.id.awakeText);
			     			alarmText.setVisibility(View.GONE);
			     			
			     			TextView sleepText = (TextView) findViewById(R.id.sleepText);
			     			sleepText.setVisibility(View.VISIBLE);
						}
	     			});
				}
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
				
		ListView alarmListView = (ListView) findViewById(R.id.alarmListView);
		
		ArrayList<String> days = new ArrayList<String>(Arrays.asList("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"));
		ArrayList<Alarm> alarms = new ArrayList<Alarm>();
		for (int i = 0; i < 7; ++i) {
			Alarm alarm = new Alarm(days.get(i), 0L, 0L, false);
			
			SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
			Log.e("LIFX", "Checking for preference: " + days.get(i));
			if (sharedPref.contains(days.get(i))) {
				Log.e("LIFX", "Found preference.");
				String encodedAlarm = sharedPref.getString(days.get(i), "");
				alarm.fromString(encodedAlarm);
			}
			alarms.add(alarm);
		}
		
		alarmListAdapter = new AlarmListAdaptor(this);
		alarmListAdapter.updateWithAlarms(alarms);
		alarmListView.setAdapter(alarmListAdapter);
	}
	
	@Override
	protected void onResume()
	{
		super.onResume();
		Log.e("LIFX", "onResume");
		
		shouldStopLifxOnPause = true;
	}
    
    protected Dialog onCreateDialog(int id)
    {
    	switch(id)
    	{
    		case 1:
    			return new TimePickerDialog(this, null, 7, 0, false);
    	}
    	return null;
    }
	
	@Override
	protected void onPause()
	{
		super.onPause();
		
//		if( shouldStopLifxOnPause)
//		{
//			System.out.println( "Stop LIFX");
//			networkContext.disconnect();
//		
//			if( ml != null) {
//				ml.release();
//				ml = null;
//			}
//		}
	}
}
