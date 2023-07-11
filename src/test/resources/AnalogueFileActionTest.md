# Simple Kotlin Comments Action Test

## From main.kt

```kotlin
fun main() {
  println("Hello, World!")
}
```

## Config

```json
{
  "directive": "translate to java"
}
```

## To new_file.java

```java
public class NewFile {
    public static void main(String[] args) {
        System.out.println("Hello, World!");
    }
}
```

