package ro.marioenache.enhancedsummon.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * Utility class for processing JSON data
 */
public class JsonProcessor {

    /**
     * Extracts text from a JSON element, handling both simple strings
     * and complex JSON text components
     */
    public String extractTextFromJson(JsonElement element) {
        // If it's a primitive (string, number, etc.), just return its string value
        if (element.isJsonPrimitive()) {
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (primitive.isString()) {
                return primitive.getAsString();
            } else {
                return primitive.toString();
            }
        }
        // If it's an array, process each element and combine them
        else if (element.isJsonArray()) {
            StringBuilder result = new StringBuilder();
            JsonArray array = element.getAsJsonArray();
            for (JsonElement arrayElement : array) {
                result.append(extractTextFromJson(arrayElement));
            }
            return result.toString();
        }
        // If it's an object, handle various text component formats
        else if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            return extractTextFromJsonObject(obj);
        }

        // Default case if we couldn't extract text
        return "";
    }

    /**
     * Extracts text from a JSON object that might contain different formats of text components
     */
    private String extractTextFromJsonObject(JsonObject obj) {
        StringBuilder result = new StringBuilder();

        // Handle Minecraft's text component format
        if (obj.has("text")) {
            result.append(obj.get("text").getAsString());
        }

        // Handle extra array of components
        if (obj.has("extra")) {
            JsonArray extraArray = obj.getAsJsonArray("extra");
            for (JsonElement extra : extraArray) {
                result.append(extractTextFromJson(extra));
            }
        }

        // Handle translate with arguments
        if (obj.has("translate")) {
            String translateKey = obj.get("translate").getAsString();
            result.append(translateKey); // We can't actually translate the key, so just use it directly

            // Process "with" array if present
            if (obj.has("with")) {
                JsonArray withArray = obj.getAsJsonArray("with");
                for (JsonElement with : withArray) {
                    result.append(" ").append(extractTextFromJson(with));
                }
            }
        }

        // Handle score component
        if (obj.has("score")) {
            JsonObject scoreObj = obj.getAsJsonObject("score");
            if (scoreObj.has("value")) {
                result.append(scoreObj.get("value").getAsString());
            }
        }

        // Handle selector component
        if (obj.has("selector")) {
            result.append(obj.get("selector").getAsString());
        }

        // Handle keybind component
        if (obj.has("keybind")) {
            result.append(obj.get("keybind").getAsString());
        }

        // Handle nbt component
        if (obj.has("nbt")) {
            result.append(obj.get("nbt").getAsString());
        }

        return result.toString();
    }
}