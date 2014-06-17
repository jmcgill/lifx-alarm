package com.example.lifx_sdk_samples;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import lifx.java.android.client.LFXClient;
import lifx.java.android.entities.LFXHSBKColor;
import lifx.java.android.entities.LFXTypes.LFXPowerState;
import lifx.java.android.light.LFXLight.LFXLightListener;
import lifx.java.android.light.LFXTaggedLightCollection;
import lifx.java.android.network_context.LFXNetworkContext;
import lifx.java.android.network_context.LFXNetworkContext.LFXNetworkContextListener;

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
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.util.Log;

// Manage an animation between several colors for an LIFX bulb.
public class LifxAnimationManager implements LFXNetworkContextListener {
	private Context context;
	private LFXNetworkContext networkContext;
	private Handler handler;
	private MulticastLock ml = null;
	private AnimationCompleteHandler completeHandler;
	private MegaLog log;
	private Boolean beginAnimation = false;
	
	// private int delayBetweenScenes = 60 * 1000 * 1;
	private int delayBetweenScenes = 11000;

	// TODO(jmcgill): Rename and factor out.
	private int wakeupCounter;
	private final ArrayList<String> wakeupColors = new ArrayList<String>(
			Arrays.asList("240,80,15", "240,80,79",
			// Purple
					"305,80,44",
					// White
					"344,84,80", "55,72,90", "247,8,98", "247,8,98"));

	public LifxAnimationManager(MegaLog log) {
		this.log = log;
	}
	
	Runnable wakeup = new Runnable() {
		@Override
		public void run() {
			Log.e("LIFX", "Running wakeup routine");

			// Avoid waking up on the first iteration because it will flash
			// brightly first.
			if (wakeupCounter == 1) {
				networkContext.getAllLightsCollection().setPowerState(
						LFXPowerState.ON);
			}

			String color = wakeupColors.get(wakeupCounter);
			String[] parts = color.split(",");

			float hBase = Float.valueOf(parts[0]);
			float sBase = (float) (Float.valueOf(parts[1]) / 100.0);
			float bBase = (float) (Float.valueOf(parts[2]) / 100.0);

			LFXHSBKColor hsbkColor = LFXHSBKColor.getColor(hBase, sBase, bBase,
					3500);
			networkContext.getAllLightsCollection().setColorOverDuration(
					hsbkColor, 10000);
			wakeupCounter += 1;

			// TODO(jmcgill): Use length.
			if (wakeupCounter < 7) {
				handler.postDelayed(wakeup, delayBetweenScenes);
			} else {
				Log.e("LIFX", "Triggering alarm");
				endAnimation();
			}
		}
	};

	public void beginAnimation(Context context, AnimationCompleteHandler completeHandler) {
		log.Log("AnimationManager", "Starting animation");
		this.completeHandler = completeHandler;

		// Register our delay handler.
		handler = new Handler();
		
		// We want to start animating as soon as the device is connected.
		beginAnimation = true;

		// A Multicast lock should be acquired, as some phones disable UDP broadcast / receive
		WifiManager wifi;
		wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		ml = wifi.createMulticastLock("lifx_samples_tag");
		ml.acquire();

		networkContext = LFXClient.getSharedInstance(context.getApplicationContext()).getLocalNetworkContext();
		networkContext.addNetworkContextListener(this);
		networkContext.connect();
	}
	
	public void endAnimation() {
		log.Log("AnimationManager", "Animation Complete");
		
		networkContext.disconnect();
		networkContext = null;
		
		ml.release();
		ml = null;
		
		if (completeHandler != null) {
			completeHandler.complete();
		}
	}
	
	@Override
	public void networkContextDidConnect(LFXNetworkContext networkContext) {
		log.Log("AnimationManager", "Network context connected");
		
		// Once connected we can begin the animation.
		// TODO(jmcgill): Do we actually need to do this, or will the client cache the state?
		if (beginAnimation == true) {
			wakeupCounter = 0;
			wakeup.run();
			
			beginAnimation = false;
		}
	}
	
	public void networkContextDidDisconnect(LFXNetworkContext networkContext) {
		log.Log("AnimationManager", "Network context disconnected");
	}
	
	@Override
	public void networkContextDidAddTaggedLightCollection(
			LFXNetworkContext networkContext,
			LFXTaggedLightCollection collection) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void networkContextDidRemoveTaggedLightCollection(
			LFXNetworkContext networkContext,
			LFXTaggedLightCollection collection) {
		// TODO Auto-generated method stub
		
	}
}