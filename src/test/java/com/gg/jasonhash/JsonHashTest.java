package com.gg.jasonhash;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.gg.jsonhash.JsonHash;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.*;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("unchecked")
public class JsonHashTest {
  private final JsonHash jh = new JsonHash();
  private final Function<String, String> calcHash = jh::calcHash;

  @Test
  void testWithSimpleJsonStringValue() {
    Map<String, Object> json = jh.applyTo(Map.of("key", "value"));
    assertEquals("value", json.get("key"));
    String expectedHash = calcHash.apply("{\"key\":\"value\"}");
    assertEquals(expectedHash, json.get("_hash"));
    assertEquals("5Dq88zdSRIOcAS-WM_lYYt", json.get("_hash"));
  }

  @Test
  void testWithSimpleJsonIntValue() {
    Map<String, Object> json = jh.applyTo(Map.of("key", 1));
    assertEquals(1, json.get("key"));
    String expectedHash = calcHash.apply("{\"key\":1}");
    assertEquals(expectedHash, json.get("_hash"));
    assertEquals("t4HVsGBJblqznOBwy6IeLt", json.get("_hash"));
  }

  @Test
  void testWithSimpleJsonDoubleValueWithoutCommas() {
    Map<String, Object> json = jh.applyTo(Map.of("key", 1.000));
    assertEquals(1.0, json.get("key"));
    String expectedHash = calcHash.apply("{\"key\":1}");
    assertEquals(expectedHash, json.get("_hash"));
    assertEquals("t4HVsGBJblqznOBwy6IeLt", json.get("_hash"));
  }

  @Test
  void testWithSimpleJsonBoolValue() {
    Map<String, Object> json = jh.applyTo(Map.of("key", true));
    assertEquals(true, json.get("key"));
    String expectedHash = calcHash.apply("{\"key\":true}");
    assertEquals(expectedHash, json.get("_hash"));
    assertEquals("dNkCrIe79x2dPyf5fywwYO", json.get("_hash"));
  }

  @Test
  void testWithSimpleJsonLongDoubleValue() {
    Map<String, Object> json = jh.applyTo(Map.of("key", 1.0123456789012345));
    String expectedHash = calcHash.apply("{\"key\":1.0123456789}");
    assertEquals(expectedHash, json.get("_hash"));
    assertEquals("Cj6IqsbT9fSKfeVVkytoqA", json.get("_hash"));
  }

  @Test
  void testWithSimpleJsonShortDoubleValue() {
    Map<String, Object> json = jh.applyTo(Map.of("key", 1.012000));
    String expectedHash = calcHash.apply("{\"key\":1.012}");
    assertEquals(expectedHash, json.get("_hash"));
    assertEquals("ppGtYoP5iHFqst5bPeAGMf", json.get("_hash"));
  }

  @Test
  void testExistingHashShouldBeOverwritten() {
    Map<String, Object> json = jh.applyTo(new HashMap<String, Object>() {
      {
        put("key", "value");
        put("_hash", "oldHash");
      }
    });
    assertEquals("value", json.get("key"));
    String expectedHash = calcHash.apply("{\"key\":\"value\"}");
    assertEquals(expectedHash, json.get("_hash"));
    assertEquals("5Dq88zdSRIOcAS-WM_lYYt", json.get("_hash"));
  }

  @Test
  void testTruncatesFloatingPointNumbers() {

    JsonHash jh = new JsonHash(22, 9);

    String hash0 = jh.applyTo(
        Map.of("key", 1.01234567890123456789)).get("_hash").toString();

    String hash1 = jh.applyTo(
        Map.of("key", 1.01234567890123456389)).get("_hash").toString();
    String expectedHash = calcHash.apply("{\"key\":1.012345678}");

    assertEquals(hash0, hash1);
    assertEquals(hash0, expectedHash);
    assertEquals(hash0, "KTqI1AvWb3gI6dYA5HPPMx");
  }

  @Test
  void testWithThreeKeyValuePairs() {
    Map<String, Object> json0 = Map.of(
        "a", "value",
        "b", 1.0,
        "c", true);

    Map<String, Object> json1 = Map.of(
        "b", 1.0,
        "a", "value",
        "c", true);

    Map<String, Object> j0 = jh.applyTo(json0);
    Map<String, Object> j1 = jh.applyTo(json1);

    String expectedHash = calcHash.apply(
        "{\"a\":\"value\",\"b\":1,\"c\":true}");

    assertEquals(expectedHash, j0.get("_hash"));
    assertEquals(expectedHash, j1.get("_hash"));
    assertEquals(j0, j1);
    assertEquals(j0.get("_hash"), j1.get("_hash"));
  }

