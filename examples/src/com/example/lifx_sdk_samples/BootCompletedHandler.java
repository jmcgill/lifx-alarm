package com.example.lifx_sdk_samples;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootCompletedHandler extends BroadcastReceiver {
	Log log;
	
	@Override
    public void onReceive(Context context, Intent intent) {
		Intent startServiceIntent = new Intent(context, RegisterAlarmService.class);
        context.startService(startServiceIntent);
    }
}