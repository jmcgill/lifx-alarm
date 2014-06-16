package com.example.lifx_sdk_samples;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class MegaLog {
  private Context context;

  MegaLog(Context context) {
	  this.context = context;
  }
  
  public void Log(String tag, String message) {
	  Log.e("LIFX-" + tag, message);
	  SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
	  String logs = prefs.getString("logs",  "");
	  
	  Date date = new Date();
	  SimpleDateFormat formatter = new SimpleDateFormat("E HH:mm:ss");
	  logs += "[" + formatter.format(date) + "] " + message + "\n";
	  
	  SharedPreferences.Editor editor = prefs.edit();
	  editor.putString("logs", logs);
	  editor.commit();
  }
}
