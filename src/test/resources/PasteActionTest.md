# Simple Kotlin Comments Action Test

## Settings

```json
{
  "indent": ""
}
```

## From

```kotlin
Print all the prime numbers between 1 and 1000.
```

## To

```kotlin
fun isPrime(num: Int): Boolean {
    if (num <= 1) return false
    for (i in 2 until num) {
        if (num % i == 0) return false
    }
    return true
}

fun main() {
    for (i in 1..1000) {
        if (isPrime(i)) println(i)
    }
}
```