  @Test
  void testNestedJsonLevel1() {
    Map<String, Object> parent = jh.applyTo(Map.of(
        "key", "value",
        "child", Map.of("key", "value")));

    Map<String, Object> child = (Map<String, Object>) parent.get("child");
    String childHash = calcHash.apply("{\"key\":\"value\"}");
    assertEquals(childHash, child.get("_hash"));

    String parentHash = calcHash.apply(
        "{\"child\":\"" + childHash + "\",\"key\":\"value\"}");
    assertEquals(parentHash, parent.get("_hash"));
  }

  @Test
  void testNestedJsonLevel2() {
    Map<String, Object> parent = jh.applyTo(Map.of(
        "key", "value",
        "child", Map.of(
            "key", "value",
            "grandChild", Map.of("key", "value"))));

    Map<String, Object> grandChild = (Map<String, Object>) ((Map<String, Object>) parent.get("child"))
        .get("grandChild");
    String grandChildHash = calcHash.apply("{\"key\":\"value\"}");
    assertEquals(grandChildHash, grandChild.get("_hash"));

    Map<String, Object> child = (Map<String, Object>) parent.get("child");
    String childHash = calcHash.apply(
        "{\"grandChild\":\"" + grandChildHash + "\",\"key\":\"value\"}");
    assertEquals(childHash, child.get("_hash"));

    String parentHash = calcHash.apply(
        "{\"child\":\"" + childHash + "\",\"key\":\"value\"}");
    assertEquals(parentHash, parent.get("_hash"));
  }

  @Test
  void testWithCompleteJsonExample() {
    Map<String, Object> json = new Gson().fromJson(exampleJson, Map.class);
    Map<String, Object> hashedJson = jh.applyTo(json);

    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    String hashedJsonString = gson.toJson(hashedJson);
    assertEquals(exampleJsonWithHashes, hashedJsonString);
  }

  @Test
  void testArrayOnTopLevelContainingOnlySimpleTypes() {
    Map<String, Object> json = jh.applyTo(Map.of(
        "key", Arrays.asList("value", 1.0, true)));

    String expectedHash = calcHash.apply(
        "{\"key\":[\"value\",1,true]}");

    assertEquals(expectedHash, json.get("_hash"));
    assertEquals("nbNb1YfpgqnPfyFTyCQ5YF", json.get("_hash"));
  }

  @Test
  void testArrayOnTopLevelContainingNestedObjects() {
    Map<String, Object> json = jh.applyTo(Map.of(
        "array", Arrays.asList("key", 1.0, true, Map.of("key1", "value1"), Map.of("key0", "value0"))));

    String h0 = calcHash.apply("{\"key0\":\"value0\"}");
    String h1 = calcHash.apply("{\"key1\":\"value1\"}");
    String expectedHash = calcHash.apply(
        "{\"array\":[\"key\",1,true,\"" + h1 + "\",\"" + h0 + "\"]}");

    assertEquals(expectedHash, json.get("_hash"));
    assertEquals("13h_Z0wZCF4SQsTyMyq5dV", json.get("_hash"));
  }

  @Test
  void testArrayOnTopLevelContainingSimpleArray() {
    Map<String, Object> json = jh.applyTo(Map.of(
        "array", Collections.singletonList(Map.of("key", "value"))));

    String itemHash = calcHash.apply("{\"key\":\"value\"}");
    List<Object> array = (List<Object>) json.get("array");
    Map<String, Object> item0 = (Map<String, Object>) array.get(0);
    assertEquals(itemHash, item0.get("_hash"));
    assertEquals("5Dq88zdSRIOcAS-WM_lYYt", itemHash);

    String expectedHash = calcHash.apply(
        "{\"array\":[\"" + itemHash + "\"]}");

    assertEquals(expectedHash, json.get("_hash"));
    assertEquals("zYcZBAUGLgR0ygMxi0V5ZT", json.get("_hash"));
  }

  @Test
  void testArrayOnTopLevelContainingNestedArrays() {
    Map<String, Object> json = jh.applyTo(Map.of(
        "array", Arrays.asList(Arrays.asList("key", 1.0, true), "hello")));

    String jsonHash = calcHash.apply(
        "{\"array\":[[\"key\",1,true],\"hello\"]}");

    assertEquals(jsonHash, json.get("_hash"));
    assertEquals("1X_6COC1sP5ECuHvKtVoDT", json.get("_hash"));
  }

