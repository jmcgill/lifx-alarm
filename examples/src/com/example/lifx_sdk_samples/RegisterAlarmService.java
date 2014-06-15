package com.example.lifx_sdk_samples;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import lifx.java.android.client.LFXClient;
import lifx.java.android.entities.LFXHSBKColor;
import lifx.java.android.entities.LFXTypes.LFXPowerState;
import lifx.java.android.network_context.LFXNetworkContext;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

public class RegisterAlarmService extends Service {
	private int wakeupCounter;
	private LFXNetworkContext networkContext;
	private Handler handler;
	private MulticastLock ml = null;
	private int delayBetweenScenes = 60 * 1000 * 1;
	// private int delayBetweenScenes = 11000;
	private final ArrayList<String> wakeupColors = new ArrayList<String>(Arrays.asList(
		"240,80,15",
		"240,80,79",
		// Purple
		"305,80,44",
		// White
		"344,84,80",
		"55,72,90",
		"247,8,98",
		"247,8,98"));
	
	Runnable wakeup = new Runnable() {
		@Override
		public void run() {
			Log.e("LIFX", "Running wakeup routine");
			
			// Avoid waking up on the first iteration because it will flash brightly first.
			if (wakeupCounter == 1) {
				networkContext.getAllLightsCollection().setPowerState(LFXPowerState.ON);
			}
			
			Log.e("LIFX", "Counter: " + new Integer(wakeupCounter).toString());
			String color = wakeupColors.get(wakeupCounter);
			String[] parts = color.split(",");
			
			float hBase = Float.valueOf(parts[0]);
			float sBase = (float)(Float.valueOf(parts[1]) / 100.0);
			float bBase = (float)(Float.valueOf(parts[2]) / 100.0); 
			
			LFXHSBKColor hsbkColor = LFXHSBKColor.getColor(hBase, sBase, bBase, 3500);
		    networkContext.getAllLightsCollection().setColorOverDuration(hsbkColor, 10000);
		    wakeupCounter += 1;
		    
		    if (wakeupCounter < 7) {
			  handler.postDelayed(wakeup, delayBetweenScenes);
			} else {
				Log.e("LIFX", "Triggering alarm");
				
				// Disconnect service.
//				networkContext.disconnect();
//				ml.release();
//				ml = null;
//				networkContext = null;

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
	
	// Schedule an alarm to wakeup when it's tomorrow.
	void scheduleNewDayAlarm() {
		Log.e("LIFX", "Setting alarm for 00:05 tomorrow");
		
		Intent i = new Intent("com.example.lifx_sdk_samples.midnight");
		PendingIntent pi = PendingIntent.getBroadcast(this, 2, i, 0);
		AlarmManager am = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
		
		// Avoid duplicate alarms by cancelling any pending intents.
		am.cancel(pi);
	
		// Run at five minutes past midnight on the next day.
		Calendar c = Calendar.getInstance();
		Log.e("LIFX-Service", "Today is: " + c.toString());
		
		c.set(Calendar.HOUR_OF_DAY, 00);
		c.set(Calendar.MINUTE, 04);
		c.set(Calendar.SECOND, 0);
		c.add(Calendar.DATE, 1);  // number of days to add
		
		Log.e("LIFX-Service", "Setting alarm for midnight.");
		Log.e("LIFX-Service", "Setting alarm for: " + c.toString());
		Log.e("LIFX-Service", "Setting midnight alarm at time: " + new Long(c.getTime().getTime()).toString());
		Log.e("LIFX-Service", "Current time is: " + new Long(Calendar.getInstance().getTime().getTime()).toString());
		
		am.setExact(AlarmManager.RTC_WAKEUP, c.getTime().getTime(), pi);
		Log.e("LIFX-Service", "The new day alarm has now been set.");
	}
	
	void scheduleAlarmForToday() {
		// Is there an alarm set for today?
        Alarm alarm = AlarmUtils.getAlarmForToday(this);
		if (alarm != null) {
			Log.e("LIFX-Service", "Scheduling alarm for " + alarm.day + " " + alarm.toString());
			
			// Schedule an alarm to go off at this time. Since this only happens on boot, all previous
			// alarms will have been cleared.
			Date d = new Date();
			d.setHours(0);
			d.setMinutes(0);
			d.setSeconds(0);
			
			// Is the alarm in the past?
			long alarmTime = d.getTime() + ((alarm.hour * 3600) + (alarm.minute * 60)) * 1000;
			
			// Subtract 8 minutes for light display.
			alarmTime -= (8 * 60 * 1000);
			
			// If so, do not bother scheduling it because it will execute immediately.
			long currentTime = (new Date()).getTime();
			if (alarmTime < currentTime) return;
			
			Intent i2 = new Intent("com.example.lifx_sdk_samples");
			PendingIntent pi = PendingIntent.getBroadcast(this, 1, i2, 0);
			AlarmManager am2 = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
		    am2.cancel(pi);
		    am2.setExact(AlarmManager.RTC_WAKEUP, d.getTime() + ((alarm.hour * 3600) + (alarm.minute * 60)) * 1000, pi);
		    Log.e("LIFX-Service", "Set alarm for today");
		}
	}
			
	BroadcastReceiver newDayHandler = new BroadcastReceiver() {
         @Override
         public void onReceive(Context c, Intent i) {	               
              Log.e("LIFX-Alarms", "It's just past midnight!");
              
              // Schedule the next alarm that should occur.
              scheduleAlarmForToday();
              
              // Schedule a new alarm for tomorrow.
              scheduleNewDayAlarm();
         }						
    };
    
	BroadcastReceiver br = new BroadcastReceiver() {
	         @Override
	         public void onReceive(Context c, Intent i) {	               
	                Log.e("LIFX-Alarms", "Waking up to run alarm from boot service.");
	                
	                Alarm alarm = AlarmUtils.getAlarmForToday(c);
	                if (alarm != null && alarm.active == true) {
	                	wakeupCounter = 0;
	                	wakeup.run();
	                }
	         }			
	    };
	   
	  @Override  
	  public int onStartCommand(Intent intent, int flags, int startId) {
		Log.e("LIFX", "Running boot handler");
		
		// Register our delay handler.
		handler = new Handler();
		
		 // A Multicast lock should be acquired, as some phones disable UDP broadcast / recieve
		WifiManager wifi;
      	wifi = (WifiManager) getSystemService( Context.WIFI_SERVICE);
      	ml = wifi.createMulticastLock( "lifx_samples_tag");
      	ml.acquire();
		
		networkContext = LFXClient.getSharedInstance( getApplicationContext()).getLocalNetworkContext();
		networkContext.connect();
		
		Log.e("LIFX", "Got network context");
		
		// Make sure we're set to handle alarms.
		registerReceiver(br, new IntentFilter("com.example.lifx_sdk_samples") );
		registerReceiver(newDayHandler, new IntentFilter("com.example.lifx_sdk_samples.midnight") );
		
		// Schedule any alarms due today.
		scheduleAlarmForToday();
		
		// Make sure we check tomorrow's alarms too!
		scheduleNewDayAlarm();

	    return Service.START_STICKY;
	  }

	  @Override
	  public IBinder onBind(Intent intent) {
	  //TODO for communication return IBinder implementation
	    return null;
	  }
	} 