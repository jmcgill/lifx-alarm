package com.example.lifx_sdk_samples;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class AlarmUtils {
	public static Alarm getAlarmForToday(Context ctx) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE");
		Date d = new Date();
		String dayOfTheWeek = sdf.format(d);
		  
		// Read the alarm time for this day.
		Alarm alarm = new Alarm(dayOfTheWeek, 0L, 0L, true);
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(ctx);
		if (sharedPref.contains(dayOfTheWeek)) {
			String encodedAlarm = sharedPref.getString(dayOfTheWeek, "");
			alarm.fromString(encodedAlarm);
			return alarm;
		}
		
		return null;
	}
}