  @Test
  void testThrowsWhenDataContainsUnsupportedType() {
    String message = "";
    try {
      jh.applyTo(Map.of("key", new Exception()));
    } catch (Exception e) {
      message = e.getMessage();
    }
    assertEquals("Unsupported type: class java.lang.Exception", message);
  }

  @Test
  void testCopyJsonEmptyJson() {
    assertEquals(JsonHash.copyJson(new HashMap<>()), new HashMap<>());
  }

  @Test
  void testCopyJsonSimpleValue() {
    assertEquals(JsonHash.copyJson(Map.of("a", 1)), Map.of("a", 1));
  }

  @Test
  void testCopyJsonNestedValue() {
    assertEquals(JsonHash.copyJson(Map.of("a", Map.of("b", 1))), Map.of("a", Map.of("b", 1)));
  }

  @Test
  void testCopyJsonListValue() {
    assertEquals(JsonHash.copyJson(Map.of("a", Arrays.asList(1, 2))), Map.of("a", Arrays.asList(1, 2)));
  }

  @Test
  void testCopyJsonListWithList() {
    assertEquals(JsonHash.copyJson(Map.of("a", Collections.singletonList(Arrays.asList(1, 2)))),
        Map.of("a", Collections.singletonList(Arrays.asList(1, 2))));
  }

  @Test
  void testCopyJsonListWithMap() {
    assertEquals(JsonHash.copyJson(Map.of("a", Collections.singletonList(Map.of("b", 1)))),
        Map.of("a", Collections.singletonList(Map.of("b", 1))));
  }

  @Test
  void testCopyJsonThrowsOnUnsupportedTypeInMap() {
    String message = "";
    try {
      JsonHash.copyJson(Map.of("a", new Exception()));
    } catch (Exception e) {
      message = e.getMessage();
    }
    assertEquals("Unsupported type: class java.lang.Exception", message);
  }

  @Test
  void testCopyJsonThrowsOnUnsupportedTypeInList() {
    String message = "";
    try {
      JsonHash.copyJson(Map.of("a", Collections.singletonList(new Exception())));
    } catch (Exception e) {
      message = e.getMessage();
    }
    assertEquals("Unsupported type: class java.lang.Exception", message);
  }

  @Test
  void testIsBasicType() {
    assertTrue(JsonHash.isBasicType(1));
    assertTrue(JsonHash.isBasicType(1.0));
    assertTrue(JsonHash.isBasicType("1"));
    assertTrue(JsonHash.isBasicType(true));
    assertTrue(JsonHash.isBasicType(false));
    assertFalse(JsonHash.isBasicType(new HashMap<>()));
  }

  @Test
  void testTruncate() {
    assertEquals(1, JsonHash.truncate(1.0, 5));
    assertEquals(1, JsonHash.truncate(1, 5));
    assertEquals(1.23, JsonHash.truncate(1.23456789, 2));
    assertEquals(1.234, JsonHash.truncate(1.23456789, 3));
    assertEquals(1.2345, JsonHash.truncate(1.23456789, 4));
    assertEquals(1.23456, JsonHash.truncate(1.23456789, 5));
    assertEquals(1.234567, JsonHash.truncate(1.23456789, 6));
    assertEquals(1.2345678, JsonHash.truncate(1.23456789, 7));
    assertEquals(1.23456789, JsonHash.truncate(1.23456789, 8));
    assertEquals(1.1, JsonHash.truncate(1.12, 1));
    assertEquals(1.12, JsonHash.truncate(1.12, 2));
    assertEquals(1.12, JsonHash.truncate(1.12, 3));
    assertEquals(1.12, JsonHash.truncate(1.12, 4));
    assertEquals(1, JsonHash.truncate(1.0, 0));
    assertEquals(1, JsonHash.truncate(1.0, 1));
    assertEquals(1, JsonHash.truncate(1.0, 2));
    assertEquals(1, JsonHash.truncate(1.0, 3));
  }

