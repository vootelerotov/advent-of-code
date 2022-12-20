package io.github.vootelerotov.aoc2022.day14

import io.github.vootelerotov.aoc2022.day14.Feature.*
import io.github.vootelerotov.util.readResourceLines
import kotlin.math.max
import kotlin.math.min

fun main() {
  val lines = readResourceLines("aoc2022/day14/input-small.txt")

  val rocks = lines.flatMap(::parseRocks).distinct()
  val cave = createCave(rocks)

  // 1st
  val simpleCave = generateSequence(cave, ::sandFall).zipWithNext().first { (one, other) -> one == other }.first
  simpleCave.slice.sumOf { row -> row.count { it == SAND } }.let(::println)
  println(toString(simpleCave))

  //2nd
  val stableCave = generateSequence(
    cave.copy(
      height = cave.height + 2,
      slice = cave.slice + Row() + Row(default = ROCK)
    ),
    ::sandFall
  ).zipWithNext().first { (one, other) -> one == other }.first
  println(toString(stableCave))
  stableCave.slice.sumOf { row -> row.count { it == SAND } }.let(::println)
}

fun sandFall(cave: Cave): Cave = (500 to 0).let { initPos ->
  generateSequence(Triple(initPos, replaceFeature(cave, initPos, SAND), SOURCE)) { (position, cave, prev) ->
    val (x, y) = position
    val newPosition =
      listOf(x to y + 1, x - 1 to y + 1, x + 1 to y + 1).firstOrNull { (x, y) -> y > cave.height || cave.slice[y][x] == EMPTY }
    newPosition?.let { (_, y) ->
      Triple(newPosition, replaceFeature(cave, position, prev).let { if (y < cave.height) replaceFeature(it, newPosition, SAND) else it }, EMPTY)
    }
  }.takeWhile { (pos, cave) -> pos.let { (_, y) -> y <= cave.height } }.last().let { (_, cave) -> cave }
}

fun createCave(rocks: List<Pos>): Cave {
  val height = rocks.maxOf { (_, y) -> y }

  val elemsGroupedByY = rocks.associateWith { ROCK }.entries.groupBy(
    { (key, _) -> key.let { (_, y) -> y } },
    { (key, value) -> key.let { (x, _) -> x to value } }
  )
  val rocksInTheSlice = (0..height).map { elemsGroupedByY.getOrDefault(it, emptyList()).toMap().let(::Row) }
  return Cave(height, rocksInTheSlice)
}

fun parseRocks(line: String): List<Rock> = parsePositions(line).zipWithNext().flatMap(::parseRockLine).distinct()
fun parseRockLine(line: Pair<Rock, Rock>): List<Rock> {
  val (start, end) = line
  val (x1, y1) = start
  val (x2, y2) = end

  return when {
    x1 == x2 -> range(y1, y2).map { x1 to it }
    y1 == y2 -> range(x1, x2).map { it to y1 }
    else -> throw IllegalArgumentException("Invalid line: $line")
  }
}

fun range(x: Int, y: Int) = if (x < y) x..y else y..x

private fun parsePositions(line: String) = line.split("->").map { it.trim() }.map(::parsePosition)
private fun parsePosition(rawPos: String) = rawPos.split(",").let { (x, y) -> x.toInt() to y.toInt() }

typealias Pos = Pair<Int, Int>
typealias Rock = Pair<Int, Int>

fun toString(cave: Cave): String =
  cave.slice.mapNotNull(Row::extremes).reduce { (minX1, maxX1), (minX2, maxX2) ->  min(minX1, minX2) to max(maxX1, maxX2) }
    .let { (minX, maxX) ->  cave.slice.joinToString("\n") {
        row -> features(row, minX..maxX).joinToString(" ", transform = ::toString)
    }
  }


fun toString(feature: Feature): String = when (feature) {
  ROCK -> "#"
  EMPTY -> "."
  SAND -> "o"
  SOURCE -> "+"
}

fun features(row: Row, range: IntRange) = range.map { row[it] }

fun replaceFeature(cave: Cave, pos: Pos, feature: Feature): Cave = pos.let { (x, y) ->
  cave.copy(slice = cave.slice.update(y, cave.slice[y].plus(x to feature)))
}

enum class Feature { ROCK, SAND, SOURCE, EMPTY }
data class Row(private val elems: Map<Int, Feature>, val default : Feature = EMPTY) {
  constructor(vararg elems: Pair<Int, Feature>, default: Feature = EMPTY) : this(elems.toMap(), default)

  operator fun get(x: Int) = elems[x] ?: default
  operator fun plus(other: Pair<Int, Feature>) = Row(elems + other)

  fun extremes() = elems.keys.minOrNull()?.let { min -> elems.keys.maxOrNull()?.let { max -> min to max } }
  fun count(predicate: (Feature) -> Boolean) = elems.values.count(predicate)

}
data class Cave(val height: Int, val slice: List<Row>)

fun <T> List<T>.update(index: Int, item: T): List<T> = toMutableList().apply { this[index] = item }