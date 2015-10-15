package com.android.reverse.mod;

import android.content.pm.ApplicationInfo;

import com.android.reverse.BuildConfig;
import com.android.reverse.apimonitor.ApiMonitorHookManager;
import com.android.reverse.collecter.DexFileInfoCollecter;
import com.android.reverse.collecter.LuaScriptInvoker;
import com.android.reverse.collecter.ModuleContext;
import com.android.reverse.util.Logger;


import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class ReverseXposedModule implements IXposedHookLoadPackage {
	private static final String ZJDROID_PACKAGENAME = BuildConfig.APPLICATION_ID;
	@Override
	public void handleLoadPackage(LoadPackageParam loadPackageParam) throws Throwable {
			if (loadPackageParam.appInfo == null){
                return;
            }
            String applicationId = loadPackageParam.packageName;
			boolean isSystemApp = (loadPackageParam.appInfo.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
			boolean isZjDroid = ZJDROID_PACKAGENAME.equals(applicationId);
			if(isSystemApp || isZjDroid || !loadPackageParam.isFirstApplication){
				return;
			}

            Logger.PACKAGENAME = applicationId;
            Logger.log("the package = "+applicationId+" has hook");
            Logger.log("the app target id = "+android.os.Process.myPid());
            PackageMetaInfo pminfo = PackageMetaInfo.fromXposed(loadPackageParam);
            ModuleContext.getInstance().initModuleContext(pminfo);
            DexFileInfoCollecter.getInstance().start();
            LuaScriptInvoker.getInstance().start();
            ApiMonitorHookManager.getInstance().startMonitor();
	}
}
