package com.example.lifx_sdk_samples;

import android.util.Log;

public class Alarm {
	Alarm(String day, Long hour, Long minute, Boolean active) {
		this.day = day;
		this.hour = hour;
		this.minute = minute;
		this.active = active;
	}
	
	public String toString() {
		return hour.toString() + "," + minute.toString() + "," + active.toString();
	}
	
	public void fromString(String input) {
		// log.e("LIFX", "Decoding alarm string: " + input);
		String[] parts = input.split(",");
		hour = new Long(parts[0]);
		minute = new Long(parts[1]);
		active = new Boolean(parts[2]);
		
		// log.e("LIFX", "Decoded alarm string: " + hour.toString() + ":" + minute.toString() + " " + active.toString());
	}
	
	public String day;
	public Long hour;
	public Long minute;
	public Boolean active;
}