  @Test
  void testJsonString() {
    assertEquals("{\"a\":1}", JsonHash.jsonString(Map.of("a", 1)));
    assertEquals("{\"a\":\"b\"}", JsonHash.jsonString(Map.of("a", "b")));
    assertEquals("{\"a\":true}", JsonHash.jsonString(Map.of("a", true)));
    assertEquals("{\"a\":false}", JsonHash.jsonString(Map.of("a", false)));
    assertEquals("{\"a\":1.0}", JsonHash.jsonString(Map.of("a", 1.0)));
    assertEquals("{\"a\":[1,2]}", JsonHash.jsonString(Map.of("a", Arrays.asList(1, 2))));
    assertEquals("{\"a\":{\"b\":1}}", JsonHash.jsonString(Map.of("a", Map.of("b", 1))));
  }

  @Test
  void testJsonStringThrowsWhenUnsupportedType() {
    String message = "";
    try {
      JsonHash.jsonString(Map.of("a", new Exception()));
    } catch (Exception e) {
      message = e.getMessage();
    }
    assertEquals("Unsupported type: class java.lang.Exception", message);
  }

  @Test
  void testConvertBasicType() {
    assertEquals("a", JsonHash.convertBasicType("a", 5));
    assertEquals(1, JsonHash.convertBasicType(1, 5));
    assertEquals(1, JsonHash.convertBasicType(1.0, 5));
    assertEquals(1, JsonHash.convertBasicType(1.00000000001, 5));
  }

  @Test
  void testConvertBasicTypeThrowsWhenUnsupportedType() {
    String message = "";
    try {
      JsonHash.convertBasicType(new Exception(), 5);
    } catch (Exception e) {
      message = e.getMessage();
    }
    assertEquals("Unsupported type: class java.lang.Exception", message);
  }

  @Test
  void testApplyToString() {
    String json = "{\"key\": \"value\"}";
    String jsonString = new JsonHash().applyToString(json);
    assertEquals("{\"key\":\"value\",\"_hash\":\"5Dq88zdSRIOcAS-WM_lYYt\"}", jsonString);
  }

  private Map<String, Object> json;

  @BeforeEach
  public void setUp() {
    json = new HashMap<>();
    Map<String, Object> a = new HashMap<>();
    Map<String, Object> b = new HashMap<>();
    Map<String, Object> c = new HashMap<>();

    c.put("_hash", "hash_c");
    c.put("d", "value");

    b.put("_hash", "hash_b");
    b.put("c", c);

    a.put("_hash", "hash_a");
    a.put("b", b);

    json.put("a", a);
  }

  private boolean allHashesChanged(Map<String, Object> json) {
    Map<String, Object> a = (Map<String, Object>) json.get("a");
    Map<String, Object> b = (Map<String, Object>) a.get("b");
    Map<String, Object> c = (Map<String, Object>) b.get("c");

    return !a.get("_hash").equals("hash_a") &&
        !b.get("_hash").equals("hash_b") &&
        !c.get("_hash").equals("hash_c");
  }

  private boolean noHashesChanged() {
    Map<String, Object> a = (Map<String, Object>) json.get("a");
    Map<String, Object> b = (Map<String, Object>) a.get("b");
    Map<String, Object> c = (Map<String, Object>) b.get("c");

    return a.get("_hash").equals("hash_a") &&
        b.get("_hash").equals("hash_b") &&
        c.get("_hash").equals("hash_c");
  }

  private List<String> changedHashes(Map<String, Object> json) {
    List<String> result = new ArrayList<>();
    Map<String, Object> a = (Map<String, Object>) json.get("a");
    Map<String, Object> b = (Map<String, Object>) a.get("b");
    Map<String, Object> c = (Map<String, Object>) b.get("c");

    if (!a.get("_hash").equals("hash_a")) {
      result.add("a");
    }

    if (!b.get("_hash").equals("hash_b")) {
      result.add("b");
    }

    if (!c.get("_hash").equals("hash_c")) {
      result.add("c");
    }

    return result;
  }

  @Test
  void testUpdateExistingHashesTrue() {
    Map<String, Object> json = new HashMap<String, Object>() {
      {
        put("a", new HashMap<String, Object>() {
          {
            put("_hash", "hash_a");
            put("b", new HashMap<String, Object>() {
              {
                put("_hash", "hash_b");
                put("c", new HashMap<String, Object>() {
                  {
                    put("_hash", "hash_c");
                    put("d", "value");
                  }
                });
              }
            });
          }
        });
      }
    };

    JsonHash jh = new JsonHash(22, 10, true, true);

    json = jh.applyTo(json);
    assertTrue(allHashesChanged(json));
  }

