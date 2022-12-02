package io.github.vootelerotov.aoc2022.day1

import io.github.vootelerotov.util.readResourceLines
import io.github.vootelerotov.util.split

fun main(){
  val lines = readResourceLines("aoc2022/day1/input.txt")

  // 1st
  val calorieLists = split(lines, "")
  val calorieCounts = calorieLists.map { it.map(String::toInt).sum() }
  println(calorieCounts.maxOf { it })

  // 2nd
  calorieCounts.sorted().takeLast(3).sum().let { println(it) }

}