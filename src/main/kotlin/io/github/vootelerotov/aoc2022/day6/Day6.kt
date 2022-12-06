package io.github.vootelerotov.aoc2022.day6

import io.github.vootelerotov.util.readResourceLines

fun main() {
  val (message) = readResourceLines("aoc2022/day6/input.txt")

  // 1st
  message.withIndex().windowed(4).first { it.map(::value).let(::isDistinct)}.last().let(::nth).let(::println)

  // 2nd
  message.withIndex().windowed(14).first { it.map(::value).let(::isDistinct)}.last().let(::nth).let(::println)

}

fun isDistinct(list: List<Char>) = list.distinct().size == list.size

fun <T> nth(indexedValue: IndexedValue<T>) = indexedValue.index + 1
fun <T> value(indexedValue: IndexedValue<T>) = indexedValue.value
