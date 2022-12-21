package io.github.vootelerotov.aoc2022.day15

import io.github.vootelerotov.aoc2022.day15.Task.SAMPLE
import io.github.vootelerotov.util.readResourceLines
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

fun main() {
  solve(SAMPLE)
}

private fun solve(task: Task) {
  val lines = readResourceLines(task.inputPath)

  val deployedSensors = lines.map(::parseDeployedSensor)

  val exclusionAreas = deployedSensors.map { (sensor, closestBeacon) ->
    ExclusionArea(sensor, manhattanDistance(sensor, closestBeacon))
  }
  val knownBeacons = deployedSensors.map { (_, beacon) -> beacon }.distinct()

  // 1st
  val row = task.firstPartLine
  val distinctExclusionOnRow = distinctExclusions(exclusionAreas, row)

  val beaconsInTheExclusionRow = knownBeacons
    .mapNotNull { (x, y) -> x.takeIf { y == row } }
    .filter { x -> distinctExclusionOnRow.any { (start, end) -> x in start..end } }

  distinctExclusionOnRow.sumOf { (start, end) -> end - start + 1 }.minus(beaconsInTheExclusionRow.size).let(::println)

  //2nd
  val max = task.secondPartUntil
  generateSequence(0L) { it + 1 }.takeWhile { it < max }.map {
    it to distinctExclusions(exclusionAreas, it).fold(listOf(Range(0, max))) { acc, exclusion ->
      acc.flatMap { range -> exclude(range, exclusion) }
    }
  }.first { (_, xRanges) -> xRanges.isNotEmpty() }
    .let { (y, xRanges) -> y + 4000000L * xRanges.first().start }.let(::println)
}

private fun distinctExclusions(exclusionAreas: List<ExclusionArea>, row: Long): List<Range> {
  val exclusionOnRow = exclusionAreas.mapNotNull { (sensor, distance) ->
    sensor.let { (x, y) ->
      numberOfCellsCoveredOnRow(distance, y, row)?.let { coverage -> Range(x - coverage, x + coverage) }
    }
  }
  val distinctExclusionOnRow = exclusionOnRow.fold(emptyList<Range>()) { acc, range ->
    combineWithExistingRanges(acc, range)
  }
  return distinctExclusionOnRow
}

fun combineWithExistingRanges(acc: List<Range>, range: Range) =
  acc.fold(range to emptyList<Range>()) { context, existingRange ->
    val (newRange, existingRanges) = context
    if (noOverlap(newRange, existingRange))
      newRange to existingRanges + existingRange
    else
      merge(newRange, existingRange) to existingRanges
  }.let { (newRange, acc) -> acc + newRange }


fun merge(one: Range, other: Range) = Range(min(one.start, other.start), max(one.end, other.end))

fun exclude(range: Range, excluded: Range) = when {
  range.start < excluded.start && range.end > excluded.end ->
    listOf(Range(range.start, excluded.start - 1), Range(excluded.end + 1, range.end))

  range.start < excluded.start -> listOf(Range(range.start, excluded.start - 1))
  range.end > excluded.end -> listOf(Range(excluded.end + 1, range.end))
  else -> listOf()
}

fun noOverlap(one: Range, other: Range) = other.end < one.start || one.end < other.start

fun numberOfCellsCoveredOnRow(distance: Long, y: Long, row: Long) = (distance - abs(y - row)).takeIf { it > 0 }

fun parseDeployedSensor(rawDeployedSensor: String): DeployedSensor =
  rawDeployedSensor.split(":").let { (rawSensor, rawBeacon) ->
    DeployedSensor(parseSensor(rawSensor), parseBeacon(rawBeacon))
  }

fun parseSensor(rawSensor: String): Sensor = rawSensor.removePrefix("Sensor at ").let(::parsePos)
fun parseBeacon(rawBeacon: String): Beacon = rawBeacon.removePrefix(" closest beacon is at ").let(::parsePos)
fun parsePos(rawPos: String) =
  rawPos.split(",").let { (rawX, rawY) -> parseCoordinate(rawX, "x=") to parseCoordinate(rawY, "y=") }

private fun parseCoordinate(rawCoordinate: String, prefix: String) = rawCoordinate.trim().removePrefix(prefix).toLong()

fun manhattanDistance(one: Pos, other: Pos) = abs(one.first - other.first) + abs(one.second - other.second)

typealias Pos = Pair<Long, Long>
typealias Sensor = Pos
typealias Beacon = Pos

data class Range(val start: Long, val end: Long)
data class DeployedSensor(val sensor: Sensor, val closestBeacon: Beacon)
data class ExclusionArea(val sensor: Sensor, val distance: Long)

enum class Task(val inputPath: String, val firstPartLine: Long, val secondPartUntil: Long ) {
  SAMPLE("aoc2022/day15/input-small.txt", 10, 20),
  PERSONAL("aoc2022/day15/input.txt", 2000000, 4000000)
}