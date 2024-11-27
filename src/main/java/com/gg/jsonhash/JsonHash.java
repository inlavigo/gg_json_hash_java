
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

  // .........................................................................
  public JsonHash(int hashLength, int floatingPointPrecision) {
    this.hashLength = hashLength;
    this.floatingPointPrecision = floatingPointPrecision;
  }

  // .........................................................................
  public Map<String, Object> applyTo(Map<String, Object> json) {
    Map<String, Object> copy = copyJson(json);
    addHashesToObject(copy);
    return copy;
  }

  // .........................................................................
  public String applyToString(String jsonString) {
    // Convert jsonString to Map
    Map<String, Object> json = new Gson().fromJson(jsonString, Map.class);

    // Apply hashing
    final Map<String, Object> hashedJson = applyTo(json);

    // Convert back to jsonString
    final String result = new Gson().toJson(hashedJson);
    return result;
  }

  // .........................................................................
  public String calcHash(String input) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
      return Base64.encodeBase64URLSafeString(hashBytes).substring(0, hashLength);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-256 algorithm not available", e);
    }
  }

  // #########
  // Private
  // #########

  // .........................................................................
  private final int hashLength;
  private final int floatingPointPrecision;

  // .........................................................................
  private void addHashesToObject(Map<String, Object> obj) {
    obj.forEach((key, value) -> {
      if (value instanceof Map) {
        addHashesToObject((Map<String, Object>) value);
      } else if (value instanceof List) {
        processList((List<Object>) value);
      }
    });

    Map<String, Object> objToHash = new HashMap<>();
    for (Map.Entry<String, Object> entry : obj.entrySet()) {
      String key = entry.getKey();
      if (key.equals("_hash"))
        continue;

      Object value = entry.getValue();
      if (value instanceof Map) {
        objToHash.put(key, ((Map<?, ?>) value).get("_hash"));
      } else if (value instanceof List) {
        objToHash.put(key, flattenList((List<Object>) value));
      } else if (value instanceof Double) {
        objToHash.put(key, truncate((Double) value, floatingPointPrecision));
      } else if (isBasicType(value)) {
        objToHash.put(key, value);
      } else {
        throw new IllegalArgumentException("Unsupported type: " + value.getClass());
      }
    }

    Map<String, Object> sortedMap = new TreeMap<>(objToHash);
    String sortedMapJson = jsonString(sortedMap);

    String hash = calcHash(sortedMapJson);
    obj.put("_hash", hash);
  }

  // .........................................................................
  private List<Object> flattenList(List<Object> list) {
    List<Object> flattenedList = new ArrayList<>();
    for (Object element : list) {
      if (element instanceof Map) {
        flattenedList.add(((Map<?, ?>) element).get("_hash"));
      } else if (element instanceof List) {
        flattenedList.add(flattenList((List<Object>) element));
      } else if (isBasicType(element)) {
        flattenedList.add(element.toString());
      }
    }
    return flattenedList;
  }

  // .........................................................................
  private void processList(List<Object> list) {
    for (Object element : list) {
      if (element instanceof Map) {
        addHashesToObject((Map<String, Object>) element);
      } else if (element instanceof List) {
        processList((List<Object>) element);
      }
    }
  }

  // .........................................................................
  private Map<String, Object> copyJson(Map<String, Object> json) {
    Map<String, Object> copy = new HashMap<>();
    for (Map.Entry<String, Object> entry : json.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();

      if (value instanceof Map) {
        copy.put(key, copyJson((Map<String, Object>) value));
      } else if (value instanceof List) {
        copy.put(key, copyList((List<Object>) value));
      } else if (isBasicType(value)) {
        copy.put(key, value);
      } else {
        throw new IllegalArgumentException("Unsupported type: " + value.getClass());
      }
    }
    return copy;
  }

  // .........................................................................
  private List<Object> copyList(List<Object> list) {
    List<Object> copy = new ArrayList<>();
    for (Object element : list) {
      if (element instanceof Map) {
        copy.add(copyJson((Map<String, Object>) element));
      } else if (element instanceof List) {
        copy.add(copyList((List<Object>) element));
      } else if (isBasicType(element)) {
        copy.add(element);
      } else {
        throw new IllegalArgumentException("Unsupported type: " + element.getClass());
      }
    }
    return copy;
  }

  // .........................................................................
  private String jsonString(Map<String, Object> map) {
    return "{" + map.entrySet().stream()
        .map(entry -> "\"" + entry.getKey() + "\":" + encodeValue(entry.getValue()))
        .collect(Collectors.joining(",")) + "}";
  }

  // .........................................................................
  private String encodeValue(Object value) {
    if (value instanceof String) {
      return "\"" + ((String) value).replace("\"", "\\\"") + "\""; // Escape double quotes
    } else if (value instanceof Number || value instanceof Boolean) {
      return value.toString();
    } else if (value == null) {
      return "null";
    } else if (value instanceof List) {
      List<?> list = (List<?>) value;
      return "[" + list.stream()
          .map(this::encodeValue)
          .collect(Collectors.joining(",")) + "]";
    } else if (value instanceof Map) {
      Map<String, Object> mapValue = (Map<String, Object>) value;
      return jsonString(mapValue);
    } else {
      throw new IllegalArgumentException("Unsupported type: " + value.getClass().getName());
    }
  }

  // .........................................................................
  private boolean isBasicType(Object value) {
    return value instanceof String || value instanceof Integer || value instanceof Double || value instanceof Boolean;
  }

  // .........................................................................
  private double truncate(double value, int precision) {
    String result = String.format(Locale.US, "%." + precision + "f", value);
    return Double.parseDouble(result);
  }

  // .........................................................................
  public static void main(String[] args) {

    // Create an example json string
    String exampleJsonString = "{\"key1\":\"value1\",\"key2\":[1,2,3],\"nested\":{\"nestedKey\":123.456789}}";

    // Apply hashing
    JsonHash hasher = new JsonHash(22, 10);
    String hashedJsonString = hasher.applyToString(exampleJsonString);

    // Print result
    System.out.println(hashedJsonString);
  }
}
