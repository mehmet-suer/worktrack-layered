package com.worktrack.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.List;

public class JsonUtils {
    private static final ObjectMapper mapper = new ObjectMapper() .findAndRegisterModules()
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);;

    public static String asJsonString(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize object to JSON", e);
        }
    }

    public static <T> T fromJsonString(String json, Class<T> clazz) {
        try {
            return mapper.readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize JSON to object", e);
        }
    }

    public static <T> List<T> fromJsonArrayString(String json, Class<T> clazz) {
        try {
            return mapper.readValue(
                    json,
                    mapper.getTypeFactory().constructCollectionType(List.class, clazz)
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize JSON to List<" + clazz.getSimpleName() + ">", e);
        }
    }
}
