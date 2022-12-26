package io.github.vootelerotov.aoc2022.day23

import io.github.vootelerotov.aoc2022.day23.Direction.*
import io.github.vootelerotov.util.head
import io.github.vootelerotov.util.readResourceLines
import kotlin.math.abs

fun main() {
  val lines = readResourceLines("aoc2022/day23/input-small.txt")
  val elves = lines.flatMapIndexed { y, line -> line.mapIndexedNotNull { x, tile -> Elf(x, y).takeIf { tile == '#' } } }

  // 1st
  generateRounds(elves.toSet()).map { (elves, _) -> elves }.drop(10).first().let { elves ->
    (abs(elves.maxOf { it.x() } - elves.minOf { it.x() }) + 1) * (abs(elves.maxOf { it.y() } - elves.minOf { it.y() }) + 1) - elves.size
  }.let(::println)

  // 2nd
  val rounds = generateRounds(elves.toSet()).map { (elves, _) -> elves }.zipWithNext()
  println(rounds.indexOfFirst { (prev, next) -> prev == next } + 1)

}

private fun generateRounds(elves: Set<Elf>) =
  generateSequence(
    elves to listOf(
      arrayOf(NE, N, NW),
      arrayOf(SE, S, SW),
      arrayOf(NW, W, SW),
      arrayOf(NE, E, SE)
    )
  ) { (elves, considering) -> round(elves, considering) }

fun round(elves: Set<Elf>, directionsToConsider: List<Array<Direction>>): Pair<Set<Elf>, List<Array<Direction>>> {
  val lookUp = lookUp(elves)
  val (validMoves, noMoves) = elves.map { move(it, lookUp, directionsToConsider) to it }
    .partition { (move, _) -> move != null }
  val moves = validMoves.groupBy { (move, _) -> move }.values
  return (moves.flatMap { list ->
    if (list.size == 1) list.single().let { (move, _) -> listOf(move!!) } else list.map { (_, pos) -> pos }
  } + noMoves.map { (_, elf) -> elf }).toSet() to head(directionsToConsider)!!.let { (head, tail) ->
    tail.plusElement(
      head
    )
  }
}

fun lookUp(elves: Set<Elf>): (Pos) -> Elf? = elves.associateBy(Elf::pos).let { map -> { pos -> map[pos] } }

fun neighbor(elf: Elf, lookUp: (Pos) -> Elf?): (Direction) -> Elf? = { direction ->
  direction(elf.pos, direction).let(lookUp)
}

private fun direction(pos: Pos, direction: Direction) = pos.let { (x, y) ->
  when (direction) {
    N -> Pos(x, y - 1)
    NE -> Pos(x + 1, y - 1)
    E -> Pos(x + 1, y)
    SE -> Pos(x + 1, y + 1)
    S -> Pos(x, y + 1)
    SW -> Pos(x - 1, y + 1)
    W -> Pos(x - 1, y)
    NW -> Pos(x - 1, y - 1)
  }
}

fun move(elf: Elf, lookup: (Pos) -> Elf?, directionsToConsider: List<Array<Direction>>): Elf? {
  val neighbor = neighbor(elf, lookup)
  val directionsWithNeighbors = Direction.values().filter { neighbor(it) != null }

  if (directionsWithNeighbors.isEmpty()) return null

  return directionsToConsider.firstOrNull { lineOfSight -> lineOfSight.none { it in directionsWithNeighbors } }
    ?.let { (_, dir, _) -> direction(elf.pos, dir).let { (x, y) -> Elf(x, y) } }
}

fun toString(elves: List<Elf>): String {
  val elvesByPos = elves.associateBy(Elf::pos)
  return (elves.minOf { it.y() }..elves.maxOf { it.y() }).joinToString("\n") { y ->
    (elves.minOf { it.x() }..elves.maxOf { it.x() }).joinToString(" ") { x ->
      elvesByPos[x to y]?.let { "#" } ?: "."
    }
  }
}

enum class Direction { N, NE, E, SE, S, SW, W, NW }
typealias Pos = Pair<Int, Int>

data class Elf(val pos: Pos) {
  constructor(x: Int, y: Int) : this(x to y)

  fun x() = pos.first
  fun y() = pos.second
}