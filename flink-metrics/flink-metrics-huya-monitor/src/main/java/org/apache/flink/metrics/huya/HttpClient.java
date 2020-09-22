package org.apache.flink.metrics.huya;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;


/**
 *
 */
public class HttpClient {

	private static final Logger LOG = LoggerFactory.getLogger(HttpClient.class);
	private static final MediaType MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

	private final OkHttpClient client;
	private final String address;
	private final String uri;

	private static final ObjectMapper MAPPER = new ObjectMapper();

	public HttpClient(String address, String uri) {

		this.address = address;
		this.uri = uri;
		client = new OkHttpClient()
			.newBuilder()
			.connectTimeout(30, TimeUnit.SECONDS)
			.writeTimeout(30, TimeUnit.SECONDS)
			.readTimeout(30, TimeUnit.SECONDS)
			.build();
	}

	public void send(List<ReportObj> reportObjs) throws IOException {
		String json = MAPPER.writeValueAsString(reportObjs);
		Request.Builder builder = new Request.Builder().url(address + uri);
		RequestBody requestBody = RequestBody.create(MEDIA_TYPE, json);
		Request request = builder.post(requestBody).build();
		Response response = client.newCall(request).execute();
		String ret;
		try (ResponseBody body = response.body()) {
			ret = body.string();
		}
		if (response.isSuccessful()) {
			LOG.info("send metrics to huya monitor system success.");
		} else {
			LOG.warn("send metrics to huya monitor system fail. code:{}, resp:{}, json:{} ", response.code(), ret, json);

		}
	}

	public void close() {
		client.dispatcher().executorService().shutdown();
		client.connectionPool().evictAll();
	}
}
