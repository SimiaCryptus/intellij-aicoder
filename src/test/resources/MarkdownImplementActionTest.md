# Simple Kotlin Comments Action Test

## Settings

```json
{
  "indent": ""
}
```

## From

```markdown
Sum all the prime numbers up to 1000
```

## To

```kotlin
fun isPrime(n: Int): Boolean {
    if (n <= 1) return false
    for (i in 2 until n) {
        if (n % i == 0) return false
    }
    return true
}

fun sumPrimeNumbers(limit: Int): Int {
    var sum = 0
    for (i in 2..limit) {
        if (isPrime(i)) {
            sum += i
        }
    }
    return sum
}

val limit = 1000
val result = sumPrimeNumbers(limit)
println("The sum of prime numbers up to $limit is: $result")
```