  @Test
  void testUpdateExistingHashesFalseWithAllObjectsHavingHashes() {
    Map<String, Object> json = new HashMap<String, Object>() {
      {
        put("a", new HashMap<String, Object>() {
          {
            put("_hash", "hash_a");
            put("b", new HashMap<String, Object>() {
              {
                put("_hash", "hash_b");
                put("c", new HashMap<String, Object>() {
                  {
                    put("_hash", "hash_c");
                    put("d", "value");
                  }
                });
              }
            });
          }
        });
      }
    };
    JsonHash jh = new JsonHash(22, 10, false, true);

    jh.applyTo(json);
    assertTrue(noHashesChanged());
  }

  @Test
  void testUpdateExistingHashesFalseWithParentsHavingNoHashes() {
    Map<String, Object> json = new HashMap<String, Object>() {
      {
        put("a", new HashMap<String, Object>() {
          {
            put("b", new HashMap<String, Object>() {
              {
                put("_hash", "hash_b");
                put("c", new HashMap<String, Object>() {
                  {
                    put("_hash", "hash_c");
                    put("d", "value");
                  }
                });
              }
            });
          }
        });
      }
    };

    JsonHash jh = new JsonHash(22, 10, false, true);

    json = jh.applyTo(json);
    assertEquals(Arrays.asList("a"), changedHashes(json));

    // json.get("a").remove("_hash");
    // json.get("a").get("b").remove("_hash");
    // jh.applyTo(json, false, true);
    // assertEquals(Arrays.asList("a", "b"), changedHashes());
  }

  @Test
  void testInPlaceFalse() {
    Map<String, Object> json = new HashMap<String, Object>() {
      {
        put("key", "value");
      }
    };

    Map<String, Object> hashedJson = jh.applyTo(json, false);
    assertEquals(Map.of("key", "value", "_hash", "5Dq88zdSRIOcAS-WM_lYYt"), hashedJson);
    assertEquals(Map.of("key", "value"), json);
  }

  @Test
  void testInPlaceTrue() {
    Map<String, Object> json = new HashMap<String, Object>() {
      {
        put("key", "value");
      }
    };

    Map<String, Object> hashedJson = jh.applyTo(json, true);
    assertEquals(Map.of("key", "value", "_hash", "5Dq88zdSRIOcAS-WM_lYYt"), hashedJson);
    assertSame(hashedJson, json);
  }

  private static final String exampleJson = "{\n" +
      "  \"layerA\": {\n" +
      "    \"data\": [\n" +
      "      {\n" +
      "        \"w\": 600.0,\n" +
      "        \"w1\": 100.0\n" +
      "      },\n" +
      "      {\n" +
      "        \"w\": 700.0,\n" +
      "        \"w1\": 100.0\n" +
      "      }\n" +
      "    ]\n" +
      "  },\n" +
      "\n" +
      "  \"layerB\": {\n" +
      "    \"data\": [\n" +
      "      {\n" +
      "        \"d\": 268.0,\n" +
      "        \"d1\": 100.0\n" +
      "      }\n" +
      "    ]\n" +
      "  },\n" +
      "\n" +
      "  \"layerC\": {\n" +
      "    \"data\": [\n" +
      "      {\n" +
      "        \"h\": 800.0\n" +
      "      }\n" +
      "    ]\n" +
      "  },\n" +
      "\n" +
      "  \"layerD\": {\n" +
      "    \"data\": [\n" +
      "      {\n" +
      "        \"wMin\": 0.0,\n" +
      "        \"wMax\": 900.0,\n" +
      "        \"w1Min\": 0.0,\n" +
      "        \"w1Max\": 900.0\n" +
      "      }\n" +
      "    ]\n" +
      "  },\n" +
      "\n" +
      "  \"layerE\": {\n" +
      "    \"data\": [\n" +
      "      {\n" +
      "        \"type\": \"XYZABC\",\n" +
      "        \"widths\": \"sLZpHAffgchgJnA++HqKtO\",\n" +
      "        \"depths\": \"k1IL2ctZHw4NpaA34w0d0I\",\n" +
      "        \"heights\": \"GBLHz0ayRkVUlms1wHDaJq\",\n" +
      "        \"ranges\": \"9rohAG49drWZs9tew4rDef\"\n" +
      "      }\n" +
      "    ]\n" +
      "  },\n" +
      "\n" +
      "  \"layerF\": {\n" +
      "    \"data\": [\n" +
      "      {\n" +
      "        \"type\": \"XYZABC\",\n" +
      "        \"name\": \"Unterschrank 60cm\"\n" +
      "      }\n" +
      "    ]\n" +
      "  },\n" +
      "\n" +
      "  \"layerG\": {\n" +
      "    \"data\": [\n" +
      "      {\n" +
      "        \"type\": \"XYZABC\",\n" +
      "        \"name\": \"Base Cabinet 23.5\"\n" +
      "      }\n" +
      "    ]\n" +
      "  }\n" +
      "}";

