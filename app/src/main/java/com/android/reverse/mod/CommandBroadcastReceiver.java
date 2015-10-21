package com.android.reverse.mod;

import java.util.List;

import com.android.reverse.request.CommandHandler;
import com.android.reverse.request.CommandHandlerParser;
import com.android.reverse.util.Logger;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import de.robv.android.xposed.XposedBridge;

public class CommandBroadcastReceiver extends BroadcastReceiver {

	public static String INTENT_ACTION = "com.zjdroid.invoke";
	public static String TARGET_KEY = "target";
	public static String COMMAND_NAME_KEY = "cmd";

	@Override
	public void onReceive(final Context ctx, Intent intent) {
		if(!INTENT_ACTION.equalsIgnoreCase(intent.getAction())){
			return;
		}
        int pid = intent.getIntExtra(TARGET_KEY, 0);
        int myPid = android.os.Process.myPid();
        if(pid != myPid){
            return;
        }
        try {
            String cmd = intent.getStringExtra(COMMAND_NAME_KEY);
            final CommandHandler handler = CommandHandlerParser
                    .parserCommand(cmd);
            if (handler != null) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        handler.doAction();
                    }
                }).start();
            }else{
                Logger.log("the cmd is invalid");
            }
        } catch (Exception e) {
            e.printStackTrace();
            XposedBridge.log("onReceive Error "+e.getMessage());
        }
	}
	
}
