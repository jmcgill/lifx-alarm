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

// Ensures alarms are run at the right time.
public class LocalAlarmManager {
	MegaLog log;
	Context context;
	
	LocalAlarmManager(MegaLog log, Context context) {
		this.log = log;
		this.context = context;
	}
	
	// Schedule an alarm to wakeup when it's tomorrow.
	void scheduleNewDayAlarm() {
		log.Log("AlarmManager", "Setting alarm for 00:05 tomorrow");
		
		Intent i = new Intent("com.example.lifx_sdk_samples.midnight");
		PendingIntent pi = PendingIntent.getBroadcast(context, 2, i, 0);
		AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		
		// Avoid duplicate alarms by cancelling any pending intents.
		am.cancel(pi);
	
		// Run at five minutes past midnight on the next day.
		Calendar c = Calendar.getInstance();
		
		c.set(Calendar.HOUR_OF_DAY, 00);
		c.set(Calendar.MINUTE, 04);
		c.set(Calendar.SECOND, 0);
		c.add(Calendar.DATE, 1);  // number of days to add
		
		Log.e("LIFX-Service", "Setting alarm for midnight.");
		Log.e("LIFX-Service", "Setting alarm for: " + c.toString());
		Log.e("LIFX-Service", "Setting midnight alarm at time: " + new Long(c.getTime().getTime()).toString());
		Log.e("LIFX-Service", "Current time is: " + new Long(Calendar.getInstance().getTime().getTime()).toString());
		
		am.setExact(AlarmManager.RTC_WAKEUP, c.getTime().getTime(), pi);
	}
	
	void scheduleAlarmForToday() {
		// Is there an alarm set for today?
        Alarm alarm = AlarmUtils.getAlarmForToday(context);
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
			PendingIntent pi = PendingIntent.getBroadcast(context, 1, i2, 0);
			AlarmManager am2 = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		    am2.cancel(pi);
		    am2.setExact(AlarmManager.RTC_WAKEUP, d.getTime() + ((alarm.hour * 3600) + (alarm.minute * 60)) * 1000, pi);
		    Log.e("LIFX-Service", "Set alarm for today");
		}
	}
	
	// This handler can be registered here because the application must have run
	// in order to register the midnight handler.
	BroadcastReceiver newDayHandler = new BroadcastReceiver() {
         @Override
         public void onReceive(Context c, Intent i) {	               
              log.Log("LocalAlarmManager", "Running midnight event scheduler.");
              
              // Schedule the next alarm that should occur.
              scheduleAlarmForToday();
              
              // Schedule a new alarm for tomorrow.
              scheduleNewDayAlarm();
         }						
    };
    
    // TODO(jmcgill): Consider moving this to a manifest intent. YourOnReceiver needs to be a broadcast receiver,
    // which could then create an activity I guess?
    // <receiver android:name="com.package.YourOnReceiver">
    // <intent-filter>
    //    <action android:name="WhatEverYouWant" />
    // </intent-filter>
    // </receiver>
    BroadcastReceiver br = new BroadcastReceiver() {
	         @Override
	         public void onReceive(Context c, Intent i) {	               
	                log.Log("LocalAlarmManager", "Alarm triggered.");
	         }			
	    };
	   
	public void initialize() {
		// Register receivers.		
		context.registerReceiver(br, new IntentFilter("com.example.lifx_sdk_samples") );
		context.registerReceiver(newDayHandler, new IntentFilter("com.example.lifx_sdk_samples.midnight") );
		
		// Schedule any alarms due today.
		scheduleAlarmForToday();
		
		// Make sure we check tomorrow's alarms too!
		scheduleNewDayAlarm();
	}
}