package com.android.reverse.apimonitor;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.RequestLine;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import com.android.reverse.hook.HookParam;
import com.android.reverse.util.Logger;
import com.android.reverse.util.RefInvoke;


public class NetWorkHook extends ApiMonitorHook {

	@Override
	public void startHook() {
		hookHttpURLConnection();
		hookHttpClient();
	}

	private void hookHttpURLConnection(){
		Method openConnectionMethod = RefInvoke.findMethodExact("java.net.URL", ClassLoader.getSystemClassLoader(), "openConnection");
		hookhelper.hookMethod(openConnectionMethod, new AbstractBahaviorHookCallBack() {
			@Override
			public void descParam(HookParam param) {
				URL url = (URL) param.thisObject;
				Logger.log_behavior("Connect to URL ->");
				Logger.log_behavior("The URL = " + url.toString());
			}
		});
	}

	private void hookHttpClient(){
		Method executeRequest = RefInvoke.findMethodExact("org.apache.http.impl.client.AbstractHttpClient", ClassLoader.getSystemClassLoader(),
				"execute", HttpHost.class, HttpRequest.class, HttpContext.class);

		hookhelper.hookMethod(executeRequest, new AbstractBahaviorHookCallBack() {
			@Override
			public void descParam(HookParam param) {
				Logger.log_behavior("Connect to URL ->");
				HttpHost host = (HttpHost) param.args[0];
				HttpRequest request = (HttpRequest) param.args[1];
				RequestLine requestLine = request.getRequestLine();
				String method = requestLine.getMethod();
				boolean isGET = "GET".equalsIgnoreCase(method);
				boolean isPOST = "POST".equalsIgnoreCase(method);
				if(isGET || isPOST){
					Logger.log_behavior("HTTP Method : " + method);
					Logger.log_behavior("HTTP URL : " + requestLine.getUri());
					logHeaders(request.getAllHeaders());
				}
				if (isPOST) {
					HttpEntity entity = ((HttpPost)request).getEntity();
					try {
						String content = EntityUtils.toString(entity, HTTP.DEFAULT_CONTENT_CHARSET);
						Logger.log_behavior("HTTP POST Content : " + content);
					}catch (IOException e){
						e.printStackTrace();
					}

				}
			}

			private void logHeaders(Header[] headers){
				if (headers != null) {
					for (int i = 0; i < headers.length; i++) {
						Logger.log_behavior(headers[i].getName() + ":" + headers[i].getValue());
					}
				}
			}

			@Override
			public void afterHookedMethod(HookParam param) {
				super.afterHookedMethod(param);
				HttpResponse resp = (HttpResponse) param.getResult();
				if (resp != null) {
					Logger.log_behavior("Status Code = " + resp.getStatusLine().getStatusCode());
					logHeaders(resp.getAllHeaders());
				}
			}
		});
	}

}
