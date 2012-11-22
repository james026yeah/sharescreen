package com.archermind.ashare;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import archermind.dlna.household.RendererService;

public class AShareReceiver extends BroadcastReceiver {
	protected final static String TAG = "AShareReceiver";
	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		Log.v(TAG, "onReceive action:" + action);
		if(action.equals(Intent.ACTION_BOOT_COMPLETED)) {
			Log.v(TAG, "onReceive ACTION_BOOT_COMPLETED try to start RendererService!");
			Intent i = new Intent(context, RendererService.class);
			context.startService(i);
		} else if(action.equals(Intent.ACTION_USER_PRESENT)) {
			Log.v(TAG, "onReceive ACTION_USER_PRESENT try to start RendererService!");
			Intent i = new Intent(context, RendererService.class);
			context.startService(i);
		}
	}
}
