package org.example.utils;

import org.json.JSONObject;
import org.json.JSONArray;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;

public class JsonUtil {
    public static JSONObject parseJsonString(String jsonString) {
        return new JSONObject(jsonString);
    }

    public static JSONArray parseJsonArray(String jsonArrayString) {
        return new JSONArray(jsonArrayString);
    }

    public static String readJsonFile(String filePath) throws IOException {
        return new String(Files.readAllBytes(Paths.get(filePath)));
    }

    public static <T> T parseJsonToObject(String jsonString, Class<T> clazz) {
        JSONObject jsonObject = parseJsonString(jsonString);
        // You might want to use a library like Jackson or Gson for more complex parsing
        return null; // Placeholder - implement with your preferred JSON parsing method
    }
}