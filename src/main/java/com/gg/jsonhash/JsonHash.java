
// @license
// Copyright (c) 2019 - 2024 Dr. Gabriel Gatzsche. All Rights Reserved.
//
// Use of this source code is governed by terms that can be
// found in the LICENSE file in the root of this package.
package com.gg.jsonhash;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

import com.google.gson.Gson;

import org.apache.commons.codec.binary.Base64;

@SuppressWarnings("unchecked")
public class JsonHash {
  private final boolean updateExistingHashes;
  private final int hashLength;
  private final int floatingPointPrecision;
  private final boolean recursive;

  public JsonHash() {
    this(22, 10, true, true);
  }

  public JsonHash(int hashLength, int floatingPointPrecision) {
    this(hashLength, floatingPointPrecision, true, true);
  }

  public JsonHash(int hashLength, int floatingPointPrecision, boolean updateExistingHashes, boolean recursive) {
    this.hashLength = hashLength;
    this.floatingPointPrecision = floatingPointPrecision;
    this.updateExistingHashes = updateExistingHashes;
    this.recursive = recursive;
  }

  public Map<String, Object> applyTo(Map<String, Object> json) {
    return applyTo(json, false);
  }

  public Map<String, Object> applyTo(Map<String, Object> json, boolean inPlace) {
    Map<String, Object> copy = inPlace ? json : copyJson(json);
    addHashesToObject(copy, recursive);
    return copy;
  }

  public String applyToString(String jsonString) {
    Gson gson = new Gson();
    Map<String, Object> json = gson.fromJson(jsonString, Map.class);
    Map<String, Object> hashedJson = applyTo(json, true);
    return gson.toJson(hashedJson);
  }

  public String calcHash(String string) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(string.getBytes(StandardCharsets.UTF_8));
      return Base64.encodeBase64URLSafeString(hash).substring(0, hashLength);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  public void validate(Map<String, Object> json) throws Exception {
    Map<String, Object> jsonWithCorrectHashes = applyTo(json, false);
    validate(json, jsonWithCorrectHashes, "");
  }

  private static void validate(Map<String, Object> jsonIs, Map<String, Object> jsonShould, String path)
      throws Exception {
    String expectedHash = (String) jsonShould.get("_hash");
    String actualHash = (String) jsonIs.get("_hash");

    if (actualHash == null) {
      String pathHint = path.isEmpty() ? "" : " at " + path;
      throw new RuntimeException("Hash" + pathHint + " is missing.");
    }

    if (!expectedHash.equals(actualHash)) {
      String pathHint = path.isEmpty() ? "" : " at " + path;
      throw new RuntimeException(
          "Hash" + pathHint + " \"" + actualHash + "\" is wrong. Should be \"" + expectedHash + "\".");
    }

    for (Map.Entry<String, Object> item : jsonIs.entrySet()) {
      if (item.getKey().equals("_hash"))
        continue;
      if (item.getValue() instanceof Map) {
        Map<String, Object> childIs = (Map<String, Object>) item.getValue();
        Map<String, Object> childShould = (Map<String, Object>) jsonShould.get(item.getKey());
        validate(childIs, childShould, path + "/" + item.getKey());
      } else if (item.getValue() instanceof List) {
        List<?> list = (List<?>) item.getValue();
        for (int i = 0; i < list.size(); i++) {
          if (list.get(i) instanceof Map) {
            Map<String, Object> itemIs = (Map<String, Object>) list.get(i);
            Map<String, Object> itemShould = (Map<String, Object>) ((List<?>) jsonShould.get(item.getKey())).get(i);
            validate(itemIs, itemShould, path + "/" + item.getKey() + "/" + i);
          }
        }
      }
    }
  }

  private void addHashesToObject(Map<String, Object> obj, boolean recursive) {
    if (!updateExistingHashes && obj.containsKey("_hash")) {
      return;
    }

    for (Map.Entry<String, Object> entry : obj.entrySet()) {
      if (entry.getValue() instanceof Map) {
        Map<String, Object> value = (Map<String, Object>) entry.getValue();
        if (value.containsKey("_hash") && !recursive) {
          continue;
        }
        addHashesToObject(value, recursive);
      } else if (entry.getValue() instanceof List) {
        processList((List<?>) entry.getValue());
      }
    }

    Map<String, Object> objToHash = new LinkedHashMap<>();
    for (Map.Entry<String, Object> entry : obj.entrySet()) {
      String key = entry.getKey();
      if (key.equals("_hash"))
        continue;
      Object value = entry.getValue();

      if (value instanceof Map) {
        objToHash.put(key, ((Map<String, Object>) value).get("_hash"));
      } else if (value instanceof List) {
        objToHash.put(key, flattenList((List<?>) value));
      } else if (isBasicType(value)) {
        objToHash.put(key, convertBasicType(value, floatingPointPrecision));
      } else {
        throw new RuntimeException("Unsupported type: " + value.getClass());
      }
    }

    Map<String, Object> sortedMap = new TreeMap<>(objToHash);
    String sortedMapJson = jsonString(sortedMap);
    String hash = calcHash(sortedMapJson);
    obj.put("_hash", hash);
  }

