# Simple Kotlin Comments Action Test

## Settings

```json
{
  "indent": ""
}
```

## From

```kotlin
// Sum all the prime numbers less than 1000
```

## To

```kotlin
fun main() {
    var sum = 0
    for (i in 2 until 1000) {
        if (isPrime(i)) {
            sum += i
        }
    }
    println("The sum of all prime numbers less than 1000 is: $sum")
}

fun isPrime(num: Int): Boolean {
    if (num <= 1) {
        return false
    }
    for (i in 2 until num) {
        if (num % i == 0) {
            return false
        }
    }
    return true
}
```

