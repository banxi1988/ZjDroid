package com.android.reverse.hook;

import java.lang.reflect.Member;


import de.robv.android.xposed.XposedBridge;

public class XposeHookHelperImpl implements HookHelperInterface {

	@Override
	public void hookMethod(Member method, MethodHookCallBack callback) {
		if(method == null || callback == null){
			XposedBridge.log("hookMethod method=" + method + ", callback=" + callback);
			XposedBridge.log("Failed to hook method or callback is null");
			return;
		}
		XposedBridge.hookMethod(method, callback);
	}

}
