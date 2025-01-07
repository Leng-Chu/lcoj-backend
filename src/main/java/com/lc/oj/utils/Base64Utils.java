package com.lc.oj.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.Base64;

@Slf4j
public class Base64Utils {

    public static String encode(String base64) {
        if (base64 == null) {
            return null;
        }
        if (base64.isEmpty()) {
            return "";
        }
        return Base64.getEncoder().encodeToString(base64.getBytes());
    }

    public static String decode(String base64) {
        if (base64 == null) {
            return null;
        }
        if (base64.isEmpty()) {
            return "";
        }
        base64 = base64.replaceAll("\r|\n", "");
        try {
            byte[] decode = Base64.getDecoder().decode(base64);
            return new String(decode);
        } catch (Exception e) {
            log.error("base64解码失败, base64: {}", base64, e);
            return null;
        }
    }

}