package io.github.vootelerotov.aoc2022.day18

import io.github.vootelerotov.aoc2022.day18.Type.*
import io.github.vootelerotov.util.readResourceLines

fun main() {
  val lines = readResourceLines("aoc2022/day18/input-small.txt")

  val cubes = lines.map { it.split(",").let { (x, y, z) -> Pos(x.toInt(), y.toInt(), z.toInt()) } }

  // 1st
  val cubesByPos = cubes.associateWith { LAVA }
  cubes.sumOf { droplet -> countExposedSides(droplet, cubesByPos) }.let(::println)

  // 2nd
  val bounds = bounds(cubes)
  val isOutOfBounds = outOfBoundsFinder(bounds)

  val mapOfElements = cubes.map { neighbors(it) }.flatten().distinct().fold(cubesByPos) { elemsByPos, pos ->
    calculateElem(elemsByPos, pos, isOutOfBounds)
  }
  cubes.sumOf { droplet -> countExteriorSides(droplet, mapOfElements) }.let(::println)
}

fun calculateElem(elemsByPos: Map<Pos, Type>, pos: Pos, isOutOfBounds: (Pos) -> Boolean): Map<Pos, Type> {
  if (elemsByPos[pos] != null)
    return elemsByPos

  if (isOutOfBounds(pos))
    return elemsByPos + (pos to EXTERIOR)

  val (visited, type) = bfsUntilKnown(neighbors(pos).filterNot { elemsByPos[it] == LAVA }, elemsByPos, isOutOfBounds)
  return elemsByPos + visited.map { it to type } + (pos to type)
}

tailrec fun bfsUntilKnown(
  layer: List<Pos>,
  elemsByPos: Map<Pos, Type>,
  outOfBounds: (Pos) -> Boolean,
  visited: Set<Pos> = emptySet()
): Pair<Set<Pos>, Type> {
  if (layer.isEmpty()) return visited to POCKET

  val updatedVisited = visited + layer

  if (layer.any { elemsByPos[it] == EXTERIOR || outOfBounds(it) }) return updatedVisited to EXTERIOR

  val newLayer = layer.flatMap { neighbors(it) }.filter { it !in updatedVisited && elemsByPos[it] != LAVA }.distinct()

  return bfsUntilKnown(newLayer, elemsByPos, outOfBounds, updatedVisited)
}

fun bounds(cubes: List<Pos>): Bounds =
  Bounds(bounds(cubes.map { it.x }), bounds(cubes.map { it.y }), bounds(cubes.map { it.z }))

fun bounds(coords: List<Int>) = coords.min()..coords.max()

fun outOfBoundsFinder(bounds: Bounds): (Pos) -> Boolean =
  { (x, y, z) -> !bounds.x.contains(x) || !bounds.y.contains(y) || !bounds.z.contains(z) }

fun countExposedSides(pos: Pos, byPos: Map<Pos, Type>): Int = neighbors(pos).filterNot { byPos[it] == LAVA }.count()

fun countExteriorSides(pos: Pos, byPos: Map<Pos, Type>): Int = neighbors(pos).count { byPos[it] == EXTERIOR }

fun neighbors(pos: Pos): List<Pos> = pos.let { (x, y, z) ->
  listOf(
    Pos(x + 1, y, z), Pos(x - 1, y, z), Pos(x, y + 1, z), Pos(x, y - 1, z), Pos(x, y, z + 1), Pos(x, y, z - 1)
  )
}

data class Bounds(val x: IntRange, val y: IntRange, val z: IntRange)
enum class Type { LAVA, POCKET, EXTERIOR }
data class Pos(val x: Int, val y: Int, val z: Int)