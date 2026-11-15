package com.minecrafttas.tas8999.utils;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class URLUtil {

	private String protocol;
	private String host;
	private int port;
	private String path;
	private Map<String, String> queryParams;
	private String ref;

	public URLUtil(String urlString) throws MalformedURLException, IllegalArgumentException, UnsupportedEncodingException {
		this(URI.create(urlString).toURL());
	}

	public URLUtil(URL url) throws UnsupportedEncodingException {
		protocol = url.getProtocol();
		host = url.getHost();
		port = url.getPort();
		path = url.getPath();
		queryParams = parseQuery(url);
		ref = url.getRef();
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public void setQueryParams(Map<String, String> queryParams) {
		this.queryParams = queryParams;
	}

	public void setRef(String ref) {
		this.ref = ref;
	}

	public String getProtocol() {
		return protocol;
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public String getPath() {
		return path;
	}

	public boolean hasQueryParam(String key) {
		return queryParams.containsKey(key);
	}

	public String getQueryParam(String key) {
		return queryParams.get(key);
	}

	public void removeQueryParam(String... keys) {
		for (String key : keys) {
			queryParams.remove(key);
		}
	}

	public void addQueryParam(String key, String value) {
		queryParams.put(key, value);
	}

	public Map<String, String> getQueryParams() {
		return queryParams;
	}

	public String getRef() {
		return ref;
	}

	public URL buildURL() throws MalformedURLException {
		return URI.create(build()).toURL();
	}

	public String build() {
		String protocol = this.protocol;
		String host = this.host;
		String port = this.port == -1 ? "" : ":" + Integer.toString(this.port);
		String path = this.path == null ? "" : this.path;
		String queryParams = encodeQuery(this.queryParams);
		String ref = this.ref == null ? "" : "#" + this.ref;
		return String.format("%s://%s%s%s%s%s", protocol, host, port, path, queryParams, ref);
	}

	public static Map<String, String> parseQuery(String urlString) throws Exception {
		return parseQuery(URI.create(urlString).toURL());
	}

	public static Map<String, String> parseQuery(URL url) throws UnsupportedEncodingException {
		String query = url.getQuery();
		Map<String, String> queryParams = new HashMap<>();

		if (query != null) {
			String[] params = query.split("&");
			for (String param : params) {
				String[] keyValue = param.split("=");
				String key = URLDecoder.decode(keyValue[0], "UTF-8");
				if (keyValue.length > 1) {
					String value = URLDecoder.decode(keyValue[1], "UTF-8");
					queryParams.put(key, value);
				} else {
					queryParams.put(key, "");
				}
			}
		}
		return queryParams;
	}

	public static String encodeQuery(Map<String, String> queryParams) {
		List<String> queries = new ArrayList<>();
		if (queryParams == null)
			return "";
		for (Map.Entry<String, String> entry : queryParams.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();
			try {
				queries.add(String.format("%s=%s", URLEncoder.encode(key, "UTF-8"), URLEncoder.encode(value, "UTF-8")));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		if (queries.isEmpty())
			return "";

		return "?" + String.join("&", queries);
	}
}