package com.lc.oj.utils;

import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

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


    public static String get(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();

        return getString(request);
    }

    public static String post(String url, String json) throws IOException {
        RequestBody body = RequestBody.create(JSON, json);
        //打印格式化的json
        //log.info(JSONUtil.parseObj(json).toStringPretty());
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        return getString(request);
    }

    @NotNull
    private static String getString(Request request) throws IOException {
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            if (response.body() == null) {
                throw new IOException("Response body is null.");
            }
            return response.body().string();
        }
    }

}