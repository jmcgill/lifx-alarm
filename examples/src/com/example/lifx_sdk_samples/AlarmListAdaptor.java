//
//  LFXSDKLightListAdapter.java
//  LIFX
//
//  Created by Jarrod Boyes on 24/03/14.
//  Copyright (c) 2014 LIFX Labs. All rights reserved.
//

package com.example.lifx_sdk_samples;

import java.lang.ref.SoftReference;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Switch;

import lifx.java.android.light.LFXLight;

public class AlarmListAdaptor extends BaseAdapter
{
	private static final int TIME_DIALOG_ID = 1;
	
	// private ArrayList<LFXLight> lights = new ArrayList<LFXLight>();
	private ArrayList<Alarm> alarms = new ArrayList<Alarm>();
	private SoftReference<Activity> activity;
	private Log log;
	
	public AlarmListAdaptor( Activity activity)
	{
		this.activity = new SoftReference<Activity>( activity);
	}
	
	public void updateWithAlarms( ArrayList<Alarm> newAlarms)
	{
		alarms.clear();
		alarms.addAll(newAlarms);
		notifyDataSetChanged();
	}
	
	@Override
	public int getCount()
	{
		log.e("LIFX", "Count is: " + new Integer(alarms.size()).toString());
		return alarms.size();
	}

	@Override
	public Object getItem(int position)
	{
		return alarms.get(position);
	}

	@Override
	public long getItemId(int position)
	{
		return position;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup listView)
	{
		if(convertView == null)
		{
			convertView = activity.get().getLayoutInflater().inflate(R.layout.lifx_list_item_layout, null);
		}
		
		final View realView = convertView;
		
		Alarm alarm = (Alarm) getItem(position);
		TextView dayText = (TextView) convertView.findViewById(R.id.dayText);
		dayText.setText(alarms.get(position).day);
		
		Switch switch_ = (Switch) convertView.findViewById(R.id.timeSwitch);
		switch_.setText(new Long(alarm.hour).toString() + ":" + String.format("%02d", alarm.minute) + " AM");
		switch_.setChecked(alarm.active);
		
		switch_.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				Alarm alarm = alarms.get(position);
				alarm.active = isChecked;
				SharedPreferences sharedPref = activity.get().getPreferences(Context.MODE_PRIVATE);
				SharedPreferences.Editor editor = sharedPref.edit();
				editor.putString(alarm.day, alarm.toString());
				editor.commit();
			}
		});
		
		dayText.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Log.e("LIFX", "Clicked");
				TimePickerDialog dialog = new TimePickerDialog(activity.get(), new TimePickerDialog.OnTimeSetListener() {
					int callCount = 0;
					@Override
					public void onTimeSet(TimePicker view, int hour, int minute) {
						if (callCount == 0) {
							callCount += 1;
							return;
						}
						
						Log.e("LIFX", "Time being sent twice");
						
						Alarm alarm = alarms.get(position);
						
						alarm.hour = (long) hour;
						alarm.minute = (long) minute;
						
						// Update display.
						Switch switch_ = (Switch)realView.findViewById(R.id.timeSwitch);
						switch_.setText(new Integer(hour).toString() + ":" + String.format("%02d", minute) + " AM");
						
						// Store new time.
						SharedPreferences sharedPref = activity.get().getPreferences(Context.MODE_PRIVATE);
						SharedPreferences.Editor editor = sharedPref.edit();
						editor.putString(alarm.day, alarm.toString());
						editor.commit();
						
						Intent i = new Intent("com.example.lifx_sdk_samples");
						PendingIntent pi = PendingIntent.getBroadcast(activity.get(), 1, i, 0);
						AlarmManager am = (AlarmManager) activity.get().getSystemService(Context.ALARM_SERVICE);
						
						// Is this today? If so, set an alarm immediately. Otherwise rely on our wakeup to pick it up.
						SimpleDateFormat sdf = new SimpleDateFormat("EEEE");
						Date d = new Date();
						String dayOfTheWeek = sdf.format(d);
						
						Log.e("LIFX", "Day of the week: " + alarm.day + " vs " + dayOfTheWeek);
						
						if (alarm.day.equalsIgnoreCase(dayOfTheWeek)) {
							
							Log.e("LIFX", "Days match");
							
							// Get the time at the start of today
							// String str_date="14-06-2014";
							// SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
							// Date date = null;
							
							// The beginning of today.
							d.setHours(0);
							d.setMinutes(0);
							d.setSeconds(0);
							
							//try {
							//	date = (Date)formatter.parse(str_date);
							//} catch (ParseException e) {
							//	// TODO Auto-generated catch block
							//	e.printStackTrace();
							//} 
							//Log.e("LIFX", "Today is " + date.getTime());
							
							Long timestamp = new Long(System.currentTimeMillis());
							am.cancel(pi);
							// TEMPORARY
							am.setExact(AlarmManager.RTC_WAKEUP, d.getTime() + ((alarm.hour * 3600) + (alarm.minute * 60)) * 1000, pi);
							
							Log.e("LIFX", "Alarm at time: " + new Long(d.getTime() + ((alarm.hour * 3600) + (alarm.minute * 60)) * 1000).toString());
							Log.e("LIFX", "Current time: " + timestamp.toString());
						}
					}
				}, (int) (long) alarms.get(position).hour, (int) (long) alarms.get(position).minute, false);
				dialog.show();
				// showTimeDialog(v);
			}
		});
		
		return convertView;
	}
}