  private static final String exampleJsonWithHashes = "{\n" +
      "  \"layerA\": {\n" +
      "    \"data\": [\n" +
      "      {\n" +
      "        \"w\": 600.0,\n" +
      "        \"w1\": 100.0,\n" +
      "        \"_hash\": \"ajRQhCx6QLPI8227B72r8I\"\n" +
      "      },\n" +
      "      {\n" +
      "        \"w\": 700.0,\n" +
      "        \"w1\": 100.0,\n" +
      "        \"_hash\": \"Jf177UAntzI4rIjKiU_MVt\"\n" +
      "      }\n" +
      "    ],\n" +
      "    \"_hash\": \"qCgcNNF3wJPfx0rkRDfoSY\"\n" +
      "  },\n" +
      "  \"layerB\": {\n" +
      "    \"data\": [\n" +
      "      {\n" +
      "        \"d\": 268.0,\n" +
      "        \"d1\": 100.0,\n" +
      "        \"_hash\": \"9mJ7aZJexhfz8IfwF6bsuW\"\n" +
      "      }\n" +
      "    ],\n" +
      "    \"_hash\": \"tb0ffNF2ePpqsRxmvMDRrt\"\n" +
      "  },\n" +
      "  \"layerC\": {\n" +
      "    \"data\": [\n" +
      "      {\n" +
      "        \"h\": 800.0,\n" +
      "        \"_hash\": \"KvMHhk1dYYQ2o5Srt6pTUN\"\n" +
      "      }\n" +
      "    ],\n" +
      "    \"_hash\": \"Z4km_FzQoxyck-YHQDZMtV\"\n" +
      "  },\n" +
      "  \"layerD\": {\n" +
      "    \"data\": [\n" +
      "      {\n" +
      "        \"wMin\": 0.0,\n" +
      "        \"wMax\": 900.0,\n" +
      "        \"w1Min\": 0.0,\n" +
      "        \"w1Max\": 900.0,\n" +
      "        \"_hash\": \"6uw0BSIllrk6DuKyvQh-Rg\"\n" +
      "      }\n" +
      "    ],\n" +
      "    \"_hash\": \"qFDAzWUsTnqICnpc_rJtax\"\n" +
      "  },\n" +
      "  \"layerE\": {\n" +
      "    \"data\": [\n" +
      "      {\n" +
      "        \"type\": \"XYZABC\",\n" +
      "        \"widths\": \"sLZpHAffgchgJnA++HqKtO\",\n" +
      "        \"depths\": \"k1IL2ctZHw4NpaA34w0d0I\",\n" +
      "        \"heights\": \"GBLHz0ayRkVUlms1wHDaJq\",\n" +
      "        \"ranges\": \"9rohAG49drWZs9tew4rDef\",\n" +
      "        \"_hash\": \"65LigWuYVGgifKnEZaOJET\"\n" +
      "      }\n" +
      "    ],\n" +
      "    \"_hash\": \"pDRglh2oWJcghTzzrzTLw6\"\n" +
      "  },\n" +
      "  \"layerF\": {\n" +
      "    \"data\": [\n" +
      "      {\n" +
      "        \"type\": \"XYZABC\",\n" +
      "        \"name\": \"Unterschrank 60cm\",\n" +
      "        \"_hash\": \"gjzETUIUf563ZJNHVEY9Wt\"\n" +
      "      }\n" +
      "    ],\n" +
      "    \"_hash\": \"r1u6gR8WLzPAZ3lEsAqREP\"\n" +
      "  },\n" +
      "  \"layerG\": {\n" +
      "    \"data\": [\n" +
      "      {\n" +
      "        \"type\": \"XYZABC\",\n" +
      "        \"name\": \"Base Cabinet 23.5\",\n" +
      "        \"_hash\": \"DEyuShUHDpWSJ7Rq_a3uz6\"\n" +
      "      }\n" +
      "    ],\n" +
      "    \"_hash\": \"3meyGs7XhOh8gWFNQFYZDI\"\n" +
      "  },\n" +
      "  \"_hash\": \"OmmdaqCAhcIKnDm7lT-_gI\"\n" +
      "}";
}
