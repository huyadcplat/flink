package org.apache.flink.metrics.huya;

import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


/**
 * send metric to http api
 */
@Slf4j
public class HttpClient {

	private static final Logger LOG = LoggerFactory.getLogger(HttpClient.class);
	private static final MediaType MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

	private final OkHttpClient client;
	private final String address;

	private static final ObjectMapper MAPPER = new ObjectMapper();

	public HttpClient(String address) {

		this.address = address;
		client = new OkHttpClient()
			.newBuilder()
			.connectTimeout(30, TimeUnit.SECONDS)
			.writeTimeout(30, TimeUnit.SECONDS)
			.readTimeout(30, TimeUnit.SECONDS)
			.build();
	}

	public void send(List<ReportRequest> reportObjs) throws IOException {
		String json = MAPPER.writeValueAsString(reportObjs);
		Request.Builder builder = new Request.Builder().url(address);
		RequestBody requestBody = RequestBody.create(MEDIA_TYPE, json);
		Request request = builder.post(requestBody).build();
		Response response = client.newCall(request).execute();
		String ret;
		try (ResponseBody body = response.body()) {
			ret = body.string();
		}
		if (response.isSuccessful()) {
			LOG.info("send metrics to huya monitor system success. size={}", reportObjs.size());
		} else {
			LOG.warn("send metrics to huya monitor system fail. code:{}, resp:{}, json:{} ", response.code(), ret, json);
		}
	}

	public void close() {
		client.dispatcher().executorService().shutdown();
		client.connectionPool().evictAll();
	}

	/**
	 * 通过接口获取需要上报的metric列表
	 *
	 * @param metricUrl
	 * @return
	 */
	public Set<String> getRequiredMetrics(String metricUrl) {
		Set<String> metricsSet = new HashSet<>();
		Request.Builder builder = new Request.Builder().url(metricUrl);
		Request request = builder.get().build();
		Response response = null;
		try {
			response = client.newCall(request).execute();
			ResponseBody body = response.body();
			String json = body.string();
			List<String> metricList = Arrays.asList(JsonUtils.getNestedObject(json, "data", String[].class));
			metricsSet = metricList.stream().map(el -> HuyaMonitorReportor.filter(el)).collect(Collectors.toSet());
		} catch (IOException e) {
			e.printStackTrace();
			log.error("error{}", e);
		}
		return metricsSet;
	}
}
