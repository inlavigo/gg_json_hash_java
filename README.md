# Gg JSON Hash JAVA

This repo provides a method that takes a JSON object, iterates all objects
and adds hashes to them. This is the algorithm:

- Iterate to the children of each object
- Objects are hashed using SHA-256 algorithm
- The hash is base64 encoded.
- The hash is truncated to 22 chars.

## Install Tools

### Java, JDK

- [JAVA](https://adoptium.net/de/)

Test:

```bash
java --version
```

Setup a `JAVA_HOME` variable in your environment and assign the JDK home dir,
e.g. `/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home`

### Maven

- [Maven](https://maven.apache.org/download.cgi)

Test:

```bash
java --version
```

### Vscode

Open Vscode and install the `Extension Pack for Java`.
It has the key `vscjava.vscode-java-pack`.

## Install dependencies

```bash
mvn clean install
```

## Execute unit tests

```bash
mvn test
```

## Example

```java
// Create an example json string
String exampleJsonString = "{\"key1\":\"value1\",\"key2\":[1,2,3],\"nested\":{\"nestedKey\":123.456789}}";

// Apply hashing
JsonHash hasher = new JsonHash(22, 10);
String hashedJsonString = hasher.applyToString(exampleJsonString);

// Print result
System.out.println(hashedJsonString);
```
