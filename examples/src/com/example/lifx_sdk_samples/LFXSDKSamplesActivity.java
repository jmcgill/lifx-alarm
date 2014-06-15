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
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
	private BroadcastReceiver br;
	private BroadcastReceiver br2;
	private Handler handler;
	private final ArrayList<String> wakeupColors = new ArrayList<String>(
			Arrays.asList(
					"240,80,1",
					"240,80,79",
					// Purple
					"305,80,44",
					// White
					"247,8,98",
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
	
	//private final int delayBetweenScenes = 2 * 60 * 1000;
	private final int delayBetweenScenes = 11000;

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
	
	Runnable wakeup = new Runnable() {
		@Override
		public void run() {
			// Avoid waking up on the first iteration because it will flash brightly first.
			if (wakeupCounter == 1) {
				networkContext.getAllLightsCollection().setPowerState(LFXPowerState.ON);
			}
			
			Log.e("LIFX", "Counter: " + new Integer(wakeupCounter).toString());
			String color = wakeupColors.get(wakeupCounter);
			String[] parts = color.split(",");
			
			hBase = Float.valueOf(parts[0]);
			sBase = (float)(Float.valueOf(parts[1]) / 100.0);
			bBase = (float)(Float.valueOf(parts[2]) / 100.0); 
			
//			String nextColor = wakeupColors.get((int)(wakeupCounter / 100) + 1);
//			String[] nextParts = nextColor.split(",");
//			
//			Log.e("LIFX", "From: " + color + " to " + nextColor);
//			
//			float hNext = Float.valueOf(nextParts[0]);
//			float sNext = (float)(Float.valueOf(nextParts[1]) / 100.0);
//			float bNext = (float)(Float.valueOf(nextParts[2]) / 100.0);
			
//			hIncrease = (hNext - hBase) / 100.0;
//			sIncrease = (sNext - sBase) / 100.0;
//			bIncrease = (bNext - bBase) / 100.0;
			
			LFXHSBKColor hsbkColor = LFXHSBKColor.getColor(hBase, sBase, bBase, 3500);
		    networkContext.getAllLightsCollection().setColorOverDuration(hsbkColor, 10000);
		    
//		    hBase += hIncrease;
//		    sBase += sIncrease;
//		    bBase += bIncrease;
		    
		    // Log.e("LIFX", "Color: " + new Float(hBase).toString() + ", " + new Float(sBase).toString() + ", " + new Float(bBase).toString());
			
		    wakeupCounter += 1;
		    
		    // TODO(jmcgill): Change to set color over duration.
		    // Log.e("LIFX", "Wakeup counter: " + new Integer(wakeupCounter).toString());
		    if (wakeupCounter < 5) {
			  handler.postDelayed(wakeup, delayBetweenScenes);
			} else {
				// Time for the alarm to go off.
				Intent i2;
        		PackageManager manager = getPackageManager();
        		i2 = manager.getLaunchIntentForPackage("com.example.lifx_sdk_samples");
				i2.addCategory(Intent.CATEGORY_LAUNCHER);
				i2.putExtra("Instruction", "Alarm");
				startActivity(i2);
			}
		}
	};
	
//	Runnable wakeup = new Runnable() {
//		@Override
//		public void run() {
//			networkContext.getAllLightsCollection().setPowerState( LFXPowerState.ON);
//			
//			if ((wakeupCounter % 100) == 0) {
//				Log.e("LIFX", "Counter: " + new Integer(wakeupCounter).toString());
//				String color = wakeupColors.get((wakeupCounter / 100));
//				String[] parts = color.split(",");
//				
//				hBase = Float.valueOf(parts[0]);
//				sBase = (float)(Float.valueOf(parts[1]) / 100.0);
//				bBase = (float)(Float.valueOf(parts[2]) / 100.0); 
//				
//				String nextColor = wakeupColors.get((int)(wakeupCounter / 100) + 1);
//				String[] nextParts = nextColor.split(",");
//				
//				Log.e("LIFX", "From: " + color + " to " + nextColor);
//				
//				float hNext = Float.valueOf(nextParts[0]);
//				float sNext = (float)(Float.valueOf(nextParts[1]) / 100.0);
//				float bNext = (float)(Float.valueOf(nextParts[2]) / 100.0);
//				
//				hIncrease = (hNext - hBase) / 100.0;
//				sIncrease = (sNext - sBase) / 100.0;
//				bIncrease = (bNext - bBase) / 100.0;
//			}
//			
//			LFXHSBKColor hsbkColor = LFXHSBKColor.getColor(hBase, sBase, bBase, 3500);
//		    networkContext.getAllLightsCollection().setColor(hsbkColor);
//		    
//		    hBase += hIncrease;
//		    sBase += sIncrease;
//		    bBase += bIncrease;
//		    
//		    // Log.e("LIFX", "Color: " + new Float(hBase).toString() + ", " + new Float(sBase).toString() + ", " + new Float(bBase).toString());
//			
//		    wakeupCounter += 1;
//		    
//		    // TODO(jmcgill): Change to set color over duration.
//		    // Log.e("LIFX", "Wakeup counter: " + new Integer(wakeupCounter).toString());
//		    if (wakeupCounter < 300) {
//				if ((wakeupCounter % 100) == 0) {
//				  handler.postDelayed(wakeup, 10000);
//				} else {
//		    	  handler.postDelayed(wakeup, 50);
//				}
//			} else {
//				Uri alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM); 
//				player  = new MediaPlayer();
//				try {
//					player.setDataSource(outerThis, alert);
//				} catch (IllegalArgumentException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				} catch (SecurityException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				} catch (IllegalStateException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//				final AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
//
//				if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
//				    player.setAudioStreamType(AudioManager.STREAM_ALARM);
//				    player.setLooping(true);
//				    try {
//						player.prepare();
//					} catch (IllegalStateException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					} catch (IOException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//				    player.start();
//				}
//				
//     			TextView alarmText = (TextView) findViewById(R.id.awakeText);
//     			alarmText.setVisibility(View.VISIBLE);
//     			
//     			alarmText.setOnClickListener(new OnClickListener() {
//					@Override
//					public void onClick(View v) {
//						player.stop();
//						
//						TextView alarmText = (TextView) findViewById(R.id.awakeText);
//		     			alarmText.setVisibility(View.GONE);	
//					}
//     			});
//			}
//		}
//	};
	
	@Override
	protected void onCreate( Bundle savedInstanceState)
	{
		super.onCreate( savedInstanceState);
		
		Log.e("LIFX", "onCreate");
		
		Window win = getWindow();
		win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
		win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
		
		// A Multicast lock should be acquired, as some phones disable UDP broadcast / recieve
		WifiManager wifi;
      	wifi = (WifiManager) getSystemService( Context.WIFI_SERVICE);
      	ml = wifi.createMulticastLock( "lifx_samples_tag");
      	ml.acquire();
		
		networkContext = LFXClient.getSharedInstance( getApplicationContext()).getLocalNetworkContext();
		networkContext.connect();
		
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
				LFXHSBKColor hsbkColor = LFXHSBKColor.getColor(305, 0.8f, 0.44f, 3500);
			    networkContext.getAllLightsCollection().setColorOverDuration(hsbkColor, 10000);
			    handler.postDelayed(sleep, 60 * 1000 * 5);
			    
			    TextView sleepText = (TextView) findViewById(R.id.sleepText);
			    sleepText.setText("Sleeping...");
			}
		});
				
		ListView alarmListView = (ListView) findViewById(R.id.alarmListView);
		
		ArrayList<String> days = new ArrayList<String>(Arrays.asList("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"));
		ArrayList<Alarm> alarms = new ArrayList<Alarm>();
		for (int i = 0; i < 7; ++i) {
			Alarm alarm = new Alarm(days.get(i), 0L, 0L, false);
			
			SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
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
	
		br = new BroadcastReceiver() {
	         @Override
	         public void onReceive(Context c, Intent i) {	               
	                Log.e("LIFX", "Waking up to run alarm");
	                
	                // Is this alarm still active?
	                SimpleDateFormat sdf = new SimpleDateFormat("EEEE");
					Date d = new Date();
					String dayOfTheWeek = sdf.format(d);
					  
					// Read the alarm time for this day.
					Alarm alarm = new Alarm(dayOfTheWeek, 0L, 0L, true);
					SharedPreferences sharedPref = outerThis.getPreferences(Context.MODE_PRIVATE);
					if (sharedPref.contains(dayOfTheWeek)) {
						Log.e("LIFX", "Found preference for " + dayOfTheWeek);
						String encodedAlarm = sharedPref.getString(dayOfTheWeek, "");
						alarm.fromString(encodedAlarm);
						
						if (alarm.active == true) {
							// We want the alarm to go off.
							wakeupCounter = 0;
							wakeup.run();
						}
					}
	        		
	                // 
	                // networkContext.getAllLightsCollection().setPowerState( LFXPowerState.ON);
	         }						
	    };
	    registerReceiver(br, new IntentFilter("com.example.lifx_sdk_samples") );
	       
	    // Setup the midnight alarm and receiver.
	    Intent i = new Intent("com.example.lifx_sdk_samples.midnight");
		PendingIntent pi = PendingIntent.getBroadcast(this, 2, i, 0);
		AlarmManager am = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
		
		
		// Run at two minutes past midnight on the next day.
		Calendar c = Calendar.getInstance();
		c.set(Calendar.HOUR_OF_DAY, 00);
		c.set(Calendar.MINUTE, 04);
		c.set(Calendar.SECOND, 0);
		c.add(Calendar.DATE, 1);  // number of days to add

		am.cancel(pi);
		Log.e("LIFX", "Setting alarm for midnight.");
		am.setExact(AlarmManager.RTC_WAKEUP, c.getTime().getTime(), pi);
		
		br2 = new BroadcastReceiver() {
	         @Override
	         public void onReceive(Context c, Intent i) {	               
	              Log.e("LIFX", "It's just past midnight!");
	              
	              // What day of the week is it?
				  SimpleDateFormat sdf = new SimpleDateFormat("EEEE");
				  Date d = new Date();
				  String dayOfTheWeek = sdf.format(d);
				  
				  // Read the alarm time for this day.
				  Alarm alarm = new Alarm(dayOfTheWeek, 0L, 0L, true);
				  SharedPreferences sharedPref = outerThis.getPreferences(Context.MODE_PRIVATE);
				  if (sharedPref.contains(dayOfTheWeek)) {
					 Log.e("LIFX", "Found preference for " + dayOfTheWeek);
					 String encodedAlarm = sharedPref.getString(dayOfTheWeek, "");
					 alarm.fromString(encodedAlarm);
				  }
				  
				  // Clear date fields to get to start of day.
				  d.setHours(0);
				  d.setMinutes(0);
				  d.setSeconds(0);
				  
				  // Schedule an alarm to go off at this time.
				  Intent i2 = new Intent("com.example.lifx_sdk_samples");
				  PendingIntent pi = PendingIntent.getBroadcast(outerThis, 1, i2, 0);
				  AlarmManager am2 = (AlarmManager) outerThis.getSystemService(Context.ALARM_SERVICE);
			      am2.cancel(pi);
				  am2.setExact(AlarmManager.RTC_WAKEUP, d.getTime() + ((alarm.hour * 3600) + (alarm.minute * 60)) * 1000, pi);
				  
				  Long timestamp = new Long(System.currentTimeMillis());
				  Log.e("LIFX", "Alarm at time: " + new Long(d.getTime() + ((alarm.hour * 3600) + (alarm.minute * 60)) * 1000).toString());
				  Log.e("LIFX", "Current time: " + timestamp.toString());
	         }						
	    };
	    registerReceiver(br2, new IntentFilter("com.example.lifx_sdk_samples.midnight") );
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
	
//	public void pressedLabelChange( View v)
//	{
//		Intent intent = new Intent( getApplicationContext(), LFXSDKLightEditLabelActivity.class);
//		shouldStopLifxOnPause = false;
//		startActivity( intent);
//	}
//	
//	public void pressedRandomColor( View v)
//	{
//		Intent intent = new Intent( getApplicationContext(), LFXSDKLightRandomColorActivity.class);
//		shouldStopLifxOnPause = false;
//		startActivity( intent);
//	}
//	
//	public void pressedPowerChange( View v)
//	{
//		Intent intent = new Intent( getApplicationContext(), LFXSDKLightPowerActivity.class);
//		shouldStopLifxOnPause = false;
//		startActivity( intent);
//	}
	
	@Override
	protected void onPause()
	{
		super.onPause();
		
//		if( shouldStopLifxOnPause)
//		{
//			System.out.println( "Stop LIFX");
//			networkContext.disconnect();
//			
//			if( ml != null)
//			{
//				ml.release();
//			}
//		}
//		else
//		{
//			System.out.println( "Don't Stop LIFX");
//		}
	}
}
