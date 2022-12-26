package io.github.vootelerotov.aoc2022.day24

import io.github.vootelerotov.util.readResourceLines

fun main() {
  val lines = readResourceLines("aoc2022/day24/input-small.txt")
  val map = Map(
    lines.first().indexOf("."),
    lines.last().indexOf("."),
    lines.size,
    lines.first().length
  )

  val blizzards = parseBlizzards(lines)
  val blizzardMover = blizzardMover(map)
  val moveValidator = moveInMapValidator(map)
  val drawer = drawer(map)

  val (firstRunMinutes, afterFirstGoal) = bfs(
    listOf(Pos(map.entrance, 0)),
    blizzardMover,
    moveValidator,
    blizzards,
    { pos -> pos.y == map.height - 1 && pos.x == map.exit },
    drawer
  )

  // 1st
  println(firstRunMinutes)

  // 2nd
  val (backToStartMinutes, againAtStartBlizzards) = bfs(
    listOf(Pos(map.exit, map.height - 1)),
    blizzardMover,
    moveValidator,
    afterFirstGoal,
    { pos -> pos.y == 0 && pos.x == map.entrance },
    drawer,
    firstRunMinutes
  )

  bfs(
    listOf(Pos(map.entrance, 0)),
    blizzardMover,
    moveValidator,
    againAtStartBlizzards,
    { pos -> pos.y == map.height - 1 && pos.x == map.exit },
    drawer,
    backToStartMinutes
  ).let { (minutes, _) -> println(minutes)}

}

tailrec fun bfs(
  positions: List<Pos>,
  blizzardMover: (Blizzard) -> Blizzard,
  moveValidator: (Pos) -> Boolean,
  blizzards: List<Blizzard>,
  end: (Pos) -> Boolean,
  drawer: (List<Blizzard>) -> Unit,
  minutes: Int = 0
): Pair<Int, List<Blizzard>> {
//  drawer(blizzards)
//  println(" ------------------------------------- ")
  val blocked = blizzards.map { it.pos }.toSet()
  val validPositions = positions.filterNot { it in blocked }


  if (validPositions.any(end)) {
    return minutes to blizzards
  }

  val nextBlizzards = blizzards.map(blizzardMover)
  val nextPositions = validPositions.flatMap { pos -> neighbors(pos).filter(moveValidator) + pos }.distinct()

  return bfs(nextPositions, blizzardMover, moveValidator, nextBlizzards, end, drawer, minutes + 1)
}

fun neighbors(pos: Pos) = listOf(
  Pos(pos.x - 1, pos.y),
  Pos(pos.x + 1, pos.y),
  Pos(pos.x, pos.y - 1),
  Pos(pos.x, pos.y + 1)
)

fun drawer(map: Map): (List<Blizzard>) -> Unit = {
  blizzards ->
    val blocked = blizzards.associateBy { it.pos }
    listOf(
      listOf((0 until map.width).map { if (it == map.entrance) '.' else '#' }.joinToString(" ")),
      (1 until map.height - 1).map { y ->
       "# " + (1 until map.width - 1).joinToString(" ") { x ->
          when (blocked[Pos(x, y)]) {
            is LeftBlizzard -> "<"
            is RightBlizzard -> ">"
            is UpBlizzard -> "^"
            is DownBlizzard -> "v"
            else -> "."
          }
        } + " #"
      },
      listOf((0 until map.width).map { if (it == map.exit) '.' else '#' }.joinToString(" "))
    ).flatten().forEach(::println)
}

fun blizzardMover(map: Map): (Blizzard) -> Blizzard = { blizzard ->
  when (blizzard) {
    is LeftBlizzard -> if (blizzard.x() == 1) LeftBlizzard(Pos(map.width - 2, blizzard.y())) else LeftBlizzard(Pos(blizzard.x() - 1, blizzard.y()))
    is RightBlizzard -> if (blizzard.x() == map.width - 2) RightBlizzard(Pos(1, blizzard.y())) else RightBlizzard(Pos(blizzard.x() + 1, blizzard.y()))
    is UpBlizzard -> if (blizzard.y() == 1) UpBlizzard(Pos(blizzard.x(), map.height - 2)) else UpBlizzard(Pos(blizzard.x(), blizzard.y() - 1))
    is DownBlizzard -> if (blizzard.y() == map.height - 2) DownBlizzard(Pos(blizzard.x(), 1)) else DownBlizzard(Pos(blizzard.x(), blizzard.y() + 1))
  }
}

fun parseBlizzards(lines: List<String>): List<Blizzard> = lines.flatMapIndexed { y: Int, row: String ->
  row.mapIndexedNotNull { x, char -> parseBlizzard(char, Pos(x, y))  }
}

fun parseBlizzard(char: Char, pos: Pos) = when (char) {
  '<' -> LeftBlizzard(pos)
  '>' -> RightBlizzard(pos)
  '^' -> UpBlizzard(pos)
  'v' -> DownBlizzard(pos)
  else -> null
}

fun moveInMapValidator(map : Map): (Pos) -> Boolean = {pos ->
  when (pos) {
    Pos(map.entrance, 0) -> true
    Pos(map.exit, map.height - 1) -> true
    else -> pos.x in 1 until map.width && pos.y in 1 until map.height-1
  }

}

data class Pos(val x: Int, val y: Int)

sealed class Blizzard(val pos: Pos) {
  fun x() = pos.x
  fun y() = pos.y
}
class LeftBlizzard(pos: Pos) : Blizzard(pos)
class RightBlizzard(pos: Pos) : Blizzard(pos)
class UpBlizzard(pos: Pos) : Blizzard(pos)
class DownBlizzard(pos: Pos) : Blizzard(pos)

data class Map(val entrance: Int, val exit: Int, val height: Int, val width: Int)