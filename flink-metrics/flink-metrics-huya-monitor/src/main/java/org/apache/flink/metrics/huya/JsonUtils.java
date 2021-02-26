package org.apache.flink.metrics.huya;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.JsonParser;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.DeserializationFeature;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class JsonUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        objectMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private JsonUtils() {}

    public static <T> T getNestedObject(String json, String key, Class<T> cls) throws IOException {
        JsonNode jsonNode = objectMapper.readTree(json);
        if (!key.contains(".")) {
            JsonNode subNode = jsonNode.get(key);
            return objectMapper.readValue(subNode.toString(), cls);
        } else {
            String curKey = key.substring(0, key.indexOf("."));
            String nextKey = key.substring(key.indexOf(".") + 1);
            JsonNode subNode = jsonNode.get(curKey);
            return getNestedObject(subNode.toString(), nextKey, cls);
        }
    }

    public static void main(String[] args) throws IOException {
        String json =
                "{\"code\":0,\"data\":\"SET env.job.idle-state-retention=1000,20000;\\nSET env.job.restart.strategies=norestart;\\n\\nCREATE TABLE `test_table_name` (\\n  `field1`  VARCHAR,\\n  `field2`  VARCHAR,\\n  `field3`  VARCHAR,\\n  `field4`  VARCHAR,\\n  `field5`  VARCHAR\\n) WITH (\\n  'connector.version' = 'universal',\\n  'connector.topic' = 'topic_name',\\n  'format.derive-schema' = 'true',\\n  'connector.startup-mode' = 'group-offsets',\\n  'connector.type' = 'kafka',\\n  'update-mode' = 'append',\\n  'connector.properties.0.value' = 'abc',\\n  'connector.properties.0.key' = 'group.id',\\n  'format.type' = 'json'\\n);\\nCREATE TABLE `pvuv_sink` (\\n  `log`  STRING,\\n  `pt`  TIMESTAMP\\n) WITH (\\n  'connector.type' = 'kafka',\\n  'connector.version' = 'universal',\\n  'connector.topic' = 'pvuv_sink',\\n  'connector.properties.0.key' = 'bootstrap.servers',\\n  'connector.properties.0.value' = '172.21.214.52:9092',\\n  'update-mode' = 'append',\\n  'format.type' = 'json',\\n  'format.derive-schema' = 'true'\\n);\\n\\nINSERT INTO `pvuv_sink`\\n(SELECT `log`, `pt`\\nFROM `test_table_name`);\\n\\n\",\"errorCode\":null,\"errMsg\":null}";
        String code = JsonUtils.getNestedObject(json, "data", String.class);
        System.out.println(json);
        System.out.println(code);
    }
}
