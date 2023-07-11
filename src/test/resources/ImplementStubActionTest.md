# Simple Kotlin Comments Action Test

## Settings

```json
{
  "indent": ""
}
```

## From

```kotlin
fun dotProduct(a: Array<Double>, b: Array<Double>): Double {
  TODO("Not yet implemented")
}
```

## To

```kotlin
fun dotProduct(a: Array<Double>, b: Array<Double>): Double {
  return a.zip(b).map { (x, y) -> x * y }.sum()
}
```

