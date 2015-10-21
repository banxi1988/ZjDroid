package com.android.reverse.collecter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.android.reverse.BuildConfig;
import com.android.reverse.hook.HookHelperFacktory;
import com.android.reverse.hook.HookHelperInterface;
import com.android.reverse.hook.HookParam;
import com.android.reverse.hook.MethodHookCallBack;
import com.android.reverse.hook.SimpleMethodHookCallbackAdapter;
import com.android.reverse.smali.MemoryBackSmali;
import com.android.reverse.util.Logger;
import com.android.reverse.util.NativeFunction;
import com.android.reverse.util.RefInvoke;
import dalvik.system.DexFile;
import dalvik.system.PathClassLoader;
import de.robv.android.xposed.XposedBridge;

public class DexFileInfoCollecter{

	public static final String DALVIK_SYSTEM_DEX_FILE = "dalvik.system.DexFile";
	public static final String DALVIK_SYSTEM_BASE_DEX_CLASS_LOADER = "dalvik.system.BaseDexClassLoader";
	private static PathClassLoader pathClassLoader;
	private static HashMap<String, DexFileInfo> dynLoadedDexInfo = new HashMap<>();
	private static DexFileInfoCollecter collecter;
	private HookHelperInterface hookhelper = HookHelperFacktory.getHookHelper();
	private final static String DVMLIB_LIB = "dvmnative";

	private DexFileInfoCollecter() {

	}

	public static DexFileInfoCollecter getInstance() {
		if (collecter == null)
			collecter = new DexFileInfoCollecter();
		return collecter;
	}

	public void start() throws Throwable {
		pathClassLoader = (PathClassLoader) ModuleContext.getInstance().getBaseClassLoader();
		hookOpenDexFileNativeMethod();
		hookDefineClassNativeMethod();
		hookFindLibraryMethod();
	}

	private void hookOpenDexFileNativeMethod(){
		XposedBridge.log("hookOpenDexFileNativeMethod systemClassLoader = "+ClassLoader.getSystemClassLoader()+", pathClassLoader="+pathClassLoader);
		Method openDexFileMethod = RefInvoke.findMethodExact(DALVIK_SYSTEM_DEX_FILE, ClassLoader.getSystemClassLoader(), "openDexFileNative",
				String.class, String.class, int.class);
		if(openDexFileMethod == null){
			openDexFileMethod = RefInvoke.findMethodExact(DALVIK_SYSTEM_DEX_FILE,ClassLoader.getSystemClassLoader(),"openDexFile",String.class,String.class,int.class);
		}
		hookhelper.hookMethod(openDexFileMethod, new SimpleMethodHookCallbackAdapter() {
			@Override
			public void afterHookedMethod(HookParam param) {
				String dexPath = (String) param.args[0];
				int mCookie = (Integer) param.getResult();
				if (mCookie != 0) {
					Logger.log("openDexFile " + dexPath);
					dynLoadedDexInfo.put(dexPath, new DexFileInfo(dexPath, mCookie));
				}
			}
		});
	}

	private void hookDefineClassNativeMethod(){
		// for 4.4 and later
		Method defineClassMethod = RefInvoke.findMethodExact(DALVIK_SYSTEM_DEX_FILE, ClassLoader.getSystemClassLoader(), "defineClassNative",
				String.class, ClassLoader.class,int.class);
		if(defineClassMethod == null){
			// for jelly bean and later
			defineClassMethod = RefInvoke.findMethodExact(DALVIK_SYSTEM_DEX_FILE, ClassLoader.getSystemClassLoader(), "defineClass",String.class,ClassLoader.class,int.class);
		}


		hookhelper.hookMethod(defineClassMethod, new SimpleMethodHookCallbackAdapter() {
			@Override
			public void afterHookedMethod(HookParam param) {
				if(!param.hasThrowable()){
					int mCookie = (Integer) param.args[2];
					setDefineClassLoader(mCookie, (ClassLoader) param.args[1]);
				}
			}
		});

	}

	private void hookFindLibraryMethod(){
		Method findLibraryMethod = RefInvoke.findMethodExact(DALVIK_SYSTEM_BASE_DEX_CLASS_LOADER, ClassLoader.getSystemClassLoader(), "findLibrary",
				String.class);
		hookhelper.hookMethod(findLibraryMethod, new SimpleMethodHookCallbackAdapter() {
			@Override
			public void afterHookedMethod(HookParam param) {
				Logger.log((String) param.args[0]);
				if (DVMLIB_LIB.equals(param.args[0]) && param.getResult() == null) {
					param.setResult("/data/data/" + BuildConfig.APPLICATION_ID + "/lib/libdvmnative.so");
				}
			}
		});
	}

