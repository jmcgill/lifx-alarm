package com.example.lifx_sdk_samples;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class RegisterAlarmService extends Service {
	  @Override
	  public int onStartCommand(Intent intent, int flags, int startId) {
		  
	    return Service.START_STICKY;
	  }

	  @Override
	  public IBinder onBind(Intent intent) {
	  //TODO for communication return IBinder implementation
	    return null;
	  }
	} 