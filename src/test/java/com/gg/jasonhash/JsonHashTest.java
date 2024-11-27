package com.gg.jasonhash;

import org.junit.jupiter.api.Test;

import com.gg.jsonhash.JsonHash;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("unchecked")
public class JsonHashTest {

  private final JsonHash hashJson = new JsonHash(22, 10);

  private String calcHash(String input) {
    return hashJson.calcHash(input);
  }

  @Test
  public void testHashSimpleJsonWithStringValue() {
    Map<String, Object> json = Map.of("key", "value");
    Map<String, Object> hashedJson = hashJson.applyTo(json);

    assertEquals("value", hashedJson.get("key"));
    String expectedHash = calcHash("{\"key\":\"value\"}");
    assertEquals(expectedHash, hashedJson.get("_hash"));
    assertEquals("5Dq88zdSRIOcAS-WM_lYYt", hashedJson.get("_hash"));
  }

  @Test
  public void testHashSimpleJsonWithIntValue() {
    Map<String, Object> json = Map.of("key", 1);
    Map<String, Object> hashedJson = hashJson.applyTo(json);

    assertEquals(1, hashedJson.get("key"));
    String expectedHash = calcHash("{\"key\":1}");
    assertEquals(expectedHash, hashedJson.get("_hash"));
    assertEquals("t4HVsGBJblqznOBwy6IeLt", hashedJson.get("_hash"));
  }

  @Test
  public void testHashSimpleJsonWithBoolValue() {
    Map<String, Object> json = Map.of("key", true);
    Map<String, Object> hashedJson = hashJson.applyTo(json);

    assertEquals(true, hashedJson.get("key"));
    String expectedHash = calcHash("{\"key\":true}");
    assertEquals(expectedHash, hashedJson.get("_hash"));
    assertEquals("dNkCrIe79x2dPyf5fywwYO", hashedJson.get("_hash"));
  }

  @Test
  public void testHashSimpleJsonWithDoubleValue() {
    Map<String, Object> json = Map.of("key", 1.0123456789012345);
    Map<String, Object> hashedJson = hashJson.applyTo(json);

    String expectedHash = calcHash("{\"key\":1.0123456789}");
    assertEquals(expectedHash, hashedJson.get("_hash"));
    assertEquals("Cj6IqsbT9fSKfeVVkytoqA", hashedJson.get("_hash"));
  }

  @Test
  public void testHashSimpleJsonWithOverwritingHash() {
    Map<String, Object> json = new HashMap<>();
    json.put("key", "value");
    json.put("_hash", "oldHash");

    Map<String, Object> hashedJson = hashJson.applyTo(json);

    assertEquals("value", hashedJson.get("key"));
    String expectedHash = calcHash("{\"key\":\"value\"}");
    assertEquals(expectedHash, hashedJson.get("_hash"));
    assertEquals("5Dq88zdSRIOcAS-WM_lYYt", hashedJson.get("_hash"));
  }

  @Test
  public void testHashJsonWithMultipleKeys() {
    Map<String, Object> json0 = Map.of(
        "a", "value",
        "b", 1.0,
        "c", true);

    Map<String, Object> json1 = Map.of(
        "b", 1.0,
        "a", "value",
        "c", true);

    Map<String, Object> hashedJson0 = hashJson.applyTo(json0);
    Map<String, Object> hashedJson1 = hashJson.applyTo(json1);

    String expectedHash = calcHash("{\"a\":\"value\",\"b\":1.0,\"c\":true}");

    assertEquals(expectedHash, hashedJson0.get("_hash"));
    assertEquals(expectedHash, hashedJson1.get("_hash"));
    assertEquals(hashedJson0, hashedJson1);
  }

  // Write test for applyToString
  @Test
  public void testApplyToString() {
    String jsonString = "{\"key\":\"value\"}";
    String hashedJsonString = hashJson.applyToString(jsonString);
    assertEquals(hashedJsonString, "{\"_hash\":\"5Dq88zdSRIOcAS-WM_lYYt\",\"key\":\"value\"}");
  }

  @Test
  public void testHashNestedJsonLevel1() {
    Map<String, Object> parent = Map.of(
        "key", "value",
        "child", Map.of("key", "value"));

    Map<String, Object> hashedParent = hashJson.applyTo(parent);
    Map<String, Object> hashedChild = (Map<String, Object>) hashedParent.get("child");

    String childHash = calcHash("{\"key\":\"value\"}");
    assertEquals(childHash, hashedChild.get("_hash"));

    String parentHash = calcHash("{\"child\":\"" + childHash + "\",\"key\":\"value\"}");
    assertEquals(parentHash, hashedParent.get("_hash"));
  }

  @Test
  public void testHashNestedJsonLevel2() {
    Map<String, Object> parent = Map.of(
        "key", "value",
        "child", Map.of(
            "key", "value",
            "grandChild", Map.of("key", "value")));

    Map<String, Object> hashedParent = hashJson.applyTo(parent);
    Map<String, Object> hashedChild = (Map<String, Object>) hashedParent.get("child");
    Map<String, Object> hashedGrandChild = (Map<String, Object>) hashedChild.get("grandChild");

    String grandChildHash = calcHash("{\"key\":\"value\"}");
    assertEquals(grandChildHash, hashedGrandChild.get("_hash"));

    String childHash = calcHash("{\"grandChild\":\"" + grandChildHash + "\",\"key\":\"value\"}");
    assertEquals(childHash, hashedChild.get("_hash"));

    String parentHash = calcHash("{\"child\":\"" + childHash + "\",\"key\":\"value\"}");
    assertEquals(parentHash, hashedParent.get("_hash"));
  }

  @Test
  public void testHashJsonWithArray() {
    Map<String, Object> json = Map.of(
        "key", List.of("value", 1.0, true));

    Map<String, Object> hashedJson = hashJson.applyTo(json);
    String expectedHash = calcHash("{\"key\":[\"value\",\"1.0\",\"true\"]}");
    assertEquals(expectedHash, hashedJson.get("_hash"));
    assertEquals("1DJgJ9oBYJWG04HMShLE9o", hashedJson.get("_hash"));
  }

  @Test
  public void testHashJsonWithNestedArrays() {
    Map<String, Object> json = Map.of(
        "array", List.of(
            List.of("key", 1.0, true),
            "hello"));

    Map<String, Object> hashedJson = hashJson.applyTo(json);
    String expectedHash = calcHash("{\"array\":[[\"key\",\"1.0\",\"true\"],\"hello\"]}");
    assertEquals(expectedHash, hashedJson.get("_hash"));
    assertEquals("TPZRhkc7IDTK8EftrWmMSw", hashedJson.get("_hash"));
  }

  @Test
  public void testHashJsonThrowsOnUnsupportedType() {
    Map<String, Object> json = new HashMap<>();
    json.put("key", new Exception("Unsupported"));

    Exception exception = assertThrows(RuntimeException.class, () -> hashJson.applyTo(json));
    assertTrue(exception.getMessage().contains("Unsupported type"));
  }
}