	public HashMap<String, DexFileInfo> dumpDexFileInfo() {
		HashMap<String, DexFileInfo> dexs = new HashMap<>(dynLoadedDexInfo);
		HashMap<String,DexFileInfo> staticDexs = dumpStaticDexFileInfo();
		dexs.putAll(staticDexs);
		return dexs;
	}

	private HashMap<String,DexFileInfo> dumpStaticDexFileInfo(){
		HashMap<String, DexFileInfo> dexs = new HashMap<>();
		Object dexPathList = RefInvoke.getFieldOjbect(DALVIK_SYSTEM_BASE_DEX_CLASS_LOADER, pathClassLoader, "pathList");
		Object[] dexElements = (Object[]) RefInvoke.getFieldOjbect("dalvik.system.DexPathList", dexPathList, "dexElements");
		DexFile dexFile = null;
		for (int i = 0; i < dexElements.length; i++) {
			dexFile = (DexFile) RefInvoke.getFieldOjbect("dalvik.system.DexPathList$Element", dexElements[i], "dexFile");
			String mFileName = (String) RefInvoke.getFieldOjbect(DALVIK_SYSTEM_DEX_FILE, dexFile, "mFileName");
			int mCookie = RefInvoke.getFieldInt(DALVIK_SYSTEM_DEX_FILE, dexFile, "mCookie");
			DexFileInfo dexinfo = new DexFileInfo(mFileName, mCookie, pathClassLoader);
			dexs.put(mFileName, dexinfo);
		}
		return dexs;
	}

	public String[] dumpLoadableClass(String dexPath) {
		int mCookie = parseCookieOfDexPath(dexPath);
		if (mCookie != 0) {
			return (String[]) RefInvoke.invokeStaticMethod(DALVIK_SYSTEM_DEX_FILE, "getClassNameList", new Class[] { int.class },
					new Object[] { mCookie });
		} else {
			Logger.log("the cookie is not right");
		}
		return null;

	}

	public void baksmaliDexFile(String filename, String dexPath) {
		File file = new File(filename);
		try {
			if (!file.exists())
				file.createNewFile();
			int mCookie = parseCookieOfDexPath(dexPath);
			if (mCookie != 0) {
				MemoryBackSmali.disassembleDexFile(mCookie, filename);
			} else {
				Logger.log("the cookie is not right");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private int parseCookieOfDexPath(String dexPath){
		int cookie = 0;
		try {
			cookie = Integer.parseInt(dexPath);
		}catch (NumberFormatException e){
			cookie = getCookie(dexPath);
		}
		return cookie;
	}
	
	public void dumpDexFile(String filename, String dexPath) {
		File file = new File(filename);
		try {
			if (!file.exists())
				file.createNewFile();
			int mCookie = parseCookieOfDexPath(dexPath);
			if (mCookie != 0) {
				FileOutputStream out = new FileOutputStream(file);
				ByteBuffer data = NativeFunction.dumpDexFileByCookie(mCookie, ModuleContext.getInstance().getApiLevel());
				data.order(ByteOrder.LITTLE_ENDIAN);
				byte[] buffer = new byte[8192];
				data.clear();
				while (data.hasRemaining()) {
					int count = Math.min(buffer.length, data.remaining());
					data.get(buffer, 0, count);
					try {
						out.write(buffer, 0, count);
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			} else {
				Logger.log("the cookie is not right");
			}
		} catch (IOException e) {
			e.printStackTrace();
			XposedBridge.log("UpDroid dumpDexFileError "+e.getMessage());
		}
	}

	private int getCookie(String dexPath) {

		if (dynLoadedDexInfo.containsKey(dexPath)) {
			DexFileInfo dexFileInfo = dynLoadedDexInfo.get(dexPath);
			return dexFileInfo.getmCookie();
		} else {
			HashMap<String,DexFileInfo> staticDexs = dumpStaticDexFileInfo();
			for(HashMap.Entry<String,DexFileInfo> entry:staticDexs.entrySet()){
				if(entry.getKey().equals(dexPath)){
					return entry.getValue().getmCookie();
				}
			}
			return 0;
		}

	}
	
	private void setDefineClassLoader(int mCookie, ClassLoader classLoader){
		Iterator<DexFileInfo> dexinfos = dynLoadedDexInfo.values().iterator();
		DexFileInfo info = null;
		while(dexinfos.hasNext()){
			info = dexinfos.next();
			if(mCookie == info.getmCookie()){
				if(info.getDefineClassLoader() == null)
				   info.setDefineClassLoader(classLoader);
			}
		}
	}

}