  public static Object convertBasicType(Object value, int floatingPointPrecision) {
    if (value instanceof String) {
      return value;
    }
    if (value instanceof Number) {
      return truncate((Number) value, floatingPointPrecision);
    } else if (value instanceof Boolean) {
      return value;
    } else {
      throw new RuntimeException("Unsupported type: " + value.getClass());
    }
  }

  private List<Object> flattenList(List<?> list) {
    List<Object> flattenedList = new ArrayList<>();
    for (Object element : list) {
      if (element instanceof Map) {
        flattenedList.add(((Map<String, Object>) element).get("_hash"));
      } else if (element instanceof List) {
        flattenedList.add(flattenList((List<?>) element));
      } else if (isBasicType(element)) {
        flattenedList.add(convertBasicType(element, floatingPointPrecision));
      }
    }
    return flattenedList;
  }

  private void processList(List<?> list) {
    for (Object element : list) {
      if (element instanceof Map) {
        addHashesToObject((Map<String, Object>) element, recursive);
      } else if (element instanceof List) {
        processList((List<?>) element);
      }
    }
  }

  public static Map<String, Object> copyJson(Map<String, Object> json) {
    Map<String, Object> copy = new LinkedHashMap<>();
    for (Map.Entry<String, Object> entry : json.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();
      if (value instanceof Map) {
        copy.put(key, copyJson((Map<String, Object>) value));
      } else if (value instanceof List) {
        copy.put(key, copyList((List<?>) value));
      } else if (isBasicType(value)) {
        copy.put(key, value);
      } else {
        throw new RuntimeException("Unsupported type: " + value.getClass());
      }
    }
    return copy;
  }

  private static List<Object> copyList(List<?> list) {
    List<Object> copy = new ArrayList<>();
    for (Object element : list) {
      if (element instanceof Map) {
        copy.add(copyJson((Map<String, Object>) element));
      } else if (element instanceof List) {
        copy.add(copyList((List<?>) element));
      } else if (isBasicType(element)) {
        copy.add(element);
      } else {
        throw new RuntimeException("Unsupported type: " + element.getClass());
      }
    }
    return copy;
  }

  public static boolean isBasicType(Object value) {
    return value instanceof String || value instanceof Integer || value instanceof Double || value instanceof Boolean;
  }

  public static Number truncate(Number value, int precision) {
    if (value instanceof Integer) {
      return value;
    }

    String result = value.toString();
    String[] parts = result.split("\\.");
    String integerPart = parts[0];
    String commaParts = parts[1];

    String truncatedCommaParts = commaParts.length() > precision ? commaParts.substring(0, precision) : commaParts;

    if (truncatedCommaParts.endsWith("0")) {
      truncatedCommaParts = truncatedCommaParts.replaceAll("0+$", "");
    }

    if (truncatedCommaParts.isEmpty()) {
      return Double.valueOf(integerPart).intValue();
    }

    result = integerPart + "." + truncatedCommaParts;
    return Double.parseDouble(result);
  }

  public static String jsonString(Map<String, Object> map) {
    StringBuilder jsonBuilder = new StringBuilder("{");
    List<String> entries = new ArrayList<>();
    for (Map.Entry<String, Object> entry : map.entrySet()) {
      String key = "\"" + entry.getKey() + "\"";
      String value = encodeValue(entry.getValue());
      entries.add(key + ":" + value);
    }
    jsonBuilder.append(String.join(",", entries));
    jsonBuilder.append("}");
    return jsonBuilder.toString();
  }

  private static String encodeValue(Object value) {
    if (value instanceof String) {
      return "\"" + ((String) value).replace("\"", "\\\"") + "\"";
    } else if (value instanceof Number || value instanceof Boolean) {
      return value.toString();
    } else if (value == null) {
      return "null";
    } else if (value instanceof List) {
      return "[" + ((List<?>) value).stream().map(JsonHash::encodeValue).collect(Collectors.joining(",")) + "]";
    } else if (value instanceof Map) {
      return jsonString((Map<String, Object>) value);
    } else {
      throw new RuntimeException("Unsupported type: " + value.getClass());
    }
  }

  public static void main(String[] args) {
    // Test the JsonHash class here
  }
}
