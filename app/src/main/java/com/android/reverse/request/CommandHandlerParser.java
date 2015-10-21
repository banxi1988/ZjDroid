package com.android.reverse.request;

import org.json.JSONException;
import org.json.JSONObject;

import com.android.reverse.request.InvokeScriptCommandHandler.ScriptType;
import com.android.reverse.util.Logger;

public class CommandHandlerParser {

	private static String ACTION_NAME_KEY = "action";

	private static String ACTION_DUMP_DEXINFO = "dump_dexinfo";
	private static String ACTION_DUMP_HEAP = "dump_heap";

	private static String ACTION_DUMP_DEXCLASS = "dump_class";
	private static String PARAM_DEXPATH = "dexpath";
	private static String ACTION_DUMP_DEXFILE = "dump_dexfile";
	private static String ACTION_BAKSMALI_DEXFILE = "baksmali";

	private static String ACTION_DUMP_MEMERY = "dump_mem";
	private static String PARAM_START_ADDR = "start";
	private static String PARAM_LENGTH = "length";

	private static String ACTION_INVOKE_SCRIPT = "invoke";
	private static String FILE_SCRIPT = "filepath";

	public static CommandHandler parserCommand(String cmd) {
		try {
			return lookupHandler(cmd);
		}catch (CommandParameterNotFoundException e){
			Logger.log(e.getMessage());
			Logger.log("Please set the "+e.parameterKey+" value");
		} catch (JSONException e) {
			e.printStackTrace();
			Logger.log("cmd error "+cmd);
			Logger.log(e.getMessage());
		}
		return null;
	}


	private static String getStringParameter(JSONObject json,String key) throws JSONException {
		if(!json.has(key)){
			throw new CommandParameterNotFoundException(key);
		}
		return json.getString(key);
	}

	private static CommandHandler lookupHandler(String cmd) throws JSONException{
		Logger.log("lookupHandler for "+cmd);
		JSONObject jsoncmd = new JSONObject(cmd);
		String action = jsoncmd.getString(ACTION_NAME_KEY);
		Logger.log("the cmd = " + action);
		if (ACTION_DUMP_DEXINFO.equals(action)) {
			return new DumpDexInfoCommandHandler();
		} else if (ACTION_DUMP_DEXFILE.equals(action)) {
			String dexpath = getStringParameter(jsoncmd, PARAM_DEXPATH);
            return  new DumpDexFileCommandHandler(dexpath);
		} else if (ACTION_BAKSMALI_DEXFILE.equals(action)) {
			String dexpath = getStringParameter(jsoncmd, PARAM_DEXPATH);
             return new BakSmaliCommandHandler(dexpath);
		} else if (ACTION_DUMP_DEXCLASS.equals(action)) {
            String dexpath = getStringParameter(jsoncmd, PARAM_DEXPATH);
            return  new DumpClassCommandHandler(dexpath);
		} else if (ACTION_DUMP_HEAP.equals(action)) {
			return new DumpHeapCommandHandler();
		} else if (ACTION_INVOKE_SCRIPT.equals(action)) {
            String filepath = getStringParameter(jsoncmd,FILE_SCRIPT);
            return  new InvokeScriptCommandHandler(filepath, ScriptType.FILETYPE);
		} else if (ACTION_DUMP_MEMERY.equals(action)) {
			int start = jsoncmd.getInt(PARAM_START_ADDR);
			int length = jsoncmd.getInt(PARAM_LENGTH);
			return new DumpMemCommandHandler(start, length);
		} else {
			Logger.log(action + " cmd is invalid! ");
		}
		return null;
	}

}

class CommandParameterNotFoundException extends IllegalArgumentException{
	String parameterKey;
	public CommandParameterNotFoundException(String parameterKey){
		super("No Cmd Parameter For Key "+parameterKey);
		this.parameterKey = parameterKey;
	}
}