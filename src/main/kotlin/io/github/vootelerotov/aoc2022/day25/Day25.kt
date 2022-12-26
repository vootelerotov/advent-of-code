package io.github.vootelerotov.aoc2022.day25

import io.github.vootelerotov.util.readResourceLines

fun main() {
  val lines = readResourceLines("aoc2022/day25/input-small.txt")

  val snafuDigits = lines.map { it.toCharArray().map(::fromSnafuDigit) }
  val sumOfSnafuNumbers = snafuDigits.map(::toSnafuNumber).sum().also(::println)

  toSnafuDigitsRecursive(sumOfSnafuNumbers).map(::toSnafuDigit).joinToString(separator = "").let(::println)
}

fun fromSnafuDigit(char: Char): Long = when(char) {
  '=' -> -2
  '-' -> -1
  '0' -> 0
  '1' -> 1
  '2' -> 2
  else -> throw IllegalArgumentException("Unknown character: $char")
}

fun toSnafuDigit(digit: Long) = when(digit) {
  -2L -> '='
  -1L -> '-'
  0L -> '0'
  1L -> '1'
  2L -> '2'
  else -> throw IllegalArgumentException("Unknown digit: $digit")
}

fun toSnafuNumber(digits: List<Long>) =
  digits.reversed().asSequence().zip(generateSequence(1L) { it * 5L }).map { (digit, power) -> digit * power }.sum()

fun toSnafuDigitsRecursive(number: Long): List<Long> = if (number == 0L)
  emptyList()
else
  normalizeReminder(divideWithReminder(number, 5L)).let { (div, rem) -> toSnafuDigitsRecursive(div) + rem }

fun divideWithReminder(number: Long, divider: Long) = number / divider to  number % divider
fun normalizeReminder(pair: Pair<Long, Long>) = if (pair.second > 2) pair.first + 1 to pair.second - 5 else pair