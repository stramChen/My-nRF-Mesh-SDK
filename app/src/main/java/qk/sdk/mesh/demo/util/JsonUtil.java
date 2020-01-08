package qk.sdk.mesh.demo.util;


import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class JsonUtil {
    private static Gson gson = new Gson();

    public static <T> T object(String json, Class<T> classOfT) {
        return gson.fromJson(json, classOfT);
    }

    public static <T> T fromJson(String json, Type typeOfT) {
        if (json == null) {
            return null;
        }
        return gson.fromJson(new JsonParser().parse(json), typeOfT);
    }

    public static <T> T formJsonToArray(JsonElement json, Type t) {
        if (json == null) {
            return null;
        }

        return gson.fromJson(json.toString(), t);
    }

    public static <T> T formJsonToArray(String json, Type t) {
        if (json == null) {
            return null;
        }

        return gson.fromJson(json, t);
    }

    public static String string(Object object) {
        return gson.toJson(object);
    }

    public static <T> List<T> getArrays(String jsonString, Class<T> cls) {

        List<T> list = new ArrayList<T>();
        try {
            JSONArray jsonArray = new JSONObject(jsonString).getJSONArray("");
            Gson gson = new Gson();
            list = gson.fromJson(jsonArray.toString(), new TypeToken<List<T>>() {
            }.getType());

        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public static JsonElement toJson(Object object) {
        if (object == null) {
            return null;
        }
        return gson.toJsonTree(object);
    }

    public static <T> T fromJson(JsonElement json, Type typeOfT) {
        if (json == null) {
            return null;
        }
        return gson.fromJson(json, typeOfT);
    }

    public static <T> T fromJsonToGeneric(String json, Class base, Class target) {
        Gson gson = new Gson();
        Type objectType = type(target, base);
        return gson.fromJson(json, objectType);
    }

    static ParameterizedType type(final Class raw, final Type... args) {
        return new ParameterizedType() {
            public Type getRawType() {
                return raw;
            }

            public Type[] getActualTypeArguments() {
                return args;
            }

            public Type getOwnerType() {
                return null;
            }
        };
    }

}
