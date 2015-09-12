package com.github.to2mbn.jyal.io;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;

public class JSONHttpRequester extends HttpRequester {

	public JSONHttpRequester() {
	}

	public JSONHttpRequester(Proxy proxy) {
		super(proxy);
	}

	public JSONObject jsonPost(String url, Map<String, Object> arguments, JSONObject post) throws JSONException, UnsupportedEncodingException, MalformedURLException, IOException {
		return new JSONObject(post(url, arguments, post.toString(), "application/json"));
	}

	public JSONObject jsonGet(String url, Map<String, Object> arguments) throws JSONException, UnsupportedEncodingException, MalformedURLException, IOException {
		return new JSONObject(get(url, arguments));
	}

}
