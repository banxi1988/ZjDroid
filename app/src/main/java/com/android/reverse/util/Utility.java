package com.android.reverse.util;

import java.lang.reflect.Method;

public class Utility {
	public static int minApiLevel = 16;
	public static int getApiLevel() {
		try {
			Class<?> mClassType = Class.forName("android.os.SystemProperties");
			Method mGetIntMethod = mClassType.getDeclaredMethod("getInt",
					String.class, int.class);
			mGetIntMethod.setAccessible(true);
			return (Integer)mGetIntMethod.invoke(null, "ro.build.version.sdk",minApiLevel);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return minApiLevel;

	}

}
