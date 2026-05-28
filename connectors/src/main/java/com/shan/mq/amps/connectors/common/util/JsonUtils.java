package com.shan.mq.amps.connectors.common.util;


import com.fasterxml.jackson.databind.ObjectMapper;

public final class JsonUtils {

    private static final ObjectMapper MAPPER =
            new ObjectMapper();

    private JsonUtils() {
    }

    public static String toJson(Object object) {

        try {
            return MAPPER.writeValueAsString(object);
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static <T> T fromJson(
            String json,
            Class<T> clazz) {

        try {
            return MAPPER.readValue(json, clazz);
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}