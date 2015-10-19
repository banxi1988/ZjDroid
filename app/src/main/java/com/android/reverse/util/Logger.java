package com.android.reverse.util;

import android.util.Log;

import com.android.reverse.BuildConfig;

import de.robv.android.xposed.XposedBridge;

public class Logger {
	
	public static String LOGTAG_COMMAN = "updroid"+BuildConfig.buildNumber+"-";
	public static String LOGTAG_WORKFLOW = "updroid"+BuildConfig.buildNumber+"-apimonitor-";
	public final static boolean DEBUG_ENABLE = true;
	public static String PACKAGENAME;
	
	public static void log(String message){
		if(DEBUG_ENABLE) {
			XposedBridge.log(LOGTAG_COMMAN + PACKAGENAME + message);
		}

	}
	
	public static void log_behavior(String message){
		if(DEBUG_ENABLE){
			XposedBridge.log(LOGTAG_WORKFLOW + PACKAGENAME+message);
		}
	}
	
}
