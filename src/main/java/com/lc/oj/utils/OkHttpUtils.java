package com.lc.oj.utils;

import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
public class OkHttpUtils {

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");


    public static String get(String url, Headers headers) throws Exception {
        Request request = new Request.Builder()
                .url(url)
                .headers(headers)
                .build();

        return getString(request);
    }

    public static String post(String url, String json, Headers headers) throws Exception {
        RequestBody body = RequestBody.create(JSON, json);
        headers.newBuilder().add("Content-Type", "application/json");
        //打印格式化的json
        //log.info(JSONUtil.parseObj(json).toStringPretty());
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .headers(headers)
                .build();

        return getString(request);
    }

    private static String getString(Request request) throws Exception {
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                if (response.code() == 429 || response.code() == 401) {
                    return null; //判题次数超限
                } else {
                    throw new IOException("Unexpected code " + response);
                }
            }
            if (response.body() == null) {
                throw new IOException("Response body is null.");
            }
            return response.body().string();
        }
    }

}